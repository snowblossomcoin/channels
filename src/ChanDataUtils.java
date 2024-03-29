package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import snowblossom.channels.proto.ChannelBlockSummary;

public class ChanDataUtils
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  public static ByteString getData(ChannelContext ctx, String key)
  {
    try
    {
    ByteString key_bs = ByteString.copyFrom(key.getBytes("UTF-8"));
    ChannelBlockSummary summary = ctx.block_ingestor.getHead();
    if (summary == null) return null;

    return ctx.db.getDataTrie().getLeafData(summary.getDataRootHash(), key_bs);
    }
    catch(java.io.UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Map<String,ByteString> getAllData(ChannelContext ctx, String base_key)
  {
    TreeMap<String, ByteString> result_map = new TreeMap<>();

    ChannelBlockSummary summary = ctx.block_ingestor.getHead();
    if (summary == null)
    {
      logger.info("No head for channel to get data from");
    }
    else
    {
      try
      {
        TreeMap<ByteString, ByteString> m = ctx.db.getDataTrie().getDataMap(summary.getDataRootHash(), ByteString.copyFrom(base_key.getBytes("UTF-8")), 1000000);

        for(Map.Entry<ByteString, ByteString> me : m.entrySet())
        {
          String k = new String(me.getKey().toByteArray());
          result_map.put(k, me.getValue());
        }
      }
      catch(java.io.UnsupportedEncodingException e)
      {
        throw new RuntimeException(e);
      }
    }
    return result_map;
  }

}
