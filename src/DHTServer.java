

package snowblossom.channels;

import com.google.protobuf.ByteString;                                                                                                                      import io.grpc.stub.StreamObserver;
import snowblossom.channels.proto.*;


public class DHTServer extends StargateServiceGrpc.StargateServiceImplBase
{
  @Override
  public void getDHTPeers(NullRequest nr, StreamObserver<PeerList> o)
  {
    o.onNext( PeerList.newBuilder().build() );
    o.onCompleted();

  }

}

