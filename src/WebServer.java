package snowblossom.channels;

import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import duckutil.Config;
import duckutil.TaskMaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import snowblossom.channels.proto.*;
import snowblossom.lib.ChainHash;
import java.util.BitSet;

public class WebServer
{
  private ChannelNode node;
  private final HttpServer http_server;
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  public WebServer(ChannelNode node)
    throws Exception
  {
    this.node = node;

    Config config = node.getConfig();

    config.require("web_port");
    int port = config.getInt("web_port");

    String web_host = config.get("web_host");

    InetSocketAddress listen = new InetSocketAddress(port);
    if (web_host != null)
    {
      listen = new InetSocketAddress(web_host, port);
    }
    logger.info("Starting web server on " + listen);
    http_server = HttpServer.create(listen, 0);
    http_server.createContext("/", new RootHandler());
    http_server.setExecutor(TaskMaster.getBasicExecutor(64,"web_server"));
    http_server.start();

  }
  public class RootHandler implements HttpHandler
  {
    @Override
    public void handle(HttpExchange t) throws IOException {
      ByteArrayOutputStream b_out = new ByteArrayOutputStream();
      PrintStream print_out = new PrintStream(b_out);
      int code = 200;

      URI uri = t.getRequestURI();
      logger.info("Web request: " + uri);

      print_out.println("Request: " + uri);

      ArrayList<String> tokens = tokenizePath(uri);
      print_out.println("Tokens: " + tokens);

      try
      {
        if (tokens.size() == 0)
        {
          handleRoot(t);
          return;
        }
        else if ((tokens.get(0).equals("channel")) && (tokens.size() >= 2))
        {
          ChannelID cid = ChannelID.fromString(tokens.get(1));
          handleChannelGet(cid, tokens, t);
        }
        else
        {
          print_out.println("No known handler for URI");
        }

      }
      catch(Throwable e)
      {
        print_out.println("Error: " + e);
        code = 400;

      }
      
      t.getResponseHeaders().add("Content-type","text/plain");

      byte[] data = b_out.toByteArray();
      t.sendResponseHeaders(code, data.length);
      OutputStream out = t.getResponseBody();
      out.write(data);
      out.close();

    }

    private void handleChannelGet(ChannelID cid, ArrayList<String> tokens, HttpExchange t)
      throws IOException
    {
      ChannelContext ctx = node.getChannelSubscriber().openChannel(cid);
      ByteArrayOutputStream b_out = new ByteArrayOutputStream();
      PrintStream print_out = new PrintStream(b_out);
      int code = 200;

      String path ="/web";
      for(int i=2; i<tokens.size(); i++)
      {
        path += "/" + tokens.get(i);
      }

      ByteString content_id = ChanDataUtils.getData(ctx, path);
      if (content_id == null)
      {
        code = 404;
        t.getResponseHeaders().add("Content-type","text/plain");
        print_out.println("Item not found: " + cid + path);
      }
      else
      {
        SignedMessage content_msg = ctx.db.getContentMap().get(content_id);
        if (content_msg == null)
        {
          code = 404;
          t.getResponseHeaders().add("Content-type","text/plain");
          print_out.println("Path entry found, but not content info message.");
        }
        else
        {
          ContentInfo ci = ChannelSigUtil.quickPayload(content_msg).getContentInfo();
          sendFile(t, ctx, new ChainHash(content_id), ci);
          return;
        }
      }

      byte[] data = b_out.toByteArray();
      t.sendResponseHeaders(code, data.length);
      OutputStream out = t.getResponseBody();
      out.write(data);
      out.close();

    }

    private void sendFile(HttpExchange t, ChannelContext ctx, ChainHash content_id, ContentInfo ci)
      throws IOException
    {
      if (ci.getMimeType() != null)
      {
        t.getResponseHeaders().add("Content-type",ci.getMimeType());
      }
      int code = 200;

      boolean using_chunks=false;
      if (ci.getContentLength() > ci.getContent().size())
      { // need to use chunks 
        using_chunks=true;

        int total_chunks = MiscUtils.getNumberOfChunks(ci);
        BitSet bs = ChunkMapUtils.getSavedChunksSet(ctx, content_id);
        if (bs.cardinality() < total_chunks)
        {
          code=404;
          t.sendResponseHeaders(code, ci.getContentLength());

          ByteArrayOutputStream b_out = new ByteArrayOutputStream();
          PrintStream print_out = new PrintStream(b_out);

          print_out.println(String.format("We only have %d of %d chunks", bs.cardinality(), total_chunks));

          byte[] data = b_out.toByteArray();
          t.sendResponseHeaders(code, data.length);
          OutputStream out = t.getResponseBody();
          out.write(data);
          out.close();

          return;

        }
        
      }


      t.sendResponseHeaders(code, ci.getContentLength());
      OutputStream out = t.getResponseBody();
      if (using_chunks)
      {
        int total_chunks = MiscUtils.getNumberOfChunks(ci);

        for(int i=0; i<total_chunks; i++)
        {
          ByteString chunk_data = ChunkMapUtils.getChunk(ctx, content_id, i);
          out.write(chunk_data.toByteArray());
        }

      }
      else
      {
        out.write(ci.getContent().toByteArray());
      }
      out.close();

    }

    private void handleRoot(HttpExchange t)
      throws IOException
    {

      ByteArrayOutputStream b_out = new ByteArrayOutputStream();
      PrintStream print_out = new PrintStream(b_out);
      int code = 200;

      print_out.println("Snowblossom Channels Web Server");


      t.getResponseHeaders().add("Content-type","text/plain");

      byte[] data = b_out.toByteArray();
      t.sendResponseHeaders(code, data.length);
      OutputStream out = t.getResponseBody();
      out.write(data);
      out.close();


    }
  }


  ArrayList<String> tokenizePath(URI uri)
  {
    StringTokenizer stok = new StringTokenizer(uri. getPath(), "/");
    ArrayList<String> tokens = new ArrayList<>();

    while(stok.hasMoreTokens())
    {
      tokens.add( stok.nextToken());
    }
    return tokens;

  }
}
