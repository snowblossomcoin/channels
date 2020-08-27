package channels;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
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
import snowblossom.proto.WalletKeyPair;
import snowblossom.proto.AddressSpec;
import java.io.PrintStream;

import snowblossom.channels.proto.EncryptedChannelConfig;
import com.google.protobuf.util.JsonFormat;

public class CipherChannelTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();


  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  public static final int MAX_WAIT_SEC=25; 
  public static final int FILES_TO_SYNC=25;
  public static final int MAX_FILE_SIZE=4000000;

  



  @Test
  public void testCipherChannel()
    throws Exception
  {
    Random rnd = new Random();
    int webport_a = 20000+rnd.nextInt(10000);
    int webport_b = 20000+rnd.nextInt(10000);
    int webport_c = 20000+rnd.nextInt(10000);
    int webport_d = 20000+rnd.nextInt(10000);

    ChannelNode node_a = startNode("rocksdb", false, webport_a);
    ChannelNode node_b = startNode("rocksdb", false, webport_b);
    ChannelNode node_c = startNode("rocksdb", false, webport_c);
    ChannelNode node_d = startNode("rocksdb", false, webport_d);

		ChannelID cid = BlockGenUtils.createChannel(node_a, node_a.getUserWalletDB(), "cipher-test");

    ChannelAccess a_a = new ChannelAccess(node_a, node_a.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_b = new ChannelAccess(node_b, node_b.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_c = new ChannelAccess(node_c, node_c.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_d = new ChannelAccess(node_d, node_d.getChannelSubscriber().openChannel(cid));

		ChannelContext ctx_a = node_a.getChannelSubscriber().openChannel(cid);
		ChannelContext ctx_b = node_b.getChannelSubscriber().openChannel(cid);
		ChannelContext ctx_c = node_c.getChannelSubscriber().openChannel(cid);
		ChannelContext ctx_d = node_c.getChannelSubscriber().openChannel(cid);
    
    File file_dir = test_folder.newFolder();

    {
	    JsonFormat.Printer printer = JsonFormat.printer();
  	  EncryptedChannelConfig.Builder conf = EncryptedChannelConfig.newBuilder();
    	conf.setProtectedPath("/prot/");
			
			PrintStream file_out = new PrintStream(new FileOutputStream(new File(file_dir, "encryption.json")));

			file_out.println(printer.print(conf.build()));
			file_out.close();
    }


		Assert.assertNull(ChannelCipherUtils.getCommonKeyID(ctx_a));

    a_a.createBlockForFiles(file_dir);
		//ChannelCipherUtils.establishCommonKey(node_a, ctx_a);

		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_a));

		ChannelCipherUtils.addKeys(node_a, ctx_a, ImmutableList.of(node_b.getUserWalletDB().getAddresses(0)));

    WalletKeyPair node_d_key = ChannelCipherUtils.getKeyForChannel(cid, node_d.getUserWalletDB());
    AddressSpec spec_d = AddressUtil.getSimpleSpecForKey(node_d_key);
		ChannelCipherUtils.addKeys(node_a, ctx_a, ImmutableList.of(spec_d));

		String key_id = ChannelCipherUtils.getCommonKeyID(ctx_a);


    TreeMap<String, ChainHash> plain_file_map = new TreeMap<>();
    TreeMap<String, ChainHash> prot_file_map = new TreeMap<>();

    for(int i=0; i<FILES_TO_SYNC; i++)
    {
			int sz = rnd.nextInt(MAX_FILE_SIZE);
      byte[] buff = new byte[sz];
      rnd.nextBytes(buff);

      String name = "r" + rnd.nextLong();

      ChainHash hash = new ChainHash(DigestUtil.hash( ByteString.copyFrom(buff) ));

      plain_file_map.put(name, hash);

      File data_file = new File(file_dir, name);
      FileOutputStream out = new FileOutputStream(data_file);
      out.write(buff);
      out.flush(); 
      out.close();
    }

    File enc_dir = new File(file_dir, "prot");
    enc_dir.mkdirs();

    for(int i=0; i<FILES_TO_SYNC; i++)
    {
			int sz = rnd.nextInt(MAX_FILE_SIZE);
      byte[] buff = new byte[sz];
      rnd.nextBytes(buff);
      
      String name = "r" + rnd.nextLong();
      ChainHash hash = new ChainHash(DigestUtil.hash( ByteString.copyFrom(buff) ));

      prot_file_map.put(name, hash);

      File data_file = new File(enc_dir, name);
      FileOutputStream out = new FileOutputStream(data_file);
      out.write(buff);
      out.flush();
      out.close();
    }

    a_a.createBlockForFiles(file_dir);

    for(int i=0; i<MAX_WAIT_SEC; i++)
    {
      System.out.println(String.format("Progress: t=%d, a %d %d b %d %d c %d %d d %d %d",
        i,
        a_a.getHeight(), a_a.getMissingChunks(),
        a_b.getHeight(), a_b.getMissingChunks(),
        a_c.getHeight(), a_c.getMissingChunks(),
        a_d.getHeight(), a_d.getMissingChunks()
      ));

      if (a_a.getHeight() == a_b.getHeight())
      if (a_a.getHeight() == a_c.getHeight())
      if (a_a.getHeight() == a_d.getHeight())
      if (a_b.getMissingChunks() == 0)
      if (a_c.getMissingChunks() == 0)
      if (a_d.getMissingChunks() == 0)
      {
        break;
      }
      Thread.sleep(1000);
    }

		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_a));
		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_b));
		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_c));
		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_d));

		Assert.assertNotNull(ChannelCipherUtils.getKeyFromChannel(ctx_a, key_id, node_a.getUserWalletDB().getKeys(0)));
		Assert.assertNotNull(ChannelCipherUtils.getKeyFromChannel(ctx_b, key_id, node_b.getUserWalletDB().getKeys(0)));
		Assert.assertNull(ChannelCipherUtils.getKeyFromChannel(ctx_c, key_id, node_c.getUserWalletDB().getKeys(0)));
		Assert.assertNull(ChannelCipherUtils.getKeyFromChannel(ctx_d, key_id, node_d.getUserWalletDB().getKeys(0)));

    for(String name : plain_file_map.keySet())
    {
      Assert.assertEquals( plain_file_map.get(name), download(webport_a, cid, name));
      Assert.assertEquals( plain_file_map.get(name), download(webport_b, cid, name));
      Assert.assertEquals( plain_file_map.get(name), download(webport_c, cid, name));
      Assert.assertEquals( plain_file_map.get(name), download(webport_d, cid, name));
    }

    for(String name : prot_file_map.keySet())
    {
      ChainHash hash = prot_file_map.get(name);
      Assert.assertEquals( hash, download(webport_a, cid, "prot/" + name));
      Assert.assertEquals( hash, download(webport_b, cid, "prot/" + name));
      //Assert.assertEquals( hash, download(webport_c, cid, "prot/" + name));
      Assert.assertEquals( hash, download(webport_d, cid, "prot/" + name));
    }

	}

  private ChannelNode startNode(String db_type, boolean skip_seeds, int webport)
    throws Exception
  {
    File base_dir = test_folder.newFolder();
    TreeMap<String,String> map = new TreeMap<>();
    map.put("key_count", "1");
    map.put("db_separate", "true");
    map.put("db_path", new File(base_dir, "db").getPath());
    map.put("wallet_path", new File(base_dir, "wallet").getPath());
    map.put("db_type", db_type);
    if (skip_seeds)
    {
      map.put("testing_skip_seeds", "true");
    }

    Random rnd = new Random();
    int port = rnd.nextInt(50000) + 1024;
    map.put("port", "" + port);
    map.put("use_need_peers", "false");
    map.put("web_port", "" + webport);

    return new ChannelNode(new ConfigMem(map));

  }

  protected ChainHash download(int webport, ChannelID cid, String filename)
    throws Exception
  {

		String url = "http://localhost:" + webport + "/channel/" + cid + "/" + filename;
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

