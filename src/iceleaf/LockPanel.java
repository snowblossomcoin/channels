package snowblossom.channels.iceleaf;

import com.google.protobuf.ByteString;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import snowblossom.client.SnowBlossomClient;
import snowblossom.client.TransactionFactory;
import snowblossom.iceleaf.BasePanel;
import snowblossom.iceleaf.ErrorUtil;
import snowblossom.iceleaf.ThreadActionListener;
import snowblossom.iceleaf.WalletComboBox;
import snowblossom.lib.AddressSpecHash;
import snowblossom.lib.AddressUtil;
import snowblossom.lib.ChainHash;
import snowblossom.lib.Globals;
import snowblossom.lib.TransactionUtil;
import snowblossom.lib.ValidationException;
import snowblossom.proto.ClaimedIdentifiers;
import snowblossom.proto.SubmitReply;
import snowblossom.proto.TransactionOutput;
import snowblossom.proto.TransactionRequirements;
import snowblossom.util.proto.*;

public class LockPanel extends BasePanel
{
  protected WalletComboBox wallet_source_box;
  protected WalletComboBox wallet_dest_box;

  protected JTextField send_amount_field;
  protected JTextField lock_until_field;

  protected JComboBox name_type_combo;
  protected JTextField fbo_address_field;
  protected JTextField name_field;

  protected JProgressBar send_bar;
  protected JButton send_button;

  // state = 0 - init
  // state = 1 - clock running
  // state = 2 - ready
  private int send_state=0;

  private SendState saved_state = null;

  private Object state_obj = new Object();
  private TransactionFactoryResult tx_result;

  public static final int SEND_DELAY=6000;
  public static final int SEND_DELAY_STEP=50;

  public LockPanel(ChannelIceLeaf ice_leaf)
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


    c.gridwidth = 1;
    panel.add(new JLabel("Wallet to send from:"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    wallet_source_box = new WalletComboBox(ice_leaf);
    panel.add(wallet_source_box, c);

    c.gridwidth = GridBagConstraints.REMAINDER;
    panel.add(new JLabel("--"), c);
    panel.add(new JLabel("Recommended: use a wallet you won't spend from to avoid accidentally spending your name registration"), c);
    c.gridwidth = 1;
    panel.add(new JLabel("Wallet to send to:"), c);
    wallet_dest_box = new WalletComboBox(ice_leaf);
    c.gridwidth = GridBagConstraints.REMAINDER;
    panel.add(wallet_dest_box, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Lock until block (optional):"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    lock_until_field = new JTextField();
    lock_until_field.setColumns(15);
    panel.add(lock_until_field, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Send amount (or 'all'):"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    send_amount_field = new JTextField("5");
    send_amount_field.setColumns(15);
    panel.add(send_amount_field, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Channel or User"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    name_type_combo = new JComboBox();
    name_type_combo.addItem("User");
    name_type_combo.addItem("Channel");
    panel.add(name_type_combo, c);

    c.gridwidth = 1;
    panel.add(new JLabel("FBO Address (chan or node):"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    fbo_address_field = new JTextField("");
    fbo_address_field.setColumns(100);
    panel.add(fbo_address_field, c);

    c.gridwidth = 1;
    panel.add(new JLabel("Name to register:"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    name_field = new JTextField("");
    name_field.setColumns(40);
    panel.add(name_field, c);

    send_bar = new JProgressBar(0, SEND_DELAY);
    panel.add(send_bar, c);

    send_button = new JButton("Send");
    panel.add(send_button, c);

    send_button.addActionListener(new SendButtonListner());


  }

  public class SendButtonListner extends ThreadActionListener
  {
    public void threadActionPerformed(ActionEvent e)
    {
			try
			{
				synchronized(state_obj)
				{
					if (send_state == 1) return;
					if (send_state == 2)
					{
            saved_state.check();
            
            SubmitReply reply = ice_leaf.getStubHolder().getBlockingStub().submitTransaction(tx_result.getTx());
            ChainHash tx_hash = new ChainHash(tx_result.getTx().getTxHash());
            setMessageBox(String.format("%s\n%s", tx_hash.toString(), reply.toString()));
            setStatusBox("");

						send_state = 0;
						setProgressBar(0, SEND_DELAY);
            setStatusBox("");

						return;
					}
					if (send_state == 0)
					{
            saved_state = new SendState();
						send_state = 1;
					}
				}

        setupTx(saved_state);

        setStatusBox("Time delay to review");
				for(int i=0; i<SEND_DELAY; i+=SEND_DELAY_STEP)
				{
					setProgressBar(i, SEND_DELAY);
					Thread.sleep(SEND_DELAY_STEP);
				}
        setStatusBox("Ready to broadcast");
				setProgressBar(SEND_DELAY, SEND_DELAY);
				synchronized(state_obj)
				{
					send_state=2;
				}
			}
			catch(Throwable t)
			{
        setStatusBox("Error");
				setMessageBox(ErrorUtil.getThrowInfo(t));
				
				synchronized(state_obj)
				{
					send_state=0;
				}
			}
    }

  }

  private void setupTx(SendState s) throws Exception
  {
    setStatusBox("Creating transaction");
    setMessageBox("");
    TransactionFactoryConfig.Builder config = TransactionFactoryConfig.newBuilder();
    config.setSign(true);
    config.setChangeFreshAddress(true);
    config.setInputConfirmedThenPending(true);
    config.setFeeUseEstimate(true);

    SnowBlossomClient dest_client = ice_leaf.getWalletPanel().getWallet( s.get("wallet_dest"));
    SnowBlossomClient src_client = ice_leaf.getWalletPanel().getWallet( s.get("wallet_source"));

    if (dest_client == null)
    {
      throw new Exception("Must specify destination wallet");
    }
    if (src_client == null)
    {
      throw new Exception("Must specify source wallet");
    }
    AddressSpecHash dest_addr = dest_client.getPurse().getUnusedAddress(false,false);

    long output_val = 0;
    if (s.get("send_amount").toLowerCase().equals("all"))
    {
      config.setSendAll(true);
    }
    else
    {
      output_val = (long) (Double.parseDouble(s.get("send_amount")) * Globals.SNOW_VALUE);
    }

    TransactionOutput.Builder tx_out = TransactionOutput.newBuilder();
    tx_out.setValue(output_val);
    tx_out.setRecipientSpecHash(dest_addr.getBytes());

    if (s.get("name").trim().length() > 0)
    {
      String name = s.get("name");
      String nametype = s.get("name_type").toLowerCase();
      String fbo = s.get("fbo_address");
      AddressSpecHash fbo_hash = null; 
      if (nametype.equals("user"))
      {
        tx_out.setIds( ClaimedIdentifiers.newBuilder().setUsername(ByteString.copyFrom(name.getBytes())).build() );
        fbo_hash = AddressUtil.getHashForAddress("node", fbo);
      }
      else if (nametype.equals("channel"))
      {
        tx_out.setIds( ClaimedIdentifiers.newBuilder().setChannelname(ByteString.copyFrom(name.getBytes())).build() );
        fbo_hash = AddressUtil.getHashForAddress("chan", fbo);
      }
      else
      { 
        throw new ValidationException("Nametype must be 'user' or 'channel'");
      }

      tx_out.setForBenefitOfSpecHash(fbo_hash.getBytes());
    }
    if (s.get("lock_until").trim().length() > 0)
    {
      int lock_block = Integer.parseInt(s.get("lock_until"));
      tx_out.setRequirements( TransactionRequirements.newBuilder().setRequiredBlockHeight(lock_block).build() );
    }

    config.addOutputs(tx_out.build());


    tx_result = TransactionFactory.createTransaction(config.build(), src_client.getPurse().getDB(), src_client);

    setMessageBox(String.format("Press Send again to when progress bar is full to send:\n%s",
      TransactionUtil.prettyDisplayTx(tx_result.getTx(), ice_leaf.getParams())));
    
  }

  public void setProgressBar(int curr, int net)
		throws Exception
  {
    int enet = Math.max(net, curr);
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run()
      {
        send_bar.setMaximum(enet);
        send_bar.setValue(curr);
      }
    });
  }

  public class SendState
  {
    TreeMap<String, String> state_map = new TreeMap<>();;
    public SendState()
    {
      state_map.put("wallet_source", (String)wallet_source_box.getSelectedItem());
      state_map.put("wallet_dest", (String)wallet_dest_box.getSelectedItem());
      state_map.put("send_amount", send_amount_field.getText());
      state_map.put("lock_until", lock_until_field.getText());
      state_map.put("name_type", (String)name_type_combo.getSelectedItem());
      state_map.put("fbo_address", fbo_address_field.getText());
      state_map.put("name", name_field.getText());
    }
    public void check()
      throws Exception
    {
      SendState c = new SendState();
      for(String k : state_map.keySet())
      {
        if (!get(k).equals(c.get(k)))
        {
          throw new Exception(String.format("Value %s was changed from %s to %s", k, get(k), c.get(k)));
        }
      }
      if (state_map.size() != c.state_map.size())
      {
        throw new Exception("SavedState size map mismatch (somehow)");
      }
    }

    public String get(String k) { return state_map.get(k);}
  }


}
