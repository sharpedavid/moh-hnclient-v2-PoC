package ca.bc.gov.hlth.hnclientv2;

import io.netty.buffer.ByteBuf;
import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {

    /**
     * Camel route that:
     *   1. Receives a message over tcp
     *   2. Retrieves a access token using Client Credential Grant
     *   3. Passes the message to an http endpoint with the JWT attached
     *   4. Returns the response
     */
    @Override
    public void configure() {
        from("netty:tcp://{{hostname}}:{{port}}")
                .log("HNClient Received a request")
                .log("Retrieving Access Token")
                .process(new OAuthProcessor())
                .log("Sending to hnsecure")
                .to("http://{{hnsecure-hostname}}:{{hnsecure-port}}/{{hnsecure-endpoint}}")
                .log("Received response from hnsecure")
                .convertBodyTo(String.class)
                .log("response message: ${body}")
                .convertBodyTo(ByteBuf.class);
    }
}
