package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.Config;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.LocalPeerInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.lib.DaemonThreadFactory;
import snowblossom.lib.db.DBMap;
import snowblossom.lib.db.DBProvider;
import snowblossom.lib.db.ProtoDBMap;

/**
 * Node wide DB, not for any specific channel
 */
public class ChannelsDB
{
  private static final Logger logger = Logger.getLogger("snowblossom.db");
  protected int max_set_return_count=100000;

  protected Executor exec;

  protected ProtoDBMap<LocalPeerInfo> peer_map;
  protected ProtoDBMap<SignedMessage> dht_data_map;
  protected DBMap subscription_map;

  private Config config;
  private DBProvider prov;

  public ChannelsDB(Config config, DBProvider prov)
    throws Exception
  {
    Runtime.getRuntime().addShutdownHook(new DBShutdownThread());

    this.config = config;
    this.prov = prov;

    open();
  }


  public void close()
  {
    prov.close();
  }

  public void open()
    throws Exception
  {
    peer_map = new ProtoDBMap(LocalPeerInfo.newBuilder().build().getParserForType(), prov.openMap("peer"));
    dht_data_map = new ProtoDBMap(SignedMessage.newBuilder().build().getParserForType(), prov.openMap("dht_data"));
    subscription_map = prov.openMap("subscription");
  }

  public ProtoDBMap<LocalPeerInfo> getPeerMap(){return peer_map; }
  public ProtoDBMap<SignedMessage> getDHTDataMap(){return dht_data_map; }
  protected DBMap getSubscriptionMap(){return subscription_map; }

  protected void dbShutdownHandler()
  {
    close();
  }

  protected synchronized Executor getExec()
  {
    if (exec == null)
    {
      exec = new ThreadPoolExecutor(
        32,
        32,
        2, TimeUnit.DAYS,
        new LinkedBlockingQueue<Runnable>(),
        new DaemonThreadFactory("db_exec"));
    }
    return exec;

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
