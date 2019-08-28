package snowblossom.channels;

import com.google.protobuf.ByteString;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.DigestUtil;

public class ChannelID extends AddressSpecHash
{
  public ChannelID(ByteString bs)
  {
    super(bs);
  }
  public ChannelID(byte[] bs)
  {
    super(bs);
  }


  public String asString()
  {
    return AddressUtil.getAddressString(ChannelGlobals.CHANNEL_ADDRESS_STRING, this);
  }

  public static ChannelID fromSignedBlockHeader(SignedMessage sm)
  {
    ByteString base_hash = sm.getMessageId();

    ByteString id_hash = ByteString.copyFrom(DigestUtil.getMDAddressSpec().digest(base_hash.toByteArray()));

    return new ChannelID(id_hash);

  }
}
