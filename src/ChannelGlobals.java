package snowblossom.channels;


public class ChannelGlobals
{
  public static final String VERSION = "v0.6.0-2020.08.30.00";

  public static final String USER_ADDRESS_STRING="user";
  public static final String NODE_ADDRESS_STRING="node";
  public static final String CHANNEL_ADDRESS_STRING="chan";

  public static final int NETWORK_PORT=4862;

  public static final String MULTICAST_IPV4_ADDRESS="224.48.62.1";

  // Subject to change after I understand WTF https://tools.ietf.org/html/rfc7371 is on about
  public static final String MULTICAST_IPV6_ADDRESS="FFFF::E030:3E01";

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

  public static final long MULTICAST_BROADCAST_PERIOD=60000L;
  public static final long MULTICAST_CACHE_EXPIRE=MULTICAST_BROADCAST_PERIOD*2L;

  // 2500 channels, 100 elements each
  public static final int DHT_CACHE_ELEMENTS = 2500 * 100;
  public static final long DHT_CACHE_EXPIRE = 120000L;
  
  // TODO - move this way up once we have saving wait for DHT peering to be ready
  public static final long DHT_SAVE_CACHE_EXPIRE = 120000L; 

  public static final long BLOCK_CHUNK_HEADER_DOWNLOAD_SIZE=100L;

  public static final long CHANNEL_TIP_SEND_MS = 30000L;

  public static final long CONTENT_DATA_BLOCK_SIZE = 1048576L;

  // Wait this long before requesting peers on a channel with no peers
  public static final long NEED_PEERS_REQUEST_MS = 60000L;

  // Special Channels

  public static final String CHAN_NEED_PEERS = "chan:ej08g32tltwx56ayzceuq3afyv2qrs2qhkg4fnn9";
  public static final String CHAN_NEED_PEERS_WARDEN = "snowblossom.channels.warden.NeedPeersWarden";

}




