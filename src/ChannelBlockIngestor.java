package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.LRUCache;
import duckutil.TimeRecord;
import duckutil.TimeRecordAuto;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.logging.Logger;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.lib.*;

/**
 * This class takes in new blocks, validates them and stores them in the db.
 * In appropritate, updates the tip of the chain.
 */
public class ChannelBlockIngestor
{
  private static final Logger logger = Logger.getLogger("channelnode.blockchain");
  private ChannelNode node;
  private SingleChannelDB db;
  private ChannelID cid;
  
  private volatile ChannelBlockSummary chainhead;

  private static final ByteString HEAD = ByteString.copyFrom(new String("head").getBytes());

  private LRUCache<ChainHash, Long> block_pull_map = new LRUCache<>(2000);

  public ChannelBlockIngestor(ChannelNode node, ChannelID cid)
  {
    this.node = node;
    this.db = node.getChannelDB(cid);
    this.cid = cid;

    chainhead = db.getBlockSummaryMap().get(HEAD);
    if (chainhead != null)
    {

      logger.info(String.format("Loaded chain tip: %d %s", 
        chainhead.getHeader().getBlockHeight(), 
        new ChainHash(chainhead.getBlockId())));
    }


  }

  

  public boolean ingestBlock(ChannelBlock blk)
    throws ValidationException
  {

    ChainHash blockhash;
    try(TimeRecordAuto tra_blk = TimeRecord.openAuto("ChannelBlockIngestor.ingestBlock"))
    {
      ChannelValidation.checkBlockBasics(cid, blk, true);

      ChannelBlockHeader header = ChannelSigUtil.validateSignedMessage(blk.getSignedHeader()).getChannelBlockHeader();
      if (header == null) throw new ValidationException("Header is null");

      blockhash = new ChainHash(blk.getSignedHeader().getMessageId());

      if (db.getBlockSummaryMap().containsKey(blockhash.getBytes() ))
      {
        return false;
      }

      ChainHash prevblock = new ChainHash(header.getPrevBlockHash());

      ChannelBlockSummary prev_summary = null;
      if (prevblock.equals(ChainHash.ZERO_HASH))
      {
      }
      else
      {
        try(TimeRecordAuto tra_prv = TimeRecord.openAuto("ChannelBlockIngestor.getPrevSummary"))
        {
          prev_summary = db.getBlockSummaryMap().get( prevblock.getBytes() );
        }
        if (prev_summary == null)
        {
          return false;
        }
      }

      ChannelBlockSummary summary = ChannelValidation.deepBlockValidation(cid, blk, prev_summary);

      try(TimeRecordAuto tra_tx = TimeRecord.openAuto("ChannelBlockIngestor.blockSave"))
      {
        HashMap<ByteString, SignedMessage> content_put_map = new HashMap<>(16,0.5f);
        for(SignedMessage content : blk.getContentList())
        {
          content_put_map.put( content.getMessageId(), content);
        }
        db.getContentMap().putAll(content_put_map);
        db.getBlockMap().put( blockhash.getBytes(), blk);
        db.getBlockSummaryMap().put( blockhash.getBytes(), summary);
      }

      BigInteger summary_work_sum = BlockchainUtil.readInteger(summary.getWorkSum());
      BigInteger chainhead_work_sum = BigInteger.ZERO;
      if (chainhead != null)
      {
        chainhead_work_sum = BlockchainUtil.readInteger(chainhead.getWorkSum());
      }

      // TODO - implement tie breakers
      if (summary_work_sum.compareTo(chainhead_work_sum) > 0)
      {
        chainhead = summary;
        db.getBlockSummaryMap().put(HEAD, summary);
        //System.out.println("UTXO at new root: " + HexUtil.getHexString(summary.getHeader().getUtxoRootHash()));
        //node.getUtxoHashedTrie().printTree(summary.getHeader().getUtxoRootHash());

        updateHeights(summary);

        logger.info(String.format("New chain tip: Height %d %s (tx:%d sz:%d)", summary.getHeader().getBlockHeight(), blockhash, blk.getContentCount(), blk.toByteString().size()));

        double age_min = System.currentTimeMillis() - summary.getHeader().getTimestamp();
        age_min = age_min / 60000.0;

        DecimalFormat df = new DecimalFormat("0.0");

        //TODO
        /*SnowUserService u = node.getUserService();
        if (u != null)
        {
          u.tickleBlocks();
        }
        node.getMemPool().tickleBlocks(new ChainHash(summary.getHeader().getUtxoRootHash()));*/
      }


      //TODO
      //node.getPeerage().sendAllTips(cid);
    }

    return true;

  }

  private void updateHeights(ChannelBlockSummary summary)
  {
    while(true)
    {
      long height = summary.getHeader().getBlockHeight();
      ChainHash found = db.getBlockHashAtHeight(height);
      ChainHash hash = new ChainHash(summary.getBlockId());
      if ((found == null) || (!found.equals(hash)))
      {
        db.setBlockHashAtHeight(height, hash);
        //TODO
        //node.getBlockHeightCache().setHash(height, hash);
        if (height == 0) return;
        summary = db.getBlockSummaryMap().get(summary.getHeader().getPrevBlockHash());
      }
      else
      {
        return;
      }
    }
  }

  public ChannelBlockSummary getHead()
  {
    return chainhead;
  }

  public long getHeight()
  {
    ChannelBlockSummary summ = getHead();
    if (summ == null) return 0;

    return summ.getHeader().getBlockHeight();
  }

  public boolean reserveBlock(ChainHash hash)
  {
    synchronized(block_pull_map)
    {
      long tm = System.currentTimeMillis();
      if (block_pull_map.containsKey(hash) && (block_pull_map.get(hash) + 15000L > tm))
      {
        return false;
      }
      block_pull_map.put(hash, tm);
      return true;
    }
  }

}
