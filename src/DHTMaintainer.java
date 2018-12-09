package snowblossom.channels;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;

import duckutil.PeriodicThread;

import com.google.protobuf.ByteString;
import snowblossom.lib.trie.ByteStringComparator;
import snowblossom.lib.ValidationException;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;



public class DHTMaintainer extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  private HashMap<AddressSpecHash, Long> connection_attempt_times = new HashMap<>(16,0.5f);
  private ImmutableSet<AddressSpecHash> current_links;

  public DHTMaintainer(ChannelNode node)
  {
    super(25000L);
    setName("DHTMaintainer");
    setDaemon(false);

    this.node = node;

  }

  @Override
  public void runPass() throws Exception
  {
    List<PeerLink> connected_peer_list = node.getPeerManager().getPeersWithReason("DHT");

    Set<AddressSpecHash> connected_peers = new TreeSet<>();
    for(PeerLink pl : connected_peer_list)
    {
      AddressSpecHash id = pl.getNodeID();
      connected_peers.add(id);
    }
    current_links = ImmutableSet.copyOf(connected_peers);

    Map<AddressSpecHash, LocalPeerInfo> target_map = pickTargetsFromDB();

    TreeSet<AddressSpecHash> extra_peers = new TreeSet<>();

    Map<AddressSpecHash, ChannelPeerInfo> connect_map = new TreeMap<>();

    for(PeerLink pl : connected_peer_list)
    {
      AddressSpecHash id = pl.getNodeID();
      if (!target_map.containsKey(id))
      {
        extra_peers.add(id);
      }
    }
    for(Map.Entry<AddressSpecHash, LocalPeerInfo> me : target_map.entrySet())
    {
      AddressSpecHash id = me.getKey();
      if (!connected_peers.contains(id))
      {
        connect_map.put(id, me.getValue().getInfo());
      }
    }
    if (connected_peers.size() + connect_map.size() < ChannelGlobals.NEAR_POINTS)
    {
      // Add Seeds
      for(ChannelPeerInfo info : getSeeds())
      {
        AddressSpecHash id = new AddressSpecHash(info.getAddressSpecHash());
        if (!connected_peers.contains(id))
        {
          connect_map.put(id, info);
        }
      }
    }
    logger.info("Connected peers: " + connected_peers);
    logger.info("New connections: " + connect_map.keySet());
    int desired_peers = ChannelGlobals.NEAR_POINTS + ChannelGlobals.LONG_RANGE_POINTS + ChannelGlobals.SHORT_RANGE_POINTS;
    int to_close_count = connected_peers.size() - desired_peers;
    while ((to_close_count > 0) && (extra_peers.size() > 0))
    {
      AddressSpecHash id = extra_peers.pollFirst();
      to_close_count--;
      logger.info("Closed extra peer: " + id);
      node.getPeerManager().removeReason(id, "DHT");
    }

    for(ChannelPeerInfo info : connect_map.values())
    {
      node.getPeerManager().connectNode(info, "DHT");
    }
    synchronized(connection_attempt_times)
    {
      for(AddressSpecHash id : connect_map.keySet())
      {
        connection_attempt_times.put(id, System.currentTimeMillis());
      }
    }

    SignedMessage sm = node.getDHTServer().getSignedPeerInfoSelf();
    for(PeerLink pl : node.getPeerManager().getPeersWithReason("DHT"))
    {
      pl.getDHTPeers(sm);
    }
  
  }

  private Map<AddressSpecHash, LocalPeerInfo> pickTargetsFromDB()
  {
    Map<AddressSpecHash, LocalPeerInfo> target_map = new TreeMap<>();

    // Close neighbors
    target_map.putAll( getClosestValid( node.getNodeID().getBytes(), ChannelGlobals.NEAR_POINTS));

    double long_wedge = 1.0 / ChannelGlobals.LONG_RANGE_POINTS;

    // Point 0 is me, so don't bother with that
    for(int i=1; i<ChannelGlobals.LONG_RANGE_POINTS; i++)
    {
      ByteString target = HashMath.shiftHashOnRing( node.getNodeID().getBytes(), long_wedge * i);
      target_map.putAll( getClosestValid( target, 1));
    }
    
    double short_wedge = long_wedge / ChannelGlobals.SHORT_RANGE_POINTS;
    int short_points = ChannelGlobals.SHORT_RANGE_POINTS;
    // So we take the closest long wedge, and put points along that.
    // maybe we hit ourself, maybe not.  Whatever.
    for(int i=0; i<=short_points; i++)
    {
      ByteString target = HashMath.shiftHashOnRing( node.getNodeID().getBytes(), -long_wedge/2.0 + short_wedge * i);
      target_map.putAll( getClosestValid( target, 1));
    }

    return target_map; 
  }

  private Map<AddressSpecHash, LocalPeerInfo> getClosestValid(ByteString target, int count)
  {
    TreeMap<ByteString, LocalPeerInfo> ordered_valid_map_down=new TreeMap<>(new ByteStringComparator());
    TreeMap<ByteString, LocalPeerInfo> ordered_valid_map_up=new TreeMap<>(new ByteStringComparator());
    TreeMap<ByteString, LocalPeerInfo> ordered_valid_map=new TreeMap<>(new ByteStringComparator());

    for(LocalPeerInfo info : node.getDB().getPeerMap().getClosest(target, count*25))
    {
      if (isValid(info))
      {
        // It is actually possible for multiple things to have the same distance from the target
        // but that is pretty unlikely and even if it did happen, it will be ok (just not hitting a node)
        // so whatever
        ByteString dist = HashMath.getAbsDiff(target, info.getInfo().getAddressSpecHash());
        BigInteger diff = HashMath.getDiff(target, info.getInfo().getAddressSpecHash());

        ordered_valid_map.put(dist, info);

        if (diff.compareTo(BigInteger.ZERO) < 0)
        {
          ordered_valid_map_down.put(dist, info);
        }
        else
        {
          ordered_valid_map_up.put(dist, info);
        }
      }
    }

    Map<AddressSpecHash, LocalPeerInfo> result_map = new HashMap<>(16,0.5f);

    if (count == 1)
    {
      // If we just want one, get it from absolute closest
 
      while((ordered_valid_map.size()>0) && (result_map.size() < count))
      {
        LocalPeerInfo info = ordered_valid_map.pollFirstEntry().getValue();
        result_map.put(new AddressSpecHash(info.getInfo().getAddressSpecHash()), info);
      }

    }
    else
    {
      // If we want more than one, get them evenly up and down

      while((ordered_valid_map_down.size()>0) && (result_map.size() < count/2))
      {
        LocalPeerInfo info = ordered_valid_map_down.pollFirstEntry().getValue();
        result_map.put(new AddressSpecHash(info.getInfo().getAddressSpecHash()), info);
      }
      while((ordered_valid_map_up.size()>0) && (result_map.size() < count))
      {
        LocalPeerInfo info = ordered_valid_map_up.pollFirstEntry().getValue();
        result_map.put(new AddressSpecHash(info.getInfo().getAddressSpecHash()), info);
      }
    }
    //System.out.println(String.format("Asked for %d got %d", count, result_map.size())); 

    return result_map;
  }

  private boolean isValid(LocalPeerInfo info)
  {

    if (info.getSignedTimestamp() + 3600000L < System.currentTimeMillis()) return false;
    AddressSpecHash id = new AddressSpecHash(info.getInfo().getAddressSpecHash());
    if (id.equals(node.getNodeID())) return false;

    synchronized(connection_attempt_times)
    {
      if (!current_links.contains(id))
      if (connection_attempt_times.containsKey(id))
      if (connection_attempt_times.get(id) + 600000L > System.currentTimeMillis()) return false;
    }

    return true;
  }

  public static List<ChannelPeerInfo> getSeeds()
    throws ValidationException
  {
    LinkedList<ChannelPeerInfo> seed_list = new LinkedList<>();

    seed_list.add( ChannelPeerInfo.newBuilder()
      .setAddressSpecHash(AddressUtil.getHashForAddress(ChannelGlobals.NODE_ADDRESS_STRING, 
        "node:hwuffxyrrrmdyx70zmmc3fsl4jw6ecz0ej38tnmn").getBytes())
      .setVersion("seed")
      .putConnectInfos("ipv6", ConnectInfo.newBuilder()
        .setProtocol("ipv6")
        .setHost("snowblossom.1209k.com")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .build());

    return seed_list;

  }
}