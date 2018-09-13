package snowblossom.channels;

import duckutil.Config;
import duckutil.ConfigFile;
import snowblossom.lib.Globals;
import snowblossom.lib.LogSetup;

import java.util.logging.Level;
import java.util.logging.Logger;


public class ChannelNode
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");

  public static void main(String args[])
		throws Exception
  {
    Globals.addCryptoProvider();

    if (args.length != 1)
    { 
      logger.log(Level.SEVERE, "Incorrect syntax. Syntax: ChannelNode <config_file>");
      System.exit(-1);
    }

    ConfigFile config = new ConfigFile(args[0]);

    LogSetup.setup(config);
		new ChannelNode(config);




  }

  public ChannelNode(Config config)
  {


  }

}
