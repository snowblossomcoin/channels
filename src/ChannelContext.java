package snowblossom.channels;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;

/**
 * Tracks state of a channel across multiple things.  Mostly so modules can find each other as relates to a single channel.
 * Created and tracked by ChannelSubscriber
 */
public class ChannelContext
{
  public SingleChannelDB db;
  public ChannelBlockIngestor block_ingestor;

  private ImmutableList<ChannelLink> links=ImmutableList.of();

  public synchronized void addLink(ChannelLink link)
  {
    LinkedList<ChannelLink> copy = new LinkedList<>();
    copy.addAll(links);
    copy.add(link);

    links = ImmutableList.copyOf(copy);
  }


  public synchronized void prune()
  {
    LinkedList<ChannelLink> good_list = new LinkedList<>();

    boolean bad=false;

    for(ChannelLink cl : links)
    {
      if (cl.isGood())
      {
        good_list.add(cl);
      }
      {
        bad = true;
      }
    }

    // If nothing is bad, don't bother remaking list
    if (bad)
    {
      links = ImmutableList.copyOf(good_list);
    }
  }

  public ImmutableList<ChannelLink> getLinks() {return links; }
  
}


