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
import java.util.TreeMap;
import snowblossom.proto.WalletDatabase;
import snowblossom.client.WalletUtil;
import snowblossom.lib.NetworkParamsProd;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import java.util.ArrayList;
import java.util.LinkedList;
import snowblossom.lib.DigestUtil;

public class ChannelValidationTest
{

  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

	@Test
  public void testChannelValidateGood()
    throws Exception
  {
    WalletDatabase admin_db = genWallet();
    WalletDatabase block_db = genWallet();
    WalletDatabase block_db2 = genWallet();

    ArrayList<WalletDatabase> user_list = new ArrayList<>();
    for(int i=0; i<10; i++) user_list.add(genWallet());

    // Generate initial block

    ChannelBlockSummary sum = null;
    ChannelID chan_id = null;

    // Inital block
    {
      ChannelSettings.Builder init_settings = ChannelSettings.newBuilder();
      init_settings.setDisplayName("test good channel");
      init_settings.addBlockSignerSpecHashes( getAddr(block_db).getBytes() );
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
      blk.setSignedHeader( ChannelSigUtil.signMessage(block_db.getAddresses(0), block_db.getKeys(0),
        SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));


      sum = ChannelValidation.deepBlockValidation(chan_id, blk.build(), null);

    }

    for(int i=0; i<20; i++)
    { // next block
      ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
      header.setBlockHeight(1L + i);

      header.setVersion(1);
      header.setChannelId( sum.getHeader().getChannelId());
      header.setPrevBlockHash( sum.getBlockId());

      ChannelBlock.Builder blk = ChannelBlock.newBuilder();
      LinkedList<ChainHash> merkle_list = new LinkedList<>();
      for(int j=0; j<i*20; j++)
      {
        SignedMessage ci = randomContent(user_list);

        blk.addContent(ci);
        merkle_list.add(new ChainHash(ci.getMessageId()));
        
      }
      if (merkle_list.size() == 0)
      {
        header.setContentMerkle( ChainHash.ZERO_HASH.getBytes());
      }
      else
      {
        header.setContentMerkle( DigestUtil.getMerkleRootForTxList(merkle_list).getBytes());
      }
  
      if (i %2 == 0)
      {
        blk.setSignedHeader( ChannelSigUtil.signMessage(block_db.getAddresses(0), block_db.getKeys(0),
          SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));
      }
      else
      {

        blk.setSignedHeader( ChannelSigUtil.signMessage(admin_db.getAddresses(0), admin_db.getKeys(0),
          SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));
      }


      sum = ChannelValidation.deepBlockValidation(chan_id, blk.build(), sum);
    }
    
    // Set new settings
    {
      ChannelSettings.Builder settings = ChannelSettings.newBuilder();
      settings.setDisplayName("test good channel update");
      settings.addBlockSignerSpecHashes( getAddr(block_db).getBytes() );
      settings.addBlockSignerSpecHashes( getAddr(block_db2).getBytes() );
      settings.addAdminSignerSpecHashes( getAddr(admin_db).getBytes() );
      settings.setActive(true);
      
      ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
      header.setBlockHeight(sum.getHeader().getBlockHeight() + 1L);
      header.setSettings(settings.build());

      header.setVersion(1);
      header.setChannelId( sum.getHeader().getChannelId());
      header.setPrevBlockHash( sum.getBlockId());
      header.setContentMerkle( ChainHash.ZERO_HASH.getBytes());


      ChannelBlock.Builder blk = ChannelBlock.newBuilder();
      blk.setSignedHeader( ChannelSigUtil.signMessage(admin_db.getAddresses(0), admin_db.getKeys(0),
        SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

      sum = ChannelValidation.deepBlockValidation(chan_id, blk.build(), sum);

    }
    // try new settings
    {
      ChannelBlockHeader.Builder header = ChannelBlockHeader.newBuilder();
      header.setBlockHeight(sum.getHeader().getBlockHeight() + 1L);

      header.setVersion(1);
      header.setChannelId( sum.getHeader().getChannelId());
      header.setPrevBlockHash( sum.getBlockId());
      header.setContentMerkle( ChainHash.ZERO_HASH.getBytes());


      ChannelBlock.Builder blk = ChannelBlock.newBuilder();
      blk.setSignedHeader( ChannelSigUtil.signMessage(block_db2.getAddresses(0), block_db2.getKeys(0),
        SignedMessagePayload.newBuilder().setChannelBlockHeader(header.build()).build()));

      sum = ChannelValidation.deepBlockValidation(chan_id, blk.build(), sum);

    }

    System.out.println(sum);

  }


  protected WalletDatabase genWallet()
  {
    TreeMap<String,String> config_map = new TreeMap<>();
    config_map.put("key_count", "1");
    WalletDatabase db = WalletUtil.makeNewDatabase(new ConfigMem(config_map), new NetworkParamsProd());
    return db;
  }

  protected AddressSpecHash getAddr(WalletDatabase db)
  {
    return AddressUtil.getHashForSpec(db.getAddresses(0));
  }

  protected SignedMessage randomContent(ArrayList<WalletDatabase> users)
    throws Exception
  {
    Random rnd = new Random();

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    ci.setMimeType("kelp");
    int len = rnd.nextInt(50000);
    byte b[] = new byte[len];
    rnd.nextBytes(b);

    ci.setContentLength(len);
    ci.setContentHash( ByteString.copyFrom(DigestUtil.getMD().digest(b)) );
    if (len < 25000)
    {
      ci.setContent( ByteString.copyFrom(b));
    }

    WalletDatabase wdb = users.get( rnd.nextInt(users.size() ));
    return ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0), 
      SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

  }


}
