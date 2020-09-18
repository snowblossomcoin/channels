package snowblossom.channels.iceleaf;

import duckutil.PeriodicThread;
import java.util.Collection;
import java.util.TreeSet;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import snowblossom.channels.ChannelID;
import snowblossom.channels.ChannelNode;

public class ChannelComboBox extends JComboBox<String>
{
  
  private ChannelNodePanel node_panel;
  protected TreeSet<String> current_select_box_items=new TreeSet<>();

  public ChannelComboBox(ChannelNodePanel node_panel)
  {
    this.node_panel = node_panel;

    UpdateThread ut = new UpdateThread();
    ut.start();

  }
  public class UpdateThread extends PeriodicThread
  {
    public UpdateThread()
    { 
      super(2000);
    }

    public void runPass() throws Exception
    { 
      try
      { 
        TreeSet<String> names = new TreeSet<>();
        ChannelNode node = node_panel.getNode();
        if (node == null) return;

        for(ChannelID cid : node.getChannelSubscriber().getChannelSet())
        {
          names.add(cid.toString());
        }
        updateBox(names);
      }
      catch(Throwable e)
      { 
        e.printStackTrace();
      }

    }

  }

  public void updateBox(Collection<String> names)
    throws Exception
  {
    synchronized(current_select_box_items)
    { 
      TreeSet<String> new_set = new TreeSet<>();
      new_set.addAll(names);

      if (current_select_box_items.equals(new_set))
      { 
        return;
      }

      SwingUtilities.invokeAndWait(new Runnable() {
        public void run()
        { 
          removeAllItems();
          addItem("<none>");
          for(String s : new_set)
          {
            addItem(s);
          }
          setSelectedIndex(0);
        }
      });

      current_select_box_items.clear();
      current_select_box_items.addAll(new_set);
    }

  }

}
