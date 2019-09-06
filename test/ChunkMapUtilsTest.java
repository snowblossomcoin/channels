package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.File;
import java.util.BitSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.lib.*;

public class ChunkMapUtilsTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();


  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }


	@Test
	public void testChunkMap()
		throws Exception
  {
		ChannelNode node = startNode();
    Random rnd = new Random();

    byte[] b = new byte[20];
    rnd.nextBytes(b);

    ChannelID cid = new ChannelID( b );

    ChannelContext ctx = node.getChannelSubscriber().openChannel(cid);

    b = new byte[32]; rnd.nextBytes(b);

    ChainHash content_id = new ChainHash(b);

    TreeSet<Integer> added=new TreeSet<>();
    for(int i=0; i<100; i++)
    {
      int idx = 0;
      while(added.contains(idx))
      {
        idx = rnd.nextInt(100000);
      }
      added.add(idx);
      if (rnd.nextDouble() < 0.5)
      {
        b = new byte[1048576];
      }
      else
      {
        b = new byte[ rnd.nextInt(1048575) + 1];
      }
      rnd.nextBytes(b);

      ByteString input_data = ByteString.copyFrom(b);

      Assert.assertNull(ChunkMapUtils.getChunk(ctx, content_id, idx));
      ChunkMapUtils.storeChunk(ctx, content_id ,idx, input_data);

      Assert.assertEquals( input_data, ChunkMapUtils.getChunk(ctx, content_id, idx));
    }

    BitSet bs = ChunkMapUtils.getSavedChunksSet(ctx, content_id);

    for(int idx : added)
    {
      Assert.assertTrue( bs.get(idx) );
    }
    Assert.assertEquals(added.size(), bs.cardinality());

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

}
