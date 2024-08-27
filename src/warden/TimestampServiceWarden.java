package snowblossom.channels.warden;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import duckutil.Config;
import duckutil.LRUCache;
import duckutil.webserver.DuckWebServer;
import duckutil.webserver.WebContext;
import duckutil.webserver.WebHandler;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Logger;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import snowblossom.channels.ChannelAccess;
import snowblossom.channels.ChannelGlobals;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelSigUtil;
import snowblossom.channels.proto.ChannelBlock;
import snowblossom.channels.proto.ChannelBlockHeader;
import snowblossom.channels.proto.ContentInfo;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.SignedMessagePayload;
import snowblossom.client.AuditLog;
import snowblossom.client.SnowBlossomClient;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.Globals;
import snowblossom.lib.HexUtil;
import snowblossom.lib.PowUtil;
import snowblossom.lib.SnowFieldInfo;
import snowblossom.lib.ValidationException;
import snowblossom.proto.Block;
import snowblossom.proto.BlockHeader;
import snowblossom.proto.RequestBlock;
import snowblossom.proto.RequestTransaction;
import snowblossom.proto.SnowPowProof;
import snowblossom.proto.Transaction;
import snowblossom.proto.WalletDatabase;
import snowblossom.util.proto.AuditLogChain;
import snowblossom.util.proto.AuditLogItem;
import snowblossom.util.proto.AuditLogReport;

public class TimestampServiceWarden extends BaseWarden
{
  private final static long SNOW_CHECKPOINT_INTERVAL = 1800L * 1000L; // 30 min

  private static final Logger logger = Logger.getLogger("channelnode.warden");

  private SnowBlossomClient snow_client;

  private ImmutableMap<ChainHash, AuditLogItem> saved_audit_items= ImmutableMap.of();

  private boolean first_run=true;

  private LRUCache<ChainHash, Long> known_messages = new LRUCache<>(2500);

  private long snow_last_saved_block_time = 0L;

  private ChainHash snow_last_save = null;
  private ChainHash snow_mempool = null;




  public TimestampServiceWarden(ChannelAccess channel_access, Config config)
  {
    super(channel_access, config);

    config.require("audit_log_source");
    config.require("wallet_path");

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

    try
    {
      snow_client = new SnowBlossomClient(config);
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }

  }

  @Override
  public long getPeriod()
  {
    return 30L * 1000L;
  }

  @Override
  public void periodicRun() throws Exception
  {

    if (first_run)
    {
      Thread.sleep(8000);
      first_run=false;
    }
    /*{
      Random rnd = new Random();
      byte [] b = new byte[32];
      rnd.nextBytes(b);
      sendTimestamp( ByteString.copyFrom(b), null);

    }*/
    // Build blocks
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
        logger.info("Saving block with " + to_include.size() + " entries");
        channel_access.createBlockWithContent(to_include);
      }
      else
      {
        break;
      }
    }

    // Save to audit log
    recordSnowblossom();

  }

  private void recordSnowblossom()
    throws Exception
  {
    AddressSpecHash audit_log_hash = AddressUtil.getHashForAddress(
      snow_client.getParams().getAddressPrefix(),
      config.get("audit_log_source"));

    AuditLogReport audit_report = AuditLog.getAuditReport(snow_client, audit_log_hash);

    AuditLogItem highest = null;

    boolean in_mem_pool=false;

    HashMap<ChainHash, AuditLogItem> new_saved_audit_items = new HashMap<>();

    AuditLogItem mempool_item = null;


    for(AuditLogChain c : audit_report.getChainsList())
    for(AuditLogItem i : c.getItemsList())
    {

      if ((highest == null) || (i.getConfirmedHeight() > highest.getConfirmedHeight()))
      {
        highest = i;
      }
      if (i.getConfirmedHeight() == 0)
      {
        in_mem_pool=true;
        mempool_item = i;
      }
      else
      {
        new_saved_audit_items.put(new ChainHash(i.getLogMsg()), i);
      }
    }

    if (highest != null)
    {
      snow_last_save = new ChainHash(highest.getTxHash());
    }
    else
    {
      snow_last_save = null; //not sure how this would happen
    }
    if (mempool_item != null)
    {
      snow_mempool = new ChainHash(mempool_item.getTxHash());
    }
    else
    {
      snow_mempool = null;
    }

    // Make this map availible for readers
    saved_audit_items = ImmutableMap.copyOf(new_saved_audit_items);

    if (channel_access.getHeight() < 1) return;

    ChainHash block_head = new ChainHash(channel_access.getHead().getBlockId());

    if (in_mem_pool)
    {
      logger.info("Checkpoint already in snowblossom mempool");
      return;
    }

    if (highest != null)
    {
      ChainHash saved_block_hash = new ChainHash(highest.getLogMsg());

      // already saved this block
      if (saved_block_hash.equals( block_head ) ) return;


      ChannelBlock chan_block = channel_access.getBlockByHash(saved_block_hash);
      SignedMessagePayload payload = ChannelSigUtil.quickPayload( chan_block.getSignedHeader() );


      long saved_block_time = payload.getTimestamp();

      snow_last_saved_block_time = saved_block_time;

      if (saved_block_time + SNOW_CHECKPOINT_INTERVAL > System.currentTimeMillis())
      {
        // too early
        return;
      }
    }

    snow_mempool = new ChainHash(AuditLog.recordLog(snow_client, block_head.getBytes()));
    logger.info("Saving block to Snowblossom chain: " + block_head);

  }

  @Override
  public void onContent(ChannelID cid, SignedMessage sm)
  {
    synchronized(known_messages)
    {
      known_messages.put(new ChainHash(sm.getMessageId()),System.currentTimeMillis());
    }

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

      if (path.equals("/api/v1/info"))
      {
        JSONObject reply = new JSONObject();

        reply.put("version", ChannelGlobals.VERSION);
        reply.put("channel", channel_access.getChannelID().toString());
        reply.put("channel_height", channel_access.getHeight());

        if (snow_last_saved_block_time > 0L)
        {
          long age = System.currentTimeMillis() - snow_last_saved_block_time;
          reply.put("snow_last_saved_age", age);
        }

        int outstanding = channel_access.getOutsiderByTime(500, true).size();
        reply.put("records_to_include", outstanding);

        {
          ChainHash ch = snow_last_save;
          if (ch != null)
          {
            reply.put("snowblossom_confirmed_tx", ch.toString());
          }
        }
        {
          ChainHash ch = snow_mempool;
          if (ch != null)
          {
            reply.put("snowblossom_mempool_tx", ch.toString());
          }
        }

        wctx.setHttpCode(200);
        wctx.setContentType("application/json");
        wctx.out().println(reply);
        return;
      }
      if (path.equals("/api/v1/publish"))
      {

        TreeMap<String, String> query_map = extractQuery(uri);
        String hash = query_map.get("hash");
        ByteString hash_b = HexUtil.hexStringToBytes(hash);


        ChainHash message_hash = sendTimestamp( hash_b, null);

        JSONObject reply = new JSONObject();
        reply.put("input_hash", hash);
        reply.put("transaction_hash", message_hash.toString());
        reply.put("channel", channel_access.getChannelID().toString());

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
    JSONObject top = new JSONObject();
    top.put("proof_ver", 1L);
    if (sm == null)
    {
      top.put("complete", false);
      synchronized(known_messages)
      {
        if (known_messages.containsKey(tx_hash))
        {
          top.put("incomplete_reason","not confirmed on channel yet");
        }
        else
        {
          top.put("incomplete_reason","token not known");
        }
      }

      return top;
    }

    ChainHash block_id = channel_access.getBlockIdForContent(tx_hash);

    SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);


    top.put("transaction_hash", tx_hash.toString());
    top.put("channel_block_hash", block_id.toString());
    top.put("channel_block_time", getBlockTime(block_id));

    ContentInfo tx_ci = payload.getContentInfo();

    top.put("data_hash", HexUtil.getHexString(tx_ci.getContent()));

    JSONArray proof_lst = new JSONArray();
    top.put("proofs", proof_lst);

    // data hash to transaction
    {
      ByteString payload_hash = DigestUtil.hash(sm.getPayload());
      proof_lst.add(getProofJson(sm.getPayload(), tx_ci.getContent(), payload_hash, "content payload"));
      proof_lst.add(getProofJson(payload_hash.concat(sm.getSignature()), payload_hash, sm.getMessageId(), "content outer"));
    }

    long included_block_height = -1L;

    // Content to channel block
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
      included_block_height = header.getBlockHeight();
      ChainHash merkle_root = new ChainHash(header.getContentMerkle());

      proof_lst.addAll(getMerkleProof(tx_hash, block_content_lst, merkle_root));

      ByteString block_payload_hash = DigestUtil.hash(block.getSignedHeader().getPayload());
      proof_lst.add( getProofJson(block.getSignedHeader().getPayload(), merkle_root.getBytes(), block_payload_hash, "block payload"));

      proof_lst.add(getProofJson(block_payload_hash.concat(block.getSignedHeader().getSignature()), block_payload_hash, block_id.getBytes(), "block outer"));

    }

    // Channel block to Snowblossom transaction
    {
      long check_height = included_block_height;
      ChainHash check_hash = channel_access.getBlockHashAtHeight(check_height);
      LinkedList<ChainHash> block_path = new LinkedList<>();
      AuditLogItem found_log = null;
      while(true)
      {
        if (saved_audit_items.containsKey(check_hash))
        {
          found_log = saved_audit_items.get(check_hash);
          // We found a log
          break;

        }

        check_height++;
        if (check_height > channel_access.getHeight()) break;

        check_hash = channel_access.getBlockHashAtHeight(check_height);
        block_path.add(check_hash);
      }

      if (found_log != null)
      {
        // Chain from the block our content was included in
        // to the block that was saved in snowblossom
        for(ChainHash block_hash : block_path)
        {
          ChannelBlock block = channel_access.getBlockByHash(block_hash);
          SignedMessagePayload block_payload = ChannelSigUtil.quickPayload(block.getSignedHeader());
          ChannelBlockHeader header = block_payload.getChannelBlockHeader();
          ChainHash prev = new ChainHash(header.getPrevBlockHash());

          ByteString block_payload_hash = DigestUtil.hash(block.getSignedHeader().getPayload());

          proof_lst.add(
            getProofJson(block.getSignedHeader().getPayload(), prev.getBytes(), block_payload_hash, "channel block path payload"));

          proof_lst.add(
            getProofJson(block_payload_hash.concat(block.getSignedHeader().getSignature()), block_payload_hash, block_hash.getBytes(), "channel block path outer"));
        }

        // Prove channel block hash in snowblossom transaction
        {
          Transaction snow_tx = snow_client.getStub().getTransaction(
            RequestTransaction.newBuilder().setTxHash( found_log.getTxHash() ).build());
          //TransactionInner snow_tx_inner = TransactionUtil.getInner(tx);

          proof_lst.add(
            getProofJson( snow_tx.getInnerData(), found_log.getLogMsg(), found_log.getTxHash(), "snow tx has channel block hash"));

          top.put("snow_transaction", HexUtil.getHexString( found_log.getTxHash()));

        }
        // prove snow transaction in snow block
        {
          ChainHash block_hash = new ChainHash(found_log.getBlockHash());

          top.put("snow_block", block_hash.toString());

          Block snow_blk = snow_client.getStub().getBlock(
            RequestBlock.newBuilder().setBlockHash(block_hash.getBytes() ).build());

          top.put("snow_block_time", snow_blk.getHeader().getTimestamp());


          LinkedList<ChainHash> tx_lst = new LinkedList<>();

          for(Transaction tx : snow_blk.getTransactionsList())
          {
            tx_lst.add(new ChainHash(tx.getTxHash()));
          }

          proof_lst.addAll( getMerkleProof(
            new ChainHash(found_log.getTxHash()),
            tx_lst,
            new ChainHash(snow_blk.getHeader().getMerkleRootHash())));

          proof_lst.addAll(getSnowblossomBlockProof(snow_blk));

        }
        top.put("complete", true);


      }
      else
      {
        top.put("complete", false);
        top.put("incomplete_reason","not in snowblossom");
      }




    }
    return top;

  }

  public long getBlockTime(ChainHash block_id)
  {
    ChannelBlock blk = channel_access.getBlockByHash(block_id);

    return ChannelSigUtil.quickPayload(blk.getSignedHeader()).getTimestamp();

  }

  public List<JSONObject> getSnowblossomBlockProof(Block snow_blk)
  {
    BlockHeader header = snow_blk.getHeader();
    ByteString pass_one = PowUtil.getHeaderBits(header);
    ByteString context = pass_one;

    LinkedList<JSONObject> out_lst = new LinkedList<>();

    out_lst.add( getProofJson(pass_one, header.getMerkleRootHash(), DigestUtil.hash(pass_one), "snow pow"));

    context = DigestUtil.hash(pass_one);

    LinkedList<SnowPowProof> proofs = new LinkedList<>();
    proofs.addAll(header.getPowProofList());

    SnowFieldInfo field_info = snow_client.getParams().getSnowFieldInfo(header.getSnowField());
    long word_count = field_info.getLength() / (long)Globals.SNOW_MERKLE_HASH_LEN;

    for(SnowPowProof proof : proofs)
    {
      long idx = proof.getWordIdx();
      long nx = PowUtil.getNextSnowFieldIndex(context.toByteArray(), word_count);
      ByteString data = proof.getMerkleComponentList().get(0);

      ByteString next_context = DigestUtil.hash( context.concat(data) );

      out_lst.add( getProofJson( context.concat(data), context, next_context, "snow pow step"));

      context = next_context;

    }

    return out_lst;


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
