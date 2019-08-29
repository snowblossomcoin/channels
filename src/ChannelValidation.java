package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.LinkedList;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.ChannelSettings;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.ContentReference;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.DHTDataSet;
import snowblossom.channels.proto.SignedMessagePayload;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.BlockchainUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.Globals;
import snowblossom.lib.Validation;
import snowblossom.lib.ValidationException;

public class ChannelValidation
{
  public static ChannelBlockHeader checkBlockHeaderBasics(ChannelID chan_id, SignedMessage signed_header)
    throws ValidationException
  {
    ChannelBlockHeader header = ChannelSigUtil.validateSignedMessage(signed_header).getChannelBlockHeader();
    if (header == null) throw new ValidationException("No header in signed header");
    if (!chan_id.equals(header.getChannelId()))
    {
      throw new ValidationException("Block on wrong channel");
    }

    if (header.getBlockHeight() == 0)
    {
      ChannelSettings settings = ChannelSigUtil.validateSignedMessage(header.getInitialSettings()).getChannelSettings();
      if (settings == null) throw new ValidationException("Missing initial settings on block zero");

      AddressSpecHash channel_id = ChannelSigUtil.getChannelId( new ChainHash(header.getInitialSettings().getMessageId() ));
      if (!channel_id.equals(header.getChannelId()))
      {
        throw new ValidationException("Message id from initial settings must be channel id");
      }

      if (header.hasSettings())
      {
        throw new ValidationException("Must not have initial_settings and settings on block zero");
      }
      if (!ChainHash.ZERO_HASH.equals( header.getPrevBlockHash()))
      {
        throw new ValidationException("Block zero must have zero prev_block_hash");

      }
    }
    else
    {
      if (header.getInitialSettings().getPayload().size() > 0)
      {
        throw new ValidationException("Must not have initial_settings on non-zero block");
      }
    }

    Validation.validateNonNegValue( header.getBlockHeight(), "block_height");
    Validation.validateNonNegValue( header.getBlockHeight(), "weight");
    if (header.getVersion() != 1)
    {
      throw new ValidationException("Unexpected block header version: " + header.getVersion());
    }

    if (header.getChannelId().size() != Globals.ADDRESS_SPEC_HASH_LEN)
    {
      throw new ValidationException("Invalid channel id");
    }
    if(header.getPrevBlockHash().size() != Globals.BLOCKCHAIN_HASH_LEN)
    {
      throw new ValidationException("Invalid prev block hash");
    }
    if(header.getContentMerkle().size() != Globals.BLOCKCHAIN_HASH_LEN)
    {
      throw new ValidationException("Invalid content merkle hash");
    }

    return header;


  }


  public static void checkBlockBasics(ChannelID chan_id, ChannelBlock blk, boolean check_content)
    throws ValidationException
  {
    ChannelBlockHeader header = checkBlockHeaderBasics(chan_id, blk.getSignedHeader());

    if (check_content)
    {
      MessageDigest md = DigestUtil.getMD();

      LinkedList<ChainHash> content_hash_list = new LinkedList<>();
      for(SignedMessage content : blk.getContentList())
      {
        validateContent(content, md);
        content_hash_list.add(new ChainHash(content.getMessageId()));
      }

      ChainHash expected_merkle = null;
      if (content_hash_list.size() ==0)
      {
        expected_merkle = ChainHash.ZERO_HASH;
      }
      else
      {
        expected_merkle = DigestUtil.getMerkleRootForTxList(content_hash_list);
      }
      if (!expected_merkle.equals(header.getContentMerkle()))
      {
        throw new ValidationException("Merkle root mismatch");
      }

    }

    //TODO

  }

  public static void validateContent(SignedMessage sm, MessageDigest md)
    throws ValidationException
  {
    ContentInfo ci = ChannelSigUtil.validateSignedMessage(sm).getContentInfo();
    validateContent(ci, md);
  }

  public static void validateContent(ContentInfo ci, MessageDigest md)
    throws ValidationException
  {
    if (ci.getContentHash().size() != Globals.BLOCKCHAIN_HASH_LEN)
    {
      throw new ValidationException("Missing content info");
    }

    Validation.validateNonNegValue( ci.getContentLength(), "content length");

    if (ci.getContent().size() > 0)
    {
      if (ci.getContent().size() != ci.getContentLength())
      {
        throw new ValidationException("content length mismatch");
      }

      ByteString found_hash = ByteString.copyFrom(md.digest(ci.getContent().toByteArray()));
      if(!ci.getContentHash().equals(found_hash))
      {
        throw new ValidationException("content hash mismatch");
      }
    }

    if (ci.getParentRef().getChannelId().size() > 0)
    {
      validateRef(ci.getParentRef());
    }
    for(ContentReference ref : ci.getRefsList())
    {
      validateRef(ref);
    }
    for(ContentInfo sub_ci : ci.getIncludedContentList())
    {
      validateContent(sub_ci, md);
    }

 
  }

  public static void validateRef(ContentReference ref)
    throws ValidationException
  {
    if (ref.getChannelId().size() != Globals.ADDRESS_SPEC_HASH_LEN)
    {
      throw new ValidationException("Invalid channel id in reference");
    }
    if (ref.getMessageId().size() != Globals.BLOCKCHAIN_HASH_LEN)
    {
      throw new ValidationException("Invalid message id in reference");
    }

  }


  public static ChannelBlockSummary deepBlockValidation(ChannelID chan_id, ChannelBlock blk, ChannelBlockSummary prev_summary)
    throws ValidationException
  {
    checkBlockBasics(chan_id, blk, true);

    SignedMessagePayload header_payload = ChannelSigUtil.validateSignedMessage(blk.getSignedHeader());

    ChannelBlockHeader header = header_payload.getChannelBlockHeader();
    
    //TODO

    // Validate signer of block vs effective settings
    // if changing settings, validate that it is admin

    ChannelSettings effective_settings = null;
    HashSet<ByteString> allowed_signers = new HashSet<>();
    HashSet<ByteString> admin_signers = new HashSet<>();
    if (prev_summary == null)
    {
      if (header.getBlockHeight() != 0)
      {
        throw new ValidationException("Must have prev block on non-zero block");
      }
      if (header.hasSettings())
      {
        throw new ValidationException("Block zero must not have settings");
      }
      effective_settings = ChannelSigUtil.quickPayload(header.getInitialSettings()).getChannelSettings();

    }
    else
    {
      if (header.getBlockHeight() != prev_summary.getHeader().getBlockHeight() + 1L)
      {
        throw new ValidationException("Block heights must increase by one");
      }

      effective_settings = prev_summary.getEffectiveSettings();
    }

    allowed_signers.addAll(effective_settings.getBlockSignerSpecHashesList());
    allowed_signers.addAll(effective_settings.getAdminSignerSpecHashesList());
    admin_signers.addAll(effective_settings.getAdminSignerSpecHashesList());

    AddressSpecHash signer = AddressUtil.getHashForSpec(header_payload.getClaim());

    if (!allowed_signers.contains(signer.getBytes()))
    {
      throw new ValidationException("Block signer not on signer list");
    }

    if (header.hasSettings())
    {
      if (!admin_signers.contains(signer.getBytes()))
      {
        throw new ValidationException("Block signer not on admin list");
      }
    }

    ChannelBlockSummary.Builder sum = ChannelBlockSummary.newBuilder();

    BigInteger prev_work_sum = BigInteger.ZERO;
    ChannelSettings settings;
    if (prev_summary != null)
    {
      prev_work_sum = BlockchainUtil.readInteger(prev_summary.getWorkSum());
      settings = prev_summary.getEffectiveSettings();
    }
    else
    {
      settings = ChannelSigUtil.validateSignedMessage(header.getInitialSettings()).getChannelSettings();
    }

    BigInteger work_sum = prev_work_sum.add( BigInteger.valueOf( header.getWeight() + 1L ) );

    if (header.hasSettings())
    {
      settings = header.getSettings();
    }

    sum.setWorkSum(work_sum.toString());
    sum.setHeader(header);
    sum.setSignedHeader(blk.getSignedHeader());
    sum.setBlockId(blk.getSignedHeader().getMessageId());
    sum.setEffectiveSettings(settings);

    return sum.build();

  }


  public static SignedMessagePayload validateDHTData(SignedMessage dht_data)
    throws ValidationException
  {
    SignedMessagePayload payload = ChannelSigUtil.validateSignedMessage(dht_data);

    if (!payload.hasDhtData())
    {
      throw new ValidationException("Payload is not dht_data");
    }

		AddressSpecHash signed_hash = AddressUtil.getHashForSpec(payload.getClaim());

		if (!signed_hash.equals(payload.getDhtData().getPeerInfo().getAddressSpecHash()))
		{
			throw new ValidationException("Signer of DHT data does not match peer info");
		}
    if (payload.getDhtData().getElementId().size() != ChannelGlobals.DHT_ELEMENT_SIZE)
    { 
      throw new ValidationException("Element id wrong length");
    }

    return payload;

  }

	public static void validateDHTDataSet(DHTDataSet ds, ByteString element_id)
    throws ValidationException
	{
		for(SignedMessage sm : ds.getDhtDataList())
		{
			SignedMessagePayload payload = validateDHTData(sm);
			if (!element_id.equals(payload.getDhtData().getElementId()))
			{ 
				throw new ValidationException("Not requested element_id");
			}	
		}
	}
}

