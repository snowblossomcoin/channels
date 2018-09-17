package snowblossom.channels;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.bouncycastle.cert.X509v3CertificateBuilder;

import org.bouncycastle.asn1.x500.X500Name;
import java.math.BigInteger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Sequence;

import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import java.io.ByteArrayOutputStream;

import com.google.protobuf.ByteString;


public class CertGen
{
  public static ByteString generateSelfSignedCert(KeyPair key_pair)
    throws Exception
  {
    String password="";
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, password.toCharArray());

    byte[] encoded_pub= key_pair.getPublic().getEncoded();
    SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(
      ASN1Sequence.getInstance(encoded_pub));

    String dn="CN=Test";
    X500Name issuer = new X500Name(dn);
    BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
    Date notBefore = new Date(System.currentTimeMillis());
    Date notAfter = new Date(System.currentTimeMillis() + 86400000L * 365L * 10L);
    X500Name subject = issuer;

    X509v3CertificateBuilder cert_builder = new X509v3CertificateBuilder(
      issuer, serial, notBefore, notAfter, subject, subjectPublicKeyInfo);

    String algorithm = "SHA256withECDSA";

    AsymmetricKeyParameter privateKeyAsymKeyParam = PrivateKeyFactory.createKey(key_pair.getPrivate().getEncoded());

    AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(algorithm);
    AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

    ContentSigner sigGen = new BcECContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);

    X509CertificateHolder certificateHolder = cert_builder.build(sigGen);

    System.out.println(certificateHolder);
    
    X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
    X509Certificate[] serverChain = new X509Certificate[1];
    serverChain[0]=cert;

    ks.setKeyEntry("alias", key_pair.getPrivate(), password.toCharArray(), serverChain);

    ByteArrayOutputStream b_out = new ByteArrayOutputStream();
    ks.store(b_out, password.toCharArray());

    return ByteString.copyFrom(b_out.toByteArray());

  }

}

