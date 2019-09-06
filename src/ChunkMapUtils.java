package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;
import snowblossom.lib.ChainHash;
import snowblossom.lib.trie.ByteStringComparator;

public class ChunkMapUtils
{

  /*
   * Inside the ChunkMap db, we put bset{contend_id}{chunk_nu} mapping to chunk number (as 4 bytes)
   * and the data itself under data{content_id}{chunk_nu}
   *
   * That way when we want the bitset of which ones we have, we can do that quickly by looking up the i
   * bset{content_id} keys rather than shifting all that data.
   *
   */
  public static ByteString set_prefix = ByteString.copyFrom("bset".getBytes());
  public static ByteString data_prefix = ByteString.copyFrom("data".getBytes());

 

  public static void storeChunk(ChannelContext ctx, ChainHash content_id, int chunk_number, ByteString chunk_data)
  {
    ByteString key = getKey(content_id, chunk_number);
    TreeMap<ByteString, ByteString> update_map = new TreeMap<>(new ByteStringComparator());


    update_map.put( data_prefix.concat(key), chunk_data );
    update_map.put( set_prefix.concat(key), key.substring( key.size() - 4) );

    ctx.db.getChunkMap().putAll(update_map);

  }

  public static BitSet getSavedChunksSet(ChannelContext ctx, ChainHash content_id)
  {
    
    Map<ByteString, ByteString> store_map = ctx.db.getChunkMap().getByPrefix( set_prefix.concat(content_id.getBytes()), 1000000);

    BitSet bs = new BitSet();
    for(ByteString v : store_map.values())
    {
      int n = v.asReadOnlyByteBuffer().getInt();
      bs.set(n);
    }

    return bs;
  }

  public static ByteString getChunk(ChannelContext ctx, ChainHash content_id, int chunk_number)
  {
    ByteString key = getKey(content_id, chunk_number);

    ByteString v = ctx.db.getChunkMap().get( data_prefix.concat(key));

    return v;
  }

  private static ByteString getKey(ChainHash content_id, int chunk_number)
  {
    byte b[]=new byte[4];
    ByteBuffer bb = ByteBuffer.wrap(b);
    bb.putInt(chunk_number);

    return content_id.getBytes().concat(ByteString.copyFrom(b));
  }

}
