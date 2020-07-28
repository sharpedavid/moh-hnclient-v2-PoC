package ca.bc.gov.hlth.hnclientv2;

import ca.bc.gov.hlth.hnclientv2.auth.ClientAuthenticationBuilder;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

public class RetrieveAccessToken {

    private static Logger logger = LoggerFactory.getLogger(RetrieveAccessToken.class);

    public String tokenEndpoint;
    public String clientId;
    public String requiredScopes;
    private ClientAuthenticationBuilder clientAuthBuilder;

    public RetrieveAccessToken(String tokenEndpoint, String clientId, String requiredScopes, ClientAuthenticationBuilder clientAuthBuilder) {
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId;
        this.requiredScopes = requiredScopes;
        this.clientAuthBuilder = clientAuthBuilder;

        Util.requireNonBlank(this.tokenEndpoint, "Requires token endpoint.");
        Util.requireNonBlank(this.clientId, "Requires client ID.");
        Objects.requireNonNull(this.clientAuthBuilder, "Requires client authentication.");
    }

    public AccessToken getToken() throws Exception {

        // Construct the client credentials grant
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // The request scope for the token (may be optional)
        Scope scope = new Scope(requiredScopes);

        // The token endpoint
        URI tokenEndpointUri = new URI(tokenEndpoint);

        // Make the token request
        TokenRequest request = new TokenRequest(tokenEndpointUri, clientAuthBuilder.build(), clientGrant, scope);

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