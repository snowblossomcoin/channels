package snowblossom.channels;

import com.google.protobuf.ByteString;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;

public class ChannelID extends AddressSpecHash
{
  public ChannelID(ByteString bs)
  {
    super(bs);
  }
   

  public String asString()
  {
    return AddressUtil.getAddressString(ChannelGlobals.CHANNEL_ADDRESS_STRING, this);
  }
}
