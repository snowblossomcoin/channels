package snowblossom.channels;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import duckutil.PeriodicThread;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.ValidationException;

public class ChannelPeerMaintainer extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  //private HashMap<AddressSpecHash, Long> connection_attempt_times = new HashMap<>(16,0.5f);
  //private ImmutableSet<AddressSpecHash> current_links;

  private static final int DESIRED_CHANNEL_PEERS = 5;

  public ChannelPeerMaintainer(ChannelNode node)
  {
    super(5000L);
    setName("ChannelPeerMaintainer");
    setDaemon(false);

    this.node = node;

  }

  // Things to do for each channel:
  // * get peers from DHT.  Do this sometimes even if we have plenty of peers.
  // * save self to DHT.
  // * connect to peers
  @Override
  public void runPass() throws Exception
  {
    ChannelPeerInfo my_info = node.getNetworkExaminer().createPeerInfo();

    for(ChannelID cid : node.getChannelSubscriber().getChannelSet())
    {
      String chan_str = cid.asString();

      // Note, this is only the outbound connections
      List<PeerLink> connected_peers = node.getPeerManager().getPeersWithReason(chan_str);

      // TODO - actually get head settings
      ChannelSettings settings = null;

      List<ByteString> dht_element_lst = node.getDHTStratUtil().getDHTLocations(cid, settings);

      saveDHT(cid, dht_element_lst, my_info);

      List<ChannelPeerInfo> possible_peers = getAllDHTPeers(cid, dht_element_lst);
      

    }

  }

  private void saveDHT(ChannelID cid, List<ByteString> dht_element_lst, ChannelPeerInfo my_info)
		throws ValidationException
  {
    for(ByteString element_id : dht_element_lst)
    {
      if (!node.getDHTCache().haveWritten(element_id))
      {
      	SignedMessage sm = node.signMessage(SignedMessagePayload.newBuilder()
        	.setDhtData( DHTData.newBuilder().setElementId(element_id).setPeerInfo(my_info).build() )
        	.build());

      	node.getDHTServer().storeDHTDataAsyncTrusted(
        	StoreDHTRequest.newBuilder()
          	.setDesiredResultCount(0)
          	.setSignedDhtData(sm)
        	.build()); 
      
        node.getDHTCache().markWrite(element_id);
      }
    }

  }

  private List<ChannelPeerInfo> getAllDHTPeers(ChannelID cid, List<ByteString> dht_element_lst)
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
}



