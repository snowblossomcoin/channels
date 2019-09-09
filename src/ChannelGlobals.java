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

  public static final long MAX_DATA_PEER_AGE=21L * 86400L * 1000L;


  public static final long PEER_LINK_TIMEOUT=60000L;
  public static final long CHANNEL_LINK_TIMEOUT=60000L;

  // 2500 channels, 100 elements each
  public static final int DHT_CACHE_ELEMENTS = 2500 * 100;
  public static final long DHT_CACHE_EXPIRE = 120000L;
  
  // TODO - move this way up once we have saving wait for DHT peering to be ready
  public static final long DHT_SAVE_CACHE_EXPIRE = 120000L; 

  public static final long BLOCK_CHUNK_HEADER_DOWNLOAD_SIZE=100L;

  public static final long CHANNEL_TIP_SEND_MS = 30000L;

  public static final long CONTENT_DATA_BLOCK_SIZE = 1048576L;

}








