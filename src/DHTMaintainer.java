package snowblossom.channels;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import duckutil.PeriodicThread;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.ValidationException;
import snowblossom.lib.trie.ByteStringComparator;

/**
 * Maintain links for the DHT peering topology.
 * insert_disaster_spider_diagram
 */
public class DHTMaintainer extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  private HashMap<AddressSpecHash, Long> connection_attempt_times = new HashMap<>(16,0.5f);
  private ImmutableSet<AddressSpecHash> current_links;

  // TODO - select older nodes if we get don't have any links
  // Something like max_age = 1h.
  // If not enough links, max_age = Math.max(max_age * 2, 1 year)
  // else max_age = 1h (to reset to one hour once we have some reasonable number)
  // https://github.com/snowblossomcoin/channels/issues/21
  public DHTMaintainer(ChannelNode node)
  {
    super(5000L);
    setName("DHTMaintainer");
    setDaemon(false);

    this.node = node;

  }

  @Override
  public void start()
  {
    super.start();
    new PruneThread().start();
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
      if (node.getConfig().getBoolean("testing_skip_seeds"))
      {
        logger.warning("Not using seeds - config option 'testing_skip_seeds'");
      }
      else
      {
        for(ChannelPeerInfo info : getSeeds())
        {
          AddressSpecHash id = new AddressSpecHash(info.getAddressSpecHash());
          if (!connected_peers.contains(id))
          {
            connect_map.put(id, info);
          }
        }
      }
    }
    logger.fine("Connected peers: " + connected_peers);
    logger.fine("New connections: " + connect_map.keySet());
    int desired_peers = ChannelGlobals.NEAR_POINTS + ChannelGlobals.RING_DIVISONS * 2;
    int to_close_count = connected_peers.size() - desired_peers;
    while ((to_close_count > 0) && (extra_peers.size() > 0))
    {
      AddressSpecHash id = extra_peers.pollFirst();
      to_close_count--;
      logger.fine("Closed extra peer: " + id);
      node.getPeerManager().removeReason(id, "DHT");
    }

    for(ChannelPeerInfo info : connect_map.values())
    {
      try
      {
      node.getPeerManager().connectNode(info, "DHT");
      }
      catch(Exception e)
      {
        logger.fine("Connect failed: " + e);
      }
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


    // Start on completely opposite side of ring
    // and reduce by half for a bunch of divisions
    double ring_dist= 0.5;
    for(int i=0; i<ChannelGlobals.RING_DIVISONS; i++)
    {
      
      ByteString t_high = HashMath.shiftHashOnRing( node.getNodeID().getBytes(), ring_dist);
      ByteString t_low =  HashMath.shiftHashOnRing( node.getNodeID().getBytes(), -ring_dist);

      target_map.putAll( getClosestValid( t_high, 1));
      target_map.putAll( getClosestValid( t_low, 1));

      ring_dist = ring_dist / 2.0;
    }

    return target_map; 
  }

  private Map<AddressSpecHash, LocalPeerInfo> getClosestValid(ByteString target, int count)
  {
    TreeMap<ByteString, LocalPeerInfo> ordered_valid_map_down=new TreeMap<>(new ByteStringComparator());
    TreeMap<ByteString, LocalPeerInfo> ordered_valid_map_up=new TreeMap<>(new ByteStringComparator());
    TreeMap<ByteString, LocalPeerInfo> ordered_valid_map=new TreeMap<>(new ByteStringComparator());

    for(LocalPeerInfo info : node.getDB().getPeerMap().getClosest(target, count*64))
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

    // only heard from in last hour
    if (info.getSignedTimestamp() + 3600000L < System.currentTimeMillis()) return false;
    AddressSpecHash id = new AddressSpecHash(info.getInfo().getAddressSpecHash());

    // is not this node
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
        "node:f6w25aj23fw0yz0ww06rqx53vgde4nz9u60uf07x").getBytes())
      .setVersion("seed")
      .putConnectInfos("ipv4", ConnectInfo.newBuilder()
        .setProtocol("ipv4")
        .setHost("h.1209k.com")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .putConnectInfos("ipv6", ConnectInfo.newBuilder()
        .setProtocol("ipv6")
        .setHost("snowblossom.1209k.com")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .build());

    seed_list.add( ChannelPeerInfo.newBuilder()
      .setAddressSpecHash(AddressUtil.getHashForAddress(ChannelGlobals.NODE_ADDRESS_STRING, 
        "node:2tcsr57am4wzlmcca56alrsplxzhpk6fawkycch3").getBytes())
      .setVersion("seed")
      .putConnectInfos("ipv4", ConnectInfo.newBuilder()
        .setProtocol("ipv4")
        .setHost("snow-chan-tx1.snowblossom.org")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .putConnectInfos("ipv6", ConnectInfo.newBuilder()
        .setProtocol("ipv6")
        .setHost("snow-chan-tx1.snowblossom.org")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .build());

    seed_list.add( ChannelPeerInfo.newBuilder()
      .setAddressSpecHash(AddressUtil.getHashForAddress(ChannelGlobals.NODE_ADDRESS_STRING, 
        "node:ut58s0rxxtyqe8u3c695xt59drxxf7vspz79p60a").getBytes())
      .setVersion("seed")
      .putConnectInfos("ipv4", ConnectInfo.newBuilder()
        .setProtocol("ipv4")
        .setHost("snowplough.kekku.li")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .putConnectInfos("ipv6", ConnectInfo.newBuilder()
        .setProtocol("ipv6")
        .setHost("snowplough.kekku.li")
        .setPort(ChannelGlobals.NETWORK_PORT)
        .build())
      .build());

    return seed_list;

  }

  /** Removes old DB entries for old nodes */
  public class PruneThread extends PeriodicThread
  {

    private Random rnd;

    public PruneThread()
    {

      super(86400L * 1000L / 256L);
      setName("DHTMaintainer/PruneThread");
      setDaemon(true);

      rnd = new Random();

    }

    public void runPass()
    {
      if (node.getPeerManager().getPeersWithReason("DHT").size() > 3)
      {
        byte[] b = new byte[1];

        rnd.nextBytes(b);

        HashSet<ByteString> to_purge = new HashSet<>();

        for(Map.Entry<ByteString, LocalPeerInfo> me : node.getDB().getPeerMap().getByPrefix(ByteString.copyFrom(b), 10000).entrySet())
        {
          ByteString key = me.getKey();
          LocalPeerInfo pi = me.getValue();
          if (pi.getSignedTimestamp() + ChannelGlobals.MAX_DATA_PEER_AGE < System.currentTimeMillis())
          {
            to_purge.add(key);
          }
        }

        for(ByteString key : to_purge)
        {
          logger.info("Purging peer: " + HexUtil.getHexString(key));
          node.getDB().getPeerMap().remove(key);
        }
  
      }

    }

  }
}
