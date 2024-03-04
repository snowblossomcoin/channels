package snowblossom.channels;

import duckutil.PeriodicThread;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.ChainHash;

public class ChannelChunkGetter extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  // Map of when each channel should be processed next
  private HashMap<ChannelID, Long> get_time_map = new HashMap<>(16,0.5f);
  private ChannelNode node;

  // Semaphore of requests for each channel
  private HashMap<ChannelID, TimeSem> request_sem_map;

  private long earliest_time = 0L;

  private static final int SIMULTANTIOUS_REQUESTS_PER_CHANNEL = 32;
  private static final int SIMULTANTIOUS_REQUESTS_PER_LINK = 4;
  public static final long SEMAPHORE_EXPIRATION = 30000L;

  public ChannelChunkGetter(ChannelNode node)
  {
    super(500L);
    this.setName("ChannelChunkGetter");
    this.setDaemon(true);
    this.node = node;

    request_sem_map = new HashMap<>(16,0.5f);

  }

  protected TimeSem getChanSem(ChannelID cid)
  {
    synchronized(request_sem_map)
    {
      TimeSem sem = request_sem_map.get(cid);
      if (sem == null)
      {
        sem = new TimeSem(SIMULTANTIOUS_REQUESTS_PER_CHANNEL, SEMAPHORE_EXPIRATION);

        request_sem_map.put(cid, sem);
      }
      return sem;

    }
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
        if (getChanSem(cid).availablePermits() > 0)
        {
          if (tm <= now)
          {
            send_set.add(cid);
          }
          else
          {
            next_earliest = Math.min(next_earliest, tm);
          }
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

  private LinkedList<ChannelLink> shuffleAndFilter(LinkedList<ChannelLink> links)
  {
    Collections.shuffle(links);
    LinkedList<ChannelLink> good_links = new LinkedList<>();
    for(ChannelLink link : links)
    {
      if (link.isGood())
      {
        if (link.getChunkHoldSem().availablePermits() < SIMULTANTIOUS_REQUESTS_PER_LINK)
        {
          good_links.add(link);
        }
      }
    }
    return good_links;

  }

  private void startPulls(ChannelID cid)
  {
    int to_send = getChanSem(cid).availablePermits();
    ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
    if (ctx != null)
    {
      List<ChainHash> want_list = ChunkMapUtils.getWantList(ctx);

      Collections.shuffle(want_list);
      LinkedList<ChannelLink> links = new LinkedList<>(ctx.getLinks());

      links = shuffleAndFilter(links);
      if (want_list.size() ==0) return;
      logger.info(String.format("Want chunks %s: links:%d, wants:%d, sem:%d",
        cid, links.size(), want_list.size(), to_send ));
      if (links.size() == 0) return;

      // TODO - do something smarter about link selection
      // like check BitSets or block heights

      for(ChainHash content_id : want_list)
      {
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
          links = shuffleAndFilter(links);
          if (links.size() == 0) return;

          ChannelLink link = links.get(0);

          if ( getChanSem(cid).tryAcquire())
          {
            logger.fine("Reserving sem, requesting chunk: " + link);
            link.setChunkSem(getChanSem(cid));
            link.getChunkHoldSem().release();
            link.writeMessage(
              ChannelPeerMessage.newBuilder()
                .setChannelId( cid.getBytes())
                .setReqChunk( RequestChunk.newBuilder().setMessageId(content_id.getBytes()).setChunk(w).build() )
                .build());
          }
          else
          {
            return;
          }

        }

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
