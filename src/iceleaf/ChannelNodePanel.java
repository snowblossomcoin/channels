package snowblossom.channels.iceleaf;

import duckutil.ConfigMem;
import duckutil.PeriodicThread;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelNode;
import snowblossom.iceleaf.BasePanel;
import snowblossom.iceleaf.IceLeaf;
import snowblossom.lib.SystemUtil;
import snowblossom.lib.ValidationException;

public class ChannelNodePanel extends BasePanel
{
  protected ChannelNode node;
  protected JProgressBar progress;
  protected boolean start_attempt;

	protected JTextField sub_chan_field;
  protected JButton sub_button;

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

        StringBuilder sb=new StringBuilder();

        sb.append("DHT peers: " + node.getPeerManager().getPeersWithReason("DHT").size());
        sb.append("\n");
        sb.append("Channels: " + node.getChannelSubscriber().getChannelSet().size());
        sb.append("\n");

        setStatusBox(sb.toString().trim());

      }
      catch(Exception e)
      {
        String text = e.toString();
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
    start_attempt=true;
    TreeMap<String, String> config_map = new TreeMap();

    config_map.put("port", ice_leaf_prefs.get("channel_service_port", null));
    config_map.put("web_port", ice_leaf_prefs.get("channel_web_port", null));

    config_map.put("db_path", ice_leaf_prefs.get("channel_db_path", null));
    config_map.put("wallet_path", ice_leaf_prefs.get("channel_wallet_path", null));
    config_map.put("db_separate","true");
    config_map.put("key_count", "1");

    setMessageBox(config_map.toString());

    ConfigMem config = new ConfigMem(config_map);

    node = new ChannelNode(config);

    setStatusBox("Node started");
    setMessageBox("");


  }

  public class SubscribeAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      try
      {
			  ChannelID cid = ChannelID.fromString(sub_chan_field.getText());
			  node.getChannelSubscriber().openChannelFuture(cid);
      }
      catch(ValidationException t)
      {
        setMessageBox(t.toString());
      } 

    }
  }

}
