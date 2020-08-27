package snowblossom.channels;

import java.io.File;
import snowblossom.node.StatusInterface;
import snowblossom.proto.WalletDatabase;
import snowblossom.util.proto.SymmetricKey;
import snowblossom.lib.ValidationException;
import snowblossom.channels.proto.EncryptedChannelConfig;


import com.google.protobuf.util.JsonFormat;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;


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

  public void setupEncrypt(ChannelContext ctx, ChannelNode node)
		throws ValidationException
  {
    File settings_file = new File(base_path, "encryption.json");

    if (settings_file.exists())
    {
      try
      {
        EncryptedChannelConfig.Builder encrypted_config = EncryptedChannelConfig.newBuilder();

        JsonFormat.Parser parser = JsonFormat.parser();
        Reader input = new InputStreamReader(new FileInputStream(settings_file));
        parser.merge(input, encrypted_config);

        String key_id = ChannelCipherUtils.getCommonKeyID(ctx);
        if (key_id == null)
        {
          ChannelCipherUtils.establishCommonKey(node, ctx);
          key_id = ChannelCipherUtils.getCommonKeyID(ctx);
        }

        SymmetricKey key = ChannelCipherUtils.getKeyFromChannel(ctx, key_id, signer.getKeys(0));
        setSymmetricKey(key);

        setEncryptPrefix(encrypted_config.getProtectedPath());
      }
      catch(java.io.IOException e)
      {
        throw new ValidationException(e);
      }
    }

  }

}
