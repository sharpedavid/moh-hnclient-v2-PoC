package ca.bc.gov.hlth.hnclientv2;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class RetrieveAccessToken {

    private static Logger logger = LoggerFactory.getLogger(RetrieveAccessToken.class);

    public String tokenEndpoint;
    public String clientId;
    public String requiredScopes;

    public RetrieveAccessToken(String tokenEndpoint, String clientId, String requiredScopes) {
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId;
        this.requiredScopes = requiredScopes;
    }

    public AccessToken getToken() throws Exception {

        // Construct the client credentials grant
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // The credentials to authenticate the client at the token endpoint
        ClientID clientID = new ClientID(clientId);
        Secret clientSecret = new Secret(System.getenv("MOH_HNCLIENT_SECRET"));
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

        // The request scope for the token (may be optional)
        Scope scope = new Scope(requiredScopes);

        // The token endpoint
        URI tokenEndpointUri = new URI(tokenEndpoint);

        // Make the token request
        TokenRequest request = new TokenRequest(tokenEndpointUri, clientAuth, clientGrant, scope);

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
        if (!response.indicatesSuccess()) {
            // TODO - handle this error
            TokenErrorResponse errorResponse = response.toErrorResponse();
            throw new IllegalStateException(errorResponse.toJSONObject().toString());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        // Get the access token
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        logger.info(String.format("Access token: %s", accessToken.toJSONString()));

        return accessToken;
    }
}