package snowblossom.channels;

import io.grpc.stub.StreamObserver;
import java.util.logging.Logger;
import snowblossom.channels.proto.ChannelPeerMessage;
import snowblossom.channels.proto.ChannelServiceGrpc;

/**
 * GRPC service for ChannelService.  Mostly just creates ChannelLinks and gets them going.
 */
public class ChannelPeerServer extends ChannelServiceGrpc.ChannelServiceImplBase
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private final ChannelNode node;

  public ChannelPeerServer(ChannelNode node)
  {
    this.node = node;
  }

  @Override
  public StreamObserver<ChannelPeerMessage> subscribePeering(StreamObserver<ChannelPeerMessage> sink)
  {

    // Note: we don't know what channel this request is about until the onNext() is called on the ChannelLink.
    // That should be immediately after this return, but who knows.
    ChannelLink cl = new ChannelLink(node, sink);
    return cl;
  }


}
