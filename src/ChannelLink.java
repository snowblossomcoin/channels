package snowblossom.channels;

import io.grpc.stub.StreamObserver;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;

public class ChannelLink implements StreamObserver<ChannelPeerMessage>
{   
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

	private final boolean server_side;
  private final boolean client_side;
	private StreamObserver<ChannelPeerMessage> sink;
  private PeerLink peer_link; // only if we are client
  private ChannelID cid;
  private volatile long last_recv;
  private volatile boolean closed;

	// As server
	public ChannelLink(StreamObserver<ChannelPeerMessage> sink)
	{
    last_recv = System.currentTimeMillis();
		this.sink = sink;
    server_side = true;
    client_side = false;

	}

  // As client
  public ChannelLink(PeerLink peer_link, ChannelID cid)
  {
    last_recv = System.currentTimeMillis();
    server_side = false;
    client_side = true;
    this.peer_link = peer_link;
    this.cid = cid;

    sink = peer_link.getChannelAsyncStub().subscribePeering(this);

  }

  public boolean isGood()
  { 
    if (closed) return false;
		if (peer_link != null)
		{
			if (!peer_link.isGood()) return false;
		}
    if (last_recv + ChannelGlobals.CHANNEL_LINK_TIMEOUT < System.currentTimeMillis())
    { 
      return false;
    }
    return true;
  }

  public void close()
  {
    if (closed) return;
    closed = true;

    if (sink != null)
    {
      sink.onCompleted();
    }

  }

	@Override
	public void onCompleted()
	{
    close();
  }
	
	@Override
	public void onError(Throwable t)
	{ 
		logger.log(Level.WARNING, "wobble", t);
		close();
	}
	
	@Override
	public void onNext(ChannelPeerMessage pm)
	{ 
    // If cid == null, then record cid and register with someone

		last_recv = System.currentTimeMillis();
    if (peer_link != null) peer_link.pokeRecv();

	}
}
