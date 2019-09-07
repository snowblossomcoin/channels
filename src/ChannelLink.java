package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.ExpiringLRUCache;
import io.grpc.stub.StreamObserver;
import java.util.BitSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.ChainHash;
import snowblossom.lib.ValidationException;

/**
 * A streaming link for peer messages.  Works for both client and server.
 * We get incoming messages via StreamObserver interface overrides.
 * Outgoing messages go via 'sink'
 */
public class ChannelLink implements StreamObserver<ChannelPeerMessage>
{   
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

	private final boolean server_side;
  private final boolean client_side;
	private final StreamObserver<ChannelPeerMessage> sink;
  private PeerLink peer_link; // only if we are client
  private ChannelNode node; // only if we are server

  private ChannelContext ctx;
  private ChannelID cid;
  private volatile long last_recv;
  private volatile boolean closed;
  private volatile ChannelTip last_tip;

  private TreeMap<Long, ChainHash> peer_block_map = new TreeMap<Long, ChainHash>();
  private ExpiringLRUCache<ChainHash, BitSet> peer_chunks = new ExpiringLRUCache<>(10000, 30000L);


	// As server
	public ChannelLink(ChannelNode node, StreamObserver<ChannelPeerMessage> sink)
	{
    this.node = node;
    last_recv = System.currentTimeMillis();
		this.sink = sink;
    server_side = true;
    client_side = false;

	}

  // As client
  public ChannelLink(ChannelNode node, PeerLink peer_link, ChannelID cid, ChannelContext ctx)
  {
    this.node = node;
    last_recv = System.currentTimeMillis();
    server_side = false;
    client_side = true;
    this.peer_link = peer_link;
    this.cid = cid;
    this.ctx = ctx;

    sink = peer_link.getChannelAsyncStub().subscribePeering(this);

  }

  /** returns remote node id if know, null if we are server */
  public AddressSpecHash getRemoteNodeID()
  {
    if (peer_link == null) return null;
    return peer_link.getNodeID();
  }

  public boolean isGood()
  { 
    if (closed) return false;
		if (peer_link != null)
		{
			if (!peer_link.isGood()) return false;
		}
    if (last_recv + ChannelGlobals.CHANNEL_LINK_TIMEOUT < System.currentTimeMillis())
    { 
      return false;
    }
    return true;
  }

  /**
   * this close does not actually close the PeerLink (if we have one)
   * that will just go away with no traffic if nothing is using it
   */
  public void close()
  {
    if (closed) return;
    closed = true;

    if (sink != null)
    {
      sink.onCompleted();
    }

  }

	@Override
	public void onCompleted()
	{
    close();
  }
	
	@Override
	public void onError(Throwable t)
	{ 
		logger.log(Level.WARNING, "wobble", t);
		close();
	}
	
  /** Concepts copied from Snowblossom PeerLink.  Basic contract is that each side sends
   * a ChannelTip ever little while (or on new blocks) and it is up to the other side to 
   * request what they want. */
	@Override
	public void onNext(ChannelPeerMessage msg)
	{ 

		last_recv = System.currentTimeMillis();
    if (peer_link != null) peer_link.pokeRecv();

    // Set ChannelID
    if ((server_side) && (cid == null))
    {
      cid = new ChannelID(msg.getChannelId());
      ctx = node.getChannelSubscriber().getContext(cid);
      if (ctx == null)
      {
        logger.log(Level.WARNING, "Client subscribed to channel we don't care about: " + cid.asString());
        close();
        return;
      }
      else
      {
        logger.log(Level.INFO, String.format("Client asked to scribe to channel %s", cid.asString()));
        ctx.addLink(this);
        node.getChannelTipSender().sendTip(cid, this);
      }
    }

    // Check ChannelID
    if (!cid.equals(msg.getChannelId()))
    {
      logger.log(Level.WARNING, "Peer sent message about wrong channel");
      close();
      return;
    }


    try
    {
      if (msg.hasTip())
      {
        ChannelTip tip = msg.getTip();

        //TODO - Record that peer is actually sending a tip in peer db
        //TODO - import peers from tip

        if (tip.getBlockHeader().getMessageId().size() > 0)
        {
          this.last_tip = tip;
          
          ChannelBlockHeader header = ChannelValidation.checkBlockHeaderBasics(cid, tip.getBlockHeader());
          ChainHash hash = new ChainHash(tip.getBlockHeader().getMessageId());
          logger.info(String.format("Channel %s got tip from remote %d %s ", cid.asString(), header.getBlockHeight(), hash.toString()));
          considerChannelHeader(hash, header);
        }
      }
      else if (msg.hasHeader())
      {
        ChannelBlockHeader header = ChannelValidation.checkBlockHeaderBasics(cid, msg.getHeader());
        considerChannelHeader(new ChainHash(msg.getHeader().getMessageId()), header);
      }
      else if (msg.hasReqBlock())
      {
        ChainHash desired_hash = null;
        RequestBlock req = msg.getReqBlock();
        if (req.getBlockHash().size() == 0)
        {
          desired_hash = ctx.db.getBlockHashAtHeight(req.getBlockHeight());
        }
        else
        {
          desired_hash = new ChainHash(req.getBlockHash());
        }
        ChannelBlock blk = ctx.db.getBlockMap().get(desired_hash.getBytes());

        writeMessage( ChannelPeerMessage.newBuilder()
          .setChannelId( cid.getBytes()) 
          .setBlock(blk)
          .build());

      }
      else if (msg.hasReqHeader())
      {
        ChainHash desired_hash = null;
        RequestBlock req = msg.getReqHeader();
        if (req.getBlockHash().size() == 0)
        {
          desired_hash = ctx.db.getBlockHashAtHeight(req.getBlockHeight());
        }
        else
        {
          desired_hash = new ChainHash(req.getBlockHash());
        }
        ChannelBlockSummary summary = ctx.db.getBlockSummaryMap().get(desired_hash.getBytes());

                  writeMessage( ChannelPeerMessage.newBuilder()
                    .setChannelId( cid.getBytes()) 
                    .setHeader(summary.getSignedHeader())
                    .build());

      }
			else if (msg.hasBlock())
      {
        // Getting a block, we probably asked for it.  See if we can eat it.
        ChannelBlock blk = msg.getBlock();
        try
        { 
          if (ctx.block_ingestor.ingestBlock(blk))
          { // we could eat it, think about getting more blocks
            long next = ChannelSigUtil.quickPayload(blk.getSignedHeader()).getChannelBlockHeader().getBlockHeight()+1L;
            synchronized(peer_block_map)
            { 
              if (peer_block_map.containsKey(next))
              { 
                ChainHash target = peer_block_map.get(next);

                if (ctx.block_ingestor.reserveBlock(target))
                { 
                  writeMessage( ChannelPeerMessage.newBuilder()
                    .setChannelId( cid.getBytes()) 
                    .setReqBlock(
                      RequestBlock.newBuilder().setBlockHash(target.getBytes()).build())
                    .build());
                }
              }
            }
          }
        }
        catch(ValidationException ve)
        { 
          logger.info("Got a block %s that didn't validate - closing link");
          close();
          throw(ve);
        }
      }
      else if (msg.hasReqContent())
      {
        RequestContent rc = msg.getReqContent();
        ChainHash content_id = new ChainHash(rc.getMessageId());
        SignedMessage ci = ctx.db.getContentMap().get(content_id.getBytes());
        if (ci != null)
        {
           writeMessage( ChannelPeerMessage.newBuilder()
              .setChannelId( cid.getBytes()) 
              .setContent(ci)
              .build());

        }
        //TODO some sort of error on no data

      }
      else if (msg.hasReqChunk())
      {
        RequestChunk rc = msg.getReqChunk();
        ChainHash content_id = new ChainHash(rc.getMessageId());
        int chunk_number = rc.getChunk();

        ContentChunk.Builder chunk_msg = ContentChunk.newBuilder();
        chunk_msg.setMessageId(content_id.getBytes());
        chunk_msg.setChunk(chunk_number);

        ByteString data = ChunkMapUtils.getChunk(ctx, content_id, chunk_number);
        if (data != null)
        {
          chunk_msg.setChunkData(data);
        }
        else
        {
          BitSet bs = ChunkMapUtils.getSavedChunksSet(ctx, content_id);
          ByteString bs_bytes = ByteString.copyFrom(bs.toByteArray());
          if (bs_bytes.size() == 0)
          {
            // Send something so it is clear we are sending something
            // otherwise not having any chunks will be the same
            // as no answer
            bs_bytes = ByteString.copyFrom(new byte[8]);
          }
          chunk_msg.setChunkHaveBitmap(bs_bytes);
        }

        writeMessage( ChannelPeerMessage.newBuilder()
          .setChannelId( cid.getBytes()) 
          .setChunk(chunk_msg.build())
          .build());

      }
      else if (msg.hasChunk())
      {
        ContentChunk chunk = msg.getChunk();

        ctx.block_ingestor.ingestChunk(chunk);
        if (chunk.getChunkHaveBitmap().size() > 0)
        {
          BitSet bs = BitSet.valueOf(chunk.getChunkHaveBitmap().asReadOnlyByteBuffer());
          peer_chunks.put( new ChainHash(chunk.getMessageId()), bs);
        }

      }
      else
      {
        logger.info("Unhandled message: " + msg);
      }

    }
    catch(Throwable t)
    {
      logger.log(Level.INFO, "Some bs from remote channel peer", t);
      close();
    }


	}

  /**
   * The basic plan is, keep asking about previous blocks
   * until we get to one we have heard of.  Then we start requesting the blocks.
   */
  private void considerChannelHeader(ChainHash block_hash, ChannelBlockHeader header)
  {
    synchronized(peer_block_map)
    {
      peer_block_map.put(header.getBlockHeight(), block_hash);
    }
    // if we don't have this block
    if (ctx == null) throw new RuntimeException("ctx");
    if (ctx.db == null) throw new RuntimeException("db");
    if (ctx.db.getBlockSummaryMap() == null) throw new RuntimeException("map");
    if (block_hash == null) throw new RuntimeException("block_hash");
    if (ctx.db.getBlockSummaryMap().get(block_hash.getBytes())==null)
    { 
      long height = header.getBlockHeight();
      if ((height == 0) || (ctx.db.getBlockSummaryMap().get(header.getPrevBlockHash())!=null))
      { // but we have the prev block - get this block 
        if (ctx.block_ingestor.reserveBlock(block_hash))
        { 
          writeMessage( ChannelPeerMessage.newBuilder()
            .setChannelId( cid.getBytes()) 
            .setReqBlock(
              RequestBlock.newBuilder().setBlockHash(block_hash.getBytes()).build())
            .build());
        }
      }
      else
      { //get more headers, still in the woods
        long next = header.getBlockHeight();
        if (ctx.block_ingestor.getHeight() + ChannelGlobals.BLOCK_CHUNK_HEADER_DOWNLOAD_SIZE < next)
        { 
          next = ctx.block_ingestor.getHeight() + ChannelGlobals.BLOCK_CHUNK_HEADER_DOWNLOAD_SIZE;
        }
        while(peer_block_map.containsKey(next))
        { 
          next--;
        }
        
        if (next >= 0)
        { 
          ChainHash prev = new ChainHash(header.getPrevBlockHash());
          synchronized(peer_block_map)
          { 
            if (peer_block_map.containsKey(next))
            { 
              if (peer_block_map.get(next).equals(prev)) return;
            }
          }
          
          writeMessage( ChannelPeerMessage.newBuilder()
            .setChannelId( cid.getBytes()) 
            .setReqHeader(
              RequestBlock.newBuilder().setBlockHeight(next).build())
            .build());
        }
      }
    
    }

  }

  public void writeMessage(ChannelPeerMessage msg)
  { 
    if (!closed)
    { 
      synchronized(sink)
      {
        sink.onNext(msg);
      }
    }
  }


}
