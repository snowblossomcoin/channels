package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.util.TreeMap;
import java.util.Random;
import snowblossom.channels.proto.*;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;
import snowblossom.lib.ValidationException;

public class ApiUtils
{
  public static JSONObject getOutsiderByTime(ChannelContext ctx)
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

    }




    return new JSONObject();

  }

}
