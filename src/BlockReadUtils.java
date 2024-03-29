package snowblossom.channels;

import com.google.protobuf.ByteString;
import duckutil.AtomicFileOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.ChainHash;
import snowblossom.lib.CipherUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.ValidationException;
import snowblossom.node.StatusInterface;
import snowblossom.proto.WalletDatabase;
import snowblossom.util.proto.SymmetricKey;

public class BlockReadUtils
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  public static void extractFiles(ChannelContext ctx, File base_path, StatusInterface status, WalletDatabase wallet)
    throws ValidationException, java.io.IOException
  {

    if (status == null) status = new DummyStatusInterface();

    ProcessStatus ps = new ProcessStatus();

    ps.set("cid", ctx.cid.toString());
    ps.set("path", base_path.toString());

    Map<String, ByteString> data_map = ChanDataUtils.getAllData(ctx, "/web/");

    ps.add("total_files", data_map.size());

    base_path.mkdirs();

    for(Map.Entry<String, ByteString> me : data_map.entrySet())
    {
      status.setStatus("Extraction in progress: " + ps.getStatusLine());

      // Now, we have to assume that the data is hostile
      String key = me.getKey();

      key = key.substring(5); // skip the "/web/" part
      ChainHash msg_id = new ChainHash(me.getValue());

      try
      {
        extractFile(ctx, base_path, key, msg_id, ps, wallet);
      }
      catch(Throwable t)
      {
        ps.add("error");
        ps.add("error_undefined");
        logger.log(Level.WARNING,"Extract file", t);
      }
    }

    status.setStatus("Extraction complete: " + ps.getStatusLine());

  }

  private static void extractFile(ChannelContext ctx, File base_path, String file_key, ChainHash msg_id, ProcessStatus ps, WalletDatabase wallet)
    throws ValidationException, java.io.IOException
  {
    File curr_path = base_path;
    File last_parent = null;

    StringTokenizer stok = new StringTokenizer(file_key, "/");
    while(stok.hasMoreTokens())
    {
      String name = stok.nextToken();
      if (name.equals("..") || (name.length()==0) || (name.equals(".")))
      {
        ps.add("error");
        ps.add("error_malformed_path");
        logger.log(Level.WARNING,"rejected Key path: " + file_key);
        return;
      }

      last_parent = curr_path;
      last_parent.mkdir();

      curr_path = new File(last_parent, name);

      // Being extra paranoid.
      // Only accept if the parent of the new file is actually the same as the parent
      if (!last_parent.getCanonicalPath().equals( curr_path.getParentFile().getCanonicalPath()))
      {
        ps.add("error");
        ps.add("error_malformed_path");
        logger.log(Level.WARNING,"rejected Key path: " + file_key);
        return;
      }
    }

    SignedMessage sm = ctx.db.getContentMap().get(msg_id.getBytes());
    if (sm == null)
    {
      ps.add("error");
      ps.add("error_no_content");
      return;
    }

    ContentInfo ci = ChannelSigUtil.quickPayload(sm).getContentInfo();
    if (ci.getContentDataMap().containsKey("auto_gen_index"))
    {
      ps.add("skipped_auto_index");
      return;
    }

    AtomicFileOutputStream out = new AtomicFileOutputStream(curr_path);
    try
    {
      streamContentOut(ctx, msg_id, ci, out, wallet);

      out.flush();
      out.close();
      ps.add("complete");
      ps.add("total_bytes", ci.getContentLength());
    }
    catch(java.io.IOException e)
    {
      out.abort();
      ps.add("error");
      ps.add("error_io");
    }

  }

  public static boolean checkKey(ChannelContext ctx, ContentInfo ci, WalletDatabase wallet)
    throws ValidationException
  {
    String key_id = HexUtil.getHexString(ci.getEncryptedKeyId());

    SymmetricKey sym_key = ChannelAccess.getKeyForChannel(ctx, key_id, wallet);

    if (sym_key == null)
    {
      return false;
    }

    return true;
  }


  public static void streamContentOut(ChannelContext ctx, ChainHash msg_id, ContentInfo ci, OutputStream out, WalletDatabase wallet)
    throws java.io.IOException, ValidationException
  {
    // TODO - check hash of stream as we stream it
    if (ci.getContentLength() == ci.getContent().size())
    {
      //TODO - support encrypted data direct in CI
      out.write(ci.getContent().toByteArray());
      return;
    }

    int total_chunks = MiscUtils.getNumberOfChunks(ci);

    SymmetricKey sym_key = null;
    if (ci.getEncryptedKeyId().size() > 0)
    {
      String key_id = HexUtil.getHexString(ci.getEncryptedKeyId());

      sym_key = ChannelAccess.getKeyForChannel(ctx, key_id, wallet);

      if (sym_key == null)
      {
        throw new ValidationException("Unable to load key: " + key_id);
      }

    }

    for(int i=0; i<total_chunks; i++)
    {
      // TODO - decrypt as needed
      ByteString chunk_data = ChunkMapUtils.getChunk(ctx, msg_id, i);
      logger.finer("Get chunk data: " + msg_id + " " + i + " sz:" + chunk_data.size());
      if ((chunk_data == null) || (chunk_data.size() == 0))
      {
        logger.warning("Missing chunk data: " + msg_id + "/" + i);
        throw new java.io.IOException("Missing chunk data: " + msg_id + "/" + i);
      }

      if (sym_key != null)
      {
        chunk_data = CipherUtil.decryptSymmetric(sym_key, chunk_data);
      }
      out.write(chunk_data.toByteArray());
    }
  }

}
