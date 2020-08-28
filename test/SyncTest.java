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

public class SyncTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();

  private ChannelNode node_a;
  private ChannelNode node_b;
  
  public static final int FILES_TO_SYNC=1000;
  public static final int MAX_WAIT_SEC=25; // Problem partly fixes self at 30s mark sometimes

  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  @Test
  public void testLargeSync()
    throws Exception
  {

    ChannelNode node_a = startNode("rocksdb", false);
    ChannelNode node_b = startNode("rocksdb", false);
    ChannelNode node_c = startNode("rocksdb", false);
    
    Thread.sleep(500);

    ChannelID cid = BlockGenUtils.createChannel(node_a, node_a.getUserWalletDB(), "sync-test");

    ChannelAccess a_a = new ChannelAccess(node_a, node_a.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_b = new ChannelAccess(node_b, node_b.getChannelSubscriber().openChannel(cid));
    ChannelAccess a_c = new ChannelAccess(node_c, node_c.getChannelSubscriber().openChannel(cid));

    File file_dir = test_folder.newFolder();
    Random rnd = new Random();

    for(int i=0; i<FILES_TO_SYNC; i++)
    {
      byte[] buff = new byte[16];
      rnd.nextBytes(buff);

      File data_file = new File(file_dir, "v" + rnd.nextInt());
      FileOutputStream out = new FileOutputStream(data_file);
      out.write(buff);
      out.flush(); out.close();
    }

    a_a.createBlockForFiles(file_dir);

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


    Assert.assertEquals(a_c.getHeight(), a_a.getHeight());
    Assert.assertEquals(a_b.getHeight(), a_a.getHeight());
    Assert.assertEquals(0, a_a.getMissingChunks());
    Assert.assertEquals(0, a_b.getMissingChunks());
    Assert.assertEquals(0, a_c.getMissingChunks());

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
