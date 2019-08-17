package channels;

import duckutil.ConfigMem;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import java.util.Random;
import java.util.TreeMap;
import org.junit.BeforeClass;
import org.junit.Test;
import snowblossom.channels.*;
import snowblossom.channels.proto.*;
import snowblossom.channels.proto.StargateServiceGrpc.StargateServiceBlockingStub;
import snowblossom.client.WalletUtil;
import snowblossom.lib.*;
import snowblossom.lib.Globals;
import snowblossom.proto.WalletDatabase;

public class CertGenTest
{

  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  @Test
  public void testGenAndService()
    throws Exception
  {
    TreeMap<String,String> config_map = new TreeMap<>();
    config_map.put("key_count", "1");

    WalletDatabase db = WalletUtil.makeNewDatabase(new ConfigMem(config_map), new NetworkParamsProd());

    Random rnd = new Random();
    int port = rnd.nextInt(60000) + 1024;
    port = 9118;
    
    Server s = NettyServerBuilder
      .forPort(port)
      .addService(new DHTServer(null))
      .sslContext(CertGen.getServerSSLContext(db))
      .build();
    s.start();

    AddressSpecHash server_spec = AddressUtil.getHashForSpec(db.getAddresses(0));

    { // With expected hash
      SslContext ssl_ctx = GrpcSslContexts.forClient()
        .trustManager(SnowTrustManagerFactorySpi.getFactory(server_spec))
      .build();

      ManagedChannel channel = NettyChannelBuilder
        .forAddress("localhost", port)
        .useTransportSecurity()
        .sslContext(ssl_ctx)
        .build();

      StargateServiceBlockingStub stub = StargateServiceGrpc.newBlockingStub(channel);
      try
      {
      stub.getDHTPeers(GetDHTPeersRequest.newBuilder().build());
      }
      catch(Throwable t)
      {
        Throwable a = t;
        while(a != null)
        {
          System.out.println("------------------------");
          for(StackTraceElement e : a.getStackTrace())
          {
            System.out.println(e);
          }
          a = a.getCause();
          System.out.println("------------------------");
        }
        throw t;
      }
    }

    { // Without expected hash
      SslContext ssl_ctx = GrpcSslContexts.forClient()
        .trustManager(SnowTrustManagerFactorySpi.getFactory(null))
      .build();

      ManagedChannel channel = NettyChannelBuilder
        .forAddress("localhost", port)
        .useTransportSecurity()
        .sslContext(ssl_ctx)
        .build();

      StargateServiceBlockingStub stub = StargateServiceGrpc.newBlockingStub(channel);
      stub.getDHTPeers(GetDHTPeersRequest.newBuilder().build());
    }



  }

}
