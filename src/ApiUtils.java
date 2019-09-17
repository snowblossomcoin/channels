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
import snowblossom.lib.DigestUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.RpcUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.WalletDatabase;

public class ApiUtils
{
  public static JSONObject getOutsiderByTime(ChannelContext ctx, int max_return)
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
      msg_list.add(getPrettyJsonForContent(sm));
    }

    reply.put("messages", msg_list);

    return reply;
  }


  public static JSONObject getPrettyJsonForContent(SignedMessage sm)
  {
    JSONObject jo = new JSONObject();
    jo.put("message_id", HexUtil.getHexString(sm.getMessageId()));

    try
    {
      jo.put("payload", RpcUtil.protoToJson( ChannelSigUtil.quickPayload(sm) ));
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

    SignedMessage sm = ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0),
      SignedMessagePayload.newBuilder().setContentInfo(ci.build()).build());

		ctx.block_ingestor.ingestContent(sm);


  }

}
