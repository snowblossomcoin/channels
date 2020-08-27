package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.ChannelSettings;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.SignedMessagePayload;
import snowblossom.client.StubHolder;
import snowblossom.lib.ValidationException;
import snowblossom.util.proto.SymmetricKey;

/**
 * Main view and access to a channel for modules that shouldn't have full low level access
 * to the underlying structure.
 */
public class ChannelAccess
{
  private final ChannelContext ctx;
  private final ChannelNode node;
  private final StubHolder snow_stub;

  public ChannelAccess(ChannelNode node, ChannelContext ctx)
  {
    this.node = node;
    this.ctx = ctx;
    this.snow_stub = node.getStubHolder();
  }

  public void watch(ChannelWatcherInterface watcher)
  {
    node.getChannelSubscriber().registerWatcher(ctx.cid, watcher);
  }

  public ChannelBlockSummary getHead()
  {
    return ctx.block_ingestor.getHead();
  }

  public long getHeight()
  {
    return ctx.block_ingestor.getHeight();
  }

  public int getMissingChunks()
  {
    return ChunkMapUtils.getWantList(ctx).size();
  }

  public StubHolder getSnowStub()
  {
    return snow_stub;
  }

  public List<SignedMessage> getOutsiderByTime(int max_return, boolean oldest_first)
  {
    TreeMap<Double, SignedMessage> message_map = new TreeMap<>();
		Random rnd = new Random();

    for(SignedMessage sm : ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 50000).values())
    {
      try
      { 
        ChannelValidation.validateOutsiderContent(sm, ctx.block_ingestor.getHead(), ctx);
        SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

        double v = payload.getTimestamp();
        v+=rnd.nextDouble();

				if (!oldest_first)
				{
					v = v * -1.0;
				}

        message_map.put( v, sm );
      }
      catch(ValidationException e){}

      while(message_map.size() > max_return)
      { 
        message_map.pollLastEntry();
      }
		}
		LinkedList<SignedMessage> lst = new LinkedList<>();
		lst.addAll(message_map.values());

		return lst;
  }

	public void createBlockWithContent(List<SignedMessage> content)
    throws ValidationException
  {
    BlockGenUtils.createBlockForContent(ctx, content, node.getUserWalletDB());
  }

  /**
   * Signs the given content with node key and makes block
   */
  public void createBlockWithContentUnsigned(List<ContentInfo> content)
    throws ValidationException
  {
    LinkedList<SignedMessage> lst=new LinkedList<>();

    for(ContentInfo ci : content)
    {
      SignedMessage sm = 
        ChannelSigUtil.signMessage(
          node.getUserWalletDB().getAddresses(0), 
          node.getUserWalletDB().getKeys(0), 
          SignedMessagePayload.newBuilder().setContentInfo(ci).build());
      lst.add(sm);
    }

    createBlockWithContent(lst);
  }

  public void createBlockForFiles(File base_path)
    throws ValidationException, java.io.IOException
  {
    FileBlockImportSettings settings = new FileBlockImportSettings(ctx, base_path, node.getUserWalletDB(), null);

    settings.setupEncrypt(ctx, node);

    BlockGenUtils.createBlockForFiles(settings);
  }


  /**
   * Strongly recommend taking existing settings and modify rather than starting from scatch
   */
  public void updateSettings(ChannelSettings new_settings)
    throws ValidationException
  {
    BlockGenUtils.createBlockForSettings(ctx, new_settings, node.getUserWalletDB());
  }

  public ChannelAccess openOtherChannel(ChannelID cid)
  {
    return new ChannelAccess(node, node.getChannelSubscriber().openChannel(cid));

  }

}
