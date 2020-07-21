package ca.bc.gov.hlth;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.net.URI;

public class OAuthProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        // Construct the client credentials grant
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // The credentials to authenticate the client at the token endpoint
        ClientID clientID = new ClientID("moh-hnclient");
        Secret clientSecret = new Secret("client-secret-goes-here");
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

        // The request scope for the token (may be optional)
        Scope scope = new Scope("address");

        // The token endpoint
        URI tokenEndpoint = new URI("https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/openid-connect/token");

        // Make the token request
        TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, clientGrant, scope);

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
        if (! response.indicatesSuccess()) {
            // We got an error response...
            TokenErrorResponse errorResponse = response.toErrorResponse();
            //TODO - handle this error
            System.out.println(errorResponse);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        // Get the access token
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        System.out.println(accessToken.toJSONString());
        System.out.println(accessToken.getScope());

        exchange.getIn().setHeader("Authorization", accessToken);
    }
}