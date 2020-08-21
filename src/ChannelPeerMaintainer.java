package snowblossom.channels;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import duckutil.ExpiringLRUCache;
import duckutil.PeriodicThread;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.ChainHash;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.ValidationException;

/**
 * For each channel we are subscribed to, maintains links, updated dht data,
 * request more peers
 */
public class ChannelPeerMaintainer extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  //private HashMap<AddressSpecHash, Long> connection_attempt_times = new HashMap<>(16,0.5f);
  //private ImmutableSet<AddressSpecHash> current_links;

  private static final int DESIRED_CHANNEL_PEERS = 7;
  private static final int CONNECT_CHANNEL_PEERS_PER_PASS = 2;

  private ExpiringLRUCache<ChannelID, Boolean> need_peer_req_cache = new ExpiringLRUCache<>(2000, ChannelGlobals.NEED_PEERS_REQUEST_MS);

  private boolean first_pass_done;

  private final long peering_start_time;
  private final boolean use_need_peers;

  public ChannelPeerMaintainer(ChannelNode node)
  {
    super(5000L);
    setName("ChannelPeerMaintainer");
    setDaemon(false);

    peering_start_time = System.currentTimeMillis();

    this.node = node;

    if (node.getConfig().isSet("use_need_peers"))
    {
      use_need_peers = node.getConfig().getBoolean("use_need_peers");
    }
    else
    {
      use_need_peers = true;
    }

  }

  // Things to do for each channel:
  // * get peers from DHT.  Do this sometimes even if we have plenty of peers.
  // * save self to DHT.
  // * connect to peers
  @Override
  public void runPass() throws Exception
  {
    if (!first_pass_done)
    {
      Thread.sleep(7500);
      first_pass_done=true;
    }
    ChannelPeerInfo my_info = node.getNetworkExaminer().createPeerInfo();

    for(ChannelID cid : node.getChannelSubscriber().getChannelSet())
    {
      String chan_str = cid.asString();

      ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
      if (ctx == null) continue; // must not be really open (maybe just not yet)

      ctx.prune();
      List<ChannelLink> links = ctx.getLinks();

      int good_link_count = ChannelLink.countActuallyOpen(links);

      if (use_need_peers)
      if (good_link_count == 0) // There are no good links
      if (ctx.block_ingestor.getHead() != null) // We have at least one block
      if (System.currentTimeMillis() > peering_start_time + ChannelGlobals.NEED_PEERS_REQUEST_MS) // It has been a little while
      {
        // Then ask for more peers
        if (need_peer_req_cache.get(cid) == null)
        {
          requestNeedPeers(cid, ctx);
          need_peer_req_cache.put(cid, true);
        }
      }

      ChannelSettings settings = null;
      if (ctx.block_ingestor.getHead() != null)
      {
        settings = ctx.block_ingestor.getHead().getEffectiveSettings();
      }
      List<ByteString> dht_element_lst = node.getDHTStratUtil().getDHTLocations(cid, settings);

      // TODO - only bother doing the save once the DHT peering is up and running reasonably
      saveDHT(cid, dht_element_lst, my_info);

      if (links.size() < DESIRED_CHANNEL_PEERS)
      {
        Set<AddressSpecHash> connected_set = getConnectedNodeSet(links);
        connected_set.add(node.getNodeID());


        // TODO - save good peers in DB in case the DHT is trashed in some way
        LinkedList<ChannelPeerInfo> possible_peers = getAllDHTPeers(cid, dht_element_lst, connected_set);
        logger.fine(String.format("Channel %s: connected %d possible %d", cid.asString(), links.size(), possible_peers.size()));
        Collections.shuffle(possible_peers);

        // TODO - be better about not hammering down/bad nodes with connects
        for(int i=0; i<CONNECT_CHANNEL_PEERS_PER_PASS; i++)
        {
          ChannelPeerInfo ci = possible_peers.poll();
          if (ci != null)
          {
            try
            {
              PeerLink pl = node.getPeerManager().connectNode(ci, chan_str);
              ChannelLink cl = new ChannelLink(node, pl, cid, ctx);
              ctx.addLink(cl);
              node.getChannelTipSender().sendTip(cid, cl);
            }
            catch(Throwable t)
            {
              logger.fine(String.format("Error trying to link to peer: %s", ci.toString()));

            }
          }
        }
      }

    }

  }

  private Set<AddressSpecHash> getConnectedNodeSet(List<ChannelLink> links)
  {
    HashSet<AddressSpecHash> set = new HashSet<>();
    for(ChannelLink link : links)
    {
      AddressSpecHash h = link.getRemoteNodeID();
      if (h != null)
      {
        set.add(h);
      }
    }
    return set;

  }

  private void saveDHT(ChannelID cid, List<ByteString> dht_element_lst, ChannelPeerInfo my_info)
		throws ValidationException
  {
    for(ByteString element_id : dht_element_lst)
    {
      if (!node.getDHTCache().haveWritten(element_id))
      {
      	SignedMessage sm = node.signMessageNode(SignedMessagePayload.newBuilder()
        	.setDhtData( DHTData.newBuilder().setElementId(element_id).setPeerInfo(my_info).build() )
        	.build());

      	node.getDHTServer().storeDHTDataAsyncTrusted(
        	StoreDHTRequest.newBuilder()
          	.setDesiredResultCount(0)
          	.setSignedDhtData(sm)
        	.build()); 
        logger.fine(String.format("DHT Saved %s for %s", new ChainHash(element_id), cid.asString()));
      
        node.getDHTCache().markWrite(element_id);
      }
    }

  }

  public LinkedList<ChannelPeerInfo> getAllDHTPeers(ChannelID cid)
  {
    return getAllDHTPeers(cid, new HashSet<AddressSpecHash>());
  }
  public LinkedList<ChannelPeerInfo> getAllDHTPeers(ChannelID cid, Set<AddressSpecHash> connected_set)
  {
    
    // TODO - actually get head settings
    ChannelSettings settings = null;
    List<ByteString> dht_element_lst = node.getDHTStratUtil().getDHTLocations(cid, settings);

    return getAllDHTPeers(cid, dht_element_lst, connected_set); 
  }

  public LinkedList<ChannelPeerInfo> getAllDHTPeers(ChannelID cid, List<ByteString> dht_element_lst, Set<AddressSpecHash> connected_set)
  {
    HashMap<ByteString, SignedMessagePayload> peer_map = new HashMap<>();

    for(ByteString element_id : dht_element_lst)
    {
      DHTDataSet set = node.getDHTCache().getData(element_id);
      if (set != null)
      {
        for(SignedMessage sm : set.getDhtDataList())
        {
          SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);
          long tm = payload.getTimestamp();

          ByteString node_id = payload.getDhtData().getPeerInfo().getAddressSpecHash();

          if (!connected_set.contains(new AddressSpecHash(node_id)))
          if ((!peer_map.containsKey(node_id)) || (peer_map.get(node_id).getTimestamp() < tm))
          {
            peer_map.put(node_id, payload);
          }
        }
      }
    }

    LinkedList<ChannelPeerInfo> lst = new LinkedList<>();
    for(SignedMessagePayload p : peer_map.values())
    {
      lst.add(p.getDhtData().getPeerInfo());
    }

    return lst;
  }

  private void requestNeedPeers(ChannelID cid, ChannelContext ctx)
    throws ValidationException
  {
    logger.info("Requesitng more peers for: " + cid);

    ChannelID cid_need_peers = ChannelID.fromStringWithNames(ChannelGlobals.CHAN_NEED_PEERS, node);
    ChannelContext ctx_need_peers = node.getChannelSubscriber().openChannel(cid_need_peers);

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    ci.addBroadcastChannelIds(cid_need_peers.getBytes());
    ci.setParentRef( 
      ContentReference.newBuilder()
        .setChannelId(cid.getBytes())
        .setRefMode( ContentReference.ReferenceMode.DIRECT).build());


    ci.setContentHash( DigestUtil.hash(ci.getContent()) );

    SignedMessagePayload.Builder payload = SignedMessagePayload.newBuilder();
    payload.setContentInfo(ci.build());

    SignedMessage sm = node.signMessageNode( payload.build() );
    ctx_need_peers.block_ingestor.ingestContent(sm);

  }

}



