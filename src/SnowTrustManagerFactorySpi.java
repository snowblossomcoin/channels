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
import snowblossom.lib.*;
import java.security.Provider;
import java.security.PublicKey;

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
      logger.info("Evaluating client cert");    

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
    {
      logger.info("Evaluating server cert");
      if (chain.length != 1)
      {
        throw new CertificateException("Unexpected cert chain length");
      }
      X509Certificate cert = chain[0];
      System.out.println(cert);

      byte[] claim_data = cert.getExtensionValue("2.5.29.134");
      if (claim_data == null)
      {
        throw new CertificateException("Missing snowblossom claim data in oid 2.5.29.134");
      }
      System.out.println("Decoded claim size: " + claim_data.length);
      AddressSpec address_spec;

      try
      {
        // It is best not to think about this
        ASN1StreamParser parser = new ASN1StreamParser(claim_data);
        ASN1Encodable o = parser.readObject();
        DEROctetStringParser dero = (DEROctetStringParser) o;
        address_spec = AddressSpec.parseFrom(dero.getOctetStream());
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
          logger.info("Server matched expected spec hash");
        }
        else
        {
          throw new CertificateException("Server did not claim the expected address");
        }
      }
      if (address_spec.getRequiredSigners() != 1) throw new CertificateException("Multisig not supported for TLS certs");
      if (address_spec.getSigSpecsCount() != 1) throw new CertificateException("Multisig not supported for TLS certs");
      try
      {

        String algo = SignatureUtil.getAlgo(address_spec.getSigSpecs(0).getSignatureType());
        PublicKey address_key = KeyUtil.decodeKey(address_spec.getSigSpecs(0).getPublicKey(), algo);

        if (!address_key.equals(cert.getPublicKey()))
        {
          throw new CertificateException("Public key mismatch");
        }

        //cert.verify(address_key, provider);
      }
      catch(Exception e)
      {
        throw new CertificateException(e);
      }
      logger.info("Certificate checks out");

      //logger.info(cert.toString());
    }




  }

}
