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

/**
 * Mostly for DHT peering.  However, this peer link is also used for outbound
 * links to peers for ChannelLinks.  In which case, the channel links will call
 * pokeRecv() to keep this link alive.
 *
 */
public class PeerLink implements StreamObserver<PeerList>
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private final ChannelNode node;
  private final ChannelPeerInfo info;

  private final StargateServiceStub stargate_stub;
  private final StargateServiceBlockingStub stargate_blocking_stub;
  private final ChannelServiceStub channel_stub;
  private final ChannelServiceBlockingStub channel_blocking_stub;
	private final ManagedChannel channel;
  private final AddressSpecHash remote_node_id;
  
  private volatile boolean closed;
  private volatile long last_recv;

  // We are the client
  public PeerLink(ChannelPeerInfo info, ChannelNode node)
		throws Exception
  {
    last_recv = System.currentTimeMillis();
    this.node = node;
    this.info = info;

		this.remote_node_id = new AddressSpecHash(info.getAddressSpecHash());

    ConnectInfo conn_info = findConnectInfo();
    if (conn_info == null)
    {
      throw new Exception("Unable to connect - no protocols in common");
    }

    SslContext ssl_ctx = GrpcSslContexts.forClient()
      .trustManager(SnowTrustManagerFactorySpi.getFactory(remote_node_id))
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
    return remote_node_id;
  }

  public void getDHTPeers(SignedMessage self_peer_info)
  {
    stargate_stub.getDHTPeers(GetDHTPeersRequest.newBuilder().setSelfPeerInfo(self_peer_info).build(), this);
  }

  public StargateServiceBlockingStub getStargateBlockingStub() {return stargate_blocking_stub; }
  public StargateServiceStub getStargateAsyncStub() {return stargate_stub; }

  public ChannelServiceBlockingStub getChannelBlockingStub() {return channel_blocking_stub; }
  public ChannelServiceStub getChannelAsyncStub() {return channel_stub; }
  

  private ConnectInfo findConnectInfo()
  {
    NetworkExaminer net_ex = node.getNetworkExaminer();
    LocalPeerDisco disco = node.getLocalPeerFinder().getDiscoCache(remote_node_id);

    //We have a lot of options here, since we have a potential for a triple stack
    // ipv4, ipv6 and tor in addition to ipv6(4/6) via tor.
    // For example, if you are on ipv4 only, you can reach ipv6 nodes via tor
    // or if a node is advertising ipv4 and ipv6 addresses but is actually blocked,
    // it might be better to reach that particular node via tor.
    // basically, we might want to monte carlo epsilon soft this.

    // If we have local discovery information for this node, prefer that over whatever
    // is in the PeerInfo.
    if (disco != null)
    {
      logger.fine("Using disco: " + disco);
      return ConnectInfo.newBuilder()
        .setHost(disco.getIpAddresses(0)) // The local peer finder trims the list to just the one we got this from
        .setPort(disco.getPort())
        .setProtocol("mcast_disco") // If something reads this proto, something has gone wrong.  But setting it just in case.
       .build();
    }

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

