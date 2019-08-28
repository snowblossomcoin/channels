package snowblossom.channels;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.channels.proto.ChannelServiceGrpc.ChannelServiceBlockingStub;
import snowblossom.channels.proto.ChannelServiceGrpc.ChannelServiceStub;
import snowblossom.channels.proto.StargateServiceGrpc.StargateServiceBlockingStub;
import snowblossom.channels.proto.StargateServiceGrpc.StargateServiceStub;
import snowblossom.lib.AddressSpecHash;

public class PeerLink implements StreamObserver<PeerList>
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  private ChannelPeerInfo info;

  private StargateServiceStub stargate_stub;
  private StargateServiceBlockingStub stargate_blocking_stub;
  private ChannelServiceStub channel_stub;
  private ChannelServiceBlockingStub channel_blocking_stub;
  private volatile boolean closed;
  private volatile long last_recv;
	private ManagedChannel channel;

  // We are the client
  public PeerLink(ChannelPeerInfo info, ChannelNode node)
		throws Exception
  {
    last_recv = System.currentTimeMillis();
    this.node = node;
    this.info = info;

    ConnectInfo conn_info = findConnectInfo(info, node.getNetworkExaminer());
    if (conn_info == null)
    {
      throw new Exception("Unable to connect - no protocols in common");
    }
		AddressSpecHash node_id = new AddressSpecHash(info.getAddressSpecHash());

    SslContext ssl_ctx = GrpcSslContexts.forClient()
      .trustManager(SnowTrustManagerFactorySpi.getFactory(node_id))
    .build();

    channel = NettyChannelBuilder
      .forAddress(conn_info.getHost(), conn_info.getPort())
			.useTransportSecurity()
			.sslContext(ssl_ctx)
      .build();

    stargate_stub = StargateServiceGrpc.newStub(channel);
    stargate_blocking_stub = StargateServiceGrpc.newBlockingStub(channel);

    channel_stub = ChannelServiceGrpc.newStub(channel);
    channel_blocking_stub = ChannelServiceGrpc.newBlockingStub(channel);
  }

  public AddressSpecHash getNodeID()
  {
    return new AddressSpecHash(info.getAddressSpecHash());
  }

  public void getDHTPeers(SignedMessage self_peer_info)
  {
    stargate_stub.getDHTPeers(GetDHTPeersRequest.newBuilder().setSelfPeerInfo(self_peer_info).build(), this);
  }

  public StargateServiceBlockingStub getStargateBlockingStub() {return stargate_blocking_stub; }
  public StargateServiceStub getStargateAsyncStub() {return stargate_stub; }

  public ChannelServiceBlockingStub getChannelBlockingStub() {return channel_blocking_stub; }
  public ChannelServiceStub getChannelAsyncStub() {return channel_stub; }
  

  public static ConnectInfo findConnectInfo(ChannelPeerInfo info, NetworkExaminer net_ex)
  {
    if (net_ex.hasIpv6())
    {
      ConnectInfo conn_info = info.getConnectInfosMap().get("ipv6");
      if (conn_info != null)
      {
        return conn_info;
      }
    }

    if (net_ex.hasIpv4())
    {
      ConnectInfo conn_info = info.getConnectInfosMap().get("ipv4");
      if (conn_info != null)
      {
        return conn_info;
      }
    }

    return null;
  }

  public boolean isGood()
  {
		if (closed) return false;
		if (last_recv + ChannelGlobals.PEER_LINK_TIMEOUT < System.currentTimeMillis())
		{
			return false;
		}
    return true;
  }

  public void close()
  {
    if (closed) return;
    closed=true;
    try
    { 
      if (channel != null)
      { 
        channel.shutdownNow();
        channel.awaitTermination(3, TimeUnit.SECONDS);
      }
    }
    catch(Throwable e){}
  }

  public void pokeRecv()
  {
    last_recv = System.currentTimeMillis();
  }


  @Override
  public void onCompleted()
  {
  }

  @Override
  public void onError(Throwable t)
  {
    //logger.log(Level.WARNING, "wobble", t);
		close();
  }

  @Override
  public void onNext(PeerList peer_list)
  {
    last_recv = System.currentTimeMillis();
    for(SignedMessage sm : peer_list.getPeersList())
    {
      node.getDHTServer().importPeer(sm);
    }
  }

}

