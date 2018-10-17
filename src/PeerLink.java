package snowblossom.channels;

import snowblossom.channels.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import snowblossom.lib.AddressSpecHash;
import io.netty.handler.ssl.SslContext;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import snowblossom.lib.AddressSpecHash;
import io.grpc.stub.StreamObserver;

import snowblossom.channels.proto.StargateServiceGrpc.StargateServiceStub;

import java.util.logging.Level;
import java.util.logging.Logger;


public class PeerLink implements StreamObserver<PeerList>
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  private ChannelPeerInfo info;

  private StargateServiceStub stargate_stub;


  public PeerLink(ChannelPeerInfo info, ChannelNode node)
		throws Exception
  {
    this.node = node;
    this.info = info;

    ConnectInfo conn_info = findConnectInfo(info, node.getNetworkExaminer());
		AddressSpecHash node_id = new AddressSpecHash(info.getAddressSpecHash());

    SslContext ssl_ctx = GrpcSslContexts.forClient()
      .trustManager(SnowTrustManagerFactorySpi.getFactory(node_id))
    .build();

    ManagedChannel channel = NettyChannelBuilder
      .forAddress(conn_info.getHost(), conn_info.getPort())
      .usePlaintext(true)
			.useTransportSecurity()
			.sslContext(ssl_ctx)
      .build();

    stargate_stub = StargateServiceGrpc.newStub(channel);
  }

  public AddressSpecHash getNodeID()
  {
    return new AddressSpecHash(info.getAddressSpecHash());
  }


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

  @Override
  public void onCompleted()
  {}

  @Override
  public void onError(Throwable t)
  {
    logger.log(Level.WARNING, "wobble", t);
  }

  @Override
  public void onNext(PeerList peer_list)
  {

    for(SignedMessage sm : peer_list.getPeersList())
    {
      node.getDHTServer().importPeer(sm);
    }

  }
}

