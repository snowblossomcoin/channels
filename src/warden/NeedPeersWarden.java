package snowblossom.channels.warden;

import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelSigUtil;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.ContentReference;
import snowblossom.channels.proto.SignedMessage;

public class NeedPeersWarden extends BaseWarden
{
  

  public NeedPeersWarden(ChannelAccess channel_access)
  {
    super(channel_access);
    logger.info("Started Need Peers Warden");
  }

  @Override
  public long getPeriod() {return 0L;}

  @Override
  public void onContent(ChannelID cid, SignedMessage sm)
  {
    ContentInfo ci = ChannelSigUtil.quickPayload(sm).getContentInfo();
    ContentReference cr = ci.getParentRef();
    if (cr == null) return;

    ChannelID new_cid = new ChannelID(cr.getChannelId());
    logger.info("Joining channel from need peers channel: " + new_cid);

    channel_access.openOtherChannel(new_cid);


  }


}
