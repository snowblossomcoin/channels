package snowblossom.channels;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import snowblossom.channels.proto.*;
import snowblossom.channels.warden.PremiumContentWarden;
import snowblossom.lib.DaemonThreadFactory;

/**
 * Manage channels we are tracking
 * Also handles notifications of events to people watching channels
 */
public class ChannelSubscriber
{
  private ChannelNode node;

  private HashMap<ChannelID, ChannelContext> chan_map;

  private HashMap<ChannelID, LinkedList<ChannelWatcherInterface> > watcher_map= new HashMap<>(16,0.5f);


  protected Executor exec;

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
      checkForWardens(ctx);
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

  private void checkForWardens(ChannelContext ctx)
  {
    ChannelAccess ca = new ChannelAccess(node, ctx);

    if (PremiumContentWarden.wantsToRun(ca))
    {
      new PremiumContentWarden(ca, null);
    }
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


  public void notifyChannelBlock(ChannelID cid, ChannelBlock blk)
  {
    node.getChannelTipSender().wakeFor(cid);

    List<ChannelWatcherInterface> notify_list = getNotifyList(cid);
    for(ChannelWatcherInterface watcher : notify_list)
    {
      getExec().execute(new Runnable(){
        public void run()
        {
          watcher.onBlock(cid, blk);
        }
      });
    }

  }

  public void notifyChannelContent(ChannelID cid, SignedMessage sm)
  {
    List<ChannelWatcherInterface> notify_list = getNotifyList(cid);
    for(ChannelWatcherInterface watcher : notify_list)
    {
      getExec().execute(new Runnable(){
        public void run()
        {
          watcher.onContent(cid, sm);
        }
      });
    }


  }


  public List<ChannelWatcherInterface> getNotifyList(ChannelID cid)
  {
    // TODO - improve this synchronization - this will be a major bottleneck
    // once we get a lot of things flowing through on a lot of channels
    // Maybe have an immutable map of immutable lists that gets cloned and updated
    // on registrations (which should be rare).
    synchronized(watcher_map)
    {
      LinkedList<ChannelWatcherInterface> lst = watcher_map.get(cid);
      if (lst != null)
      {
        return ImmutableList.copyOf(lst);
      }
      else
      {
        return ImmutableList.of();
      }
    }

  }

  public void registerWatcher(ChannelID cid, ChannelWatcherInterface watcher)
  {
    synchronized(watcher_map)
    {
      LinkedList<ChannelWatcherInterface> lst = watcher_map.get(cid);
      if (lst == null)
      {
        lst = new LinkedList<ChannelWatcherInterface>();
        watcher_map.put(cid, lst);
      }
      lst.add(watcher);
    }

  }


  protected synchronized Executor getExec()
  {
    if (exec == null)
    {
      exec = new ThreadPoolExecutor(
        32,
        32,
        2, TimeUnit.DAYS,
        new LinkedBlockingQueue<Runnable>(),
        new DaemonThreadFactory("watcher_exec"));
    }
    return exec;

  }

}
