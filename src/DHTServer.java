package snowblossom.channels;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import snowblossom.channels.proto.*;

import snowblossom.lib.ValidationException;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

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
      SignedMessagePayload payload = ChannelSigUtil.validateSignedMessage(peer_signed_message);

      if (payload.getPeerInfo() == null) throw new ValidationException("Signed peer info has no peer info");

      ChannelPeerInfo peer_info = payload.getPeerInfo();
      
      AddressSpecHash signed_address = AddressUtil.getHashForSpec(payload.getClaim());
      AddressSpecHash node_id = new AddressSpecHash(peer_info.getAddressSpecHash());
      if (!signed_address.equals(node_id))
      {
        throw new ValidationException("Signed address does not match node id");
      }

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

    return node.signMessage(p.build());
    
  }

}
