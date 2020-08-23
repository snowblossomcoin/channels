package snowblossom.channels;

import java.io.File;
import snowblossom.node.StatusInterface;
import snowblossom.proto.WalletDatabase;
import snowblossom.util.proto.SymmetricKey;

public class FileBlockImportSettings
{
  public final ChannelContext ctx;
  public final File base_path;
  public final WalletDatabase signer;
  public final StatusInterface status;

  public FileBlockImportSettings(ChannelContext ctx, File base_path, WalletDatabase signer, StatusInterface status)
  {
    this.ctx = ctx;
    this.base_path = base_path;
    this.signer = signer;
    if (status == null)
    {
      this.status = new DummyStatusInterface();
    }
    else
    {
      this.status = status;
    }

  }

  private SymmetricKey encryption_key;
  private String encrypt_prefix;

  public void setSymmetricKey(SymmetricKey encryption_key)
  {
    this.encryption_key = encryption_key;
  }
  public void setEncryptPrefix(String encrypt_prefix)
  {
    this.encrypt_prefix = encrypt_prefix;
  }

  public SymmetricKey getSymmetricKey(){ return encryption_key; }
  public boolean encrypt(String prefix)
  {
    if (encryption_key == null) return false;

    if (prefix.startsWith(encrypt_prefix)) return true;
    return false;


  }

}
