package ca.bc.gov.hlth;

import io.netty.buffer.ByteBuf;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class MyRouteBuilder extends RouteBuilder {

    Processor oAuthProcessor = new OAuthProcessor();

    /**
     * Camel route that:
     *   1. Receives a message over tcp
     *   2. Retrieves a access token using Client Credential Grant
     *   3. Passes the message to an http endpoint with the JWT attached
     *   4. Returns the response
     */
    @Override
    public void configure() {
        from("netty:tcp://localhost:8080")
                .log("HNClient Received a request")
                .log("Retrieving Access Token")
                .process(oAuthProcessor)
                .log("Sending to hnsecure")
                .to("http://localhost:9090/hl7v2") //Send it to the mock hnsecure
                .log("Received response from hnsecure")
                .convertBodyTo(String.class)
                .log("response message: ${body}")
                .convertBodyTo(ByteBuf.class);
    }
}
