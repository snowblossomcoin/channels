package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import snowblossom.channels.proto.*;
import snowblossom.lib.ChainHash;
import snowblossom.lib.ValidationException;

/**
 * View into a single channel as well as some methods to publish some changes
 */
public class ChannelInterface
{
  private ChannelContext ctx;

  public ChannelInterface(ChannelContext ctx)
  {
    this.ctx = ctx;
  }


  public ChannelBlockSummary getHead()
  {
    return ctx.block_ingestor.getHead();
  }

  public ChannelSettings getSettings()
  {
    ChannelBlockSummary head = getHead();
    if (head == null) return null;
    return head.getEffectiveSettings();
  }

  /**
   * Returns N where N is the number of blocks, so blocks
   * 0 - (n-1) exist.
   */
  public long getBlockCount()
  {
    ChannelBlockSummary head = getHead();
    if (head==null) return 0;

    return head.getHeader().getBlockHeight() + 1L;
  }

  public ChannelBlock getBlockByHash(ChainHash hash)
  {
    if (hash == null) return null;
    return ctx.db.getBlockMap().get(hash.getBytes());

  }

  /**
   * Note: it is always possible to get a chain reorg in the middle of any processing.
   * So if you must have a consistent view of the chain, start from the getHead() and work
   * backwords, following each prev_hash rathen than this method.
   */
  public ChannelBlock getBlockByHeight(long height)
  {
    ChainHash hash = ctx.db.getBlockHashAtHeight(height);
    return getBlockByHash(hash);

  }

  public SignedMessage getContent(ChainHash hash)
  {
    if (hash == null) return null;
    return ctx.db.getContentMap().get(hash.getBytes());
  }

  public ByteString getDataAtHead(String key)
  {
    return ChanDataUtils.getData(ctx, key);
  }


  public ByteString getDataAtBlock(ChainHash hash, String key)
  {
    //TODO implement
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Returns unconfirmed outsider messagers order by time desc
   * (most recent first).
   */
  public List<SignedMessage> getOutsiderByTime(int max_return)
  {
    TreeMap<Double, SignedMessage> message_map = new TreeMap<>();

    Random rnd = new Random();

    for(SignedMessage sm : ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 100000).values())
    {
      try
      {
        ChannelValidation.validateOutsiderContent(sm, ctx.block_ingestor.getHead());
        SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

        double v = payload.getTimestamp();
        v+=rnd.nextDouble();

        message_map.put( -v, sm);
      }
      catch(ValidationException e){}

      while(message_map.size() > max_return)
      {
        message_map.pollLastEntry();
      }
    }

    LinkedList<SignedMessage> lst = new LinkedList();
    lst.addAll(message_map.values());

    return lst;

  }


}
