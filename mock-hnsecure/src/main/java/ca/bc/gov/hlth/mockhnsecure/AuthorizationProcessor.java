package ca.bc.gov.hlth.mockhnsecure;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationProcessor implements Processor {

    private static Logger logger = LoggerFactory.getLogger(AuthorizationProcessor.class);

    public void process(Exchange exchange) {
        logger.info("ACCESS TOKEN: " + exchange.getIn().getHeader("Authorization"));
    }
}
