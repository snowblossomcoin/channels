package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.io.File;
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

public class DHTTest
{
  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();


  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  @Test
  public void testDHTReadWrite()
    throws Exception
  {

    // if B is working, but C is not, likely there is something wrong with the multicast discovery

    ChannelNode node_a = startNode("rocksdb", false);
    ChannelNode node_b = startNode("rocksdb", false);
    ChannelNode node_c = startNode("rocksdb", true);


    Thread.sleep(500);

    // Note: if this is failing, we are probably unable to get in touch with a seed node
    // to join the network
    Assert.assertTrue(node_a.getPeerManager().getPeersWithReason("DHT").size() > 0);
    Assert.assertTrue(node_b.getPeerManager().getPeersWithReason("DHT").size() > 0);

    // C is going to take a bit longer
    // Assert.assertTrue(node_c.getPeerManager().getPeersWithReason("DHT").size() > 0);

    for(int i=0; i<45; i++)
    {
      Thread.sleep(1000);
      if (node_a.getPeerManager().getPeersWithReason("DHT").size() >= 3)
      if (node_b.getPeerManager().getPeersWithReason("DHT").size() >= 3)
      if (node_c.getPeerManager().getPeersWithReason("DHT").size() >= 3)
      {
        break;
      }
    }

    Assert.assertTrue(node_a.getPeerManager().getPeersWithReason("DHT").size() >= 3);
    Assert.assertTrue(node_b.getPeerManager().getPeersWithReason("DHT").size() >= 3);
    Assert.assertTrue("Likely a multicast problem", node_c.getPeerManager().getPeersWithReason("DHT").size() >= 3);
    

    Random rnd = new Random();
    byte[] id_bytes = new byte[32];
   
    int b_match = 0;
    int c_match = 0;
    for(int i=0; i<100; i++)
    {
      ChannelPeerInfo my_info = node_a.getNetworkExaminer().createPeerInfo();

      rnd.nextBytes(id_bytes);
      ByteString id = ByteString.copyFrom(id_bytes);


      SignedMessage sm = node_a.signMessageNode(SignedMessagePayload.newBuilder()
        .setDhtData( DHTData.newBuilder().setElementId(id).setPeerInfo(my_info).build() )
        .build());

      try
      {

        DHTDataSet ds_put = node_a.getDHTServer().storeDHTData(
          StoreDHTRequest.newBuilder()
            .setDesiredResultCount(16)
            .setSignedDhtData(sm)
          .build());

        DHTDataSet ds_get_a = node_a.getDHTServer().getDHTData(
          GetDHTRequest.newBuilder()
            .setDesiredResultCount(16)
            .setElementId(id)
          .build());

        DHTDataSet ds_get_b = node_b.getDHTServer().getDHTData(
          GetDHTRequest.newBuilder()
            .setDesiredResultCount(16)
            .setElementId(id)
          .build());

        DHTDataSet ds_get_c = node_c.getDHTServer().getDHTData(
          GetDHTRequest.newBuilder()
            .setDesiredResultCount(16)
            .setElementId(id)
          .build());


        Assert.assertTrue(ds_put.getDhtDataCount() == 1);
        Assert.assertTrue(ds_get_a.getDhtDataCount() == 1);
        if (ds_get_b.getDhtDataCount() > 0) b_match++;
        if (ds_get_c.getDhtDataCount() > 0) c_match++;

      }
      catch(Throwable t)
      {
        System.out.println("Error in put and get: " + t);
      }

    }
    System.out.println("B match: " + b_match);
    Assert.assertTrue(b_match > 60);
  
    System.out.println("C match: " + c_match);
    Assert.assertTrue(c_match > 60);
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
    int port = rnd.nextInt(30000) + 10240;
    map.put("port", "" + port);
    map.put("use_need_peers", "false");


    return new ChannelNode(new ConfigMem(map));

	}

}
