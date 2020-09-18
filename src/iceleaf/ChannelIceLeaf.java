package snowblossom.channels.iceleaf;

import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import snowblossom.channels.ChannelGlobals;
import snowblossom.iceleaf.IceLeaf;
import snowblossom.lib.Globals;
import snowblossom.lib.NetworkParams;
import snowblossom.lib.NetworkParamsProd;

public class ChannelIceLeaf extends IceLeaf
{
  protected ChannelSettingsPanel channel_settings_panel;
  protected ChannelNodePanel channel_node_panel;
  protected LockPanel lock_panel;

  public static void main(String args[]) throws Exception
  {
    Globals.addCryptoProvider();
    new ChannelIceLeaf(new NetworkParamsProd(), null);
  }

  public ChannelIceLeaf(NetworkParams params, Preferences prefs)
    throws Exception
  {
    super(params, Preferences.userNodeForPackage(
      ClassLoader.getSystemClassLoader().loadClass("snowblossom.iceleaf.IceLeaf")));
  }

  @Override
  public String getTitle()
  {
    return "SnowBlossom Channels - IceLeaf " + ChannelGlobals.VERSION;
  }

  @Override
  public String getResourceBasePath()
  {
    return "/external/snowblossom";
  }

  @Override
  public void setupMorePanels( JTabbedPane tab_pane, JFrame f)
  {
    f.setSize(1125, 600);

    channel_settings_panel = new ChannelSettingsPanel(this);
    channel_node_panel = new ChannelNodePanel(this);
    lock_panel = new LockPanel(this);

    channel_settings_panel.setup();
    channel_node_panel.setup();
    lock_panel.setup();

    tab_pane.add("Channel Settings", channel_settings_panel.getPanel());
    tab_pane.add("Channel Node", channel_node_panel.getPanel());
    tab_pane.add("Reserve Name", lock_panel.getPanel());
  }

}
