package snowblossom.channels;

import duckutil.PeriodicThread;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import snowblossom.channels.proto.*;

public class ChannelTipSender extends PeriodicThread
{

  private HashMap<ChannelID, Long> send_time_map = new HashMap<>(16,0.5f);
  private ChannelNode node;

  private long earliest_time = 0L;

  public ChannelTipSender(ChannelNode node)
  {
    super(500L);
    this.node = node;

  }

  @Override
  public void runPass()
  {
    long now = System.currentTimeMillis();
    synchronized(send_time_map)
    {
      if (now < earliest_time) return;
    }

    
    Set<ChannelID> chan_set = node.getChannelSubscriber().getChannelSet();

    long next_earliest = now + ChannelGlobals.CHANNEL_TIP_SEND_MS;
    Set<ChannelID> send_set = new HashSet<>();

    synchronized(send_time_map)
    {
      for(ChannelID cid : chan_set)
      {
        long tm = 0L;
        if (send_time_map.containsKey(cid))
        {
          tm = send_time_map.get(cid);
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
        ChannelBlockSummary head_sum = ctx.block_ingestor.getHead();
        if (head_sum != null)
        {
          List<ChannelLink> links = ctx.getLinks();
          ChannelPeerMessage msg = ChannelPeerMessage.newBuilder()
            .setChannelId( cid.getBytes())
            .setTip(ChannelTip.newBuilder()
              .setBlockHeader( head_sum.getSignedHeader() )
              .build())
            .build();

          for(ChannelLink link : links)
          {
            link.writeMessage(msg);
          }
        }

      }
      synchronized(send_time_map)
      {
        send_time_map.put(cid, now + ChannelGlobals.CHANNEL_TIP_SEND_MS);
      }

    }
    
    

  }

  public void wakeFor(ChannelID cid)
  {
    synchronized(send_time_map)
    {
      send_time_map.put(cid, 0L);
      earliest_time = 0L;
    }
    this.wake();
  }
  

}
