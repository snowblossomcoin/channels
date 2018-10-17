package snowblossom.channels;

import duckutil.NetUtil;
import snowblossom.channels.proto.*;

public class NetworkExaminer
{
	private String ipv4_host;
	private String ipv6_host;
  private ChannelNode node;

	public NetworkExaminer(ChannelNode node)
	{
    this.node = node;
		updateHosts();
	}


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

    System.out.println("IPV4: " + ipv4_host);
    System.out.println("IPV6: " + ipv6_host);
  }

  public boolean hasIpv4() { return ipv4_host != null; }
  public boolean hasIpv6() { return ipv6_host != null; }


  public ChannelPeerInfo createPeerInfo()
  {
    ChannelPeerInfo.Builder info = ChannelPeerInfo.newBuilder();

    info.setVersion(ChannelGlobals.VERSION);
    info.setAddressSpecHash(node.getNodeID().getBytes());



    return info.build();
  }



}
