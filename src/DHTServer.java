package snowblossom.channels;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.ValidationException;

/**
 * GRPC DHT server.  Some methods can also be called directly.  Whatever.
 */
public class DHTServer extends StargateServiceGrpc.StargateServiceImplBase
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;

  public DHTServer(ChannelNode node)
  {
    this.node = node;
  }

  @Override
  public void getDHTPeers(GetDHTPeersRequest req, StreamObserver<PeerList> o)
  {
    PeerList.Builder peer_list = PeerList.newBuilder();
    importPeer(req.getSelfPeerInfo() );

    try
    {
      // Add Self
      peer_list.addPeers(getSignedPeerInfoSelf());

      // Add connected peers
      for(PeerLink link : node.getPeerManager().getPeersWithReason("DHT"))
      {
        AddressSpecHash node_id = link.getNodeID();

        LocalPeerInfo local_info = node.getDB().getPeerMap().get(node_id.getBytes());
        if (local_info != null)
        {
          peer_list.addPeers( local_info.getSignedPeerInfo() );
        }
      }
    }
    catch(Exception e)
    {
      logger.log(Level.WARNING, "List share failure", e);
      e.printStackTrace();
    }

    o.onNext( peer_list.build() );
    o.onCompleted();
  }

  public void importPeer(SignedMessage peer_signed_message)
  {
    try
    {
      ChannelPeerInfo peer_info = ChannelSigUtil.validatePeerInfo(peer_signed_message);

      AddressSpecHash node_id = new AddressSpecHash(peer_info.getAddressSpecHash());
      SignedMessagePayload payload = ChannelSigUtil.quickPayload(peer_signed_message);

      LocalPeerInfo.Builder new_local_info = LocalPeerInfo.newBuilder();

      LocalPeerInfo local_info = node.getDB().getPeerMap().get(node_id.getBytes());
      if (local_info != null)
      {
        new_local_info.mergeFrom(local_info);
      }

      if (new_local_info.getSignedTimestamp() < payload.getTimestamp())
      {
        new_local_info.setSignedTimestamp( payload.getTimestamp());
        new_local_info.setSignedPeerInfo( peer_signed_message );
        new_local_info.setInfo( peer_info );

        node.getDB().getPeerMap().put(node_id.getBytes(), new_local_info.build());
      }

    }
    catch(Throwable t)
    {
      logger.log(Level.INFO, "Import peer failed: " + t, t);
    }
  }

  public SignedMessage getSignedPeerInfoSelf()
    throws ValidationException
  {
    SignedMessagePayload.Builder p = SignedMessagePayload.newBuilder();
    p.setPeerInfo( node.getNetworkExaminer().createPeerInfo() );
    SignedMessage spi = node.signMessageNode(p.build());
    return spi;
  }

  @Override
  public void storeDHTData(StoreDHTRequest req, StreamObserver<DHTDataSet> o)
  {
    try
    {
      DHTDataSet ds = storeDHTData(req);
      o.onNext(ds);
      o.onCompleted();
    }
    catch(Throwable t)
    {
      o.onError(t);
    }
  }


  public void storeDHTDataAsyncTrusted(StoreDHTRequest req)
  {
      SignedMessagePayload payload = ChannelSigUtil.quickPayload(req.getSignedDhtData());
      DHTData data = payload.getDhtData();
      ByteString target = data.getElementId();
      PeerLink next_peer = findClosestPeer(target);

      if (next_peer != null)
      {
        next_peer.getStargateAsyncStub().storeDHTData(req, new NullStreamer<DHTDataSet>());
      }
      else
      {
        ByteString key = target.concat( AddressUtil.getHashForSpec( payload.getClaim()) .getBytes() );
        node.getDB().getDHTDataMap().put(key, req.getSignedDhtData());
      }
  }


  public DHTDataSet storeDHTData(StoreDHTRequest req)
    throws ValidationException
  {
      SignedMessagePayload payload = ChannelValidation.validateDHTData(req.getSignedDhtData());

      if (payload.getTimestamp() + ChannelGlobals.MAX_DHT_DATA_AGE < System.currentTimeMillis())
      {
        throw new ValidationException("Request too old");
      }

      DHTData data = payload.getDhtData();
      ByteString target = data.getElementId();
      PeerLink next_peer = findClosestPeer(target);

      if (next_peer != null)
      {
        DHTDataSet ds = next_peer.getStargateBlockingStub().storeDHTData(req);
        return ds;
      }
      else
      {
        ByteString key = target.concat( AddressUtil.getHashForSpec( payload.getClaim()) .getBytes() );
        logger.fine(String.format("Saving DHT data for %s", HexUtil.getHexString(key)));
        node.getDB().getDHTDataMap().put(key, req.getSignedDhtData());

        return getDHTLocal(target,  req.getDesiredResultCount());
      }
  }

  @Override
  public void getDHTData(GetDHTRequest req, StreamObserver<DHTDataSet> o)
  {
    try
    {
      DHTDataSet ds = getDHTData(req);
      o.onNext(ds);
      o.onCompleted();
    }
    catch(Throwable t)
    {
      o.onError(t);
    }
  }


  public DHTDataSet getDHTData(GetDHTRequest req)
  {
    ByteString target = req.getElementId();

    PeerLink next_peer = findClosestPeer(target);

    if (next_peer != null)
    {
      DHTDataSet ds = next_peer.getStargateBlockingStub().getDHTData(req);
      return ds;
    }
    else
    {
      return getDHTLocal(target, req.getDesiredResultCount());
    }
  }

  public void getDHTDataAsync(GetDHTRequest req, StreamObserver<DHTDataSet> so)
  {
    ByteString target = req.getElementId();

    PeerLink next_peer = findClosestPeer(target);

    if (next_peer != null)
    {
      next_peer.getStargateAsyncStub().getDHTData(req, so);
    }
    else
    {
      DHTDataSet set = getDHTLocal(target, req.getDesiredResultCount());
      so.onNext(set);
    }

  }

  public DHTDataSet getDHTLocal(ByteString target, int desired)
  {
    DHTDataSet.Builder set = DHTDataSet.newBuilder();
    if (desired > 0)
    {
      Map<ByteString, SignedMessage> m = node.getDB().getDHTDataMap().getByPrefix(target, 100000);
      LinkedList<SignedMessage> lst = new LinkedList<>();
      lst.addAll(m.values());
      Collections.shuffle(lst);

      while(lst.size() > desired)
      {
        lst.pop();
      }

      set.addAllDhtData(lst);
    }

    return set.build();
  }

  /**
   * Returns cloests peer, or null if none are closer that me
   */
  public PeerLink findClosestPeer(ByteString target)
  {
    ByteString self_hash = node.getNodeID().getBytes();
    BigInteger min_diff = HashMath.getAsInt( HashMath.getAbsDiff(target, self_hash) );

    PeerLink close = null;

    for(PeerLink link : node.getPeerManager().getPeersWithReason("DHT"))
    {
      if (link.isGood())
      {
        AddressSpecHash node_id = link.getNodeID();
        BigInteger diff = HashMath.getAsInt( HashMath.getAbsDiff(target, node_id.getBytes()));
        if (diff.compareTo(min_diff) < 0)
        {
          close = link;
          min_diff = diff;
        }
      }

    }

    return close;
  }

}
