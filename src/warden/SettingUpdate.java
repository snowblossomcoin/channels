package snowblossom.channels.warden;

import com.google.protobuf.ByteString;
import java.util.HashSet;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.proto.ChannelSettings;
import snowblossom.lib.AddressSpecHash;

public class SettingUpdate extends BaseWarden
{

  private boolean first_run=true;

  public SettingUpdate(ChannelAccess channel_access)
  {
    super(channel_access);

  }

  @Override
  public void periodicRun() throws Exception
  {
    logger.info("Meow");    

    ChannelSettings current_settings = channel_access.getHead().getEffectiveSettings();

    HashSet<AddressSpecHash> admins = new HashSet<>();

    for(ByteString a : current_settings.getAdminSignerSpecHashesList())
    {
      admins.add(new AddressSpecHash(a));
    }

    // Fireduck key
    AddressSpecHash node = new AddressSpecHash("node:3usv5u30e0wt7q4r69upxz0z3dc6x7fl0ktncc5c", "node");

    if (!admins.contains(node))
    {
      logger.info("Updating channel settings");
      channel_access.updateSettings( ChannelSettings.newBuilder().mergeFrom(current_settings).addAdminSignerSpecHashes(node.getBytes()).build() );
    }





  }

}
