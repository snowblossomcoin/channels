package channels;

import duckutil.ConfigMem;

import snowblossom.lib.*;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import java.util.TreeMap;


import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Random;
import com.google.protobuf.ByteString;


public class DHTTest
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
  public void testDHTReadWrite()
    throws Exception
  {
    ChannelNode node_a = startNode();
    ChannelNode node_b = startNode();

    Thread.sleep(500);

    Assert.assertTrue(node_a.getPeerManager().getPeersWithReason("DHT").size() > 0);
    Assert.assertTrue(node_b.getPeerManager().getPeersWithReason("DHT").size() > 0);

    Thread.sleep(45000);

    //Assert.assertTrue(node_a.getPeerManager().getPeersWithReason("DHT").size() > 2);
    //Assert.assertTrue(node_b.getPeerManager().getPeersWithReason("DHT").size() > 2);

    Random rnd = new Random();
    byte[] id_bytes = new byte[16];

   
    int b_match = 0;
    for(int i=0; i<1000; i++)
    {
      ChannelPeerInfo my_info = node_a.getNetworkExaminer().createPeerInfo();

      rnd.nextBytes(id_bytes);
      ByteString id = ByteString.copyFrom(id_bytes);


      SignedMessage sm = node_a.signMessage(SignedMessagePayload.newBuilder()
        .setDhtData( DHTData.newBuilder().setElementId(id).setPeerInfo(my_info).build() )
        .build());

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

      Assert.assertTrue(ds_put.getDhtDataCount() == 1);
      Assert.assertTrue(ds_get_a.getDhtDataCount() == 1);
      if (ds_get_b.getDhtDataCount() > 0) b_match++;

    }
    System.out.println("B match: " + b_match);
    Assert.assertTrue(b_match > 700);
  
  }

	private ChannelNode startNode()
    throws Exception
	{
    File base_dir = test_folder.newFolder();
    TreeMap<String,String> map = new TreeMap<>();
    map.put("key_count", "1");
    map.put("db_separate","true");
    map.put("db_path", new File(base_dir, "db").getPath());
    map.put("wallet_path", new File(base_dir, "wallet").getPath());

    Random rnd = new Random();
    int port = rnd.nextInt(30000) + 10240;
    map.put("port", "" + port);


    return new ChannelNode(new ConfigMem(map));

	}

}
