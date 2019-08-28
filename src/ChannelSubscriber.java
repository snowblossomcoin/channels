package snowblossom.channels;

import java.util.HashMap;
import duckutil.SimpleFuture;


/** Managed channels we are tracking */
public class ChannelSubscriber
{
  private ChannelNode node;
  private HashMap<ChannelID, SimpleFuture<ChannelContext> > chan_map;
  
  public ChannelSubscriber(ChannelNode node)
  {
    this.node = node;
    chan_map = new HashMap<>(16,0.5f);
  }

  /**
   * open a channel and get context
   * or if already open, get the context
   * safe to call a bunch simulantiously for the same channel
   */
  public ChannelContext openChannel(ChannelID cid)
  {
    SimpleFuture<ChannelContext> cc = null;
    boolean doOpen = false;
    synchronized(chan_map)
    {
      cc = chan_map.get(cid);
      if (cc == null)
      {
        cc = new SimpleFuture<ChannelContext>();
        chan_map.put(cid, cc);
        doOpen = true;
      }
    }

    if (doOpen)
    {
      cc.setResult(openChannelInternal(cid));
    }

    return cc.get();
  }

  /**
   * Get a channel context, but don't open it if not already open
   */
  public ChannelContext getContext(ChannelID cid)
  {
    SimpleFuture<ChannelContext> cc = null;
    synchronized(chan_map)
    {
      cc = chan_map.get(cid);
      if (cc == null)
      {
        return null;
      }
    }

    return cc.get();
   
  }

  private ChannelContext openChannelInternal(ChannelID cid)
  {
      ChannelContext ctx = new ChannelContext();
      ctx.db = node.getChannelDB(cid);

      // TODO - tickle channel peer manager

      return ctx;
  }

  public void dropChannel(ChannelID cid)
  { //TODO - something

  }

}
