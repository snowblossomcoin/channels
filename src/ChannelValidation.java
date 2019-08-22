package snowblossom.channels;

import java.math.BigInteger;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelSettings;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.lib.BlockchainUtil;
import snowblossom.lib.ValidationException;
import snowblossom.lib.Validation;

public class ChannelValidation
{
  public static void checkBlockBasics(ChannelBlock blk, boolean require_content)
    throws ValidationException
  {
    ChannelBlockHeader header = ChannelSigUtil.validateSignedMessage(blk.getSignedHeader()).getChannelBlockHeader();
    if (header == null) throw new ValidationException("No headerd in signed header");
    if (header.getBlockHeight() == 0)
    {
      ChannelSettings settings = ChannelSigUtil.validateSignedMessage(header.getInitialSettings()).getChannelSettings();
      if (settings == null) throw new ValidationException("Missing initial settings on block zero");

      if (header.getSettings() != null)
      {
        throw new ValidationException("Must not have initial_settings and settings on block zero");
      }
     
    }
    else
    {
      if (header.getInitialSettings() != null)
      {
        throw new ValidationException("Must not have initial_settings on non-zero block");
      }
    }

    Validation.validateNonNegValue( header.getBlockHeight(), "block_height");
    Validation.validateNonNegValue( header.getBlockHeight(), "weight");
    if (header.getVersion() != 1)
    {
      throw new ValidationException("Unexpected block header version: " + header.getVersion());
    }

    //TODO

  }

  public static ChannelBlockSummary deepBlockValidation(SingleChannelDB db, ChannelBlock blk, ChannelBlockSummary prev_summary)
    throws ValidationException
  {
    checkBlockBasics(blk, true);
    
    //TODO


    ChannelBlockHeader header = ChannelSigUtil.validateSignedMessage(blk.getSignedHeader()).getChannelBlockHeader();

    ChannelBlockSummary.Builder sum = ChannelBlockSummary.newBuilder();

    BigInteger prev_work_sum = BigInteger.ZERO;
    ChannelSettings settings;
    if (prev_summary != null)
    {
      prev_work_sum = BlockchainUtil.readInteger(prev_summary.getWorkSum());
      settings = prev_summary.getEffectiveSettings();
    }
    else
    {
      settings = ChannelSigUtil.validateSignedMessage(header.getInitialSettings()).getChannelSettings();
    }

    BigInteger work_sum = prev_work_sum.add( BigInteger.valueOf( header.getWeight() + 1L ) );

    if (header.getSettings() != null)
    {
      settings = header.getSettings();
    }

    sum.setWorkSum(work_sum.toString());
    sum.setHeader(header);
    sum.setBlockId(blk.getSignedHeader().getMessageId());
    sum.setEffectiveSettings(settings);

    return sum.build();

  }
}

