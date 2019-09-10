package snowblossom.channels;

import snowblossom.proto.WalletDatabase;
import java.io.File;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.ChainHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.ValidationException;
import java.util.LinkedList;
import com.google.protobuf.ByteString;

public class BlockGenUtils
{
  /** Creates a new channel and publishes block 0 for it */
  public static ChannelID createChannel(ChannelNode node, WalletDatabase admin, String display_name)
		throws ValidationException
  {
    ChannelSettings.Builder init_settings = ChannelSettings.newBuilder();
    init_settings.setDisplayName(display_name);
    init_settings.addAdminSignerSpecHashes( getAddr(admin).getBytes() );
    
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
   * Creates a block for the files in the directory and broadcasts it to the channel
   */ 
  public static void createBlockForFiles(ChannelContext ctx, File base_path, WalletDatabase admin)
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
    throws ValidationException
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

      // Save hash
      // save mime-type
      // save merkle list

      SignedMessage sm = 
        ChannelSigUtil.signMessage( sig.getAddresses(0), sig.getKeys(0),
          SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

      blk.addContent(sm);
      file_map_ci.putChanMapUpdates("/web" + prefix, sm.getMessageId());

      // save chunks
    }
  }

  protected static AddressSpecHash getAddr(WalletDatabase db)
  {
    return AddressUtil.getHashForSpec(db.getAddresses(0));
  }


}
