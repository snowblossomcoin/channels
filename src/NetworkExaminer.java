package snowblossom.channels;

import duckutil.NetUtil;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import snowblossom.channels.proto.*;

public class NetworkExaminer
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private String ipv4_host;
  private String ipv6_host;
  private String onion_host;
  private ChannelNode node;

  private InetSocketAddress tor_http_relay;

  private final boolean tor_only;

  public NetworkExaminer(ChannelNode node)
  {
    this.node = node;

    tor_only = node.getConfig().getBoolean("tor_only");

    updateHosts();
  }

  // TODO - on failure of this, might want to use getAllAddresses().
  // Any non-link-local ipv6 address is a good bet for IPV6.
  // Any non-private ipv4 address is also probably good.
  private void updateHosts()
  {

    if(!isTorOnly())
    {
      try{
        ipv4_host = NetUtil.getUrlLine("http://ipv4-lookup.snowblossom.org/myip");
      }
      catch(Throwable t){ipv4_host=null;}

      try{
        ipv6_host = NetUtil.getUrlLine("http://ipv6-lookup.snowblossom.org/myip");
      }
      catch(Throwable t){ipv6_host=null;}

      logger.info("IPV4: " + ipv4_host);
      logger.info("IPV6: " + ipv6_host);
    }

    if (node.getConfig().isSet("tor_advertise"))
    {
      if (node.getConfig().get("tor_advertise").endsWith(".onion"))
      {
        onion_host = node.getConfig().get("tor_advertise");
        logger.info("Onion: " + onion_host);
      }
      else
      {
        logger.warning("tor_advertise does not seem to end with .onion - not using");
      }
    }

    if (node.getConfig().isSet("tor_http_relay"))
    {
      String[] split = node.getConfig().get("tor_http_relay").split(":");
      int port = Integer.parseInt(split[1]);
      String host = split[0];

      tor_http_relay = new InetSocketAddress(host, port);
      logger.info("Tor HTTP relay: " + tor_http_relay);
    }

    if (!isTorOnly())
    {
      try
      {
        tryUpnp();
      }
      catch(Exception e)
      {
        logger.info("UPNP failure: " + e);
      }
    }
  }

  protected void tryUpnp() throws Exception
  {
    logger.info("Attemping UPNP");
    int port = node.getPort();

    GatewayDiscover discover = new GatewayDiscover();
    discover.discover();
    GatewayDevice d = discover.getValidGateway();
    if (d != null)
    {
      logger.info(String.format("Found UPNP gateway %s %s", d.getModelName(), d.getModelDescription()));
      InetAddress localAddress = d.getLocalAddress();
      PortMappingEntry portMapping = new PortMappingEntry();
      if (d.getSpecificPortMappingEntry(port,"TCP",portMapping))
      {
        String mapped = portMapping.getInternalClient();
        String local = localAddress.getHostAddress();
        if (local.equals(mapped) && (port == portMapping.getInternalPort()))
        {
          logger.info("Port already mapped to me.  Cool.");
        }
        else
        {
          logger.warning(String.format("Port %d already mapped to %s:%d. Consider using a different port.", port, mapped, portMapping.getInternalPort()));
          logger.warning(String.format("While I am on %s:%d", local, port));
        }
      }
      else
      {
        if(d.addPortMapping(port, port, localAddress.getHostAddress(),"TCP","snowchannel"))
        {
          logger.info("Port mapped with upnp gateway");
        }
      }
    }
    logger.info("Done with UPNP");

  }

  /**
   * Get all local addresses on all interfaces, ipv4 and ipv6 minus any loopback addresses.
   * Might or might not be globally routable, will include internal IPs (behind NAT)
   * and IPv6 link-local addresses.
   */
  public Set<InetAddress> getAllAddresses()
    throws java.io.IOException
  {
    HashSet<InetAddress> addrs = new HashSet<>();

    Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
    while(faces.hasMoreElements())
    {
      NetworkInterface ni = faces.nextElement();
      for(InterfaceAddress ia : ni.getInterfaceAddresses())
      {
        InetAddress a = ia.getAddress();
        if (!a.  isLoopbackAddress())
        {
          InetAddress a2 = InetAddress.getByAddress(a.getAddress());
          addrs.add(a2);
        }
      }
    }
    return addrs;

  }

  public boolean hasIpv4() { return ipv4_host != null; }
  public boolean hasIpv6() { return ipv6_host != null; }

  public boolean canConnectToIpv4() {return !tor_only;}
  public boolean canConnectToIpv6() {return hasIpv6();}
  public boolean canConnectToTor() { return tor_http_relay!=null;}

  public InetSocketAddress getTorHttpRelay(){return tor_http_relay;}

  public boolean isTorOnly()
  {
    return tor_only;
  }


  public ChannelPeerInfo createPeerInfo()
  {
    ChannelPeerInfo.Builder info = ChannelPeerInfo.newBuilder();

    info.setVersion(ChannelGlobals.VERSION);
    info.setAddressSpecHash(node.getNodeID().getBytes());

    if (ipv6_host != null)
    {
      info.putConnectInfos( "ipv6", ConnectInfo.newBuilder()
        .setProtocol("ipv6")
        .setHost(ipv6_host)
        .setPort( node.getPort() )
        .build() );
    }

    if (ipv4_host != null)
    {
      info.putConnectInfos( "ipv4", ConnectInfo.newBuilder()
        .setProtocol("ipv4")
        .setHost(ipv4_host)
        .setPort( node.getPort() )
        .build() );
    }

    if (onion_host != null)
    {
      info.putConnectInfos( "onion", ConnectInfo.newBuilder()
        .setProtocol("onion")
        .setHost(onion_host)
        .setPort( node.getPort() )
        .build() );
    }

    return info.build();
  }



}
