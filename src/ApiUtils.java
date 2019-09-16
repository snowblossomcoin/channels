package snowblossom.channels;

import org.json.JSONObject;
import org.json.JSONArray;
import com.google.protobuf.ByteString;

public class ApiUtils
{
  public static JSONObject getOutsiderByTime(ChannelContext ctx)
  {
    ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 10000);


    return new JSONObject();

  }

}
