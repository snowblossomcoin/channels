package snowblossom.channels;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.ManagerFactoryParameters;

import java.security.cert.X509Certificate;
import java.net.Socket;
import java.security.KeyStore;

import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import snowblossom.proto.AddressSpec;
import snowblossom.channels.proto.SignedMessage;
import snowblossom.channels.proto.SignedMessagePayload;
import snowblossom.lib.*;
import java.security.Provider;
import java.security.PublicKey;
import com.google.protobuf.ByteString;

import org.bouncycastle.asn1.*;

public class SnowTrustManagerFactorySpi extends TrustManagerFactorySpi
{
  private static final Logger logger = Logger.getLogger("snowblossom.channels");
  private AddressSpecHash expected_server_spec_hash;
  private Provider provider;

  public SnowTrustManagerFactorySpi(AddressSpecHash expected_server_spec_hash, Provider provider)
    throws Exception
  {
    this.expected_server_spec_hash = expected_server_spec_hash;
    this.provider = provider;

    
  }

  /**
   * if provided, the expected server spec hash is used to only validate certs
   * that match that.  If null, then allow any cert.
   */
  public static TrustManagerFactory getFactory(AddressSpecHash expected_server_spec_hash)
    throws Exception
  {
    String algo = TrustManagerFactory. getDefaultAlgorithm();
    Provider prov = TrustManagerFactory.getInstance(algo).getProvider();

    return new SnowTrustManagerFactory(new SnowTrustManagerFactorySpi(expected_server_spec_hash, prov), prov, algo);
  }

  @Override
  public TrustManager[] engineGetTrustManagers()
  {
    return new TrustManager[] { new SnowTrustManager() } ;
  }

  @Override
  public void engineInit(KeyStore ks)
  {
    throw new RuntimeException("Keystores are for jerks");
  }

  @Override
  public void engineInit(ManagerFactoryParameters spec)
  {
    throw new RuntimeException("Don't need instructions to know how to rock");
  }

  public class SnowTrustManager implements X509TrustManager
  {
    @Override
    public X509Certificate[] getAcceptedIssuers(){ return new X509Certificate[0]; }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
    {
      logger.log(Level.FINER,"Evaluating client cert");    

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
    {
      logger.log(Level.FINER, "Evaluating server cert");
      if (chain.length != 1)
      {
        throw new CertificateException("Unexpected cert chain length");
      }
      X509Certificate cert = chain[0];

      byte[] claim_data = cert.getExtensionValue("2.5.29.134");
      if (claim_data == null)
      {
        throw new CertificateException("Missing snowblossom claim data in oid 2.5.29.134");
      }
      SignedMessage sm;
      AddressSpec address_spec;
      ByteString tls_pub_key;

      try
      {
        // It is best not to think about this
        ASN1StreamParser parser = new ASN1StreamParser(claim_data);
        ASN1Encodable o = parser.readObject();
        DEROctetStringParser dero = (DEROctetStringParser) o;
        sm = SignedMessage.parseFrom(dero.getOctetStream());


        ChannelSigUtil.validateSignedMessage(sm);
        SignedMessagePayload payload = SignedMessagePayload.parseFrom(sm.getPayload());
        address_spec = payload.getClaim();
        tls_pub_key = payload.getTlsPublicKey();
      }
      catch(Exception e)
      {
        throw new CertificateException(e);
      }

      AddressSpecHash found_claim = AddressUtil.getHashForSpec(address_spec);

      if (expected_server_spec_hash != null)
      {
        if (found_claim.equals(expected_server_spec_hash))
        {
          logger.log(Level.FINER,"Server matched expected spec hash");
        }
        else
        {
          throw new CertificateException("Server did not claim the expected address");
        }
      }

      try
      {

        String algo = "RSA";
        PublicKey address_key = KeyUtil.decodeKey(tls_pub_key, algo);

        // Since we can't use verify below, just checking that the keys
        // are the same
        ByteString address_key_bs = ByteString.copyFrom(address_key.getEncoded());
        ByteString cert_key_bs = ByteString.copyFrom(cert.getPublicKey().getEncoded());

        //System.out.println("Address key: " + HexUtil.getHexString(address_key_bs));
        //System.out.println("Cert key: " + HexUtil.getHexString(cert_key_bs));

        if (!address_key_bs.equals(cert_key_bs))
        {
          throw new CertificateException("Public key mismatch");
        }

        // This gets into some recusion loop and overflows the stack.  shrug.
        //cert.verify(address_key, provider);
      }
      catch(Exception e)
      {
        throw new CertificateException(e);
      }
      logger.log(Level.FINER, "Certificate checks out");

    }




  }

}
