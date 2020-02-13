package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.File;
import java.util.Random;
import java.util.TreeMap;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.lib.Globals;

public class ChannelTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();


  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

	@Test
  public void testChannelDB()
    throws Exception
  {
    ChannelNode node_a = startNode();

    Random rnd = new Random();
    byte[] buff = new byte[20];
    for(int i=0; i<100; i++)
    {
      rnd.nextBytes(buff);
      ChannelID cid = new ChannelID(ByteString.copyFrom(buff));
      node_a.getChannelDB(cid);
    }

    // Give the DB a few seconds to settle
    Thread.sleep(5000);

  }




  private ChannelNode startNode()
    throws Exception
  {
    File base_dir = test_folder.newFolder();
    TreeMap<String,String> map = new TreeMap<>();
    map.put("key_count", "1");
    map.put("db_path", new File(base_dir, "db").getPath());
    map.put("wallet_path", new File(base_dir, "wallet").getPath());

    Random rnd = new Random();
    int port = rnd.nextInt(30000) + 10240;
    map.put("port", "" + port);
    map.put("use_need_peers", "false");


    return new ChannelNode(new ConfigMem(map));

  }


}
