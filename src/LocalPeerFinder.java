package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.ExpiringLRUCache;
import duckutil.PeriodicThread;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ValidationException;

public class LocalPeerFinder
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;

  private ExpiringLRUCache<AddressSpecHash, LocalPeerDisco> disco_cache = 
    new ExpiringLRUCache<>(10000, ChannelGlobals.MULTICAST_CACHE_EXPIRE);

  public LocalPeerFinder(ChannelNode node)
  {
    this.node = node;
  }
  public void start()
  {
    try
    {
      new PacketWatcher(InetAddress.getByName(ChannelGlobals.MULTICAST_IPV4_ADDRESS)).start();
      new PacketWatcher(InetAddress.getByName(ChannelGlobals.MULTICAST_IPV6_ADDRESS)).start();

      new PacketSender(InetAddress.getByName(ChannelGlobals.MULTICAST_IPV4_ADDRESS)).start();
      new PacketSender(InetAddress.getByName(ChannelGlobals.MULTICAST_IPV6_ADDRESS)).start();
    }
    catch(Throwable t)
    {
      logger.log(Level.WARNING, "Error in LocalPeerFinder", t);
    }
  }

  public class PacketSender extends PeriodicThread
  {
    private MulticastSocket ds;
    private InetAddress group;
    private int port;

    public PacketSender(InetAddress group)
      throws java.io.IOException
    {
      super(ChannelGlobals.MULTICAST_BROADCAST_PERIOD);
      
      setDaemon(true);
      setName("LocalPeerFinder/PacketSender/" + group);


      this.group = group;
      this.port = ChannelGlobals.NETWORK_PORT;
      ds = new MulticastSocket(port);
      ds.setLoopbackMode(false);
      ds.joinGroup(group);
      
      logger.fine("Sender on: " + group + " from " + ds.getInterface());
      logger.fine("Addresses: " + node.getNetworkExaminer().getAllAddresses());

    }

    public void runPass()
      throws Exception
    {
      String msg = "hello";
     	SignedMessagePayload.Builder p = SignedMessagePayload.newBuilder();
     	p.setPeerInfo( node.getNetworkExaminer().createPeerInfo() );
	    SignedMessage signed_peer_info = node.signMessage(p.build());

      LocalPeerDisco.Builder lpd = LocalPeerDisco.newBuilder();
      lpd.setSignedPeerInfo(signed_peer_info);
      lpd.setPort(node.getPort());

      for(InetAddress ia : node.getNetworkExaminer().getAllAddresses())
      {
        lpd.addIpAddresses(ia.getHostAddress());
      }

      SignedMessage signed_local = node.signMessage(
        SignedMessagePayload.newBuilder().setLocalPeerDisco(lpd.build()).build()
        );

      ByteString bs = signed_local.toByteString();
      if (bs.size() > 1400)
      {
        logger.warning("Multicast size getting large: " + bs.size());
      }

      DatagramPacket dp = new DatagramPacket( bs.toByteArray(), bs.size(), group, port);
      ds.send(dp);

    }

  }

  public class PacketWatcher extends Thread
  {
    private MulticastSocket ds;
    private InetAddress group;
    private int port;

    public PacketWatcher(InetAddress group)
      throws java.io.IOException
    {
      setDaemon(true);
      setName("LocalPeerFinder/PacketWatcher/" + group);

      this.group = group;
      this.port = ChannelGlobals.NETWORK_PORT;
      ds = new MulticastSocket(port);
      ds.joinGroup(group);
    }

    public void run()
    {
      boolean last_error=false;
      while(ds.isBound())
      {
        try
        {
          if (last_error) Thread.sleep(5000);
          DatagramPacket p = new DatagramPacket(new byte[8192], 8192);
          ds.receive(p);
          ByteString data = ByteString.copyFrom(p.getData(), 0, p.getLength());
          logger.fine("Packet from: " + p.getSocketAddress() + " " + data.size());

          SignedMessage outer_sm = SignedMessage.parseFrom(data);
          SignedMessagePayload outer_payload = ChannelSigUtil.validateSignedMessage(outer_sm);
          if (!outer_payload.hasLocalPeerDisco())
          {
            throw new ValidationException("Multicast does not contian local_peer_disco");
          }

          LocalPeerDisco disco = outer_payload.getLocalPeerDisco();

          ChannelPeerInfo peer_info = ChannelSigUtil.validatePeerInfo(disco.getSignedPeerInfo());
          AddressSpecHash node_id = new AddressSpecHash(peer_info.getAddressSpecHash());

          SignedMessagePayload peer_info_payload = ChannelSigUtil.quickPayload(disco.getSignedPeerInfo());

          if (!outer_payload.getClaim().equals(peer_info_payload.getClaim()))
          {
            throw new ValidationException("Outer and inner signed by different addresses");
          }

          InetAddress from = ((InetSocketAddress) p.getSocketAddress()).getAddress();
          HashSet<String> allowed_ips = new HashSet<>();
          for(String ip : disco.getIpAddressesList())
          {
            allowed_ips.add(ip);
          }
          if (!allowed_ips.contains(from.getHostAddress()))
          {
            throw new ValidationException(String.format("Packet from %s - not in allowed: %s", from.getHostAddress(), allowed_ips));
          }

          String node_addr = AddressUtil.getAddressString(ChannelGlobals.NODE_TAG, node_id);
          logger.fine("Valid multicast for: " + node_addr + " from " + from.getHostAddress() + " " + disco.getPort());

          LocalPeerDisco.Builder disco_clone = LocalPeerDisco.newBuilder();


          // Trim the address list to just the one we received from
          // So that PeerLink will know which to connect to
          disco_clone.mergeFrom(disco);
          disco_clone.clearIpAddresses();
          disco_clone.addIpAddresses( from.getHostAddress() );

          synchronized(disco_cache)
          {
            disco_cache.put(node_id, disco_clone.build());
          }

          node.getDHTServer().importPeer(disco.getSignedPeerInfo());

          last_error=false;
        }
        catch(Throwable t)
        {
          last_error=true;
          logger.log(Level.WARNING, "Error in PacketWatcher", t);

        }
      }

    }

  }

  public LocalPeerDisco getDiscoCache(AddressSpecHash node_id)
  {
    synchronized(disco_cache)
    {
      return disco_cache.get(node_id);
    }
  }

}
