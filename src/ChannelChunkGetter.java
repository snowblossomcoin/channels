package snowblossom.channels;

import duckutil.PeriodicThread;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import snowblossom.channels.proto.*;
import snowblossom.lib.ChainHash;
import java.util.Collections;
import java.util.BitSet;

public class ChannelChunkGetter extends PeriodicThread
{

  private HashMap<ChannelID, Long> get_time_map = new HashMap<>(16,0.5f);
  private ChannelNode node;

  private long earliest_time = 0L;

  private static final int SENDS_PER_PASS = 4;

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
      startPulls(cid);

      synchronized(get_time_map)
      {
        get_time_map.put(cid, now + ChannelGlobals.CHANNEL_TIP_SEND_MS);
      }

    }
  }

  private void startPulls(ChannelID cid)
  {
    ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
    if (ctx != null)
    {
      List<ChainHash> want_list = ChunkMapUtils.getWantList(ctx);
      List<ChannelLink> links = ctx.getLinks();

      if (links.size() == 0) return;

      int sent = 0;

      // TODO - do something smarter about link selection

      for(ChainHash content_id : want_list)
      {
        Collections.shuffle(links);
        ContentInfo ci = ctx.db.getContentInfo(content_id);
        if (ci == null) continue;

        BitSet bs = ChunkMapUtils.getSavedChunksSet(ctx, content_id);
        int total_chunks = MiscUtils.getNumberOfChunks(ci);
        List<Integer> chunk_want_list = new LinkedList<>();
        for(int i=0; i<total_chunks; i++)
        {
          if (!bs.get(i))
          {
            chunk_want_list.add(i);
          }
        }
        Collections.shuffle(chunk_want_list);

        for(int w : chunk_want_list)
        {
          Collections.shuffle(links);
          ChannelLink link = links.get(0);
    
          link.writeMessage( 
            ChannelPeerMessage.newBuilder()
              .setChannelId( cid.getBytes())
              .setReqChunk( RequestChunk.newBuilder().setMessageId(content_id.getBytes()).setChunk(w).build() )
              .build());

          if (sent >= SENDS_PER_PASS) return;
        }



  
        if (sent >= SENDS_PER_PASS) return;
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
