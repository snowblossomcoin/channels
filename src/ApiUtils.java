package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.RpcUtil;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.WalletDatabase;
import snowblossom.client.StubHolder;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.RequestAddress;
import snowblossom.proto.TxOutList;
import snowblossom.proto.TransactionOutput;
import snowblossom.proto.TxOutPoint;

public class ApiUtils
{
  public static JSONObject getOutsiderByTime(ChannelNode node, ChannelContext ctx, int max_return)
  {
    TreeMap<Double, SignedMessage> message_map = new TreeMap<>();

    Random rnd = new Random();
    
    for(SignedMessage sm : ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 10000).values())
    {
      try
      {
        ChannelValidation.validateOutsiderContent(sm, ctx.block_ingestor.getHead());
        SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

        double v = payload.getTimestamp();
        v+=rnd.nextDouble();

        message_map.put( -v, sm);
      }
      catch(ValidationException e){}

      while(message_map.size() > max_return)
      {
        message_map.pollLastEntry();
      }
    }

    JSONObject reply = new JSONObject();

    JSONArray msg_list = new JSONArray();
    while(message_map.size() > 0)
    {
      SignedMessage sm = message_map.pollFirstEntry().getValue();
      msg_list.add(getPrettyJsonForContent(sm, node));
    }

    reply.put("messages", msg_list);

    return reply;
  }


  public static JSONObject getPrettyJsonForContent(SignedMessage sm, ChannelNode node)
  {
    JSONObject jo = new JSONObject();
    jo.put("message_id", HexUtil.getHexString(sm.getMessageId()));

    SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

    String name = getNameForPayload(payload, node);

    if (name != null)
    {
      jo.put("sender", name);
    }
    else
    {
      AddressSpecHash sender = AddressUtil.getHashForSpec(payload.getClaim());
      jo.put("sender", AddressUtil.getAddressString( ChannelGlobals.NODE_ADDRESS_STRING , sender));
    }

    try
    {
      jo.put("payload", RpcUtil.protoToJson( payload ));
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }
    return jo;
  }

  public static JSONObject readJSON(InputStream in)
    throws Exception
  {
    JSONParser parser = new JSONParser(JSONParser.MODE_STRICTEST);

    return (JSONObject) parser.parse(in);
  }

  public static void submitContent(JSONObject input, ChannelNode node, ChannelContext ctx)
		throws Exception
  {
    ContentInfo.Builder ci = ContentInfo.newBuilder();

		if (input.containsKey("content"))
    {
      ci.setContent( ByteString.copyFrom(input.get("content").toString().getBytes()));
      ci.setContentLength( ci.getContent().size() );
    }
    if ((input.containsKey("data_map")) && (input.get("data_map") instanceof JSONObject))
    {
      JSONObject data_map = (JSONObject) input.get("data_map");

      for(Map.Entry<String, Object> me : data_map.entrySet())
      {
        String key = me.getKey();
        String value = me.getValue().toString();
        ci.putContentDataMap(key, ByteString.copyFrom(value.getBytes()));
      }
    }

    ci.addBroadcastChannelIds(ctx.cid.getBytes());

		ci.setContentHash( ByteString.copyFrom( DigestUtil.getMD().digest( ci.getContent().toByteArray() ) ) );
   
		WalletDatabase wdb = node.getWalletDB();

    TxOutPoint fbo_out = getFboOutpoint(node, wdb.getAddresses(0));
    System.out.println("FBO OUT: " + fbo_out);


    SignedMessagePayload.Builder payload = SignedMessagePayload.newBuilder();

    payload.setContentInfo(ci.build());
    if (fbo_out != null)
    {
      payload.setFboUtxo(fbo_out);
    }

    SignedMessage sm = ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0),
      payload.build());

		ctx.block_ingestor.ingestContent(sm);
  }

  public static TxOutPoint getFboOutpoint(ChannelNode node, AddressSpec address)
  {
    AddressSpecHash address_hash = AddressUtil.getHashForSpec(address);
    TxOutList out_list = node.getStubHolder().getBlockingStub().getFBOList( 
      RequestAddress.newBuilder().setAddressSpecHash( address_hash.getBytes() ).build() );

    // TODO - do something smarter if multiple
    if (out_list.getOutListCount() > 0)
    {
      return out_list.getOutList(0);
    }
    return null;
  }

  public static String getNameForPayload(SignedMessagePayload payload, ChannelNode node)
  {
    if (payload.getFboUtxo().getTxHash().size() == 0) return null;
    // TODO, validate transactoin output isn't lies
    // TODO, validate UTXO is still valid

    TransactionOutput tx_out = payload.getFboUtxo().getOut();
    if (tx_out.getIds().getUsername().size() ==0) return null;

    // TODO, make sure name is correct for this address

    return new String(tx_out.getIds().getUsername().toByteArray());
  } 

}
