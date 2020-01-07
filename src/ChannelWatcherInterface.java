package snowblossom.channels;

import snowblossom.channels.proto.*;

public interface ChannelWatcherInterface
{

  public void onBlock(ChannelID cid, ChannelBlock blk);

  public void onContent(ChannelID cid, SignedMessage sm);

}
