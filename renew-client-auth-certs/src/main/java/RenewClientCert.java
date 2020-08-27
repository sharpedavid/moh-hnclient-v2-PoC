
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class RenewClientCert {

    private static final String tokenEndpoint = "https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/openid-connect/token";
    //client id is jwt-test
    private static final String certUploadEndpoint = "https://common-logon-dev.hlth.gov.bc.ca/auth/admin/realms/moh_applications/clients/8a88eb56-a862-4ad5-a361-80142a4b7e01/certificates/jwt.credential/upload-certificate";
    private static final String pathToKeystore = "C:\\\\Dev\\\\Downloads\\\\jwt-test.jks";
    private static final String pathToCert = "C:\\\\Dev\\\\Downloads\\\\jwt-test.cer";
    private static final String keystorePassword = System.getenv("JWT_TEST_KEYSTORE_PASSWORD");
    private static final String clientId = "jwt-test";
    private static final int certExpiryYears = 1;


    public static void main(String[] args) throws Exception {

        // check expiry of existing cert
        KeyStore keystore = AccessTokenRetriever.loadKeyStore(new File(pathToKeystore), keystorePassword, "jks");
        Date certExpiryDate = ((X509Certificate) keystore.getCertificate(clientId)).getNotAfter();
        LocalDate certExpiry = LocalDate.ofInstant(certExpiryDate.toInstant(), ZoneId.systemDefault());
        LocalDate dateRangeToRenewCert = LocalDate.now().plusDays(400);

        if (!certExpiry.isBefore(dateRangeToRenewCert)) {
            System.out.println("Certificates do not expire before " + dateRangeToRenewCert + ". Certificates will not be renewed");
            return;
        }
        System.out.println("Certificates expire before " + dateRangeToRenewCert + ". Certificates will be renewed");


        //Generate a keypair
        KeyPair kp = generateKeyPair();

        //Generate an x509 certificate using the keypair (required for creating java key stores)
        X509Certificate x509Certificate = generateX509(kp);
        X509Certificate[] certChain = new X509Certificate[1];
        certChain[0] = x509Certificate;

        //Generate a java key store
        KeyStore ks = KeyStore.getInstance("jks");
        char[] password = keystorePassword.toCharArray();
        ks.load(null, password);
        ks.setKeyEntry(clientId, kp.getPrivate(), password, certChain);

        // get an access token using the existing keystore
        AccessTokenRetriever accessTokenRetriever = new AccessTokenRetriever(tokenEndpoint, pathToKeystore, keystorePassword);
        AccessToken accessToken = accessTokenRetriever.getToken();

        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(x509Certificate);
        }
        // Store the public key in cert format
        FileOutputStream certFos = new FileOutputStream(pathToCert, false);
        certFos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
        certFos.close();


        // use the access token upload the public key to Keycloak
        MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
                .addPart("keystoreFormat", "Certificate PEM")
                .addPart("keyAlias", "undefined")
                .addPart("keyPassword", "undefined")
                .addPart("storePassword", "undefined")
                .addPart("file", Path.of(pathToCert));

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest certUpdateRequest = HttpRequest.newBuilder()
                .uri(URI.create(certUploadEndpoint))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "en-CA,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Content-Type", "multipart/form-data; boundary=" + publisher.getBoundary())
                .header("cache-control", "no-cache")
                .POST(publisher.build())
                .build();

        HttpResponse<String> response = httpClient.send(certUpdateRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        // Keycloak seems to take about an hour before the new cert starts working, a PUT to the client url seems to update it immediately
        // but this appears to wipe out the service account role required to update the client again in the future
        // This will not be an issue in a system where Keycloak "client management" api service handles the actual interaction with keycloak

//        HttpRequest clientUpdateRequest = HttpRequest.newBuilder()
//                .uri(URI.create("https://common-logon-dev.hlth.gov.bc.ca/auth/admin/realms/moh_applications/clients/8a88eb56-a862-4ad5-a361-80142a4b7e01/"))
//                .header("Authorization", "Bearer " + accessToken)
//                .header("Accept", "application/json")
//                .header("Content-Type", "application/json")
//                .header("cache-control", "no-cache")
//                .PUT(HttpRequest.BodyPublishers.ofString(
//                        "{\"clientId\": \"jwt-test\"}"))
//                .build();
//
//        HttpResponse<String> clientUpdateResponse = httpClient.send(clientUpdateRequest, HttpResponse.BodyHandlers.ofString());
//        System.out.println(clientUpdateResponse.body());

        // Store the keystore.
        if (response.statusCode() / 100 != 2) { //Check for a 200 in the update, we dont' want to save the new cert if it didn't upload
            throw new Exception("error updating the certificate");
        }
        // TODO look into archiving old certs instead of replacing
        FileOutputStream fos = new FileOutputStream(pathToKeystore, false);
        ks.store(fos, password);
        fos.close();
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    public static X509Certificate generateX509(KeyPair keyPair) throws OperatorCreationException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        //Setup effective and expiry dates
        //Bouncy Castle currently only works with java.util.Date but it's still nicer to create them with java.time.LocalDate
        LocalDate effectiveLocalDate = LocalDate.now();
        LocalDate expiryLocalDate = effectiveLocalDate.plusYears(certExpiryYears);

        Date effectiveDate = java.sql.Date.valueOf(effectiveLocalDate);
        Date expiryDate = java.sql.Date.valueOf(expiryLocalDate);

        //Random for the cert serial
        SecureRandom random = new SecureRandom();

        //Set X509 initialization properties
        X500Name issuer = new X500Name("CN=" + clientId);
        BigInteger serial = new BigInteger(160, random);
        Time notBefore = new Time(effectiveDate);
        Time notAfter = new Time(expiryDate);
        X500Name subject = new X500Name("CN=" + clientId);
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        //Create cert builder
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);
        //Create cert signer using private key
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        //Create the X509 certificate
        X509CertificateHolder certHolder = certBuilder.build(signer);
        //Extract X509 cert from custom Bouncy Castle wrapper
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certHolder);

        cert.verify(keyPair.getPublic());

        return cert;
    }


}
