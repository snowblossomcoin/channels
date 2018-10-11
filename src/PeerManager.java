package snowblossom.channels;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import snowblossom.lib.AddressSpecHash;

import snowblossom.channels.proto.*;
import com.google.common.collect.TreeMultimap;

public class PeerManager
{
  private ChannelNode node;

  // protected by sync on itself
  private HashMap<AddressSpecHash, PeerLink> link_map;

  // protected by sync on itself
  private TreeMultimap<String, AddressSpecHash> reason_map;

  public PeerManager(ChannelNode node)
  {
    this.node = node;
    link_map = new HashMap<>(256, 0.5f);
    reason_map = TreeMultimap.create();

  }

  public PeerLink connectNode(ChannelPeerInfo info, String reason)
  {
    AddressSpecHash node_id = new AddressSpecHash(info.getAddressSpecHash());
    synchronized(reason_map)
    {
      reason_map.put(reason, node_id);
    }

    PeerLink link = null;
    synchronized(link_map)
    {
      link = link_map.get(node_id);
    }
    if (link != null) return link;

    link = new PeerLink(info);
    synchronized(link_map)
    {
      link_map.put(node_id, link);
    }
    return link;
  }


  public List<PeerLink> getPeersWithReason(String reason)
  {
    LinkedList<AddressSpecHash> ids = new LinkedList<>();
    synchronized(reason_map)
    {
      ids.addAll(reason_map.get(reason));

    }

    LinkedList<PeerLink> links = new LinkedList<>();
    synchronized(link_map)
    {
      for(AddressSpecHash id : ids )
      {
        PeerLink link = link_map.get(id);
        if (link != null)
        {
          links.add(link);
        }
      }
    }

    return links;
  
  }

  
}
