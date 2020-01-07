package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.LRUCache;
import duckutil.TimeRecord;
import duckutil.TimeRecordAuto;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.ChannelPeerMessage;
import snowblossom.channels.proto.ContentChunk;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.lib.*;
import snowblossom.lib.trie.HashUtils;

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
  private ChannelContext ctx;
  
  private volatile ChannelBlockSummary chainhead;

  private static final ByteString HEAD = ByteString.copyFrom(new String("head").getBytes());

  private LRUCache<ChainHash, Long> block_pull_map = new LRUCache<>(2000);

  public ChannelBlockIngestor(ChannelNode node, ChannelID cid, ChannelContext ctx)
  {
    this.node = node;
    this.ctx = ctx;
    this.db = ctx.db;
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

      ChannelBlockSummary.Builder summary = ChannelValidation.deepBlockValidation(cid, blk, prev_summary);

      ByteString data_hash = HashUtils.hashOfEmpty();
      if (prev_summary != null)
      {
        data_hash = prev_summary.getDataRootHash();
      }

      try(TimeRecordAuto tra_tx = TimeRecord.openAuto("ChannelBlockIngestor.blockSave"))
      {
        HashMap<ByteString, SignedMessage> content_put_map = new HashMap<>(16,0.5f);
        for(SignedMessage content : blk.getContentList())
        {
          content_put_map.put( content.getMessageId(), content);
          ContentInfo ci = ChannelSigUtil.quickPayload(content).getContentInfo();
          if (ci.getChanMapUpdatesCount() > 0)
          {
            data_hash = updateDataTrie( ci, data_hash);
          }
          if (ci.getContent().size() < ci.getContentLength())
          {
            int chunks = MiscUtils.getNumberOfChunks(ci);
            if (chunks != ChunkMapUtils.getSavedChunksSet(ctx, new ChainHash(content.getMessageId())).cardinality())
            {
              ChunkMapUtils.markWant(ctx, new ChainHash(content.getMessageId()));
            }
          }
        }
        summary.setDataRootHash(data_hash);
        db.getContentMap().putAll(content_put_map);
        db.getBlockMap().put( blockhash.getBytes(), blk);
        db.getBlockSummaryMap().put( blockhash.getBytes(), summary.build());
      }

      node.getChannelChunkGetter().wakeFor(cid);
      
      ChannelBlockSummary summary_fin = summary.build();

      BigInteger summary_work_sum = BlockchainUtil.readInteger(summary.getWorkSum());
      BigInteger chainhead_work_sum = BigInteger.ZERO;
      if (chainhead != null)
      {
        chainhead_work_sum = BlockchainUtil.readInteger(chainhead.getWorkSum());
      }

      // TODO - implement tie breakers
      if (summary_work_sum.compareTo(chainhead_work_sum) > 0)
      {
        chainhead = summary_fin;
        db.getBlockSummaryMap().put(HEAD, summary_fin);
        //System.out.println("UTXO at new root: " + HexUtil.getHexString(summary.getHeader().getUtxoRootHash()));
        //node.getUtxoHashedTrie().printTree(summary.getHeader().getUtxoRootHash());

        updateHeights(summary_fin);

        logger.info(String.format("New chain tip: Height %d %s (tx:%d sz:%d)", summary_fin.getHeader().getBlockHeight(), blockhash, blk.getContentCount(), blk.toByteString().size()));

        double age_min = System.currentTimeMillis() - summary_fin.getHeader().getTimestamp();
        age_min = age_min / 60000.0;

        DecimalFormat df = new DecimalFormat("0.0");
        
        node.getChannelSubscriber().notifyChannelBlock(cid, blk);
      }

    }

    return true;

  }

  private ByteString updateDataTrie(ContentInfo ci, ByteString data_hash)
    throws ValidationException
  {
    if ((data_hash == null) || (data_hash.size() != Globals.BLOCKCHAIN_HASH_LEN))
    {
      throw new ValidationException("Unset data_hash");
    }
    
    HashMap<ByteString, ByteString> update_map = new HashMap<>();
    for(Map.Entry<String, ByteString> me : ci.getChanMapUpdates().entrySet())
    {
      ByteString key = ByteString.copyFrom(me.getKey().getBytes());
      update_map.put(key, me.getValue());
    }

    data_hash = db.getDataTrie().mergeBatch(data_hash, update_map);
    return data_hash;

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

  public void ingestContent(SignedMessage sm)
		throws ValidationException
  {
			ChannelValidation.validateOutsiderContent(sm, ctx.block_ingestor.getHead(), ctx);

			if (ctx.db.getOutsiderMap().get(sm.getMessageId()) == null)
			{
				ctx.db.getOutsiderMap().put(sm.getMessageId(), sm);

				ChannelPeerMessage m_out = ChannelPeerMessage.newBuilder()
					.setChannelId(cid.getBytes())
					.setContent(sm)
					.build();

				for(ChannelLink link : ctx.getLinks())
				{
					link.writeMessage(m_out);
				}
        node.getChannelSubscriber().notifyChannelContent(cid, sm);
			}

  }

  public void ingestChunk(ContentChunk chunk)
    throws ValidationException
  {
    ingestChunk(chunk, false, null);
  }

  public void ingestChunk(ContentChunk chunk, boolean force, ContentInfo ci)
    throws ValidationException
  {
    ChainHash content_id = new ChainHash(chunk.getMessageId());
    int n = chunk.getChunk();

    if (chunk.getChunkData().size() == 0) return;

    if (!force)
    {
      if (!ChunkMapUtils.doIWant(ctx, content_id))
      {
        logger.info(String.format("I don't want chunk: %s", content_id));
        return;
      }
    }

    if (ci == null)
    {
      ci = ChannelSigUtil.quickPayload(db.getContentMap().get(content_id.getBytes())).getContentInfo();
    }

    ByteString expected_hash = null;
    if (ci.getContentLength() <= ChannelGlobals.CONTENT_DATA_BLOCK_SIZE)
    {
      expected_hash = ci.getContentHash();
    }
    else
    {
      expected_hash = ci.getChunkHash(n);
    }

    ByteString found_hash = ByteString.copyFrom(DigestUtil.getMD().digest(chunk.getChunkData().toByteArray()));
    if (!expected_hash.equals(found_hash))
    {
      throw new ValidationException("Chunk hash mismatch");
    }

    ChunkMapUtils.storeChunk(ctx, content_id, n, chunk.getChunkData());
    logger.info(String.format("Saved channel %s saved chunk %s %d (%d)", cid, content_id, n, chunk.getChunkData().size() ));

    BitSet bs = ChunkMapUtils.getSavedChunksSet(ctx, content_id);
    long saved_count = bs.cardinality();

    if (saved_count * ChannelGlobals.CONTENT_DATA_BLOCK_SIZE >= ci.getContentLength())
    {
      ChunkMapUtils.markDone(ctx, content_id);
      logger.info(String.format("Saved channel %s file complete %s", cid, content_id));
    }

    node.getChannelChunkGetter().wakeFor(cid);

  }

}
