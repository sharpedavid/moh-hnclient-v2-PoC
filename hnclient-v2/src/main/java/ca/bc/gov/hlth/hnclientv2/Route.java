package ca.bc.gov.hlth.hnclientv2;

import ca.bc.gov.hlth.hnclientv2.auth.ClientAuthenticationBuilder;
import ca.bc.gov.hlth.hnclientv2.auth.ClientIdSecretBuilder;
import ca.bc.gov.hlth.hnclientv2.auth.SignedJwtBuilder;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import io.netty.buffer.ByteBuf;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import java.io.File;
import java.net.URISyntaxException;

public class Route extends RouteBuilder {

    @PropertyInject(value = "token-endpoint", defaultValue ="")
    String tokenEndpoint;

    @PropertyInject(value = "client-id", defaultValue = "")
    String clientId;

    @PropertyInject(value = "scopes", defaultValue = "")
    String scopes;

    @PropertyInject(value = "client-auth-type", defaultValue = "")
    String clientAuthType;

    @PropertyInject(value = "jks-file", defaultValue = "")
    private String jksFile;

    @PropertyInject(value = "jks-key-alias", defaultValue = "")
    private String keyAlias;

    /**
     * Camel route that:
     *   1. Receives a message over tcp
     *   2. Retrieves a access token using Client Credential Grant
     *   3. Passes the message to an http endpoint with the JWT attached
     *   4. Returns the response
     */
    @Override
    public void configure() throws URISyntaxException {

        ClientAuthenticationBuilder clientAuthenticationBuilder = getClientAuthentication();
        RetrieveAccessToken retrieveAccessToken = new RetrieveAccessToken(tokenEndpoint, scopes, clientAuthenticationBuilder);

        from("netty:tcp://{{hostname}}:{{port}}")
                .log("HNClient received a request")
                .log("Retrieving Access Token")
                .setHeader("Authorization").method(retrieveAccessToken)
                .log("Sending to HNSecure")
                .to("http://{{hnsecure-hostname}}:{{hnsecure-port}}/{{hnsecure-endpoint}}")
                .log("Received response from HNSecure")
                .convertBodyTo(String.class)
                .log("Response message: ${body}")
                .convertBodyTo(ByteBuf.class);
    }

    private ClientAuthenticationBuilder getClientAuthentication() {
        if (clientAuthType.equals("SIGNED_JWT")) {
            return new SignedJwtBuilder(new File(jksFile), keyAlias, tokenEndpoint);
        } else if (clientAuthType.equals("CLIENT_ID_SECRET")) {
            return new ClientIdSecretBuilder(clientId);
        } else {
            throw new IllegalStateException(String.format("Unrecognized client authentication type: '%s'", clientAuthType));
        }
    }
}
