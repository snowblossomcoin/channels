package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import snowblossom.channels.proto.*;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.DigestUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.RpcUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.AddressSpec;
import snowblossom.proto.RequestAddress;
import snowblossom.proto.TransactionOutput;
import snowblossom.proto.TxOutList;
import snowblossom.proto.TxOutPoint;
import snowblossom.proto.WalletDatabase;

public class ApiUtils
{

  public static JSONObject getBlockTail(ChannelNode node, ChannelContext ctx, int max_return)
  {
    JSONArray block_lst = new JSONArray();

    ChainHash next_block_hash = null;
    
    ChannelBlockSummary head_summary = ctx.block_ingestor.getHead();
    if (head_summary != null)
    {
      next_block_hash = new ChainHash(head_summary.getBlockId());
    }

    for(int i=0; i<max_return; i++)
    {
      if ((next_block_hash == null) || (next_block_hash.equals(ChainHash.ZERO_HASH)))
      {
        break;
      }
      ChannelBlock blk = ctx.db.getBlockMap().get(next_block_hash.getBytes());
      ChannelBlockHeader header = ChannelSigUtil.quickPayload(blk.getSignedHeader()).getChannelBlockHeader();

      block_lst.add(getPrettyJsonForBlock(blk, node));

      next_block_hash = new ChainHash(header.getPrevBlockHash());



    }


    JSONObject reply = new JSONObject();
    reply.put("blocks", block_lst);


    return reply;

  }


  public static JSONObject getOutsiderByTime(ChannelNode node, ChannelContext ctx, int max_return)
  {
    TreeMap<Double, SignedMessage> message_map = new TreeMap<>();

    Random rnd = new Random();
    
    for(SignedMessage sm : ctx.db.getOutsiderMap().getByPrefix(ByteString.EMPTY, 10000).values())
    {
      try
      {
        ChannelValidation.validateOutsiderContent(sm, ctx.block_ingestor.getHead());
        SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

        double v = payload.getTimestamp();
        v+=rnd.nextDouble();

        message_map.put( -v, sm);
      }
      catch(ValidationException e){}

      while(message_map.size() > max_return)
      {
        message_map.pollLastEntry();
      }
    }

    JSONObject reply = new JSONObject();

    JSONArray msg_list = new JSONArray();
    while(message_map.size() > 0)
    {
      SignedMessage sm = message_map.pollFirstEntry().getValue();
      msg_list.add(getPrettyJsonForContent(sm, node));
    }

    reply.put("messages", msg_list);

    return reply;
  }


  public static JSONObject getPrettyJsonForBlock(ChannelBlock blk, ChannelNode node)
  {
    ChannelBlockHeader header = ChannelSigUtil.quickPayload(blk.getSignedHeader()).getChannelBlockHeader();
    JSONObject jo = new JSONObject();

    jo.put("block_id", HexUtil.getHexString(blk.getSignedHeader().getMessageId()));
    jo.put("block_height", header.getBlockHeight());
    jo.put("prev_block_hash", HexUtil.getHexString( header.getPrevBlockHash() ));
    jo.put("channel_id", new ChannelID(header.getChannelId()).toString());
    jo.put("timestamp", header.getTimestamp());

    JSONArray content = new JSONArray();

    for(SignedMessage sm : blk.getContentList())
    {
      content.add(getPrettyJsonForContent(sm, node));
    }

    jo.put("content", content);

    return jo;

  }
  public static JSONObject getPrettyJsonForContent(SignedMessage sm, ChannelNode node)
  {
    JSONObject jo = new JSONObject();
    jo.put("message_id", HexUtil.getHexString(sm.getMessageId()));

    SignedMessagePayload payload = ChannelSigUtil.quickPayload(sm);

    String name = getNameForPayload(payload, node);

    if (name != null)
    {
      jo.put("sender", name);
    }
    else
    {
      AddressSpecHash sender = AddressUtil.getHashForSpec(payload.getClaim());
      jo.put("sender", AddressUtil.getAddressString( ChannelGlobals.NODE_ADDRESS_STRING , sender));
    }

    try
    {
      jo.put("payload", RpcUtil.protoToJson( payload ));
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }
    return jo;
  }

  public static JSONObject readJSON(InputStream in)
    throws Exception
  {
    JSONParser parser = new JSONParser(JSONParser.MODE_STRICTEST);

    return (JSONObject) parser.parse(in);
  }

  /**
   * Take a JSON object and convert it into a ContentInfo and sign it with this node's key
   */
  public static SignedMessage getContentFromJson(JSONObject input, ChannelNode node, ChannelContext ctx)
		throws Exception
  {
    ContentInfo.Builder ci = ContentInfo.newBuilder();

    if (input.containsKey("mime_type"))
    {
      ci.setMimeType( input.get("mime_type").toString() );
    }
		if (input.containsKey("content"))
    {
      ci.setContent( ByteString.copyFrom(input.get("content").toString().getBytes()));
      ci.setContentLength( ci.getContent().size() );
      
    }
    if ((input.containsKey("data_map")) && (input.get("data_map") instanceof JSONObject))
    {
      JSONObject data_map = (JSONObject) input.get("data_map");

      for(Map.Entry<String, Object> me : data_map.entrySet())
      {
        String key = me.getKey();
        String value = me.getValue().toString();
        ci.putContentDataMap(key, ByteString.copyFrom(value.getBytes()));
      }
    }

    if (ctx != null)
    {
      ci.addBroadcastChannelIds(ctx.cid.getBytes());
    }

		ci.setContentHash( ByteString.copyFrom( DigestUtil.getMD().digest( ci.getContent().toByteArray() ) ) );
   
		WalletDatabase wdb = node.getWalletDB();

    TxOutPoint fbo_out = getFboOutpoint(node, wdb.getAddresses(0));
    //System.out.println("FBO OUT: " + fbo_out);


    SignedMessagePayload.Builder payload = SignedMessagePayload.newBuilder();

    payload.setContentInfo(ci.build());
    if (fbo_out != null)
    {
      payload.setFboUtxo(fbo_out);
    }

    SignedMessage sm = ChannelSigUtil.signMessage( wdb.getAddresses(0),wdb.getKeys(0),
      payload.build());
    return sm;

  }

  public static void submitContent(JSONObject input, ChannelNode node, ChannelContext ctx)
		throws Exception
  {
    SignedMessage sm = getContentFromJson(input, node, ctx);
		ctx.block_ingestor.ingestContent(sm);
  }

  public static void submitBlock(JSONObject input, ChannelNode node, ChannelContext ctx)
		throws Exception
  {
    LinkedList<SignedMessage> content = new LinkedList<>();

    if (input.containsKey("content_list"))
    {
      JSONArray content_array = (JSONArray) input.get("content_list");
      for(int i = 0; i<content_array.size(); i++)
      {
        JSONObject msg = (JSONObject) content_array.get(i);
        content.add(getContentFromJson( msg, node, null));
      }

    }

    BlockGenUtils.createBlockForContent(ctx, content,  node.getWalletDB());
  }

  public static void submitFileBlock(InputStream in, ChannelNode node, ChannelContext ctx)
    throws Exception
  {
    MultipartSlicer ms = new MultipartSlicer(in);

    ContentInfo.Builder ci_proto = ContentInfo.newBuilder();

    ci_proto.putContentDataMap("blog_entry", ByteString.copyFrom("true".getBytes()));

    BlockGenUtils.createBlockForFilesMultipart(ctx, ms, node.getWalletDB(), ci_proto.build()); 

  }


  public static TxOutPoint getFboOutpoint(ChannelNode node, AddressSpec address)
  {
    AddressSpecHash address_hash = AddressUtil.getHashForSpec(address);
    TxOutList out_list = node.getStubHolder().getBlockingStub().getFBOList( 
      RequestAddress.newBuilder().setAddressSpecHash( address_hash.getBytes() ).build() );

    // TODO - do something smarter if multiple
    if (out_list.getOutListCount() > 0)
    {
      return out_list.getOutList(0);
    }
    return null;
  }

  public static String getNameForPayload(SignedMessagePayload payload, ChannelNode node)
  {
    if (payload.getFboUtxo().getTxHash().size() == 0) return null;
    // TODO, validate transactoin output isn't lies
    // TODO, validate UTXO is still valid

    TransactionOutput tx_out = payload.getFboUtxo().getOut();
    if (tx_out.getIds().getUsername().size() ==0) return null;

    // TODO, make sure name is correct for this address

    return new String(tx_out.getIds().getUsername().toByteArray());
  } 

  public static JSONObject amIBlockSigner(ChannelNode node, ChannelContext ctx)
    throws Exception
  {
    JSONObject reply = new JSONObject();

    ChannelBlockSummary head_summary = ctx.block_ingestor.getHead();
    if (head_summary != null)
    {
      ChannelSettings settings = head_summary.getEffectiveSettings();

      reply.put("settings", RpcUtil.protoToJson(settings));

      HashSet<ByteString> allowed_signers = new HashSet<>();
      allowed_signers.addAll(settings.getBlockSignerSpecHashesList());
      allowed_signers.addAll(settings.getAdminSignerSpecHashesList());

		  WalletDatabase wdb = node.getWalletDB();

      AddressSpec addr = wdb.getAddresses(0);

      AddressSpecHash hash = AddressUtil.getHashForSpec(addr);
      if (allowed_signers.contains(hash.getBytes()))
      {
        reply.put("result", true);
      }
      else
      {
        reply.put("result", false);

      }
    }
    else
    {
      reply.put("result", false);
    }

    return reply;

  }

}
