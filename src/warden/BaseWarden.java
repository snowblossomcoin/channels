package snowblossom.channels.warden;

import duckutil.PeriodicThread;
import java.util.logging.Logger;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelWatcherInterface;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.SignedMessage;

public abstract class BaseWarden implements ChannelWatcherInterface
{
  protected static final Logger logger = Logger.getLogger("snowblossom.channels.warden");
  protected final ChannelAccess channel_access;

  public BaseWarden(ChannelAccess channel_access)
  {
    this.channel_access = channel_access;
    this.channel_access.watch(this);

    if (getPeriod() > 0)
    {
      new PeriodRunner().start();
    }
  }

  public class PeriodRunner extends PeriodicThread
  {
    public PeriodRunner()
    {
      super(getPeriod());
    }
    public void runPass() throws Exception
    {
      periodicRun();
    }
  }

  /**
   * Set to the period in ms that this task should be run, or zero to disable.
   */
  public long getPeriod()
  {
    return 120L*1000L;
  }

  /**
   * Extend this is this warden needs to run something periodically even if there are no updates
   */
  public void periodicRun() throws Exception
  {

  }

  public void onContent(ChannelID cid, SignedMessage sm)
  {

  }
  
  public void onBlock(ChannelID cid, ChannelBlock sm)
  {

  }
}
