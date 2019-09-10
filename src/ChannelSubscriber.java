package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Manage channels we are tracking */
public class ChannelSubscriber
{
  private ChannelNode node;

  private HashMap<ChannelID, ChannelContext> chan_map;
  
  public ChannelSubscriber(ChannelNode node)
  {
    this.node = node;
    chan_map = new HashMap<>(16,0.5f);

    
  }

  public void loadFromDB()
  {
    for(ByteString c : node.getDB().getSubscriptionMap().getByPrefix(ByteString.EMPTY, 1000000).values())
    {
      ChannelID cid = new ChannelID(c);
      openChannel(cid);
    }
  }
  
  /**
   * open a channel and get context
   * or if already open, get the context
   * safe to call a bunch simulantiously for the same channel
   */
  public ChannelContext openChannel(ChannelID cid)
  {
    ChannelContext ctx = null;
    boolean opened=false;
    synchronized(chan_map)
    {
      ctx = chan_map.get(cid);

      if (ctx == null)
      {
        ctx = openChannelInternal(cid);
        chan_map.put(cid, ctx);
        opened=true;
      }
    }

    if (opened)
    {
      node.getChannelPeerMaintainer().wake();
    }

    return ctx; 
  }

  /**
   * Get a channel context, but don't open it if not already open
   */
  public ChannelContext getContext(ChannelID cid)
  {
    synchronized(chan_map)
    {
      return chan_map.get(cid);
    }
   
  }

  private ChannelContext openChannelInternal(ChannelID cid)
  {
      ChannelContext ctx = new ChannelContext();
      ctx.cid = cid;
      ctx.db = node.getChannelDB(cid);
      ctx.block_ingestor = new ChannelBlockIngestor(node, cid, ctx);

      node.getDB().getSubscriptionMap().put(cid.getBytes(), cid.getBytes());

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
