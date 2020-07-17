package ca.bc.gov.hlth;

import io.netty.buffer.ByteBuf;
import org.apache.camel.builder.RouteBuilder;

public class MyRouteBuilder extends RouteBuilder {


    /**
     * Let's configure the Camel routing rules using Java code...
     */
    @Override
    public void configure() {
        from("netty:tcp://localhost:8080/hnclient")
                .log("HNClient Received a request")
                .log("Sending to hnsecure")
                .to("http://localhost:9090/hl7v2") //Send it to the mock hnsecure
                .log("Received response from hnsecure")
                .convertBodyTo(String.class)
                .log("response message: ${body}")
                .convertBodyTo(ByteBuf.class);
    }

}
