package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.WalletDatabase;

public class BlockGenUtils
{
  /** Creates a new channel and publishes block 0 for it */
  public static ChannelID createChannel(ChannelNode node, WalletDatabase admin, String display_name)
		throws ValidationException
  {
    ChannelSettings.Builder init_settings = ChannelSettings.newBuilder();
    init_settings.setDisplayName(display_name);
    init_settings.addAdminSignerSpecHashes( getAddr(admin).getBytes() );

    // For chat channel
    init_settings.setAllowOutsideMessages(true);
    init_settings.setMaxOutsiderMessageSize(8192);
    init_settings.setMaxOutsiderAgeMs( 86400L * 14L * 1000L); //2 weeks
    
    SignedMessage signed_init_settings = ChannelSigUtil.signMessage(admin.getAddresses(0), admin.getKeys(0),
      SignedMessagePayload.newBuilder().setChannelSettings(init_settings.build()).build());

    ChannelID chan_id = ChannelSigUtil.getChannelId(new ChainHash(signed_init_settings.getMessageId()));

		ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
		header.setInitialSettings(signed_init_settings);
		header.setBlockHeight(0);

		header.setVersion(1);
		header.setChannelId( chan_id.getBytes());
		header.setPrevBlockHash( ChainHash.ZERO_HASH.getBytes());
		header.setContentMerkle( ChainHash.ZERO_HASH.getBytes());

		ChannelBlock.Builder blk = ChannelBlock.newBuilder();
		blk.setSignedHeader( ChannelSigUtil.signMessage(admin.getAddresses(0), admin.getKeys(0),
			SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

		ChannelContext ctx = node.getChannelSubscriber().openChannel(chan_id);

		ctx.block_ingestor.ingestBlock(blk.build());
    
    return chan_id;
  }

  /**
   * Create a block with the provided list of contentinfo messages
   */
  public static void createBlockForContent(ChannelContext ctx, List<SignedMessage> content, WalletDatabase admin)
    throws ValidationException
  {
    ChannelBlockSummary prev_sum = ctx.block_ingestor.getHead();
    if (prev_sum == null)
    {
      throw new ValidationException("Unknown previous block");
    }
    ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
    header.setBlockHeight(1L + prev_sum.getHeader().getBlockHeight());

    header.setVersion(1);
    header.setChannelId( ctx.cid.getBytes() );
    header.setPrevBlockHash( prev_sum.getBlockId());

    ChannelBlock.Builder blk = ChannelBlock.newBuilder();

    LinkedList<ChainHash> merkle_list = new LinkedList<>();
    for(SignedMessage sm : content)
    {
      blk.addContent(sm);
      merkle_list.add(new ChainHash(sm.getMessageId()));
    }

    header.setContentMerkle( DigestUtil.getMerkleRootForTxList(merkle_list).getBytes());
    
    blk.setSignedHeader( ChannelSigUtil.signMessage(admin.getAddresses(0), admin.getKeys(0),
      SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

    ctx.block_ingestor.ingestBlock(blk.build());

  }

  /**
   * @param base_content_info merge this into all the created content info objects
   */
  public static void createBlockForFilesMultipart(ChannelContext ctx, MultipartSlicer ms, WalletDatabase admin, ContentInfo base_content_info)
    throws ValidationException
  {
    List<SignedMessage> content_list = new LinkedList<>();

    for(MultipartSlicer.FileData fd : ms.getFiles())
    {
      ContentInfo.Builder ci = ContentInfo.newBuilder();

      ci.mergeFrom(base_content_info);

      ci.setContentLength(fd.file_data.size());

      String mime = fd.meta.get("Content-Type");
      if (mime != null)
      {
        ci.setMimeType(mime);
      } 
      String filename = fd.meta.get("filename");
      if (filename != null)
      {
        ci.putContentDataMap("filename", ByteString.copyFrom(filename.getBytes()));
      }

      MessageDigest md_part = DigestUtil.getMD();

      int loc = 0;
      int len = fd.file_data.size();

      TreeMap<Integer, ByteString> chunk_map = new TreeMap<>();
      int chunk_no =0;
      while (loc < len)
      {
        int read_len = (int) Math.min( len-loc, ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

        ByteString chunk_data = fd.file_data.substring(loc, read_len+loc);
        loc += read_len;

        ByteString part_hash = DigestUtil.hash(chunk_data);
        ci.addChunkHash(part_hash);

        chunk_map.put(chunk_no, chunk_data);
        chunk_no++;
      }
      
      ci.setContentHash(DigestUtil.hash(fd.file_data));

      ChannelValidation.validateContent(ci.build(), DigestUtil.getMD());

      SignedMessage sm = 
        ChannelSigUtil.signMessage( admin.getAddresses(0), admin.getKeys(0),
          SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

      content_list.add(sm);

      for(int c : chunk_map.keySet())
      {
        ctx.block_ingestor.ingestChunk(
          ContentChunk.newBuilder()
            .setMessageId(sm.getMessageId())
            .setChunk(c) 
            .setChunkData(chunk_map.get(c))
            .build()
          ,true, ci.build());
      }
    }

    // once we are done playing with files, call 
    createBlockForContent(ctx, content_list, admin);

  }

  /**
   * Creates a block for the files in the directory and broadcasts it to the channel
   */ 
  public static void createBlockForFiles(ChannelContext ctx, File base_path, WalletDatabase admin)
    throws ValidationException, java.io.IOException
  {
    ChannelBlockSummary prev_sum = ctx.block_ingestor.getHead();
    if (prev_sum == null)
    {
      throw new ValidationException("Unknown previous block");
    }
    ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
    header.setBlockHeight(1L + prev_sum.getHeader().getBlockHeight());

    header.setVersion(1);
    header.setChannelId( ctx.cid.getBytes() );
    header.setPrevBlockHash( prev_sum.getBlockId());

    ChannelBlock.Builder blk = ChannelBlock.newBuilder();

    ContentInfo.Builder file_map_ci = ContentInfo.newBuilder();
    file_map_ci.setContentHash( ByteString.copyFrom(DigestUtil.getMD().digest(new byte[0])) );

    addFiles(ctx, base_path, "", blk, file_map_ci, admin);

    blk.addContent(
      ChannelSigUtil.signMessage( admin.getAddresses(0),admin.getKeys(0),
          SignedMessagePayload.newBuilder().setContentInfo(file_map_ci.build()).build()));

    LinkedList<ChainHash> merkle_list = new LinkedList<>();
    for(SignedMessage sm : blk.getContentList())
    {
      merkle_list.add(new ChainHash(sm.getMessageId()));
    }

    header.setContentMerkle( DigestUtil.getMerkleRootForTxList(merkle_list).getBytes());
    
    blk.setSignedHeader( ChannelSigUtil.signMessage(admin.getAddresses(0), admin.getKeys(0),
      SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

    ctx.block_ingestor.ingestBlock(blk.build());

  }

  private static void addFiles(ChannelContext ctx, File path, 
    String prefix, ChannelBlock.Builder blk, ContentInfo.Builder file_map_ci, WalletDatabase sig)
    throws ValidationException, java.io.IOException
  {
    if (path.isDirectory())
    {
      for(File f : path.listFiles())
      {
        addFiles(ctx, f, prefix + "/" + f.getName(), blk, file_map_ci, sig);
      }
    }
    else
    {
      long len = path.length();
      ContentInfo.Builder ci = ContentInfo.newBuilder();
      ci.setContentLength(len);

      String mime = Mimer.guessContentType(path.getName());
      if (mime != null)
      {
        ci.setMimeType(mime);
      } 
      // Save hash
      // save merkle list
      MessageDigest md_whole = DigestUtil.getMD();
      MessageDigest md_part = DigestUtil.getMD();
      DataInputStream din = new DataInputStream(new FileInputStream(path));
      long loc = 0;
      while (loc < len)
      {
        int read_len = (int) Math.min( len-loc, ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

        byte[] b = new byte[read_len];
        din.readFully(b);
        loc += read_len;

        ByteString part_hash = ByteString.copyFrom(md_part.digest(b));
        ci.addChunkHash(part_hash);
        md_whole.update(b);
      }
      din.close();

      ci.setContentHash(ByteString.copyFrom(md_whole.digest()));

      //check existing data
      //
      ByteString old_content_id = ChanDataUtils.getData(ctx, "/web" + prefix);
      if (old_content_id != null)
      {
        SignedMessage old_content_msg = ctx.db.getContentMap().get(old_content_id);
        if (old_content_msg != null)
        {
          ContentInfo old_ci = ChannelSigUtil.quickPayload(old_content_msg).getContentInfo();
          if(ci.getContentHash().equals(old_ci.getContentHash()))
          {
            return; 
          }
        }

      }


      ChannelValidation.validateContent(ci.build(), DigestUtil.getMD());

      SignedMessage sm = 
        ChannelSigUtil.signMessage( sig.getAddresses(0), sig.getKeys(0),
          SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

      blk.addContent(sm);
      file_map_ci.putChanMapUpdates("/web" + prefix, sm.getMessageId());

      din = new DataInputStream(new FileInputStream(path));
      loc = 0;
      int chunk_count =0;
      while (loc < len)
      {
        int read_len = (int) Math.min( len-loc, ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

        byte[] b = new byte[read_len];
        din.readFully(b);
        loc += read_len;

        ctx.block_ingestor.ingestChunk(
          ContentChunk.newBuilder()
            .setMessageId(sm.getMessageId())
            .setChunk(chunk_count) 
            .setChunkData(ByteString.copyFrom(b))
            .build()
          ,true, ci.build());

        chunk_count++;
      }
      din.close();
    }
  }

  protected static AddressSpecHash getAddr(WalletDatabase db)
  {
    return AddressUtil.getHashForSpec(db.getAddresses(0));
  }


}
