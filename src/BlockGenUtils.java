package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.CipherUtil;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.Globals;
import snowblossom.lib.ValidationException;
import snowblossom.node.StatusInterface;
import snowblossom.proto.WalletDatabase;

public class BlockGenUtils
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

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

  public static void createBlockForSettings(ChannelContext ctx, ChannelSettings settings, WalletDatabase admin)
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

    header.setSettings(settings);

    ChannelBlock.Builder blk = ChannelBlock.newBuilder();

		header.setContentMerkle( ChainHash.ZERO_HASH.getBytes() );
    
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

  //public static void createBlockForFiles(ChannelContext ctx, File base_path, WalletDatabase admin, StatusInterface status)
  //  throws ValidationException, java.io.IOException
  public static void createBlockForFiles(FileBlockImportSettings settings)
    throws ValidationException, java.io.IOException
  {
    ProcessStatus ps = new ProcessStatus();
    ps.set("cid", settings.ctx.cid.toString());
    ps.set("path", settings.base_path.toString());

    TreeSet<String> done_set = new TreeSet<>();
    while(true)
    {
      settings.status.setStatus("Importing files: " + ps.getStatusLine());
      if (createSingleBlockForFiles(settings, ps, done_set)) break;
    }
    // TODO - prune any existing files that not in the done_set

    settings.status.setStatus("Import complete: " + ps.getStatusLine());
  }

  /**
   * Creates a block for the files in the directory and broadcasts it to the channel
   * @return true if all files fit in one block, false if there are blocks to write
   */ 
  public static boolean createSingleBlockForFiles(FileBlockImportSettings settings, ProcessStatus ps, TreeSet<String> done_set)
    throws ValidationException, java.io.IOException
  {
    ChannelBlockSummary prev_sum = settings.ctx.block_ingestor.getHead();
    if (prev_sum == null)
    {
      throw new ValidationException("Unknown previous block");
    }
    ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
    header.setBlockHeight(1L + prev_sum.getHeader().getBlockHeight());

    header.setVersion(1);
    header.setChannelId( settings.ctx.cid.getBytes() );
    header.setPrevBlockHash( prev_sum.getBlockId());

    ChannelBlock.Builder blk = ChannelBlock.newBuilder();

    ContentInfo.Builder file_map_ci = ContentInfo.newBuilder();
    file_map_ci.setContentHash( DigestUtil.hash(ByteString.EMPTY) );

    boolean all_fit = addFiles(settings, settings.base_path, "", blk, file_map_ci, ps, done_set);

    blk.addContent(
      ChannelSigUtil.signMessage( settings.signer.getAddresses(0), settings.signer.getKeys(0),
          SignedMessagePayload.newBuilder().setContentInfo(file_map_ci.build()).build()));

    LinkedList<ChainHash> merkle_list = new LinkedList<>();
    for(SignedMessage sm : blk.getContentList())
    {
      merkle_list.add(new ChainHash(sm.getMessageId()));
    }

    header.setContentMerkle( DigestUtil.getMerkleRootForTxList(merkle_list).getBytes());
    
    blk.setSignedHeader( ChannelSigUtil.signMessage(settings.signer.getAddresses(0), settings.signer.getKeys(0),
      SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

    ps.add("blocks_added");
    settings.ctx.block_ingestor.ingestBlock(blk.build());

    return all_fit;
  }

  
  /**
   * @return true iff we were able to add all files
   */
  private static boolean addFiles(FileBlockImportSettings settings, File path,
    String prefix, ChannelBlock.Builder blk, ContentInfo.Builder file_map_ci,
    ProcessStatus ps, TreeSet<String> done_set)
    throws ValidationException, java.io.IOException
  {
    if (done_set.contains(prefix)) return true;

    if (blk.build().toByteString().size() + file_map_ci.build().toByteString().size() > Globals.MAX_BLOCK_SIZE*3/4) return false;

    if (path.isDirectory())
    {
      settings.status.setStatus("Importing files: " + ps.getStatusLine());

      boolean has_index=false;
      for(File f : path.listFiles())
      {
        // TODO - if we really want to murder some drives make this multithreaded
        boolean res = addFiles(settings, f, prefix + "/" + f.getName(), blk, file_map_ci, ps, done_set);

        if (f.getName().equals("index.html")) has_index=true;

        if (!res) return false;
      }
      // Make index maybe
      if (!has_index)
      {
        addIndexFile(settings, path, prefix + "/index.html" , blk, file_map_ci, ps);
      }

    }
    else
    {
      long len = path.length();
      ContentInfo.Builder ci = ContentInfo.newBuilder();
      ci.setContentLength(len);

      if (settings.encrypt(prefix))
      {
        ci.setEncryptionIvBase( ChannelCipherUtils.randomIv() );
        ci.setEncryptedKeyId( settings.getSymmetricKey().getKeyId() );
        ci.setPlainContentLength( len );
      }

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
      int chunk_no = 0;
      long output_len = 0;
      while (loc < len)
      {
        int read_len = (int) Math.min( len-loc, ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

        byte[] b = new byte[read_len];
        din.readFully(b);
        loc += read_len;

        if (settings.encrypt(prefix))
        {
          
          ByteString iv = ChannelCipherUtils.getIv( ci.getEncryptionIvBase(), chunk_no );
          ByteString encrypted_data = CipherUtil.encryptSymmetric(settings.getSymmetricKey(), ByteString.copyFrom(b) , iv);

          ByteString part_hash = ByteString.copyFrom(md_part.digest(encrypted_data.toByteArray()));
          ci.addChunkHash(part_hash);
          md_whole.update(encrypted_data.toByteArray());

          output_len += encrypted_data.size();

        }
        else
        {

          ByteString part_hash = ByteString.copyFrom(md_part.digest(b));
          ci.addChunkHash(part_hash);
          md_whole.update(b);
          output_len += read_len;
        }
        chunk_no++;
      }
      din.close();

      ci.setContentHash(ByteString.copyFrom(md_whole.digest()));
      ci.setContentLength(output_len);

      //check existing data
      //
      ByteString old_content_id = ChanDataUtils.getData(settings.ctx, "/web" + prefix);
      if (old_content_id != null)
      {
        SignedMessage old_content_msg = settings.ctx.db.getContentMap().get(old_content_id);
        if (old_content_msg != null)
        {
          ContentInfo old_ci = ChannelSigUtil.quickPayload(old_content_msg).getContentInfo();
          if(ci.getContentHash().equals(old_ci.getContentHash()))
          {
            done_set.add(prefix);
            ps.add("files_preexisting");

            return true; 
          }
        }
      }


      ChannelValidation.validateContent(ci.build(), DigestUtil.getMD());

      SignedMessage sm = 
        ChannelSigUtil.signMessage( settings.signer.getAddresses(0), settings.signer.getKeys(0),
          SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

      blk.addContent(sm);
      ps.add("files_saved");
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

        ByteString chunk_data = ByteString.copyFrom(b);

        if (settings.encrypt(prefix))
        {
          ByteString iv = ChannelCipherUtils.getIv( ci.getEncryptionIvBase(), chunk_count );
          ByteString encrypted_data = CipherUtil.encryptSymmetric(settings.getSymmetricKey(), chunk_data , iv);
          
          ByteString part_hash = ByteString.copyFrom(md_part.digest(encrypted_data.toByteArray()));

          chunk_data = encrypted_data;
        }
        


        settings.ctx.block_ingestor.ingestChunk(
          ContentChunk.newBuilder()
            .setMessageId(sm.getMessageId())
            .setChunk(chunk_count) 
            .setChunkData(chunk_data)
            .build()
          ,true, ci.build());
        ps.add("chunks_saved");
        ps.add("total_bytes_saved", read_len);
        settings.status.setStatus("Importing files: " + ps.getStatusLine());

        chunk_count++;
      }
      din.close();
    }

    done_set.add(prefix);
    return true;
  }

  private static void addIndexFile(FileBlockImportSettings settings, File path, 
    String prefix, ChannelBlock.Builder blk, ContentInfo.Builder file_map_ci, ProcessStatus ps)
    throws ValidationException, java.io.IOException
  {
    
    // TODO - do things
    String content = HtmlUtils.getIndex(path);
    byte[] content_bytes = content.getBytes();

    long len = content_bytes.length;

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    ci.setContentLength(len);

    ci.setMimeType("text/html");

    MessageDigest md_whole = DigestUtil.getMD();
    MessageDigest md_part = DigestUtil.getMD();
    DataInputStream din = new DataInputStream(new ByteArrayInputStream(content_bytes));
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
    ci.putContentDataMap("auto_gen_index",ByteString.copyFrom("true".getBytes()));


    //check existing data
    //
    ByteString old_content_id = ChanDataUtils.getData(settings.ctx, "/web" + prefix);
    if (old_content_id != null)
    {
      SignedMessage old_content_msg = settings.ctx.db.getContentMap().get(old_content_id);
      if (old_content_msg != null)
      {
        ContentInfo old_ci = ChannelSigUtil.quickPayload(old_content_msg).getContentInfo();
        if(ci.getContentHash().equals(old_ci.getContentHash()))
        {
          ps.add("index_preexisting");

          return; 
        }
      }
    }


    ChannelValidation.validateContent(ci.build(), DigestUtil.getMD());

    SignedMessage sm = 
      ChannelSigUtil.signMessage( settings.signer.getAddresses(0), settings.signer.getKeys(0),
        SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

    blk.addContent(sm);
    ps.add("index_saved");
    file_map_ci.putChanMapUpdates("/web" + prefix, sm.getMessageId());

    din = new DataInputStream(new ByteArrayInputStream(content_bytes));
    loc = 0;
    int chunk_count =0;
    while (loc < len)
    {
      int read_len = (int) Math.min( len-loc, ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

      byte[] b = new byte[read_len];
      din.readFully(b);
      loc += read_len;

      settings.ctx.block_ingestor.ingestChunk(
        ContentChunk.newBuilder()
          .setMessageId(sm.getMessageId())
          .setChunk(chunk_count) 
          .setChunkData(ByteString.copyFrom(b))
          .build()
        ,true, ci.build());
      ps.add("chunks_saved");
      ps.add("total_bytes_saved", read_len);
      settings.status.setStatus("Importing files: " + ps.getStatusLine());

      chunk_count++;
    }
    din.close();
    
  }
  protected static AddressSpecHash getAddr(WalletDatabase db)
  {
    return AddressUtil.getHashForSpec(db.getAddresses(0));
  }


}
