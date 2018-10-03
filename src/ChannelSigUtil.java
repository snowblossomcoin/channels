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
  public static void validateSignedMessage(SignedMessage sm)
    throws ValidationException
  {
    try
    {
      SignedMessagePayload payload = SignedMessagePayload.parseFrom(sm.getPayload());
      ByteString signature = sm.getSignature();

      AddressSpec claim = payload.getClaim();

      if ((claim.getRequiredSigners() != 1) || (claim.getSigSpecsCount() != 1))
      {
        throw new ValidationException("Multisig not supported");
      }

      MessageDigest md = DigestUtil.getMD();
      byte[] hash = md.digest( sm.getPayload().toByteArray());
      SigSpec sig_spec = claim.getSigSpecs(0);

      if (!SignatureUtil.checkSignature(sig_spec, ByteString.copyFrom(hash), signature))
      {
        throw new ValidationException("Signature match failure");
      }


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

    return signed.build();
  }


}
