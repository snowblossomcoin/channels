package snowblossom.channels;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import duckutil.Config;
import duckutil.ConfigCat;
import duckutil.ConfigMem;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.LocalPeerInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.lib.ChainHash;
import snowblossom.lib.db.DBMap;
import snowblossom.lib.db.DBProvider;
import snowblossom.lib.db.ProtoDBMap;
import snowblossom.lib.db.rocksdb.JRocksDB;
import snowblossom.lib.trie.HashedTrie;
import snowblossom.lib.trie.TrieDBMap;

/**
 * Database releated to a single channel
 */
public class SingleChannelDB
{
  private static final Logger logger = Logger.getLogger("channelnode.db");
  protected int max_set_return_count=100000;

  private Config config;

  private DBProvider prov;

  protected ProtoDBMap<ChannelBlock> block_map;
  protected ProtoDBMap<LocalPeerInfo> peer_map;
  protected ProtoDBMap<SignedMessage> content_map;
  protected ProtoDBMap<ChannelBlockSummary> summary_map;
  protected DBMap block_height_map;
  protected DBMap data_map;
  protected HashedTrie data_trie;
  protected DBMap chunk_map;

  public SingleChannelDB(Config base_config, ChannelID cid)
    throws Exception
  {
    Runtime.getRuntime().addShutdownHook(new DBShutdownThread());

    base_config.require("db_path");

    File db_path_int = new File(base_config.get("db_path"), "channels");
    File db_path = new File(db_path_int, cid.asString());

    this.config = new ConfigCat(new ConfigMem(
      ImmutableMap.of("db_path", db_path.getPath(), "db_separate", "false")), base_config);
    
    this.prov = new JRocksDB(config);

    open();
  }


  public void close()
  {
    prov.close();
  }

  public void open()
    throws Exception
  {
    block_map = new ProtoDBMap(ChannelBlock.newBuilder().build().getParserForType(), prov.openMap("blocks"));
    peer_map = new ProtoDBMap(LocalPeerInfo.newBuilder().build().getParserForType(), prov.openMap("peer"));
    content_map = new ProtoDBMap(SignedMessage.newBuilder().build().getParserForType(), prov.openMap("c"));
    summary_map = new ProtoDBMap(ChannelBlockSummary.newBuilder().build().getParserForType(), prov.openMap("block_sum"));
		block_height_map = prov.openMap("height");

    data_map = prov.openMap("d");
    chunk_map = prov.openMap("k");
    data_trie = new HashedTrie(new TrieDBMap(data_map), true, true);

  }

  public ProtoDBMap<ChannelBlock> getBlockMap(){return block_map; }
  public ProtoDBMap<LocalPeerInfo> getPeerMap(){return peer_map; }
  public ProtoDBMap<SignedMessage> getContentMap(){return content_map; }
  public ProtoDBMap<ChannelBlockSummary> getBlockSummaryMap(){return summary_map; }
  public HashedTrie getDataTrie() {return data_trie; }
  public DBMap getChunkMap(){return chunk_map; }

  public ChainHash getBlockHashAtHeight(long height)
  {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.putLong(height);
    ByteString hash = block_height_map.get(ByteString.copyFrom(bb.array()));
    if (hash == null) return null;

    return new ChainHash(hash);
  }

  public void setBlockHashAtHeight(long height, ChainHash hash)
  { 
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.putLong(height);

    block_height_map.put(ByteString.copyFrom(bb.array()), hash.getBytes());
  }


  protected void dbShutdownHandler()
  {
    close();
  }

  public class DBShutdownThread extends Thread
  {
    public DBShutdownThread()
    {
      setName("DBShutdownHandler");
    }

    public void run()
    {
      try
      {
        dbShutdownHandler();
      }
      catch(Throwable t)
      {
        logger.log(Level.WARNING, "Exception in DB shutdown", t);
        t.printStackTrace();
      }
    }
  }

  public ContentInfo getContentInfo(ChainHash hash)
  {
    SignedMessage sm = getContentMap().get(hash.getBytes());
    if (sm == null) return null;

    return ChannelSigUtil.quickPayload(sm).getContentInfo();

  }
}
