package ca.bc.gov.hlth;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AuthorizationProcessor implements Processor {

    public void process(Exchange exchange) {
        System.out.println("ACCESS TOKEN:" + exchange.getIn().getHeader("Authorization"));
    }
}
