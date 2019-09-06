package snowblossom.channels;

import duckutil.SimpleFuture;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Manage channels we are tracking */
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
      node.getChannelPeerMaintainer().wake();
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
      ctx.cid = cid;
      ctx.db = node.getChannelDB(cid);
      ctx.block_ingestor = new ChannelBlockIngestor(node, cid, ctx);

      return ctx;
  }

  public void dropChannel(ChannelID cid)
  { //TODO - something

  }

  public Set<ChannelID> getChannelSet()
  {
    HashSet<ChannelID> s = new HashSet<>(16,0.5f);
    synchronized(chan_map)
    {
      s.addAll(chan_map.keySet());
    }
    return s;
  }


  public void notifyChannelBlock(ChannelID cid)
  {
    node.getChannelTipSender().wakeFor(cid);

  }

}
