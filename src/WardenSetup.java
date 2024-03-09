package snowblossom.channels;

import com.google.common.collect.ImmutableMap;
import duckutil.Config;
import duckutil.ConfigFile;
import duckutil.ConfigMem;
import java.lang.reflect.Constructor;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import snowblossom.channels.warden.BaseWarden;

public class WardenSetup
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  public static void setupFromConfig(ChannelNode node)
    throws Exception
  {
    if (node.getConfig().isSet("warden_map"))
    for(String p : node.getConfig().getList("warden_map"))
    {
      StringTokenizer stok = new StringTokenizer(p, "/");
      String warden = stok.nextToken();
      String channel = stok.nextToken();

      Config config = new ConfigMem(ImmutableMap.of());
      if (stok.hasMoreTokens())
      {
        String config_path = stok.nextToken();
        if (node.getConfig().isSet("warden_config_dir"))
        {
          config_path = node.getConfig().get("warden_config_dir") + "/" + config_path;
        }
        config = new ConfigFile(config_path);
      }

      ChannelID cid = ChannelID.fromStringWithNames(channel, node);
      loadWarden(node, warden, cid, config);

    }
    if (node.getConfig().getBoolean("support_need_peers"))
    {
      ChannelID cid = ChannelID.fromStringWithNames(ChannelGlobals.CHAN_NEED_PEERS, node);
      loadWarden(node, ChannelGlobals.CHAN_NEED_PEERS_WARDEN, cid, new ConfigMem(ImmutableMap.of()));
    }

  }

  public static void loadWarden(ChannelNode node, String warden, ChannelID cid, Config config)
    throws Exception
  {
    logger.info(String.format("Loading %s for %s",warden, cid));

    Class clazz = Class.forName(warden);
    Constructor<?> constructor = clazz.getConstructor(ChannelAccess.class, Config.class);

    ChannelAccess ca = new ChannelAccess(node, node.getChannelSubscriber().openChannel(cid));


    BaseWarden ward = (BaseWarden) constructor.newInstance(ca, config);

  }


}
