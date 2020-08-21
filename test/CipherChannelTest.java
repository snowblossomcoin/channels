package channels;


import duckutil.ConfigMem;
import java.io.File;
import java.io.FileOutputStream;
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
import com.google.common.collect.ImmutableList;


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


  @Test
  public void testCipherChannel()
    throws Exception
  {

    ChannelNode node_a = startNode("rocksdb", false);
    ChannelNode node_b = startNode("rocksdb", false);
    ChannelNode node_c = startNode("rocksdb", false);

		ChannelID cid = BlockGenUtils.createChannel(node_a, node_a.getUserWalletDB(), "cipher-test");

    ChannelAccess a_a = new ChannelAccess(node_a, node_a.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_b = new ChannelAccess(node_b, node_b.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_c = new ChannelAccess(node_c, node_c.getChannelSubscriber().openChannel(cid));

		ChannelContext ctx_a = node_a.getChannelSubscriber().openChannel(cid);
		ChannelContext ctx_b = node_b.getChannelSubscriber().openChannel(cid);
		ChannelContext ctx_c = node_c.getChannelSubscriber().openChannel(cid);

		Assert.assertNull(ChannelCipherUtils.getCommonKeyID(ctx_a));

		ChannelCipherUtils.establishCommonKey(node_a, ctx_a);

		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_a));

		ChannelCipherUtils.addKeys(node_a, ctx_a, ImmutableList.of(node_b.getUserWalletDB().getAddresses(0)));

		String key_id = ChannelCipherUtils.getCommonKeyID(ctx_a);


    for(int i=0; i<MAX_WAIT_SEC; i++)
    {
      System.out.println(String.format("Progress: t=%d, a %d %d b %d %d c %d %d",
        i,
        a_a.getHeight(), a_a.getMissingChunks(),
        a_b.getHeight(), a_b.getMissingChunks(),
        a_c.getHeight(), a_c.getMissingChunks()
      ));


      if (a_a.getHeight() == a_b.getHeight())
      if (a_a.getHeight() == a_c.getHeight())
      if (a_b.getMissingChunks() == 0)
      if (a_c.getMissingChunks() == 0)
      {
        break;
      }
      Thread.sleep(1000);

    }

		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_a));
		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_b));
		Assert.assertNotNull(ChannelCipherUtils.getCommonKeyID(ctx_c));

		Assert.assertNotNull(ChannelCipherUtils.getKeyFromChannel(ctx_a, key_id, node_a.getUserWalletDB().getKeys(0)));
		Assert.assertNotNull(ChannelCipherUtils.getKeyFromChannel(ctx_b, key_id, node_b.getUserWalletDB().getKeys(0)));
		Assert.assertNull(ChannelCipherUtils.getKeyFromChannel(ctx_c, key_id, node_c.getUserWalletDB().getKeys(0)));


	}

  private ChannelNode startNode(String db_type, boolean skip_seeds)
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

    return new ChannelNode(new ConfigMem(map));

  }

  



}

