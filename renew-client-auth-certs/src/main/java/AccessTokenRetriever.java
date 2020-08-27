import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;

public class AccessTokenRetriever {

    private URI tokenEndpointUri;
    private RSAPrivateKey privateKey;

    public AccessTokenRetriever(String tokenEndpoint, String pathToCert, String keystorePassword) throws Exception {
        File keystoreFile = new File(pathToCert);
        KeyStore keystore = loadKeyStore(keystoreFile, keystorePassword, "jks");

        this.privateKey = (RSAPrivateKey) keystore.getKey("jwt-test", keystorePassword.toCharArray());
        this.tokenEndpointUri = new URI(tokenEndpoint);
    }

    public AccessToken getToken() throws Exception {

        // Construct the client credentials grant type
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // Construct the client authentication method
        ClientID clientID = new ClientID("jwt-test");
        ClientAuthentication clientAuthentication = new PrivateKeyJWT(clientID, tokenEndpointUri, JWSAlgorithm.RS256, privateKey, null, null);

        // Make the token request (need roles scope for updating cert)
        TokenRequest request = new TokenRequest(tokenEndpointUri, clientAuthentication, clientGrant, new Scope("roles"));

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
        if (!response.indicatesSuccess()) {
            // TODO - handle this error
            TokenErrorResponse errorResponse = response.toErrorResponse();
            throw new IllegalStateException(errorResponse.toJSONObject().toString());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        // Get the access token
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        return accessToken;

    }

    public static KeyStore loadKeyStore(File keystoreFile, String password, String keyStoreType) throws Exception {
        try (InputStream is = keystoreFile.toURI().toURL().openStream()) {
            KeyStore keystore = KeyStore.getInstance(keyStoreType);
            keystore.load(is, password.toCharArray());
            return keystore;
        }
    }
}
