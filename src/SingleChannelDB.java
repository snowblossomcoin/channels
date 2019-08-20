package snowblossom.channels;

import com.google.common.collect.ImmutableMap;
import duckutil.Config;
import duckutil.ConfigCat;
import duckutil.ConfigMem;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.LocalPeerInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.lib.db.DBProvider;
import snowblossom.lib.db.ProtoDBMap;
import snowblossom.lib.db.rocksdb.JRocksDB;

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


  public SingleChannelDB(Config base_config, ChannelID cid)
    throws Exception
  {
    Runtime.getRuntime().addShutdownHook(new DBShutdownThread());

    config.require("db_path");

    File db_path = new File(config.get("db_path"), cid.asString());

    this.config = new ConfigCat(new ConfigMem(ImmutableMap.of("db_path", db_path.getPath())), base_config);
    
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

  }

  public ProtoDBMap<ChannelBlock> getBlockMap(){return block_map; }
  public ProtoDBMap<LocalPeerInfo> getPeerMap(){return peer_map; }
  public ProtoDBMap<SignedMessage> getContentMap(){return content_map; }
  public ProtoDBMap<ChannelBlockSummary> getBlockSummaryMap(){return summary_map; }

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
}
