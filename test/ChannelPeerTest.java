package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

public class ChannelPeerTest
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
  public void testChannelPeerAndSync()
    throws Exception
  {
    ChannelNode node_a = startNode();
    ChannelNode node_b = startNode();

    Thread.sleep(500);

    // Note: if this is failing, we are probably unable to get in touch with a seed node
    // to join the network
    Assert.assertTrue(node_a.getPeerManager().getPeersWithReason("DHT").size() > 0);
    Assert.assertTrue(node_b.getPeerManager().getPeersWithReason("DHT").size() > 0);

    for(int i=0; i<45; i++)
    {
      Thread.sleep(1000);
      if (node_a.getPeerManager().getPeersWithReason("DHT").size() >= 3)
      if (node_b.getPeerManager().getPeersWithReason("DHT").size() >= 3)
      {
        break;
      }
    }

    Assert.assertTrue(node_a.getPeerManager().getPeersWithReason("DHT").size() >= 3);
    Assert.assertTrue(node_b.getPeerManager().getPeersWithReason("DHT").size() >= 3);

    WalletDatabase admin_db = TestUtil.genWallet();
    WalletDatabase user_db = TestUtil.genWallet();
    ChannelID chan_id = null;
		ChainHash prev_hash = null;

		ChannelContext ctx_a;
    ChannelContext ctx_b;


    { 
      ChannelSettings.Builder init_settings = ChannelSettings.newBuilder();
      init_settings.setDisplayName("test peer channel");
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
			Thread.sleep(500);
			ctx_b = node_b.getChannelSubscriber().openChannel(chan_id);

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
      LinkedList<ContentChunk> large_chunks = new LinkedList<>();
      for(int j=0; j<20; j++)
      {
        SignedMessage ci = randomContent(user_db, large_chunks, (j==5));
        blk.addContent(ci);
        merkle_list.add(new ChainHash(ci.getMessageId()));

      }
      header.setContentMerkle( DigestUtil.getMerkleRootForTxList(merkle_list).getBytes());

        blk.setSignedHeader( ChannelSigUtil.signMessage(admin_db.getAddresses(0), admin_db.getKeys(0),
          SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

			prev_hash = new ChainHash(blk.getSignedHeader().getMessageId());
			ctx_a.block_ingestor.ingestBlock(blk.build());

      for(ContentChunk c : large_chunks)
      {
        ctx_a.block_ingestor.ingestChunk(c);
      }

    }

		Assert.assertEquals(20, ctx_a.block_ingestor.getHead().getHeader().getBlockHeight());

    for(int i=0; i<100; i++)
    {
      Thread.sleep(100);
      if (ctx_a.getLinks().size() + ctx_b.getLinks().size() >= 2) break;
    }
		Assert.assertEquals(1, ctx_a.getLinks().size());
		Assert.assertEquals(1, ctx_b.getLinks().size());
    for(int i=0; i<25; i++)
    {
      Thread.sleep(100);
      if (ctx_b.block_ingestor.getHead() != null)
      if (ctx_b.block_ingestor.getHead().getHeader().getBlockHeight() == 20) break;
    }

		Assert.assertEquals(20, ctx_b.block_ingestor.getHead().getHeader().getBlockHeight());
    
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

  protected SignedMessage randomContent(WalletDatabase wdb, List<ContentChunk> large_chunks, boolean large)
    throws Exception
  { 
    Random rnd = new Random();

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    ci.setMimeType("kelp");
    int len = rnd.nextInt(25000);

    if (large)
    {
      len = rnd.nextInt(2000000)+(int)ChannelGlobals.CONTENT_DATA_BLOCK_SIZE;
    }
    byte b[] = new byte[len];
    rnd.nextBytes(b);

    ByteString data = ByteString.copyFrom(b);

    ci.setContentLength(len);
    ci.setContentHash( ByteString.copyFrom(DigestUtil.getMD().digest(b)) );

    ArrayList<ByteString> chunks = new ArrayList<>();

    if (len < 10000)
    { 
      ci.setContent( data);
    }
		else
    {
      MessageDigest md = DigestUtil.getMD();
      for(int chunk = 0; chunk*ChannelGlobals.CONTENT_DATA_BLOCK_SIZE < len; chunk++)
      {
        int idx = (int) (chunk * ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);
        int end = (int) (Math.min(idx + ChannelGlobals.CONTENT_DATA_BLOCK_SIZE, len));

        ByteString chunk_data = data.substring(idx, end);

        ci.addChunkHash( ByteString.copyFrom(md.digest(chunk_data.toByteArray())));

        chunks.add(chunk_data);

      }
    
    }

    SignedMessage sm = ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0),
      SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

    for(int i=0; i<chunks.size(); i++)
    {
      large_chunks.add( ContentChunk.newBuilder().setMessageId(sm.getMessageId()).setChunk(i).setChunkData(chunks.get(i)).build());
    }

    return sm;
  }



}
