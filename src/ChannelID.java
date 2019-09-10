package snowblossom.channels;

import com.google.protobuf.ByteString;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.Duck32;
import snowblossom.lib.ValidationException;

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

  public static ChannelID fromString(String str)
    throws ValidationException
  {
    return new ChannelID( Duck32.decode(ChannelGlobals.CHANNEL_ADDRESS_STRING, str) );
    
  }

  public String toString()
  {
    return asString();
  }
  public String asString()
  {
    return AddressUtil.getAddressString(ChannelGlobals.CHANNEL_ADDRESS_STRING, this);
  }

  // Suitable for file systems, but can still be rendered with 'fromString'
  public String asStringWithoutColon()
  {
    String s = asString();
    return s.split(":")[1];
  }

  public static ChannelID fromSignedBlockHeader(SignedMessage sm)
  {
    ByteString base_hash = sm.getMessageId();

    ByteString id_hash = ByteString.copyFrom(DigestUtil.getMDAddressSpec().digest(base_hash.toByteArray()));

    return new ChannelID(id_hash);

  }
}
