package snowblossom.channels.iceleaf;

import snowblossom.lib.Globals;
import snowblossom.lib.NetworkParams;
import snowblossom.lib.NetworkParamsProd;
import snowblossom.iceleaf.IceLeaf;
import java.util.prefs.Preferences;
import snowblossom.channels.ChannelGlobals;

public class ChannelIceLeaf extends IceLeaf
{
  public static void main(String args[]) throws Exception
  {
    Globals.addCryptoProvider();
    new ChannelIceLeaf(new NetworkParamsProd(), null);
  }

  public ChannelIceLeaf(NetworkParams params, Preferences prefs)
		throws Exception
  {
    super(params, Preferences.userNodeForPackage(
      ClassLoader.getSystemClassLoader().loadClass("snowblossom.iceleaf.IceLeaf")));
  }

  @Override
  public String getTitle()
  {
    return "SnowBlossom Channels - IceLeaf " + ChannelGlobals.VERSION;
  }

  @Override
  public String getResourceBasePath()
  {
    return "/external/snowblossom";
  }

}
