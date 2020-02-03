package snowblossom.channels;

import snowblossom.channels.proto.ChannelPeerInfo;
import snowblossom.channels.proto.ConnectInfo;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;

public class PrintUtil
{

  public static String print(ChannelPeerInfo info)
  {
    StringBuilder sb = new StringBuilder();
    AddressSpecHash hash = new AddressSpecHash(info.getAddressSpecHash());

    sb.append(AddressUtil.getAddressString(ChannelGlobals.NODE_ADDRESS_STRING, hash));
    sb.append(" ");
    sb.append(info.getVersion());
    sb.append("\n");

    for(String type : info.getConnectInfosMap().keySet())
    {
      ConnectInfo conn_info = info.getConnectInfosMap().get(type);
      sb.append(String.format("    %s - %s %d\n", type, conn_info.getHost(), conn_info.getPort()));

    }



    return sb.toString();

  }
}
