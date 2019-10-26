package snowblossom.channels;

import duckutil.Config;
import duckutil.ConfigCat;
import duckutil.ConfigFile;
import duckutil.ConfigMem;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import java.io.File;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.channels.proto.StargateServiceGrpc.StargateServiceBlockingStub;
import snowblossom.client.StubHolder;
import snowblossom.client.StubUtil;
import snowblossom.client.WalletUtil;
import snowblossom.lib.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.db.rocksdb.JRocksDB;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.WalletDatabase;

/**
 * Top level project class.  Starts everything and wires it up.
 */
public class ChannelNode
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private Config config;
  private StubHolder stub_holder;
  private WalletDatabase wallet_db;
  private NetworkParams params;
  private ChannelsDB db;
  private NetworkExaminer net_ex;
  private PeerManager peer_manager;
  private DHTServer dht_server;
  private DHTMaintainer dht_maintainer;
  private ChannelSubscriber channel_subscriber;
  private ChannelPeerMaintainer channel_peer_maintainer;
  private ChannelPeerServer channel_peer_server;
  private DHTCache dht_cache;
  private DHTStratUtil dht_strat_util;
  private ChannelTipSender channel_tip_sender;
  private ChannelChunkGetter channel_chunk_getter;
  private ChannelOutsiderSender channel_outsider_sender;

  private HashMap<ChannelID, SingleChannelDB> db_map;

  public static void main(String args[])
		throws Exception
  {
    Globals.addCryptoProvider();

    if (args.length != 1)
    { 
      logger.log(Level.SEVERE, "Incorrect syntax. Syntax: ChannelNode <config_file>");
      System.exit(-1);
    }

    ConfigFile f_config = new ConfigFile(args[0]);

    TreeMap<String,String> mem_config = new TreeMap<>();
    mem_config.put("db_separate","true");
    mem_config.put("key_count", "1");
    Config config = new ConfigCat(new ConfigMem(mem_config), f_config);

    LogSetup.setup(config);

		new ChannelNode(config);
  }

  public ChannelNode(Config config)
		throws Exception
  {
    this(config, new StubHolder(StubUtil.openChannel(config, new NetworkParamsProd())));
  }

  public ChannelNode(Config config, StubHolder stub_holder)
		throws Exception
  {
    this.config = config;
    this.stub_holder = stub_holder;

    config.require("wallet_path");

    File wallet_path = new File(config.get("wallet_path"));

    params = NetworkParams.loadFromConfig(config);

    wallet_db = WalletUtil.loadWallet(wallet_path, true, params);
    if (wallet_db == null)
    { 
      logger.log(Level.WARNING, String.format("Directory %s does not contain wallet, creating new wallet", wallet_path.getPath()));
      wallet_db = WalletUtil.makeNewDatabase(config, params);
      WalletUtil.saveWallet(wallet_db, wallet_path);
    }

    db = new ChannelsDB(config, new JRocksDB(config) );
    db_map = new HashMap<>(16,0.5f);

    net_ex = new NetworkExaminer(this);
    peer_manager = new PeerManager(this);
    dht_server = new DHTServer(this);
    dht_maintainer = new DHTMaintainer(this);
    channel_subscriber = new ChannelSubscriber(this);
    channel_peer_maintainer = new ChannelPeerMaintainer(this);
    channel_peer_server = new ChannelPeerServer(this);
    dht_cache = new DHTCache(this);
    dht_strat_util = new DHTStratUtil();
    channel_tip_sender = new ChannelTipSender(this);
    channel_chunk_getter = new ChannelChunkGetter(this);
    channel_outsider_sender = new ChannelOutsiderSender(this);
   
    startServer();

    dht_maintainer.start();
    peer_manager.start();
    channel_peer_maintainer.start();
    channel_tip_sender.start();
    channel_chunk_getter.start();
    channel_outsider_sender.start();

    channel_subscriber.loadFromDB();    


    if (config.isSet("web_port"))
    {
      new WebServer(this);
    }

    String node_addr = AddressUtil.getAddressString(ChannelGlobals.NODE_TAG, getNodeID());
    logger.info("My node address is: " + node_addr);

    if (config.isSet("channel_list"))
    {
      for(String s : config.getList("channel_list"))
      {
        getChannelSubscriber().openChannel(ChannelID.fromString(s));
      }
    }

    //testSelf();

  }
  public int getPort()
  {
    return config.getIntWithDefault("port", ChannelGlobals.NETWORK_PORT);
  }

  public SingleChannelDB getChannelDB(ChannelID cid)
  {
    synchronized(db_map)
    {
      SingleChannelDB cdb = db_map.get(cid);
      if (cdb != null) return cdb;

      try
      {
        cdb = new SingleChannelDB(config, cid);
        db_map.put(cid, cdb);
        return cdb;
      }
      catch(Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  private void startServer()
    throws Exception
  {  
    int port = getPort();

    Server s = NettyServerBuilder
      .forPort(port)
      .addService(dht_server)
      .addService(channel_peer_server)
      .sslContext(CertGen.getServerSSLContext(wallet_db))
      .build();
    s.start();

  }

  public NetworkExaminer getNetworkExaminer(){return net_ex;}
  public PeerManager getPeerManager(){return peer_manager;}
  public ChannelsDB getDB(){return db;}
  public DHTServer getDHTServer(){return dht_server;}
  public ChannelSubscriber getChannelSubscriber(){return channel_subscriber;}
  public ChannelPeerMaintainer getChannelPeerMaintainer(){return channel_peer_maintainer;}
  public DHTCache getDHTCache(){return dht_cache;}
  public DHTStratUtil getDHTStratUtil(){return dht_strat_util;}
  public ChannelTipSender getChannelTipSender(){ return channel_tip_sender;}
  public ChannelChunkGetter getChannelChunkGetter(){ return channel_chunk_getter;}
  public Config getConfig(){ return config;}
  public WalletDatabase getWalletDB() {return wallet_db; }
  public StubHolder getStubHolder() {return stub_holder; }

  public void testSelf()
		throws Exception
  {
    logger.log(Level.FINEST, "Starting test self");

    Random rnd = new Random();
    int port = rnd.nextInt(60000) + 1024;
    port = ChannelGlobals.NETWORK_PORT;

      SslContext ssl_ctx = GrpcSslContexts.forClient()
      .trustManager(SnowTrustManagerFactorySpi.getFactory(null))
    .build();

    ManagedChannel channel = NettyChannelBuilder
      .forAddress("127.0.0.1", port)
      .useTransportSecurity()
      .sslContext(ssl_ctx)
      .build();

    StargateServiceBlockingStub stub = StargateServiceGrpc.newBlockingStub(channel);
    stub.getDHTPeers(GetDHTPeersRequest.newBuilder().setSelfPeerInfo(dht_server.getSignedPeerInfoSelf()).build());

  }

  public AddressSpecHash getNodeID()
  {
    AddressSpec spec = wallet_db.getAddresses(0);
    return AddressUtil.getHashForSpec(spec); 
  }

  public SignedMessage signMessage(SignedMessagePayload starting_payload)
    throws ValidationException
  {
    return ChannelSigUtil.signMessage( wallet_db.getAddresses(0), wallet_db.getKeys(0), starting_payload);

  }

}
