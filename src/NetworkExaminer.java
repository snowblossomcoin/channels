package snowblossom.channels;

import duckutil.NetUtil;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import snowblossom.channels.proto.*;

public class NetworkExaminer
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

	private String ipv4_host;
	private String ipv6_host;
  private ChannelNode node;

	public NetworkExaminer(ChannelNode node)
	{
    this.node = node;
		updateHosts();
	}

  

  // TODO - on failure of this, might want to use getAllAddresses().
  // Any non-link-local ipv6 address is a good bet for IPV6.
  // Any non-private ipv4 address is also probably good.
  private void updateHosts()
  {
		try{
			ipv4_host = NetUtil.getUrlLine("http://ipv4-lookup.snowblossom.org/myip");
		}
		catch(Throwable t){ipv4_host=null;}

		try{
			ipv6_host = NetUtil.getUrlLine("http://ipv6-lookup.snowblossom.org/myip");
		}
		catch(Throwable t){ipv6_host=null;}

    logger.info("IPV4: " + ipv4_host);
    logger.info("IPV6: " + ipv6_host);

    try
    {
      tryUpnp();
    }
    catch(Exception e)
    {
      logger.info("UPNP failure: " + e);
    }
  }

  protected void tryUpnp() throws Exception
  {
    int port = node.getPort();

    GatewayDiscover discover = new GatewayDiscover();
    discover.discover();
    GatewayDevice d = discover.getValidGateway();
    if (d != null)
    {
      logger.info(String.format("Found UPNP gateway %s %s", d.getModelName(), d.getModelDescription()));
      InetAddress localAddress = d.getLocalAddress();
      PortMappingEntry portMapping = new PortMappingEntry();
      if (d.getSpecificPortMappingEntry(port,"TCP",portMapping)) 
      {
        String mapped = portMapping.getInternalClient();
        String local = localAddress.getHostAddress();
        if (local.equals(mapped) && (port == portMapping.getInternalPort()))
        {
          logger.info("Port already mapped to me.  Cool.");
        }
        else
        {
          logger.warning(String.format("Port %d already mapped to %s:%d. Consider using a different port.", port, mapped, portMapping.getInternalPort())); 
          logger.warning(String.format("While I am on %s:%d", local, port));
        }
      }
      else
      {
        if(d.addPortMapping(port, port, localAddress.getHostAddress(),"TCP","snowchannel"))
        {
          logger.info("Port mapped with upnp gateway");
        }
      }
    }

  }

  /**
   * Get all local addresses on all interfaces, ipv4 and ipv6 minus any loopback addresses.
   * Might or might not be globally routable, will include internal IPs (behind NAT)
   * and IPv6 link-local addresses.
   */
  public Set<InetAddress> getAllAddresses()
    throws java.io.IOException
  {
		HashSet<InetAddress> addrs = new HashSet<>();

		Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
		while(faces.hasMoreElements())
		{ 
			NetworkInterface ni = faces.nextElement();
			for(InterfaceAddress ia : ni.getInterfaceAddresses())
			{ 
				InetAddress a = ia.getAddress();
				if (!a.	isLoopbackAddress())
				{
          InetAddress a2 = InetAddress.getByAddress(a.getAddress());
					addrs.add(a2);
				}
			}
		}
		return addrs;

  }

  public boolean hasIpv4() { return ipv4_host != null; }
  public boolean hasIpv6() { return ipv6_host != null; }

  public ChannelPeerInfo createPeerInfo()
  {
    ChannelPeerInfo.Builder info = ChannelPeerInfo.newBuilder();

    info.setVersion(ChannelGlobals.VERSION);
    info.setAddressSpecHash(node.getNodeID().getBytes());

    if (ipv6_host != null)
    {
      info.putConnectInfos( "ipv6", ConnectInfo.newBuilder()
        .setProtocol("ipv6")
        .setHost(ipv6_host)
        .setPort( node.getPort() )
        .build() );
    }

    if (ipv4_host != null)
    {
      info.putConnectInfos( "ipv4", ConnectInfo.newBuilder()
        .setProtocol("ipv4")
        .setHost(ipv4_host)
        .setPort( node.getPort() )
        .build() );
    }

    return info.build();
  }



}
