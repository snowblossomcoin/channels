package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.lib.*;
import snowblossom.proto.WalletDatabase;

public class ChannelDataTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();

  private ChannelNode node_a;
  private ChannelNode node_b;

  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  @Test
  public void testChannelData()
    throws Exception
  {
    ChannelNode node_a = startNode();

    WalletDatabase admin_db = TestUtil.genWallet();
    ChannelID chan_id = null;
		ChainHash prev_hash = null;

		ChannelContext ctx_a;

      
    TreeMap<String, ByteString> data_map = new TreeMap<>();

    { 
      ChannelSettings.Builder init_settings = ChannelSettings.newBuilder();
      init_settings.setDisplayName("data test peer channel");
      init_settings.addAdminSignerSpecHashes( getAddr(admin_db).getBytes() );

      SignedMessage signed_init_settings = ChannelSigUtil.signMessage(admin_db.getAddresses(0), admin_db.getKeys(0),
        SignedMessagePayload.newBuilder().setChannelSettings(init_settings.build()).build());

      chan_id = ChannelSigUtil.getChannelId(new ChainHash(signed_init_settings.getMessageId()));

      ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
      header.setInitialSettings(signed_init_settings);
      header.setBlockHeight(0);

      header.setVersion(1);
      header.setChannelId( chan_id.getBytes());
      header.setPrevBlockHash( ChainHash.ZERO_HASH.getBytes());
      header.setContentMerkle( ChainHash.ZERO_HASH.getBytes());
			

      ChannelBlock.Builder blk = ChannelBlock.newBuilder();
      blk.setSignedHeader( ChannelSigUtil.signMessage(admin_db.getAddresses(0), admin_db.getKeys(0),
        SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));
			
			ctx_a = node_a.getChannelSubscriber().openChannel(chan_id);
			prev_hash = new ChainHash(blk.getSignedHeader().getMessageId());
			ctx_a.block_ingestor.ingestBlock(blk.build());
    }

    for(int i=0; i<20; i++)
    { // next block
      ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
      header.setBlockHeight(1L + i);

      header.setVersion(1);
      header.setChannelId( chan_id.getBytes() );
      header.setPrevBlockHash( prev_hash.getBytes());

      ChannelBlock.Builder blk = ChannelBlock.newBuilder();
		
      LinkedList<ChainHash> merkle_list = new LinkedList<>();
      for(int j=0; j<20; j++)
      { 
        SignedMessage ci = randomDataContent(admin_db, data_map);
        blk.addContent(ci);
        merkle_list.add(new ChainHash(ci.getMessageId()));
      }
      header.setContentMerkle( DigestUtil.getMerkleRootForTxList(merkle_list).getBytes());

      blk.setSignedHeader( ChannelSigUtil.signMessage(admin_db.getAddresses(0), admin_db.getKeys(0),
        SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

			prev_hash = new ChainHash(blk.getSignedHeader().getMessageId());
			ctx_a.block_ingestor.ingestBlock(blk.build());

			for(String key : data_map.keySet())
			{
				Assert.assertEquals( data_map.get(key), ChanDataUtils.getData(ctx_a, key));
			}

    }

		Assert.assertEquals(20, ctx_a.block_ingestor.getHead().getHeader().getBlockHeight());

  }

	private ChannelNode startNode()
    throws Exception
	{
    File base_dir = test_folder.newFolder();
    TreeMap<String,String> map = new TreeMap<>();
    map.put("key_count", "1");
		map.put("db_separate", "true");
    map.put("db_path", new File(base_dir, "db").getPath());
    map.put("wallet_path", new File(base_dir, "wallet").getPath());

    Random rnd = new Random();
    int port = rnd.nextInt(30000) + 10240;
    map.put("port", "" + port);


    return new ChannelNode(new ConfigMem(map));

	}
  protected AddressSpecHash getAddr(WalletDatabase db)
  {
    return AddressUtil.getHashForSpec(db.getAddresses(0));
  }

  protected SignedMessage randomDataContent(WalletDatabase user, Map<String, ByteString> data_map)
    throws Exception
  {
    Random rnd = new Random();

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    ci.setMimeType("kelp");
    int len = rnd.nextInt(1000);
    byte b[] = new byte[len];
    rnd.nextBytes(b);

    ci.setContentLength(len);
    ci.setContentHash( ByteString.copyFrom(DigestUtil.getMD().digest(b)) );
    if (len < 25000)
    { 
      ci.setContent( ByteString.copyFrom(b));
    }

		for(int i=0;i<20; i++)
		{
      String key = "/chantestdata/" + rnd.nextLong();
      b = new byte[32];
      rnd.nextBytes(b);

      ci.putChanMapUpdates(key, ByteString.copyFrom(b));
      data_map.put(key, ByteString.copyFrom(b));

		}	

    WalletDatabase wdb = user;
    return ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0),
      SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

  }

}
