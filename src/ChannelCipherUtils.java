package snowblossom.channels;

import snowblossom.util.proto.SymmetricKey;
import com.google.protobuf.ByteString;
import snowblossom.lib.AddressSpecHash;
import snowblossom.proto.WalletKeyPair;
import snowblossom.lib.CipherUtil;
import snowblossom.lib.ValidationException;
import snowblossom.lib.AddressUtil;

public class ChannelCipherUtils
{
  public static final String common_key_id_path="/keys/common_id";
  public static final ByteString key_path=ByteString.copyFrom("/keys/".getBytes());
  public static final ByteString path_sep=ByteString.copyFrom("/".getBytes());

  public static ByteString getCommonKeyID(ChannelContext ctx)
  {
    return ChanDataUtils.getData(ctx, common_key_id_path);
  }

  public static boolean hasKeyInChannel(ChannelContext ctx, ByteString key_id, AddressSpecHash addr)
  {
    ByteString path = key_path.concat(key_id).concat(path_sep).concat(addr.getBytes());

    ByteString data = ChanDataUtils.getData(ctx, path);
    if (data != null) return true;

    return false;

  }

  public static SymmetricKey getKeyFromChannel(ChannelContext ctx, ByteString key_id, WalletKeyPair wkp)
    throws ValidationException
  {
    AddressSpecHash addr = AddressUtil.getHashForSpec( AddressUtil.getSimpleSpecForKey( wkp ) );

    ByteString path = key_path.concat(key_id).concat(path_sep).concat(addr.getBytes());

    ByteString data = ChanDataUtils.getData(ctx, path);
    if (data == null) return null;

    ByteString dec = CipherUtil.decrypt(wkp, data);

    try
    {
      SymmetricKey sk = SymmetricKey.parseFrom(dec);

      return sk;
    }
    catch(com.google.protobuf.InvalidProtocolBufferException e)
    {
      throw new ValidationException(e);
    }
  }

}
