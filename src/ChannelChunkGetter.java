package snowblossom.channels;

import duckutil.PeriodicThread;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import snowblossom.channels.proto.*;

public class ChannelChunkGetter extends PeriodicThread
{

  private HashMap<ChannelID, Long> get_time_map = new HashMap<>(16,0.5f);
  private ChannelNode node;

  private long earliest_time = 0L;

  public ChannelChunkGetter(ChannelNode node)
  {
    super(500L);
    this.setName("ChannelChunkGetter");
    this.setDaemon(true);
    this.node = node;

  }

  @Override
  public void runPass()
  {
    long now = System.currentTimeMillis();
    synchronized(get_time_map)
    {
      if (now < earliest_time) return;
    }

    
    Set<ChannelID> chan_set = node.getChannelSubscriber().getChannelSet();

    long next_earliest = now + ChannelGlobals.CHANNEL_TIP_SEND_MS;
    Set<ChannelID> send_set = new HashSet<>();

    synchronized(get_time_map)
    {
      for(ChannelID cid : chan_set)
      {
        long tm = 0L;
        if (get_time_map.containsKey(cid))
        {
          tm = get_time_map.get(cid);
        }
        if (tm <= now)
        {
          send_set.add(cid);
        }
        else
        {
          next_earliest = Math.min(next_earliest, tm);
        }

      }
      earliest_time = next_earliest;

    }
    for(ChannelID cid : send_set)
    {

      ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
      if (ctx != null)
      {
        //TODO -- do things
				List<ChannelLink> links = ctx.getLinks();
      }
      synchronized(get_time_map)
      {
        get_time_map.put(cid, now + ChannelGlobals.CHANNEL_TIP_SEND_MS);
      }

    }
  }

  public void wakeFor(ChannelID cid)
  {
    synchronized(get_time_map)
    {
      get_time_map.put(cid, 0L);
      earliest_time = 0L;
    }
    this.wake();
  }

  

}
