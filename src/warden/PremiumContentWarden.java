package snowblossom.channels.warden;

import com.google.protobuf.ByteString;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelID;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.util.proto.SymmetricKey;

public class PremiumContentWarden extends BaseWarden
{
  public PremiumContentWarden(ChannelAccess channel_access)
  {
    super(channel_access);
  }

  public static boolean wantsToRun(ChannelAccess channel_access)
  {
    try
    {
      ByteString encryption_json_data = channel_access.readFile("/web/encryption.json");

      if (encryption_json_data == null) return false;
      if (!channel_access.amIBlockSigner()) return false;

      return true;
    }
    catch(Exception e)
    {
      return false;
    }

  }

	private SymmetricKey sym_key;

  @Override
  public void periodicRun() throws Exception
  {
    // Read encryption settings file
    ByteString encryption_json_data = channel_access.readFile("/web/encryption.json");

    // if exists, load sym key
    SymmetricKey sym_key = channel_access.getCommonKeyForChannel();

    // watch snow address
    // on payments to address, compare to required amount
    // if so, addKey for recipient


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
