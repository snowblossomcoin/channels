package snowblossom.channels.iceleaf;

import java.awt.GridBagConstraints;
import java.io.File;
import javax.swing.JLabel;
import snowblossom.channels.ChannelGlobals;
import snowblossom.iceleaf.BasePanel;
import snowblossom.iceleaf.components.*;
import snowblossom.lib.NetworkParams;
import snowblossom.lib.SystemUtil;

public class ChannelSettingsPanel extends BasePanel
{
  protected NetworkParams params;

  public ChannelSettingsPanel(ChannelIceLeaf ice_leaf)
  {
    super(ice_leaf);
    this.params = ice_leaf.getParams();
  }

  @Override
  public void setupPanel()
  {
      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 0.0;
      c.weighty= 0.0;
      c.gridheight = 1;
      c.anchor = GridBagConstraints.WEST;

      c.gridwidth = 1;
      panel.add(new JLabel("Channel DB Directory"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      File default_channel_db_path = new File(SystemUtil.getNodeDataDirectory(params), "channel_db");
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_db_path", default_channel_db_path.toString(),70),c);


      String existing_wallet_path = ice_leaf_prefs.get("channel_wallet_path", null);
      String default_channel_node_wallet_path = new File(SystemUtil.getImportantDataDirectory(params), "channel_node_wallet").toString();
      String default_channel_user_wallet_path = new File(SystemUtil.getImportantDataDirectory(params), "channel_user_wallet").toString();
      if (existing_wallet_path!=null)
      {
        default_channel_node_wallet_path = existing_wallet_path;
        default_channel_user_wallet_path = existing_wallet_path;
      }

      c.gridwidth = 1;
      panel.add(new JLabel("Channel Node Key Directory"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_node_wallet_path", default_channel_node_wallet_path, 70),c);

      c.gridwidth = 1;
      panel.add(new JLabel("Channel User Key Directory"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_user_wallet_path", default_channel_user_wallet_path, 70),c);

      c.gridwidth = 1;
      panel.add(new JLabel("Channel Upload Directory"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      File default_channel_upload_path = new File(SystemUtil.getNodeDataDirectory(params), "channel_upload");
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_upload_path", default_channel_upload_path.toString(),70),c);

      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentCheckBox(ice_leaf_prefs, "Autojoin Channels", "auto_join", false), c);

      c.gridwidth = 1;
      panel.add(new JLabel("Service Port"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_service_port", ""+ChannelGlobals.NETWORK_PORT,8),c);

      c.gridwidth = 1;
      panel.add(new JLabel("Web Port"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_web_port", "8080",8),c);


      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentCheckBox(ice_leaf_prefs, "Run SOCKS5 proxy", "channel_run_socks5", true), c);


      c.gridwidth = 1;
      panel.add(new JLabel("SOCKS5 Port"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "", "channel_socks5_port", "1080",8),c);


      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentCheckBox(ice_leaf_prefs, "Use Tor HTTP proxy", "channel_tor_http_proxy_enable", false), c);

      c.gridwidth = 1;
      panel.add(new JLabel("Tor HTTP Proxy"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "Tor HTTP Proxy", "channel_tor_http_proxy", "localhost:9080",70),c);

      c.gridwidth = 1;
      panel.add(new JLabel("Tor Hidden Service"), c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentTextField(ice_leaf_prefs, "Tor Hidden Service Host", "channel_tor_advertise", "",70),c);

      c.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(new PersistentComponentCheckBox(ice_leaf_prefs, "Use only tor", "channel_tor_only", false), c);


      panel.add(new JLabel("NOTE: almost all changes here will require a restart to take effect.\n"),c);
      c.weightx=10.0;
      c.weighty=10.0;
      panel.add(new LinkLabel("https://wiki.snowblossom.org/index.php/IceLeaf","IceLeaf Documentation"),c);

  }


}
