package snowblossom.channels;

import java.math.BigInteger;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.lib.BlockchainUtil;
import snowblossom.lib.ValidationException;

public class ChannelValidation
{
  public static void checkBlockBasics(ChannelBlock blk, boolean require_content)
    throws ValidationException
  {
   //TODO

  }

  public static ChannelBlockSummary deepBlockValidation(SingleChannelDB db, ChannelBlock blk, ChannelBlockSummary prev_summary)
    throws ValidationException
  {
    
     //TODO

     ChannelBlockHeader header = ChannelSigUtil.validateSignedMessage(blk.getSignedHeader()).getChannelBlockHeader();


     ChannelBlockSummary.Builder sum = ChannelBlockSummary.newBuilder();

     BigInteger prev_work_sum = BigInteger.ZERO;
     if (prev_summary != null)
     {
        prev_work_sum = BlockchainUtil.readInteger(prev_summary.getWorkSum());
     }

     BigInteger work_sum = prev_work_sum.add( BigInteger.valueOf( header.getWeight() + 1L ) );

     sum.setWorkSum(work_sum.toString());
     sum.setHeader(header);
     sum.setBlockId(blk.getSignedHeader().getMessageId());
    

     return sum.build();

  }
}

