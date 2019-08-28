package snowblossom.channels;

import io.grpc.stub.StreamObserver;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.lib.AddressSpecHash;
import snowblossom.channels.proto.*;

/**
 * A streaming link for peer messages.  Works for both client and server.
 * We get incoming messages via StreamObserver interface overrides.
 * Outgoing messages go via 'sink'
 */
public class ChannelLink implements StreamObserver<ChannelPeerMessage>
{   
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

	private final boolean server_side;
  private final boolean client_side;
	private final StreamObserver<ChannelPeerMessage> sink;
  private PeerLink peer_link; // only if we are client
  private ChannelNode node; // only if we are server

  private ChannelContext ctx;
  private ChannelID cid;
  private volatile long last_recv;
  private volatile boolean closed;

	// As server
	public ChannelLink(ChannelNode node, StreamObserver<ChannelPeerMessage> sink)
	{
    this.node = node;
    last_recv = System.currentTimeMillis();
		this.sink = sink;
    server_side = true;
    client_side = false;

	}

  // As client
  public ChannelLink(PeerLink peer_link, ChannelID cid, ChannelContext ctx)
  {
    last_recv = System.currentTimeMillis();
    server_side = false;
    client_side = true;
    this.peer_link = peer_link;
    this.cid = cid;

    sink = peer_link.getChannelAsyncStub().subscribePeering(this);

  }

  /** returns remote node id if know, null if we are server */
  public AddressSpecHash getRemoteNodeID()
  {
    if (peer_link == null) return null;
    return peer_link.getNodeID();
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

    if ((server_side) && (cid == null))
    {
      cid = new ChannelID(pm.getChannelId());
      ctx = node.getChannelSubscriber().getContext(cid);
      if (ctx == null)
      {
        logger.log(Level.WARNING, "Client subscribed to channel we don't care about: " + cid);
        close();
      }
      else
      {
        ctx.addLink(this);
      }
    }

	}
}
