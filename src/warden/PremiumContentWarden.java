package snowblossom.channels.warden;

import snowblossom.channels.ChannelAccess;

import snowblossom.util.proto.SymmetricKey;
import snowblossom.channels.ChannelID;
import snowblossom.channels.proto.SignedMessage;

import snowblossom.channels.proto.ChannelBlock;

public class PremiumContentWarden extends BaseWarden
{
  public PremiumContentWarden(ChannelAccess channel_access)
  {
    super(channel_access);

  }

	private SymmetricKey sym_key;

  @Override
  public void periodicRun() throws Exception
  {

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
