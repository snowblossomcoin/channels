package snowblossom.channels.warden;

import snowblossom.channels.ChannelAccess;

import snowblossom.util.proto.SymmetricKey;
import snowblossom.channels.ChannelID;
import snowblossom.channels.proto.SignedMessage;

import snowblossom.channels.proto.ChannelBlock;
import com.google.protobuf.ByteString;

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
    // Read encryption settings file
    ByteString encryption_json_data = channel_access.readFile("/web/encryption.json");

    // if exists, load sym key
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
