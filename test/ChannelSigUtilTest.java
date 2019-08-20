package channels;

import com.google.protobuf.ByteString;
import duckutil.ConfigMem;
import java.util.Random;
import java.util.TreeMap;
import org.junit.BeforeClass;
import org.junit.Test;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.client.WalletUtil;
import snowblossom.lib.*;
import snowblossom.lib.Globals;
import snowblossom.proto.WalletDatabase;

public class ChannelSigUtilTest
{

  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  @Test
  public void testSignAndVerify()
    throws Exception
  {
    TreeMap<String,String> config_map = new TreeMap<>();
    config_map.put("key_count", "1");

    WalletDatabase db = WalletUtil.makeNewDatabase(new ConfigMem(config_map), new NetworkParamsProd());

    SignedMessagePayload.Builder starting_payload = SignedMessagePayload.newBuilder();

    byte[] data=new byte[1048576];
    Random rnd = new Random();
    rnd.nextBytes(data);

    starting_payload.setPeerInfo( ChannelPeerInfo.newBuilder().setAddressSpecHash(ByteString.copyFrom(data)));

    SignedMessage sm = ChannelSigUtil.signMessage( db.getAddresses(0), db.getKeys(0), starting_payload.build());

    ChannelSigUtil.validateSignedMessage(sm);

  }

}
