package snowblossom.channels.warden;

import java.util.LinkedList;
import java.util.List;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelSigUtil;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.SignedMessage;

public class BasicChatWarden extends BaseWarden
{

  private boolean first_run=true;

  public BasicChatWarden(ChannelAccess channel_access)
  {
    super(channel_access);

  }

  /*@Override
  public long getPeriod()
  {
    return 20L * 1000L;
  }*/

  @Override
  public void periodicRun() throws Exception
  {
    if (first_run)
    {
      first_run=false;
      return;
    }
    logger.info("Meow");    
    List<SignedMessage> content_list = channel_access.getOutsiderByTime(100, true);

    LinkedList<SignedMessage> to_include = new LinkedList<>();

    for(SignedMessage sm : content_list)
    {
      ContentInfo ci = ChannelSigUtil.quickPayload(sm).getContentInfo();
      if (ci.getContentDataMapCount() == 0)
      if (ci.getChanMapUpdatesCount() == 0)
      if (ci.getMimeType().equals("text/chat"))
      {
        to_include.add(sm);
        logger.info("Message: " + ci);
      }
    }
    if (to_include.size() > 0)
    {
      channel_access.createBlockWithContent(to_include);
    }

  }

  @Override
  public void onContent(ChannelID cid, SignedMessage sm)
  {

  }
  
  @Override
  public void onBlock(ChannelID cid, ChannelBlock sm)
  {

  }
}
