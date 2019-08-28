package snowblossom.channels;

import com.google.common.collect.TreeMultimap;
import duckutil.PeriodicThread;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;

public class PeerManager extends PeriodicThread
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;

  // protected by sync on itself
  private HashMap<AddressSpecHash, PeerLink> link_map;

  // protected by sync on itself
  private TreeMultimap<String, AddressSpecHash> reason_map;

  public PeerManager(ChannelNode node)
  {
    super(60000L);
    setName("PeerManager");
    setDaemon(true);

    this.node = node;
    link_map = new HashMap<>(256, 0.5f);
    reason_map = TreeMultimap.create();

  }

  public void runPass()
  {
    LinkedList<AddressSpecHash> to_remove = new LinkedList<>();
    synchronized(link_map)
    {
      for(Map.Entry<AddressSpecHash, PeerLink> me : link_map.entrySet())
      {
        PeerLink pl = me.getValue();
        if (!pl.isGood())
        {
          pl.close();
          to_remove.add(me.getKey());
        }
      }
      for(AddressSpecHash id : to_remove)
      {
        link_map.remove(id);
      }
    }
    if (to_remove.size() > 0)
    {
      logger.info(String.format("Removed %d stale links", to_remove.size()));
    }

  }

  public PeerLink connectNode(ChannelPeerInfo info, String reason)
    throws Exception
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

    link = new PeerLink(info, node);
    synchronized(link_map)
    {
      link_map.put(node_id, link);
    }
    return link;
  }
  
  public void removeReason(AddressSpecHash node_id, String reason)
  {
    synchronized(reason_map)
    {
      reason_map.remove(reason, node_id);
    }

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
          if (link.isGood())
          {
            links.add(link);
          }
        }
      }
    }
    return links;
  }
  
}
