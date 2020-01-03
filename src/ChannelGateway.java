package snowblossom.channels;


/**
 * This represents a read only view of channel data and a way to propogate certain changes.  
 * To be handled to things like moderation agents and such
 * who should not have a direct database access but need to read a wide range of information about channels
 * and the ability to publish some changes
 */
public class ChannelGateway
{
  private ChannelNode node;

  public ChannelGateway(ChannelNode node)
  {
    this.node = node;

  }


  public ChannelInterface getChannelInterface(ChannelID cid)
  {
    ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
    if (ctx == null) return null;

    return new ChannelInterface(ctx);

  }


}

