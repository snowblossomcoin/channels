package snowblossom.channels;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import duckutil.Config;
import duckutil.webserver.DuckWebServer;
import duckutil.webserver.WebContext;
import duckutil.webserver.WebHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import net.minidev.json.JSONObject;
import snowblossom.channels.proto.*;
import snowblossom.client.OfferPayInterface;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.HexUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.WalletKeyPair;
import snowblossom.util.proto.Offer;
import snowblossom.util.proto.OfferAcceptance;

public class WebServer
{
  private ChannelNode node;
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  public WebServer(ChannelNode node)
    throws Exception
  {
    this.node = node;

    Config config = node.getConfig();

    config.require("web_port");
    int port = config.getInt("web_port");

    String web_host = config.get("web_host");

    new DuckWebServer(web_host, port, new RootHandler(), 64);

  }

  public class RootHandler implements WebHandler
  {

    // this is a mess
    // TODO - make error reporting helper function to sets code and returns some message
    // TODO - what the hell is this decomposition
    // TODO - support range requests
    // TODO - add hash to returned headers on data get (why not)
    @Override
    public void handle(WebContext wctx) throws Exception
    {
      wctx.setHttpCode(200);

      URI uri = wctx.getURI();

      wctx.out().println("Request: " + uri);

      List<String> tokens = tokenizePath(uri);
      wctx.out().println("Tokens: " + tokens);

      List<String> channel_base_tokens = new ArrayList<>();

      try
      {
        ChannelID cid = null;
        String host = wctx.getHost();
        cid = getChannelFromHost(host);

        if ((tokens.size() >= 2) && (tokens.get(0).equals("channel")))
        {
          cid = ChannelID.fromStringWithNames(tokens.get(1), node);
          channel_base_tokens = tokens.subList(0, 2);
          tokens = tokens.subList(2, tokens.size());
        }

        if (tokens.size() == 0)
        {
          if (cid == null)
          {
            wctx.resetBuffer();
            handleRoot(wctx);
            return;
          }
        }
        if (cid != null)
        {
          if ((tokens.size() >= 1) && (tokens.get(0).equals("api")))
          {
            handleChannelApi(cid, tokens, wctx, channel_base_tokens);
          }
          else
          {
            handleChannelGet(cid, tokens, wctx, channel_base_tokens);
          }
        }
        else
        {
          wctx.out().println("No known handler for URI");
        }

      }
      catch(Throwable e)
      {
        wctx.setException(e);
      }

    }

    private void handleChannelApi(ChannelID cid, List<String> tokens, WebContext wctx, List<String> channel_base_tokens)
      throws Exception
    {
      wctx.resetBuffer();

      ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
      ChannelAccess ca = new ChannelAccess(node, ctx);

      if (ctx == null)
      {
        if (node.getAutoJoin())
        {
          ctx = node.getChannelSubscriber().openChannel(cid);
        }
        else
        {
          wctx.setHttpCode(404);
          wctx.out().println("Channel not subscribed and autojoin is disabled.");
          return;
        }
      }

      String api_path = "";
      for(int i=1; i<tokens.size(); i++)
      {
        api_path += "/" + tokens.get(i);
      }

      if (api_path.equals("/beta/outsider/order_by_time"))
      {
        wctx.setContentType("application/json");
        wctx.out().println(ApiUtils.getOutsiderByTime(node, ctx, 1000));
      }
      else if (api_path.equals("/beta/outsider/submit"))
      {
        if (!wctx.getRequestMethod().equals("POST"))
        {
          wctx.setHttpCode(401);
          wctx.out().println("Submit must be a POST");
        }
        else
        {
          JSONObject input = ApiUtils.readJSON(wctx.getRequestBody());
          ApiUtils.submitContent(input, node, ctx);
        }
      }
      else if (api_path.equals("/beta/block/submit"))
      {
        if (!wctx.getRequestMethod().equals("POST"))
        {
          wctx.setHttpCode(401);
          wctx.out().println("Submit must be a POST");
        }
        else
        {
          JSONObject input = ApiUtils.readJSON(wctx.getRequestBody());
          ApiUtils.submitBlock(input, node, ctx);
        }
      }
      else if (api_path.equals("/beta/block/tail"))
      {
        wctx.setContentType("application/json");
        wctx.out().println(ApiUtils.getBlockTail(node, ctx, 100));
      }
      else if (api_path.equals("/beta/am_i_block_signer"))
      {
        wctx.setContentType("application/json");
        wctx.out().println(ApiUtils.amIBlockSigner(node, ctx));

      }
      else if (api_path.equals("/beta/block/submit_files"))
      {
        processFileUpload(wctx, ctx);
      }
      else if (api_path.startsWith("/beta/content/get"))
      {
        String id = api_path.substring("/beta/content/get/".length());
        processApiGet(wctx, ctx, id, channel_base_tokens);
        return;

      }
      else if (api_path.startsWith("/beta/premium_pay"))
      {
        wctx.setContentType("text/plain");
        wctx.out().println("Wyrd");

        payPremium(wctx.out(), ctx, ca);

      }
      else
      {
        wctx.setHttpCode(404);
        wctx.out().println("Unknown api: " + api_path);
      }

    }

    private void processApiGet(WebContext wctx, ChannelContext ctx, String id, List<String> channel_base_tokens)
      throws IOException, ValidationException
    {
      ByteString content_id = HexUtil.hexStringToBytes(id);
      SignedMessage content_msg = ctx.db.getContentMap().get(content_id);
      if (content_msg == null)
      {
        wctx.setHttpCode(404);
        wctx.setContentType("text/plain");
        wctx.out().println("Path entry found, but not content info message.");
      }
      else
      {
        ContentInfo ci = ChannelSigUtil.quickPayload(content_msg).getContentInfo();
        sendFile(wctx, ctx, new ChainHash(content_id), ci, channel_base_tokens);
      }

    }


    private void handleChannelGet(ChannelID cid, List<String> tokens, WebContext wctx, List<String> channel_base_tokens)
      throws IOException, ValidationException
    {
      wctx.setHttpCode(200);

      ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
      if (ctx == null)
      {
        if (node.getAutoJoin())
        {
          ctx = node.getChannelSubscriber().openChannel(cid);
        }
        else
        {
          wctx.setHttpCode(404);
          wctx.out().println("Channel not subscribed and autojoin is disabled.");
          return;
        }
      }

      String path = "/web";
      for(int i=0; i<tokens.size(); i++)
      {
        path += "/" + tokens.get(i);
      }
      if (wctx.getURI().getPath().endsWith("/"))
      {
        path+= "/index.html";
      }

      ByteString content_id = ChanDataUtils.getData(ctx, path);

      if (content_id == null)
      {
        wctx.setHttpCode(404);
        wctx.out().println("Item not found: " + cid + path);
      }
      else
      {
        SignedMessage content_msg = ctx.db.getContentMap().get(content_id);
        if (content_msg == null)
        {
          wctx.setHttpCode(404);
          wctx.out().println("Path entry found, but not content info message.");
        }
        else
        {
          ContentInfo ci = ChannelSigUtil.quickPayload(content_msg).getContentInfo();
          wctx.out().println(tokens);
          sendFile(wctx, ctx, new ChainHash(content_id), ci, channel_base_tokens);
          return;
        }
      }

    }

    private void payPremium(PrintStream out, ChannelContext ctx, ChannelAccess ca)
      throws Exception
    {
      if (ca.getCommonKeyForChannel() != null)
      {
        out.println("We already have the key for this channel");
        return;
      }
      ByteString encryption_json_data = ca.readFile("/web/encryption.json");
      if (encryption_json_data==null)
      {
        out.println("No encryption.json file found");
        return;
      }
      EncryptedChannelConfig.Builder config = EncryptedChannelConfig.newBuilder();

      JsonFormat.Parser parser = JsonFormat.parser();
      Reader input = new InputStreamReader(new ByteArrayInputStream(encryption_json_data.toByteArray()));
      parser.merge(input, config);

      Offer offer = config.getOffer();

      WalletKeyPair wkp = ChannelCipherUtils.getKeyForChannel(ctx.cid, node.getUserWalletDB());

      OfferAcceptance.Builder oa = OfferAcceptance.newBuilder();
      oa.setForAddressSpec(AddressUtil.getSimpleSpecForKey(wkp));

      OfferPayInterface pay = node.getStubHolder().getOfferPayInterface();
      if (pay == null)
      {
        out.println("No OfferPayInterface is loaded");
        return;
      }

      pay.maybePayOffer(offer, oa.build());

      out.println("Payment info sent to 'Send' tab of client UI");
      out.println(offer);
      out.println(oa);


    }

    private void sendFile(WebContext wctx, ChannelContext ctx, ChainHash content_id, ContentInfo ci, List<String> channel_base_tokens)
      throws IOException, ValidationException
    {
      //if (ci.getMimeType() != null)
      {
        wctx.setContentType( ci.getMimeType() );
      }
      wctx.setHttpCode(200);

      boolean using_chunks = false;
      if (ci.getContentLength() > ci.getContent().size())
      { // need to use chunks
        using_chunks = true;

        int total_chunks = MiscUtils.getNumberOfChunks(ci);
        BitSet bs = ChunkMapUtils.getSavedChunksSet(ctx, content_id);
        if (bs.cardinality() < total_chunks)
        {
          wctx.setHttpCode(404);

          wctx.out().println(String.format("We only have %d of %d chunks", bs.cardinality(), total_chunks));
          return;
        }
      }

      long len = ci.getContentLength();
      if (ci.getEncryptedKeyId().size() > 0)
      {
        len = ci.getPlainContentLength();
        // Check keys
        if (!BlockReadUtils.checkKey(ctx, ci, node.getUserWalletDB()))
        {
          wctx.resetBuffer();
          wctx.setContentType("text/html");
          wctx.setHttpCode(403);

          wctx.out().println(HtmlUtils.getHeader("Unable to decrypt"));
          wctx.out().println("<br>Base: " + channel_base_tokens);
          wctx.out().println("<br>Unable to decrypt - we don't have the key");

          String pay_url = "";
          for(String s : channel_base_tokens)
          {
            pay_url += "/";
            pay_url += s;
          }
          pay_url += "/api/beta/premium_pay";
          wctx.out().println(String.format("<br>Try payments <a href='%s'>pay</a>", pay_url));

          return;
        }
      }


      wctx.setOutputSize(len);

      wctx.writeHeaders();

      OutputStream out = wctx.getOutStream();
      BlockReadUtils.streamContentOut(ctx, content_id, ci, out, node.getUserWalletDB());
      out.close();

    }

    private void handleRoot(WebContext wctx)
      throws IOException
    {
      PrintStream print_out = wctx.out();

      print_out.println("Snowblossom Channels Web Server");
      print_out.println(wctx.getRequestMethod());
      print_out.println(wctx.getURI());

      /*for(String k : t.getRequestHeaders().keySet())
      {
        for(String v : t.getRequestHeaders().get(k))
        {
          print_out.println(k + ": " + v);
        }
      }*/
    }
  }

  private ChannelID getChannelFromHost(String host)
    throws Exception
  {
    if (host.endsWith(".snowblossom.io"))
    {
      StringTokenizer stok = new StringTokenizer(host,".");
      ArrayList<String> tokens = new ArrayList<>();

      while(stok.hasMoreTokens())
      {
        tokens.add( stok.nextToken());
      }
      if (tokens.size() == 3)
      {
        return ChannelID.fromStringWithNames(tokens.get(0), node);
      }
    }

    return null;
  }

  private ArrayList<String> tokenizePath(URI uri)
  {
    StringTokenizer stok = new StringTokenizer(uri. getPath(), "/");
    ArrayList<String> tokens = new ArrayList<>();

    while(stok.hasMoreTokens())
    {
      tokens.add( stok.nextToken());
    }
    return tokens;

  }

  private void processFileUpload(WebContext wctx, ChannelContext ctx)
    throws Exception
  {

    wctx.out().println("");
    wctx.out().println("Submit called");

    ApiUtils.submitFileBlock(wctx.getRequestBody(), node, ctx);
  }

}
