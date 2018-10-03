package snowblossom.channels;

import snowblossom.lib.ValidationException;
import snowblossom.lib.SignatureUtil;
import snowblossom.channels.proto.*;
import snowblossom.proto.WalletKeyPair;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.SigSpec;

import com.google.protobuf.ByteString;

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


      SigSpec sig_spec = claim.getSigSpecs(0);

      SignatureUtil.checkSignature(sig_spec, sm.getPayload(), signature);

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

    signed.setSignature( SignatureUtil.sign(wkp, payload_data) );

    return signed.build();
  }


}
