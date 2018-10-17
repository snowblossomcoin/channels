package snowblossom.channels;

import java.util.Map;
import java.util.TreeMap;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;

import duckutil.PeriodicThread;

import com.google.protobuf.ByteString;

public class DHTMaintainer extends PeriodicThread
{
  private ChannelNode node;

  public DHTMaintainer(ChannelNode node)
  {
    super(25000L);
    setName("DHTMaintainer");
    setDaemon(false);

    this.node = node;


  }

  @Override
  public void runPass() throws Exception
  {
    Map<AddressSpecHash, LocalPeerInfo> target_map = pickTargetsFromDB(); 
     

  }

  private Map<AddressSpecHash, LocalPeerInfo> pickTargetsFromDB()
  {
    Map<AddressSpecHash, LocalPeerInfo> target_map = new TreeMap<>();

    // Close neighbors
    target_map.putAll( getClosestValid( node.getNodeID().getBytes(), ChannelGlobals.NEAR_POINTS));

    double long_wedge = 1.0 / ChannelGlobals.LONG_RANGE_POINTS;

    // So if there are 4 point, point 0 is me, so don't bother with that
    for(int i=1; i<ChannelGlobals.LONG_RANGE_POINTS; i++)
    {
      ByteString target = HashMath.shiftHashOnRing( node.getNodeID().getBytes(), long_wedge * i);
      target_map.putAll( getClosestValid( target, 1));
    }

    
    double short_wedge = long_wedge / ChannelGlobals.SHORT_RANGE_POINTS;
    int short_points = ChannelGlobals.SHORT_RANGE_POINTS;
    // So we take the closest long wedge, and put points along that.
    // maybe we hit ourself, maybe not.  Whatever.
    for(int i=0; i<=short_points; i++)
    {
      ByteString target = HashMath.shiftHashOnRing( node.getNodeID().getBytes(), -long_wedge/2.0 + short_wedge * i);
      target_map.putAll( getClosestValid( target, 1));
    }

    return target_map; 
  }

  private Map<AddressSpecHash, LocalPeerInfo> getClosestValid(ByteString target, int count)
  {
    return null;
  }
}
