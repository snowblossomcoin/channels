package snowblossom.channels;

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

      ChannelID cid = ChannelID.fromStringWithNames(channel, node);
      loadWarden(node, warden, cid);

    }

  }

  public static void loadWarden(ChannelNode node, String warden, ChannelID cid)
    throws Exception
  {
    logger.info(String.format("Loading %s for %s",warden, cid));

    Class clazz = Class.forName(warden);
    Constructor<?> constructor = clazz.getConstructor(ChannelAccess.class);

    ChannelAccess ca = new ChannelAccess(node, node.getChannelSubscriber().openChannel(cid));

    BaseWarden ward = (BaseWarden) constructor.newInstance(ca);

  }


}
