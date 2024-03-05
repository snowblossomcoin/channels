package snowblossom.channels;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockSummary;
import snowblossom.channels.proto.ChannelSettings;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.SignedMessagePayload;
import snowblossom.client.StubHolder;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.ValidationException;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.WalletDatabase;
import snowblossom.proto.WalletKeyPair;
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

  public ChannelID getChannelID()
  {
    return ctx.cid;
  }

  public List<SignedMessage> getOutsiderByTime(int max_return, boolean oldest_first)
  {
    TreeMap<Double, SignedMessage> message_map = new TreeMap<>();
    Random rnd = new Random();

    for(SignedMessage sm : ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 50000, true).values())
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

  /**
   * Reads file from channel or null if it isn't there
   */
  public ByteString readFile(String path)
    throws java.io.IOException, ValidationException
  {
    ByteString content_id = ChanDataUtils.getData(ctx, path);
    if (content_id == null) return null;
    SignedMessage content_msg = ctx.db.getContentMap().get(content_id);
    if (content_msg == null) return null;

    ContentInfo ci = ChannelSigUtil.quickPayload(content_msg).getContentInfo();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BlockReadUtils.streamContentOut(ctx, new ChainHash(content_id), ci, out, node.getUserWalletDB());

    return ByteString.copyFrom(out.toByteArray());
  }

  public WalletDatabase getUserWalletDB()
  {
    return node.getUserWalletDB();
  }

  public boolean amIBlockSigner()
    throws Exception
  {

    ChannelBlockSummary head_summary = ctx.block_ingestor.getHead();
    if (head_summary != null)
    {
      ChannelSettings settings = head_summary.getEffectiveSettings();

      HashSet<ByteString> allowed_signers = new HashSet<>();
      allowed_signers.addAll(settings.getBlockSignerSpecHashesList());
      allowed_signers.addAll(settings.getAdminSignerSpecHashesList());

      WalletDatabase wdb = node.getUserWalletDB();

      AddressSpec addr = wdb.getAddresses(0);

      AddressSpecHash hash = AddressUtil.getHashForSpec(addr);
      if (allowed_signers.contains(hash.getBytes()))
      {
        return true;
      }
      else
      {
        return false;
      }
    }
    else
    {
      return false;
    }

  }

  public SymmetricKey getCommonKeyForChannel()
    throws ValidationException
  {
    String key_id = getCommonKeyId();
    if (key_id == null) return null;

    return getKeyForChannel(ctx, key_id, node.getUserWalletDB());
  }

  public static SymmetricKey getKeyForChannel(ChannelContext ctx, String key_id, WalletDatabase wallet)
    throws ValidationException
  {
    SymmetricKey sym_key = ChannelCipherUtils.getKeyFromChannel(ctx, key_id, wallet.getKeys(0));

    if (sym_key == null)
    {
      WalletKeyPair wkp = ChannelCipherUtils.getKeyForChannel(ctx.cid, wallet);
      sym_key = ChannelCipherUtils.getKeyFromChannel(ctx, key_id, wkp);
    }

    return sym_key;
  }


  public String getCommonKeyId()
  {
    return ChannelCipherUtils.getCommonKeyID(ctx);
  }

  public void addKey(AddressSpec hash)
    throws ValidationException
  {
    addKeys(ImmutableList.of(hash));
  }
  public void addKeys(List<AddressSpec> addrs)
    throws ValidationException
  {
    ChannelCipherUtils.addKeys(node, ctx, addrs);
  }

  public boolean hasKeyInChannel(AddressSpecHash addr)
  {
    String key_id = ChannelCipherUtils.getCommonKeyID(ctx);
    return ChannelCipherUtils.hasKeyInChannel(ctx, key_id, addr);

  }

  public SignedMessage getContentByHash(ChainHash content_id)
  {
    return ctx.db.getContentMap().get(content_id.getBytes());
  }

  public ChainHash getBlockIdForContent(ChainHash content_id)
  {
    ByteString s = ctx.db.getContentToBlockMap().get(content_id.getBytes());
    if (s == null) return null;
    return new ChainHash(s);

  }

  public ChannelBlock getBlockByHash(ChainHash block_id)
  {
    return ctx.db.getBlockMap().get(block_id.getBytes());

  }

  public ChainHash getBlockHashAtHeight(long height)
  {
    return ctx.db.getBlockHashAtHeight(height);
  }


  public void broadcast(SignedMessage sm)
    throws ValidationException
  {
    ctx.block_ingestor.ingestContent(sm);

  }

}
