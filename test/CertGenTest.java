package channels;

import snowblossom.lib.KeyUtil;

import org.junit.Test;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.security.KeyPair;
import snowblossom.lib.Globals;
import snowblossom.channels.CertGen;



public class CertGenTest
{

  @BeforeClass
  public static void loadProvider()
  {
    Globals.addCryptoProvider();
  }

  @Test
  public void testGen()
    throws Exception
  {
    KeyPair pair = KeyUtil.generateECCompressedKey();
    CertGen.generateSelfSignedCert(pair);


  }

}
