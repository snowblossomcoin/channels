package snowblossom.channels;

import snowblossom.util.proto.SymmetricKey;
import com.google.protobuf.ByteString;
import snowblossom.lib.AddressSpecHash;
import snowblossom.proto.WalletKeyPair;
import snowblossom.lib.CipherUtil;
import snowblossom.lib.ValidationException;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.DigestUtil;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.proto.AddressSpec;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import java.nio.ByteBuffer;


/**
 * Stuff to do encrypted content on channels and key management
 */
public class ChannelCipherUtils
{
  public static final String common_key_id_path="/sym_keys/common_id";
  public static final String key_path="/sym_keys/";

  public static String getCommonKeyID(ChannelContext ctx)
  {
    ByteString key_id = ChanDataUtils.getData(ctx, common_key_id_path);
    if (key_id == null) return null;

    return HexUtil.getHexString(key_id);
  }

  public static boolean hasKeyInChannel(ChannelContext ctx, String key_id, AddressSpecHash addr)
  {
    String path = key_path + key_id + "/" + AddressUtil.getAddressString(ChannelGlobals.USER_ADDRESS_STRING, addr);

    ByteString data = ChanDataUtils.getData(ctx, path);
    if (data != null) return true;

    return false;

  }

  public static SymmetricKey getKeyFromChannel(ChannelContext ctx, String key_id, WalletKeyPair wkp)
    throws ValidationException
  {
    AddressSpecHash addr = AddressUtil.getHashForSpec( AddressUtil.getSimpleSpecForKey( wkp ) );

    String path = key_path + key_id + "/" + AddressUtil.getAddressString(ChannelGlobals.USER_ADDRESS_STRING, addr);

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

  /**
   * Should not be called on a non-synced channel - might cause inconsistencies
   * in which things are signed by which keys (or more likely, just an orphan block)
   */
  public static void establishCommonKey(ChannelNode node, ChannelContext ctx)
    throws ValidationException
  {
    if (getCommonKeyID(ctx) != null)
    {
      throw new ValidationException("Channel already has a common key");
    }

    SymmetricKey sym_key = CipherUtil.generageSymmetricKey();

    ContentInfo.Builder ci = ContentInfo.newBuilder();
    
    ci.setContentHash( DigestUtil.hash(ByteString.EMPTY) );

    ci.putChanMapUpdates(common_key_id_path, sym_key.getKeyId());

    addKey(ci, sym_key, node.getUserWalletDB().getAddresses(0));

    ChannelAccess ca = new ChannelAccess(node, ctx);
    ca.createBlockWithContentUnsigned(ImmutableList.of(ci.build()));
  }

  public static void addKeys(ChannelNode node, ChannelContext ctx, List<AddressSpec> specs)
    throws ValidationException
  {
    String key_id = getCommonKeyID(ctx);

    SymmetricKey sym_key = getKeyFromChannel(ctx, key_id, node.getUserWalletDB().getKeys(0));
    
    ContentInfo.Builder ci = ContentInfo.newBuilder();

    ci.setContentHash( DigestUtil.hash(ByteString.EMPTY) );

    for(AddressSpec spec : specs)
    {
      addKey(ci, sym_key, spec);
    }

    ChannelAccess ca = new ChannelAccess(node, ctx);
    ca.createBlockWithContentUnsigned(ImmutableList.of(ci.build()));
    
  }

  public static void addKey(ContentInfo.Builder ci, SymmetricKey sym_key, AddressSpec spec)
    throws ValidationException
  {
    if (spec.getSigSpecsCount() != 1) 
      throw new ValidationException("AddressSpec must have exactly one public key");


    ByteString en_data = CipherUtil.encrypt(spec.getSigSpecs(0), sym_key.toByteString());

    String key_id = HexUtil.getHexString(sym_key.getKeyId());
    AddressSpecHash addr = AddressUtil.getHashForSpec(spec);

    String path = key_path + key_id + "/" + AddressUtil.getAddressString(ChannelGlobals.USER_ADDRESS_STRING, addr);
    ci.putChanMapUpdates(path, en_data);

  }

  public static ByteString getIv(ByteString iv_base, int chunk_no)
  {
    byte b[]=new byte[4];
    ByteBuffer.wrap(b).putInt(chunk_no);

    ByteString iv = DigestUtil.hash( iv_base.concat(ByteString.copyFrom(b) )).substring(0,16);

    return iv;

  }

  public static ByteString randomIv()
  {
    Random rnd = new Random();
    byte b[]=new byte[16];
    rnd.nextBytes(b);
    return ByteString.copyFrom(b);

  }

  


}
