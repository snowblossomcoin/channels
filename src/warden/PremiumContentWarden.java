package snowblossom.channels.warden;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.logging.Logger;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelID;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.EncryptedChannelConfig;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.client.MonitorInterface;
import snowblossom.client.MonitorTool;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.Globals;
import snowblossom.lib.NetworkParams;
import snowblossom.lib.NetworkParamsProd;
import snowblossom.lib.TransactionUtil;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.Transaction;
import snowblossom.proto.TransactionInner;
import snowblossom.proto.TransactionOutput;
import snowblossom.util.proto.Offer;
import snowblossom.util.proto.OfferAcceptance;
import snowblossom.util.proto.OfferCurrency;
import snowblossom.util.proto.SymmetricKey;

public class PremiumContentWarden extends BaseWarden implements MonitorInterface
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels.warden");

  public PremiumContentWarden(ChannelAccess channel_access)
  {
    super(channel_access);
  }

  public static boolean wantsToRun(ChannelAccess channel_access)
  {
    try
    {
      ByteString encryption_json_data = channel_access.readFile("/web/encryption.json");

      if (encryption_json_data == null) return false;
      if (!channel_access.amIBlockSigner()) return false;
      if (channel_access.getCommonKeyForChannel() == null) return false;

      logger.info("We want to run for " + channel_access.getChannelID());

      return true;
    }
    catch(Exception e)
    {
      return false;
    }

  }

  private SymmetricKey sym_key;
  private EncryptedChannelConfig channel_config;
  private MonitorTool snow_monitor_tool;

  @Override
  public void periodicRun() throws Exception
  {
    logger.info("Running on " + channel_access.getChannelID());
    if (sym_key == null)
    {
      // if exists, load sym key
      sym_key = channel_access.getCommonKeyForChannel();
    }

    // maybe we want to re-read this config on occasion
    if (channel_config == null)
    {
      // Read encryption settings file
      ByteString encryption_json_data = channel_access.readFile("/web/encryption.json");

      EncryptedChannelConfig.Builder new_config = EncryptedChannelConfig.newBuilder();

      JsonFormat.Parser parser = JsonFormat.parser();
      Reader input = new InputStreamReader(new ByteArrayInputStream(encryption_json_data.toByteArray()));
      parser.merge(input, new_config);

      this.channel_config = new_config.build();

    }

    Offer offer = channel_config.getOffer();
    for(Map.Entry<String, OfferCurrency> me : offer.getOfferPriceMap().entrySet())
    {
      String currency = me.getKey();
      OfferCurrency oc = me.getValue();
      if (currency.equals("SNOW"))
      {
        if (snow_monitor_tool == null)
        {
          NetworkParams params = new NetworkParamsProd();
          snow_monitor_tool = new MonitorTool(params, channel_access.getSnowStub(), this);
          AddressSpecHash hash = new AddressSpecHash( oc.getAddress(), params);

          snow_monitor_tool.addAddress(hash);

        }
      }
      else
      {
        logger.warning("PremiumContentWarden: Don't know how to handle currency: " + currency);
      }
    }

  }

  @Override
  public void onContent(ChannelID cid, SignedMessage sm)
  {}

  @Override
  public void onBlock(ChannelID cid, ChannelBlock sm)
  {}

  @Override
  public void onInbound(Transaction tx, int tx_out_idx)
  {
    try
    {
      ChainHash tx_hash = new ChainHash(tx.getTxHash());
      logger.info("Examining payment: " + tx_hash + ":" + tx_out_idx);

      Offer offer = channel_config.getOffer();
      OfferCurrency oc = offer.getOfferPriceMap().get("SNOW");

      TransactionInner inner = TransactionUtil.getInner(tx);

      TransactionOutput output = inner.getOutputs(tx_out_idx);

      long out_val = output.getValue();
      long required_val = (long)Math.round( oc.getPrice() * Globals.SNOW_VALUE_D);
      if (out_val < required_val)
      {
        logger.info(String.format("Payment value too low %d %d", out_val, required_val));
        return;
      }

      OfferAcceptance oa = OfferAcceptance.parseFrom(inner.getExtra());

      AddressSpec addr_spec = oa.getForAddressSpec();
      AddressSpecHash spec_hash = AddressUtil.getHashForSpec(addr_spec);

      if (channel_access.hasKeyInChannel(spec_hash))
      {
        logger.info("Key already saved");
        return;
      }

      channel_access.addKey(addr_spec);
      logger.info("Key added");
    }
    catch(Exception e)
    {
      logger.info("Exception in TX processing: " + e);

    }
  }

  @Override
  public void onOutbound(Transaction tx, int tx_in_idx)
  {}



}
