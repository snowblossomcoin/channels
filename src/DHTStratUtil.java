package snowblossom.channels;

import com.google.protobuf.ByteString;
import snowblossom.channels.proto.ChannelSettings;

import java.util.LinkedList;
import snowblossom.lib.DigestUtil;
import java.security.MessageDigest;


// Not making these static methods because they will
// have to read snowblossom network state for some strats

public class DHTStratUtil
{

  /**
   * @return An ordered list of dht addresses to try
   * @param settings - if we have it.  null otherwise
   */
  public LinkedList<ByteString> getDHTLocations(ChannelID cid, ChannelSettings settings)
  {
    // TODO - actually check the strategies
    return getDHTLocationsBasic(cid, 10); 

  }

  public LinkedList<ByteString> getDHTLocationsBasic(ChannelID cid, int count)
  {
    LinkedList<ByteString> lst = new LinkedList<>();
    
    MessageDigest md = DigestUtil.getMD();

    for(int i=0; i<count; i++)
    {
      md.update(cid.getBytes().toByteArray()); 
      String s = "" + i;
      md.update(s.getBytes());

      ByteString element_id = ByteString.copyFrom(md.digest());
      lst.add(element_id);
    }


    return lst;
  }

}
