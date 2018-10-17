package snowblossom.channels;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import snowblossom.channels.proto.*;

import snowblossom.lib.ValidationException;
import snowblossom.lib.AddressSpecHash;

public class DHTServer extends StargateServiceGrpc.StargateServiceImplBase
{
  private ChannelNode node;

  public DHTServer(ChannelNode node)
  {
    this.node = node;
  }

  @Override
  public void getDHTPeers(GetDHTPeersRequest req, StreamObserver<PeerList> o)
  {
    PeerList.Builder peer_list = PeerList.newBuilder();

    try
    {

      // Add Self
      peer_list.addPeers(getSignedPeerInfoSelf());


      // Add connected peers
      for(PeerLink link : node.getPeerManager().getPeersWithReason("DHT"))
      {
        AddressSpecHash node_id = link.getNodeID();

        LocalPeerInfo local_info = node.getDB().getPeerMap().get(node_id.getBytes());
        peer_list.addPeers( local_info.getSignedPeerInfo() );

      }


    }
    catch(Exception e)
    {
      e.printStackTrace();
    }


    o.onNext( peer_list.build() );
    o.onCompleted();
  }



  public SignedMessage getSignedPeerInfoSelf()
    throws ValidationException
  {
    SignedMessagePayload.Builder p = SignedMessagePayload.newBuilder();
    p.setPeerInfo( node.getNetworkExaminer().createPeerInfo() );

    return node.signMessage(p.build());
    
  }

}
