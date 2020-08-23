package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

public class WebServerTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();

  private ChannelNode node_a;

  private int webport;

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

    for(int i=0; i<25; i++)
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
        SignedMessage ci = randomContent(admin_db, large_chunks, (j!=7));
        blk.addContent(ci);
        merkle_list.add(new ChainHash(ci.getMessageId()));
      }
      {
        SignedMessage ci = webDataMsg(admin_db, blk);
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
      for(SignedMessage sm : blk.getContentList())
      {
        ContentInfo ci = ChannelSigUtil.quickPayload(sm).getContentInfo();
        if (ci.getContentLength() > 0)
        {
          System.out.println(String.format("Downloading %s - expecting %d ",  new ChainHash(sm.getMessageId()), ci.getContentLength()));

          ByteArrayOutputStream bout = new ByteArrayOutputStream();
          String url = "http://localhost:" + webport + "/channel/" + chan_id + "/" + new ChainHash(sm.getMessageId());

          ChainHash hash = downloadAndDigest(url, bout);
          ChainHash expected_hash = new ChainHash(ci.getContentHash());

          

          if (!expected_hash.equals(hash))
          {
            byte[] d = bout.toByteArray();
            System.out.println("Mismatch.  Found data size: " + d.length + " expected: " + ci.getContentLength());
            Thread.sleep(2500);
          }

          Assert.assertEquals(expected_hash, hash);
        }
      }
    }

    Assert.assertEquals(0, ChunkMapUtils.getWantList(ctx_a).size());
		Assert.assertEquals(25, ctx_a.block_ingestor.getHead().getHeader().getBlockHeight());

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
    
    webport = rnd.nextInt(30000) + 10240;
    map.put("web_port", "" + webport);
    map.put("use_need_peers", "false");

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
    ci.setMimeType("application/gzip");
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


  protected SignedMessage webDataMsg(WalletDatabase user, ChannelBlock.Builder blk)
    throws Exception
  {

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    ci.setMimeType("kelp");
    int len = 0;
    byte b[] = new byte[len];

    ci.setContentLength(len);
    ci.setContentHash( ByteString.copyFrom(DigestUtil.getMD().digest(b)) );
    ci.setContent( ByteString.copyFrom(b));

		for(SignedMessage sm : blk.getContentList())
		{
      ContentInfo f = ChannelSigUtil.quickPayload(sm).getContentInfo();
      ci.putChanMapUpdates( "/web/" + new ChainHash(sm.getMessageId()), sm.getMessageId());

		}

    WalletDatabase wdb = user;
    return ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0),
      SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

  }

  protected ChainHash downloadAndDigest(String url, OutputStream save_out)
    throws Exception
  {

    long start_t = System.currentTimeMillis();
    System.out.println(url);
    URL u = new URL(url);
    HttpURLConnection conn = (HttpURLConnection) u.openConnection();



    InputStream in = conn.getInputStream();

    MessageDigest md = DigestUtil.getMD();
    byte[] b = new byte[8192];
    long sz =0;

    while(true)
    {
      int r = in.read(b);
      if (r < 0) break;
      if (r > 0)
      {
        md.update(b,0,r);
        save_out.write(b,0,r);
        sz+=r;
      }
    }
    in.close();
    long delta_t = System.currentTimeMillis() - start_t;
    System.out.println("Read " + url + " - " + sz + " code: " + conn. getResponseCode() + " in ms:" + delta_t);
    System.out.println("Timeout: " + conn.getReadTimeout());


    return new ChainHash(md.digest());

  }

}
