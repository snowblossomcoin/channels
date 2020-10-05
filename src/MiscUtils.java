package snowblossom.channels;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import snowblossom.channels.proto.ContentInfo;

public class MiscUtils
{
  public static int getNumberOfChunks(ContentInfo ci)
  {
    int x = (int)(ci.getContentLength() / ChannelGlobals.CONTENT_DATA_BLOCK_SIZE);

    if (ci.getContentLength() % ChannelGlobals.CONTENT_DATA_BLOCK_SIZE != 0) x++;
    return x;
  }

  public static String printStackTrace(Throwable t)
  {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream pout = new PrintStream(bout);

    t.printStackTrace(pout);

    return new String(bout.toByteArray());
  }

  public static String getStringCodePoints(String in )
  {
    StringBuilder sb = new StringBuilder();
    for(int i=0; i<in.length(); i++)
    {
      sb.append(" " + in.charAt(i) +"/"+in.codePointAt(i));
    }
    return sb.toString();

  }
}
