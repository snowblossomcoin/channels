package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.ExpiringLRUCache;
import io.grpc.stub.StreamObserver;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.ValidationException;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;

/**
 * Cache DHT entries, keep track of which things we have written.
 */
public class DHTCache
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  private ChannelNode node;
  private ExpiringLRUCache<ByteString, DHTDataSet> get_cache;
  private ExpiringLRUCache<ByteString, Boolean> save_cache;

  public DHTCache(ChannelNode node)
  {
    this.node = node;
    get_cache = new ExpiringLRUCache<>(ChannelGlobals.DHT_CACHE_ELEMENTS, ChannelGlobals.DHT_CACHE_EXPIRE);
    save_cache = new ExpiringLRUCache<>(ChannelGlobals.DHT_CACHE_ELEMENTS, ChannelGlobals.DHT_CACHE_EXPIRE);
  }

  public boolean haveWritten(ByteString element_id)
  {
    synchronized(save_cache)
    {
      Boolean b = save_cache.get(element_id);
      if (b == null) return false;
      return b;
    }
  }
  public void markWrite(ByteString element_id)
  {
    synchronized(save_cache)
    {
      save_cache.put(element_id, true);
    }
  }

  /**
   * The underlying calls are async, so the first call this for any element_id
   * will almost always return empty.  Call it again in a few seconds to get the real data.
   */
  public DHTDataSet getData(ByteString element_id)
  {
    DHTDataSet e = null;
    synchronized(get_cache)
    {
      e = get_cache.get(element_id);
    }
    if (e != null) return e;

    // Avoid triggering another get while in flight
    synchronized(get_cache)
    {
      get_cache.put(element_id, DHTDataSet.newBuilder().build());
    }

    node.getDHTServer().getDHTDataAsync(
      GetDHTRequest.newBuilder().setElementId(element_id).setDesiredResultCount(16).build(),
      new CacheWriter(element_id));

    return DHTDataSet.newBuilder().build();
  }

  public class CacheWriter implements StreamObserver<DHTDataSet>
  {
    private ByteString element_id;
    public CacheWriter(ByteString element_id)
    {
      this.element_id = element_id;
    }

    @Override
    public void onCompleted(){}

    @Override
    public void onError(Throwable t)
    {
      logger.warning("Error in get DHT data: " + t);
    }

    @Override
    public void onNext(DHTDataSet ds)
    {
      try
      {
        for(SignedMessage sm : ds.getDhtDataList())
        {
          SignedMessagePayload payload = ChannelSigUtil.validateSignedMessage(sm);

          if (!element_id.equals(payload.getDhtData().getElementId()))
          {
            throw new ValidationException("Not requested element_id");
          }
          AddressSpecHash signed_hash = AddressUtil.getHashForSpec(payload.getClaim());

          if (!signed_hash.equals(payload.getDhtData().getPeerInfo().getAddressSpecHash()))
          {
            throw new ValidationException("Signer of DHT data does not match peer info");
          }

        }
      }
      catch(ValidationException e)
      {
        logger.warning("Invalid data for DHT: " + e);
        return;
      }

      synchronized(get_cache)
      {
        get_cache.put(element_id, ds);
      }

    }
  }



}
