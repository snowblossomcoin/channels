package snowblossom.channels.warden;

import com.google.protobuf.ByteString;
import duckutil.Config;
import duckutil.webserver.DuckWebServer;
import duckutil.webserver.WebContext;
import duckutil.webserver.WebHandler;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelSigUtil;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.SignedMessagePayload;
import snowblossom.lib.ChainHash;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.Globals;
import snowblossom.lib.HexUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.WalletDatabase;

public class TimestampServiceWarden extends BaseWarden
{

  public TimestampServiceWarden(ChannelAccess channel_access, Config config)
  {
    super(channel_access, config);

    if (config.isSet("web_port"))
    {
      try
      {
        config.require("web_port");
        int port = config.getInt("web_port");

        String web_host = config.get("web_host");
        new DuckWebServer(web_host, port, new RootHandler(), 64);
      }
      catch(Exception e)
      {
        throw new RuntimeException(e);
      }
    }

  }

  @Override
  public void periodicRun() throws Exception
  {
    /*{
      Random rnd = new Random();
      byte [] b = new byte[32];
      rnd.nextBytes(b);
      sendTimestamp( ByteString.copyFrom(b), null);

    }*/
    while(true)
    {
      List<SignedMessage> content_list = channel_access.getOutsiderByTime(500, true);

      LinkedList<SignedMessage> to_include = new LinkedList<>();
      for(SignedMessage sm : content_list)
      {
        to_include.add(sm);
      }
      if (to_include.size() > 0)
      {
        channel_access.createBlockWithContent(to_include);
      }
      else
      {
        return;
      }
    }

  }

  @Override
  public void onContent(ChannelID cid, SignedMessage sm)
  {

  }

  @Override
  public void onBlock(ChannelID cid, ChannelBlock sm)
  {

  }


  public ChainHash sendTimestamp(ByteString hash, Map<String, ByteString> content_data)
    throws ValidationException
  {
    ContentInfo.Builder ci = ContentInfo.newBuilder();
    if (content_data != null)
    {
      ci.putAllContentDataMap(content_data);
    }
    ci.addBroadcastChannelIds( channel_access.getChannelID().getBytes() );
    ci.setContent(hash);
    ci.setContentLength(hash.size());
    ci.setContentHash( DigestUtil.hash(ci.getContent()) );

    {
      Random rnd = new Random();
      byte[] b = new byte[32];
      rnd.nextBytes(b);
      ci.setNonce(ByteString.copyFrom(b));
    }

    WalletDatabase wdb = channel_access.getUserWalletDB();

    SignedMessagePayload.Builder payload = SignedMessagePayload.newBuilder();
    payload.setContentInfo(ci.build());
    payload.setTimestamp(System.currentTimeMillis());

    SignedMessage sm = ChannelSigUtil.signMessage(
      wdb.getAddresses(0),
      wdb.getKeys(0),
      payload.build());

    channel_access.broadcast(sm);

    return new ChainHash(sm.getMessageId());

  }

  public class RootHandler implements WebHandler
  {
    public void handle(WebContext wctx) throws Exception
    {
      URI uri = wctx.getURI();
      String path = uri.getPath();

      if (path.equals("/api/v1/publish"))
      {

        TreeMap<String, String> query_map = extractQuery(uri);
        String hash = query_map.get("hash");
        ByteString hash_b = HexUtil.hexStringToBytes(hash);


        ChainHash message_hash = sendTimestamp( hash_b, null);

        JSONObject reply = new JSONObject();
        reply.put("input_hash", hash);
        reply.put("transaction_hash", message_hash.toString());

        wctx.setHttpCode(200);
        wctx.setContentType("application/json");
        wctx.out().println(reply);

        return;
      }
      if (path.equals("/api/v1/getproof"))
      {
        TreeMap<String, String> query_map = extractQuery(uri);
        String tx_hash_str = query_map.get("transaction_hash");

        ChainHash tx_hash = new ChainHash(tx_hash_str);

        JSONObject proof = getProof(tx_hash);

        wctx.setHttpCode(200);
        wctx.setContentType("application/json");
        wctx.out().println(proof);
        return;

      }

      wctx.out().println("Path not found");
      wctx.setHttpCode(404);

      System.out.println("path: " + path);


    }

    public TreeMap<String, String> extractQuery(URI uri)
    {
      String query = uri.getQuery();

      TreeMap<String, String> map = new TreeMap<>();

      StringTokenizer stok = new StringTokenizer(query, "&");
      while(stok.hasMoreTokens())
      {
        String part = stok.nextToken();
        StringTokenizer part_tok = new StringTokenizer(part, "=");
        String key = part_tok.nextToken();
        String value = part_tok.nextToken();
        map.put(key, value);

      }

      return map;
    }
  }


  public JSONObject getProof(ChainHash tx_hash)
  {
    SignedMessage sm = channel_access.getContentByHash(tx_hash);

    ChainHash block_id = channel_access.getBlockIdForContent(tx_hash);

    SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

    JSONObject top = new JSONObject();

    top.put("transaction_hash", tx_hash.toString());
    top.put("channel_block_hash", block_id.toString());

    ContentInfo tx_ci = payload.getContentInfo();

    top.put("data_hash", HexUtil.getHexString(tx_ci.getContent()));

    JSONArray proof_lst = new JSONArray();
    top.put("proofs", proof_lst);

    // data hash to transaction
    {
      ByteString payload_hash = DigestUtil.hash(sm.getPayload());
      proof_lst.add(getProofJson(sm.getPayload(), tx_ci.getContent(), payload_hash, "transaction payload"));
      proof_lst.add(getProofJson(payload_hash.concat(sm.getSignature()), payload_hash, sm.getMessageId(), "transaction outer"));
    }

    {
      ChannelBlock block = channel_access.getBlockByHash(block_id);
      LinkedList<ChainHash> block_content_lst = new LinkedList<>();
      for(SignedMessage bsm : block.getContentList())
      {
        ChainHash id = new ChainHash(bsm.getMessageId());
        block_content_lst.add(id);
      }


      SignedMessagePayload block_payload = ChannelSigUtil.quickPayload(block.getSignedHeader());
      ChannelBlockHeader header = block_payload.getChannelBlockHeader();
      ChainHash merkle_root = new ChainHash(header.getContentMerkle());

      proof_lst.addAll(getMerkleProof(tx_hash, block_content_lst, merkle_root));

      ByteString block_payload_hash = DigestUtil.hash(block.getSignedHeader().getPayload());
      proof_lst.add( getProofJson(block.getSignedHeader().getPayload(), merkle_root.getBytes(), block_payload_hash, "block payload"));

      proof_lst.add(getProofJson(block_payload_hash.concat(block.getSignedHeader().getSignature()), block_payload_hash, block_id.getBytes(), "block outer"));

    }
    return top;

  }
  public List<JSONObject> getMerkleProof(ChainHash tx_hash, List<ChainHash> merkle_list, ChainHash merkle_root)
  {
    ChainHash found_merkle = DigestUtil.getMerkleRootForTxList(merkle_list);

    if (!found_merkle.equals(merkle_root))
    {
      throw new RuntimeException("Merkle mismatch " + merkle_root + " " + found_merkle + " " + merkle_list);
    }


    LinkedList<JSONObject> out_lst = new LinkedList<>();

    HashSet<ChainHash> interest_set = new HashSet<>();
    interest_set.add(tx_hash);

    ArrayList<ChainHash> src = new ArrayList<>();
    src.addAll(merkle_list);

    ArrayList<ChainHash> sink = new ArrayList<>();
    MessageDigest md = DigestUtil.getMD();

    if (src.size() ==0)
    {
      throw new RuntimeException("Can't merkle empty list");
    }

    while(src.size() > 1)
    {

      for(int i=0; i<src.size(); i=i+2)
      {
        if (i+1 == src.size())
        {
          //Other implementations would hash this with itself.
          //I don't see the point of that.  Seems it would just make the merkle proof longer.

          sink.add(src.get(i));
        }
        else
        {
          ChainHash a = src.get(i);
          ChainHash b = src.get(i+1);

          ByteString data = a.getBytes().concat(b.getBytes());

          ChainHash h = new ChainHash(md.digest( data.toByteArray() ));


          if (interest_set.contains(a))
          {
            interest_set.add(h);
            out_lst.add( getProofJson(data, a.getBytes(), h.getBytes(), "merkle left"));
          }
          if (interest_set.contains(b))
          {
            interest_set.add(h);
            out_lst.add( getProofJson(data, b.getBytes(), h.getBytes(), "merkle right"));
          }

          sink.add(h);

        }
      }
      src = sink;
      sink = new ArrayList<>();
    }

    if(!src.get(0).equals(merkle_root))
    {
      throw new RuntimeException("Merkle fail");
    }


    return out_lst;

  }

  public JSONObject getProofJson(ByteString payload, ByteString input, ByteString expected, String label)
  {
    JSONObject tx=new JSONObject();

    tx.put("action","hash");
    tx.put("algo",Globals.BLOCKCHAIN_HASH_ALGO);
    tx.put("mid_data", HexUtil.getHexString(input));
    //tx.put("expected", HexUtil.getHexString(expected));
    ByteString out = DigestUtil.hash(payload);
    tx.put("output", HexUtil.getHexString(out));

    if (! out.equals(expected)) throw new RuntimeException("Unexpected result  " );
    if (label != null)
    {
      tx.put("label", label);
    }

    boolean found = false;

    for(int i=0; i<=payload.size() - input.size(); i++)
    {
      ByteString sub = payload.substring(i, i+input.size());
      if (sub.equals(input))
      {
        ByteString prefix = payload.substring(0, i);
        ByteString post = payload.substring(i+input.size());
        tx.put("prefix_data", HexUtil.getHexString(prefix));
        tx.put("postfix_data", HexUtil.getHexString(post));
        found=true;
        break;
      }

    }
    if (!found) throw new RuntimeException("Hash not found in payload " + HexUtil.getHexString(payload) + " " + tx);

    return tx;

  }

}
