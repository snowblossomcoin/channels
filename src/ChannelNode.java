package snowblossom.channels;

import duckutil.Config;
import duckutil.ConfigFile;
import snowblossom.lib.*;
import snowblossom.proto.WalletDatabase;
import snowblossom.proto.AddressSpec;
import snowblossom.lib.AddressSpecHash;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.KeyPair;
import snowblossom.channels.*;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;

import com.google.protobuf.ByteString;

import java.security.cert.X509Certificate;
import java.util.Random;

import snowblossom.channels.proto.StargateServiceGrpc.StargateServiceBlockingStub;
import snowblossom.channels.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslContext;
import snowblossom.lib.db.rocksdb.JRocksDB;
import snowblossom.client.WalletUtil;

import java.io.File;


public class ChannelNode
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private WalletDatabase wallet_db;
  private NetworkParams params;
  private ChannelsDB db;
  private NetworkExaminer net_ex;
  private PeerManager peer_manager;

  public static void main(String args[])
		throws Exception
  {
    Globals.addCryptoProvider();

    if (args.length != 1)
    { 
      logger.log(Level.SEVERE, "Incorrect syntax. Syntax: ChannelNode <config_file>");
      System.exit(-1);
    }

    ConfigFile config = new ConfigFile(args[0]);

    LogSetup.setup(config);
		new ChannelNode(config);

  }

  public ChannelNode(Config config)
		throws Exception
  {

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
    net_ex = new NetworkExaminer(this);
    peer_manager = new PeerManager(this);
   
    startServer();

    testSelf();

  }

  private void startServer()
    throws Exception
  {  
    int port = 9118;

    Server s = NettyServerBuilder
      .forPort(port)
      .addService(new DHTServer(this))
      .sslContext(CertGen.getServerSSLContext(wallet_db))
      .build();
    s.start();

  }

  public NetworkExaminer getNetworkExaminer(){return net_ex;}
  public PeerManager getPeerManager(){return peer_manager;}
  public ChannelsDB getDB(){return db;}

  public void testSelf()
		throws Exception
  {
    logger.log(Level.FINEST, "Starting test self");

    Random rnd = new Random();
    int port = rnd.nextInt(60000) + 1024;
    port = 9118;
      SslContext ssl_ctx = GrpcSslContexts.forClient()
      .trustManager(SnowTrustManagerFactorySpi.getFactory(null))
    .build();

    ManagedChannel channel = NettyChannelBuilder
      .forAddress("127.0.0.1", port)
      .useTransportSecurity()
      .sslContext(ssl_ctx)
      .build();

    StargateServiceBlockingStub stub = StargateServiceGrpc.newBlockingStub(channel);
    stub.getDHTPeers(GetDHTPeersRequest.newBuilder().build());

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
