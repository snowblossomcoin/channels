package snowblossom.channels;

public class ChannelGlobals
{
  public static final String VERSION = "wakka";

  public static final String NODE_ADDRESS_STRING="node";
  public static final String CHANNEL_ADDRESS_STRING="chan";



  public static final int NETWORK_PORT=4862;

  public static final long ALLOWED_CLOCK_SKEW=45000; //ms

  public static final String NODE_TAG = "node";

  
  // Always connect to this many near points
  public static final int NEAR_POINTS=6;

  // Number of divisions around the ring to connect to, halving each time
  public static final int RING_DIVISONS=8;


  public static final int DHT_ELEMENT_SIZE = snowblossom.lib.Globals.BLOCKCHAIN_HASH_LEN;

  // Max age to be stored.  Older data may be served but new data with signatures older than this won't be
  // saved.
  public static final long MAX_DHT_DATA_AGE=120000L;

  public static final long MAX_DATA_PEER_AGE=7200L * 1000L;


}

