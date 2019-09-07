package snowblossom.channels;

import snowblossom.channels.proto.ContentInfo;

public class MiscUtils
{
  public static int getNumberOfChunks(ContentInfo ci)
  {
    int x = (int)(ci.getContentLength() / ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

    if (ci.getContentLength() % ChannelGlobals.CONTENT_DATA_BLOCK_SIZE != 0) x++;
    return x;

  }

}
