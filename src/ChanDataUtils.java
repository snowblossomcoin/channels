package snowblossom.channels;

import com.google.protobuf.ByteString;
import snowblossom.channels.proto.ChannelBlockSummary;

public class ChanDataUtils
{
  public static ByteString getData(ChannelContext ctx, String key)
  {
    ChannelBlockSummary summary = ctx.block_ingestor.getHead();
    if (summary == null) return null;
    ByteString key_bs = ByteString.copyFrom(key.getBytes());
    
    return ctx.db.getDataTrie().getLeafData(summary.getDataRootHash(), key_bs);
  }

}
