package channels;

import duckutil.ConfigMem;
import java.util.TreeMap;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.client.WalletUtil;
import snowblossom.lib.NetworkParamsProd;
import snowblossom.proto.WalletDatabase;

public class TestUtil
{
  public static WalletDatabase genWallet()
  { 
    TreeMap<String,String> config_map = new TreeMap<>();
    config_map.put("key_count", "1");
    WalletDatabase db = WalletUtil.makeNewDatabase(new ConfigMem(config_map), new NetworkParamsProd());
    return db;
  }

}
