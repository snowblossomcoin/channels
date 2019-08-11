package snowblossom.channels;

import snowblossom.lib.ValidationException;
import snowblossom.lib.SignatureUtil;
import snowblossom.lib.DigestUtil;
import snowblossom.channels.proto.*;
import snowblossom.proto.WalletKeyPair;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.SigSpec;

import com.google.protobuf.ByteString;
import java.security.MessageDigest;


public class ChannelSigUtil
{
  public static SignedMessagePayload validateSignedMessage(SignedMessage sm)
    throws ValidationException
  {
    try
    {
      SignedMessagePayload payload = SignedMessagePayload.parseFrom(sm.getPayload());
      ByteString signature = sm.getSignature();

      AddressSpec claim = payload.getClaim();

      if ((claim.getRequiredSigners() != 1) || (claim.getSigSpecsCount() != 1))
      {
        throw new ValidationException("Multisig not supported (yet)");
      }

      MessageDigest md = DigestUtil.getMD();
      byte[] hash = md.digest( sm.getPayload().toByteArray());
      SigSpec sig_spec = claim.getSigSpecs(0);

      // TODO - remove this isEmpty test, it shouldn't be empty
      if (!sm.getMessageId().isEmpty())
      {
        md.update(hash);
        md.update(sm.getSignature().toByteArray());
        ByteString message_id = ByteString.copyFrom(md.digest());

        if (!sm.getMessageId().equals(message_id))
        {
          throw new ValidationException("Included message_id does not match");
        }
      }


      if (!SignatureUtil.checkSignature(sig_spec, ByteString.copyFrom(hash), signature))
      {
        throw new ValidationException("Signature match failure");
      }
      if (payload.getTimestamp() > ChannelGlobals.ALLOWED_CLOCK_SKEW + System.currentTimeMillis())
      {
        throw new ValidationException("Signed message too far into future");
      }

      return payload;

    }
    catch(com.google.protobuf.InvalidProtocolBufferException e)
    {
      throw new ValidationException(e);
    }


  }

  /**
   * @param starting_payload should have of oneof z specified for the body.
   */
  public static SignedMessage signMessage(AddressSpec claim, WalletKeyPair wkp, SignedMessagePayload starting_payload)
    throws ValidationException
  {
    if ((claim.getRequiredSigners() != 1) || (claim.getSigSpecsCount() != 1))
    {
      throw new ValidationException("Multisig not supported");
    }
    SignedMessagePayload.Builder payload = SignedMessagePayload.newBuilder();

    payload.mergeFrom(starting_payload);

    payload.setTimestamp(System.currentTimeMillis());
    payload.setClaim(claim);
    
    ByteString payload_data = payload.build().toByteString();

    SignedMessage.Builder signed = SignedMessage.newBuilder();

    signed.setPayload(payload_data);

    MessageDigest md = DigestUtil.getMD();
    byte[] hash = md.digest( payload_data.toByteArray());

    signed.setSignature( SignatureUtil.sign(wkp, ByteString.copyFrom(hash)) );

    md.update(hash);
    md.update( signed.getSignature().toByteArray() );


    signed.setMessageId( ByteString.copyFrom( md.digest() ) );

    return signed.build();
  }


}
