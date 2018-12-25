package snowblossom.channels;

public class ChannelGlobals
{
  public static final String VERSION = "wakka";

  public static final String NODE_ADDRESS_STRING="node";


  public static final int NETWORK_PORT=4862;

  public static final long ALLOWED_CLOCK_SKEW=45000; //ms

  public static final String NODE_TAG = "node";


  //public static final int LONG_RANGE_POINTS=2;
  //public static final int SHORT_RANGE_POINTS=2;
  public static final int NEAR_POINTS=6;
  public static final int RING_DIVISONS=8;


  public static final int DHT_ELEMENT_SIZE = snowblossom.lib.Globals.BLOCKCHAIN_HASH_LEN;

  public static final long MAX_DHT_DATA_AGE=120000L;

  public static final long MAX_DATA_PEER_AGE=2L * 86400L * 1000L;



}
