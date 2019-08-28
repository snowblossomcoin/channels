package snowblossom.channels;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import duckutil.PeriodicThread;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.ValidationException;
import snowblossom.lib.trie.ByteStringComparator;



public class ChannelPeerMaintainer extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  private HashMap<AddressSpecHash, Long> connection_attempt_times = new HashMap<>(16,0.5f);
  private ImmutableSet<AddressSpecHash> current_links;

  public ChannelPeerMaintainer(ChannelNode node)
  {
    super(5000L);
    setName("ChannelPeerMaintainer");
    setDaemon(false);

    this.node = node;

  }


  // Things to do for each channel:
  // * get peers from DHT.  Do this sometimes even if we have plenty of peers.
  // * save self to DHT.
  // * connect to peers
  @Override
  public void runPass() throws Exception
  {

  }
}



