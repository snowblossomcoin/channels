package snowblossom.channels.iceleaf;

import duckutil.ConfigMem;
import duckutil.PeriodicThread;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import snowblossom.channels.BlockGenUtils;
import snowblossom.channels.BlockReadUtils;
import snowblossom.channels.ChannelContext;
import snowblossom.channels.ChannelGlobals;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelLink;
import snowblossom.channels.ChannelNode;
import snowblossom.channels.ChunkMapUtils;
import snowblossom.channels.MiscUtils;
import snowblossom.channels.PeerLink;
import snowblossom.channels.PrintUtil;
import snowblossom.channels.SocksServer;
import snowblossom.channels.FileBlockImportSettings;
import snowblossom.channels.proto.ChannelPeerInfo;
import snowblossom.iceleaf.BasePanel;
import snowblossom.iceleaf.IceLeaf;
import snowblossom.iceleaf.ThreadActionListener;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.HexUtil;
import snowblossom.lib.SystemUtil;
import snowblossom.node.StatusInterface;

public class ChannelNodePanel extends BasePanel
{
  protected volatile ChannelNode node;
  protected JProgressBar progress;
  protected boolean start_attempt;

  protected JTextField sub_chan_field;
  protected JButton sub_button;

  protected JTextField create_chan_field;
  protected JButton create_button;

  protected ChannelComboBox channel_import_box;
  protected JButton import_button;

  protected ChannelComboBox channel_export_box;
  protected JButton export_button;

  protected ChannelComboBox channel_info_box;
  protected JButton info_button;

  public ChannelNodePanel(IceLeaf ice_leaf)
  {
    super(ice_leaf);
	}

  @Override
	public void setupPanel()
	{
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 0.0;
			c.weighty= 0.0;
			c.gridheight = 1;
			c.anchor = GridBagConstraints.WEST;

    c.gridwidth = GridBagConstraints.REMAINDER;
	  panel.add(new JLabel("Starting local node"), c);
    new NodeUpdateThread().start();

    progress = new JProgressBar(0,0);
    panel.add(progress, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Subscribe to channel: "), c);
    sub_chan_field = new JTextField();
    sub_chan_field.setColumns(50);
    panel.add(sub_chan_field, c);

    c.gridwidth = GridBagConstraints.REMAINDER;
    sub_button = new JButton("Subscribe");
    sub_button.addActionListener( new SubscribeAction());
    panel.add(sub_button, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Create channel: "), c);
    create_chan_field = new JTextField();
    create_chan_field.setColumns(50);
    panel.add(create_chan_field, c);

    c.gridwidth = GridBagConstraints.REMAINDER;
    create_button = new JButton("Create");
    create_button.addActionListener( new CreateAction());
    panel.add(create_button, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Import files to channel: "), c);
    channel_import_box = new ChannelComboBox(this);
    panel.add(channel_import_box, c);

    c.gridwidth = GridBagConstraints.REMAINDER;
    import_button = new JButton("Import");
    import_button.addActionListener( new ImportAction());
    panel.add(import_button, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Export files from channel: "), c);
    channel_export_box = new ChannelComboBox(this);
    panel.add(channel_export_box, c);

    c.gridwidth = GridBagConstraints.REMAINDER;
    export_button = new JButton("Export");
    export_button.addActionListener( new ExportAction());
    panel.add(export_button, c);


    c.gridwidth = 1;
    panel.add(new JLabel("Get channel info: "), c);
    channel_info_box = new ChannelComboBox(this);
    panel.add(channel_info_box, c);

    c.gridwidth = GridBagConstraints.REMAINDER;
    info_button = new JButton("Info");
    info_button.addActionListener( new InfoAction());
    panel.add(info_button, c);


  }

  // May very well be null on startup
  public ChannelNode getNode()
  {
    return node;
  }

  public class NodeUpdateThread extends PeriodicThread
  {
    public NodeUpdateThread()
    {
      super(1000);
    }

    public void runPass() throws Exception
    {
      try
      {
        if (!SystemUtil.isJvm64Bit())
        {
          setStatusBox("Node requires a 64-bit JVM");
          setMessageBox("Snowblososm node uses rocksdb, which requires a 64-bit JVM to run.\n"
           + "See https://wiki.snowblossom.org/index.php/Download to download 64-bit JVM");
          return;
        }
        if (!start_attempt)
        {
          startNode();
        }

        if (node == null) return;

        StringBuilder sb=new StringBuilder();

        sb.append("Local node ID: " + AddressUtil.getAddressString( ChannelGlobals.NODE_ADDRESS_STRING,  node.getNodeID()));
        sb.append("\n");
        sb.append("Local user ID: " + AddressUtil.getAddressString( ChannelGlobals.NODE_ADDRESS_STRING,  node.getUserID()));
        sb.append("\n");

        sb.append("DHT peers: " + 
          PeerLink.countActuallyOpen(node.getPeerManager().getPeersWithReason("DHT"))
          + " "
          + PeerLink.getTypeSummary(node.getPeerManager().getPeersWithReason("DHT"))
          );
        sb.append("\n");
        sb.append("Channels: " + node.getChannelSubscriber().getChannelSet().size());
        sb.append("\n");
        for(ChannelID cid : node.getChannelSubscriber().getChannelSet())
        {
          sb.append("  ");
          sb.append(cid);
          sb.append(" ");
          ChannelContext ctx = node.getChannelSubscriber().getContext(cid);
          if (ctx != null)
          {
            if (ctx.block_ingestor.getHead() != null)
            {
              sb.append("{");
              sb.append( HexUtil.getSafeString(ctx.block_ingestor.getHead().getEffectiveSettings().getDisplayName()));
              sb.append("}");
              sb.append(String.format(" blocks:%d ", ctx.block_ingestor.getHead().getHeader().getBlockHeight()));
            }
            sb.append(String.format("peers:%d ", ChannelLink.countActuallyOpen(ctx.getLinks())));
            sb.append(String.format("missing_chunks:%d", ChunkMapUtils.getWantList(ctx).size()));
          }
          sb.append("\n");
        }

        setStatusBox(sb.toString().trim());

      }
      catch(Exception e)
      {
        String text = MiscUtils.printStackTrace(e);
        setMessageBox(text);
        e.printStackTrace();
       
      }

    }

  }

  public void setProgressBar(int curr, int net)
  {
    int enet = Math.max(net, curr);
    SwingUtilities.invokeLater(new Runnable() {
      public void run()
      {
        progress.setMaximum(enet);
        progress.setValue(curr);
      }
    });
  }




  private void startNode()
    throws Exception
  {
    //There are too many side effects to try this more than once
    start_attempt=true;
    TreeMap<String, String> config_map = new TreeMap();

    config_map.put("port", ice_leaf_prefs.get("channel_service_port", null));
    config_map.put("web_port", ice_leaf_prefs.get("channel_web_port", null));

    config_map.put("db_path", ice_leaf_prefs.get("channel_db_path", null));
    config_map.put("node_wallet_path", ice_leaf_prefs.get("channel_node_wallet_path", null));
    config_map.put("user_wallet_path", ice_leaf_prefs.get("channel_user_wallet_path", null));
    config_map.put("db_separate","true");
    config_map.put("key_count", "1");

    if (ice_leaf_prefs.getBoolean("channel_tor_only", false))
    {
      config_map.put("tor_only", "true");
    }
    if (ice_leaf_prefs.getBoolean("channel_tor_http_proxy_enable", false))
    {
      config_map.put("tor_http_relay",ice_leaf_prefs.get("channel_tor_http_proxy", null));
    }
    if (ice_leaf_prefs.get("channel_tor_advertise", "").trim().length() > 0)
    {
      config_map.put("tor_advertise", ice_leaf_prefs.get("channel_tor_advertise", "").trim());
    }

    boolean autojoin = ice_leaf_prefs.getBoolean("auto_join", false);

    config_map.put("autojoin", "" + autojoin);

    setMessageBox(config_map.toString());

    ConfigMem config = new ConfigMem(config_map);

    node = new ChannelNode(config, ice_leaf.getStubHolder());

    if (ice_leaf_prefs.getBoolean("channel_run_socks5", true))
    {
      int socks_port = Integer.parseInt(ice_leaf_prefs.get("channel_socks5_port", null));
      int web_port = Integer.parseInt(ice_leaf_prefs.get("channel_web_port", null));
      new SocksServer(socks_port, "127.0.0.1", web_port);
    }

    setStatusBox("Node started");
    setMessageBox("");

  }

  public class CreateAction extends ThreadActionListener
  {
    public void threadActionPerformed(ActionEvent e)
    {
      try
      {
        ChannelID cid = BlockGenUtils.createChannel(node, node.getUserWalletDB(), create_chan_field.getText().trim());

        String base_upload = ice_leaf_prefs.get("channel_upload_path", null);
        File channel_upload_path = new File(base_upload, cid.asStringWithoutColon());
        channel_upload_path.mkdirs();

        setMessageBox("Channel created: " + cid);
      }
      catch(Throwable t)
      {
        setMessageBox(MiscUtils.printStackTrace(t));
      } 

    }
  }

  public class ImportAction extends ThreadActionListener implements StatusInterface
  {
    public void threadActionPerformed(ActionEvent e)
    {
      try
      {
        ChannelID cid = ChannelID.fromString((String)channel_import_box.getSelectedItem());

        String base_upload = ice_leaf_prefs.get("channel_upload_path", null);
        File channel_upload_path = new File(base_upload, cid.asStringWithoutColon());

        BlockGenUtils.createBlockForFiles( new FileBlockImportSettings( 
          node.getChannelSubscriber().openChannel(cid), 
          channel_upload_path, 
          node.getUserWalletDB(), 
          this));
      }
      catch(Throwable t)
      {
        setMessageBox(MiscUtils.printStackTrace(t));
      } 

    }
    @Override
    public void setStatus(String msg)
    {
      setMessageBox(msg);
    }
  }

  public class ExportAction extends ThreadActionListener implements StatusInterface
  {
    public void threadActionPerformed(ActionEvent e)
    {
      try
      {
        ChannelID cid = ChannelID.fromString((String)channel_export_box.getSelectedItem());

        String base_upload = ice_leaf_prefs.get("channel_upload_path", null);
        File channel_upload_path = new File(base_upload, cid.asStringWithoutColon());

        BlockReadUtils.extractFiles( node.getChannelSubscriber().openChannel(cid), channel_upload_path, this, node.getUserWalletDB());
      }
      catch(Throwable t)
      {
        setMessageBox(MiscUtils.printStackTrace(t));
      } 

    }
    @Override
    public void setStatus(String msg)
    {
      setMessageBox(msg);
    }
  }


  public class InfoAction extends ThreadActionListener implements StatusInterface
  {
    public void threadActionPerformed(ActionEvent e)
    {
      try
      {
        ChannelID cid = ChannelID.fromString((String)channel_info_box.getSelectedItem());
        ChannelContext ctx = node.getChannelSubscriber().getContext(cid);

        StringBuilder sb = new StringBuilder();
        sb.append("Channel info: " + cid); sb.append('\n');

        if (ctx != null)
        {
          if (ctx.block_ingestor.getHead() != null)
          {
            sb.append("{");
            sb.append( HexUtil.getSafeString(ctx.block_ingestor.getHead().getEffectiveSettings().getDisplayName()));
            sb.append("}");
            sb.append(String.format(" blocks:%d ", ctx.block_ingestor.getHead().getHeader().getBlockHeight()));
            sb.append("Settings: " + ctx.block_ingestor.getHead().getEffectiveSettings());
          }
          sb.append(String.format("peers:%d ", ChannelLink.countActuallyOpen(ctx.getLinks())));
          sb.append(String.format("missing_chunks:%d", ChunkMapUtils.getWantList(ctx).size()));
          sb.append("\n");

          sb.append("Peers:\n");
          for(ChannelLink link : ctx.getLinks())
          {
            sb.append("  " + link);
            sb.append("\n");
          }
          sb.append("DHT Peers:\n");
          LinkedList<ChannelPeerInfo> peer_infos = node.getChannelPeerMaintainer().getAllDHTPeers(cid);
          for(ChannelPeerInfo info : peer_infos)
          {
            sb.append("  ");
            sb.append(PrintUtil.print(info));
          }
        }


        setMessageBox(sb.toString());
      }
      catch(Throwable t)
      {
        setMessageBox(MiscUtils.printStackTrace(t));
      } 

    }
    @Override
    public void setStatus(String msg)
    {
      setMessageBox(msg);
    }
  }



  public class SubscribeAction extends ThreadActionListener
  {
    public void threadActionPerformed(ActionEvent e)
    {
      try
      {
			  ChannelID cid = ChannelID.fromStringWithNames(sub_chan_field.getText().trim(), node);
			  node.getChannelSubscriber().openChannel(cid);
        setMessageBox("Channel added: " + cid);
      }
      catch(Throwable t)
      {
        setMessageBox(MiscUtils.printStackTrace(t));
      } 

    }
  }

}
