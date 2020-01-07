package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.PeriodicThread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import snowblossom.channels.proto.*;
import snowblossom.lib.ValidationException;

public class ChannelOutsiderSender extends PeriodicThread
{

  private HashMap<ChannelID, Long> get_time_map = new HashMap<>(16,0.5f);
  private ChannelNode node;

  private long earliest_time = 0L;

  public ChannelOutsiderSender(ChannelNode node)
  {
    super(500L);
    this.setName("ChannelOutsiderSender");
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
      startSend(cid);

      synchronized(get_time_map)
      {
        get_time_map.put(cid, now + ChannelGlobals.CHANNEL_TIP_SEND_MS);
      }

    }
  }

  private void startSend(ChannelID cid)
  {
    ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
    if (ctx != null)
    {
      //TODO - use random rather than just getting everything
      ArrayList<SignedMessage> full_list = new ArrayList<>();
      
      full_list.addAll(ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 100000).values());
      if (full_list.size() == 0) return;

      Random rnd = new Random();

      SignedMessage sm = full_list.get(rnd.nextInt(full_list.size()));

      try
      {
        ChannelValidation.validateOutsiderContent(sm, ctx.block_ingestor.getHead(), ctx);

        ChannelPeerMessage m_out = ChannelPeerMessage.newBuilder()
          .setChannelId(cid.getBytes())
          .setContent(sm)
          .build();

        for(ChannelLink link : ctx.getLinks())
        {
          link.writeMessage(m_out);
        }
      }
      catch(ValidationException e)
      {
        ctx.db.getOutsiderMap().remove( sm.getMessageId() );
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
