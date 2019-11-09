package snowblossom.channels;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.net.InetAddress;
import java.net.Inet4Address;


/**
 * A super simple socks5 server that directs things towards a specific port regardless of what it is asked
 */
public class SocksServer
{
  public static final int TIMEOUT=1800 * 1000;
  private static final Logger logger = Logger.getLogger("snowblossom.channels");
  private final ServerSocket ss;
  private final Inet4Address target_addr;
  private final int target_port;
  private final String target_host;

  public SocksServer(int listen_port, String target_host, int target_port)
    throws java.io.IOException
  {

    this.target_addr = (Inet4Address) InetAddress.getByName(target_host);
    this.target_host = target_host;

    ss = new ServerSocket(listen_port);
    ss.setReuseAddress(true);

    this.target_port = target_port;

    new AcceptThread().start();

  }

  public class AcceptThread extends Thread
  {
    public AcceptThread()
    {
      setName("SocksServer/AcceptThread");
      setDaemon(true);
    }

    public void run()
    {
      while(ss.isBound())
      {
        try
        {
          Socket sock = ss.accept();
          new ConnectionThread(sock).start();

        }
        catch(java.io.IOException e)
        {
          logger.log(Level.WARNING, "Error in SocksServer accept", e);
        }
      }

    }
  }

  public class ConnectionThread extends Thread
  {
    private Socket sock;

    public ConnectionThread(Socket sock)
    {
      this.sock = sock;
    }
    public void run()
    {
      try
      {
        sock.setTcpNoDelay(true);
        sock.setSoTimeout(TIMEOUT);



        DataInputStream in = new DataInputStream(sock.getInputStream());
        OutputStream out = sock.getOutputStream();
        
        { // auth negotiation
          int ver = readByteAsUnsigned(in);
          int method_count = readByteAsUnsigned(in);
          byte[] methods = new byte[method_count];
          in.readFully(methods);

          StringBuilder sb = new StringBuilder();
          sb.append(String.format("Client ver %d methods (", ver));
          for(int i=0; i<method_count; i++)
          {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%d", (int)methods[i]));
            
          }
          sb.append(")");
          logger.log(Level.INFO, sb.toString());
        }


        byte bq[] =new byte[2];
        bq[0]=5;
        bq[1]=0;
        out.write(bq);
        //out.write(5); // version
        //out.write(0); // no auth required
        out.flush();

        //while(true)
        {
          int ver = readByteAsUnsigned(in);
          if (ver != 5)
          {
            sock.close();
            return;
          }
          int cmd = readByteAsUnsigned(in);
          if (cmd != 1)
          {
            sock.close();
            return;
          }
          int rsv = readByteAsUnsigned(in);
          int address_type = readByteAsUnsigned(in);
          int addr_len = 0;
          String addr_str = null;
          byte[] addr = null;
          InetAddress addr_ia = null;
          if (address_type == 1)
          {
            addr_len = 4;
            addr = new byte[addr_len];
            in.readFully(addr);
            addr_ia = InetAddress.getByAddress(addr);
            addr_str = addr_ia.toString();
          }
          else if (address_type == 3)
          {
            addr_len = readByteAsUnsigned(in);
            addr = new byte[addr_len];
            in.readFully(addr);
            addr_str = new String(addr);
            addr_ia = InetAddress.getByName(addr_str);
          }
          else if (address_type == 4)
          {
            addr_len = 16;
            addr = new byte[addr_len];
            in.readFully(addr);
            addr_ia = InetAddress.getByAddress(addr);
            addr_str = addr_ia.toString();

          }
          int port = readShortAsUnsigned(in);

          logger.log(Level.INFO, String.format("Socks request v%d c%d a%d %s %d", ver, cmd, address_type, addr_str, port));


          out.write(5); // version
          out.write(0); // success
          out.write(0); // reserved
          out.write(1); // ipv4

          //We aren't exactly setting these accurately, but no one cares
          out.write(target_addr.getAddress());
          writeShortAsUnsigned(out, port);

          out.flush();

          Socket relay_sock = new Socket(addr_ia, port);

          relay_sock.setTcpNoDelay(true);
          relay_sock.setSoTimeout(TIMEOUT);
          new XferThread(sock, relay_sock).start();
          new XferThread(relay_sock, sock).start();

        }


      }
      catch(Throwable t)
      {
        logger.log(Level.INFO, "Sock5 error",t);
      }
      
    }


  }

  public class XferThread extends Thread
  {
    private Socket in;
    private Socket out;
    public XferThread(Socket in, Socket out)
    {
      this.in = in;
      this.out = out;
    }

    public void run()
    {
      try ( Socket s_in = in; Socket s_out = out )
      {
        InputStream in = s_in.getInputStream();
        OutputStream out = s_out.getOutputStream();

        byte[] buff = new byte[8192];

        while(s_in.isConnected() && s_out.isConnected())
        {
          int r = in.read(buff);
          if ( r < 0) return;
          out.write(buff, 0, r);
          out.flush();

        }

      }
      catch(IOException e)
      {
        logger.log(Level.INFO, "Sock5 xfer", e);
      }

    }
    

  }

  private static int readByteAsUnsigned(DataInputStream in)
    throws IOException
  {
    byte[] b=new byte[4];
    in.readFully(b,3,1);
    ByteBuffer bb = ByteBuffer.wrap(b);
    return bb.getInt();

  }
  private static int readShortAsUnsigned(DataInputStream in)
    throws IOException
  {
    byte[] b=new byte[4];
    in.readFully(b,2,2);
    ByteBuffer bb = ByteBuffer.wrap(b);
    return bb.getInt();

  }
  private static void writeShortAsUnsigned(OutputStream out, int v)
    throws IOException
  {
    logger.info("Port: " +v);
    byte[] b = new byte[4];
    ByteBuffer bb = ByteBuffer.wrap(b);
    bb.putInt(v);

    out.write(b,2,2);
  }

}
