package ca.bc.gov.hlth.mockhnsecure;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Properties;

public class MainMethodTest extends CamelTestSupport {

    public static final int PORT = AvailablePortFinder.getNextAvailable();
    public static final String HOSTNAME = "localhost";
    public static final String ENDPOINT = "hl7v2";
    public static final String INPUT_MESSAGE = InputMessages.MESSAGE_1;

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new Route();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties extra = new Properties();
        extra.put("hostname", HOSTNAME);
        extra.put("port", PORT);
        extra.put("endpoint", ENDPOINT);
        extra.put("valid-v2-message-types",  "r03, r07, r09");
        return extra;
    }

    @Test
    public void smokeTest() throws Exception {
        AdviceWithRouteBuilder.adviceWith(context, "hnsecure-route", a -> {
            // TODO: Really test ValidateAccessToken. You need an access token and a public key.
            a.weaveById("ValidateAccessToken").remove();
            a.weaveById("V2PayloadValidator").remove();
        });
        context.start();

        String endpointUri = String.format("http://%s:%s/%s", HOSTNAME, PORT, ENDPOINT);
        String response = template.requestBody(endpointUri, INPUT_MESSAGE, String.class);

        String expectedContains = "LASTNAME^FIRST^S";
        assertTrue(
                String.format("Response should contain '%s', but was '%s'.", expectedContains, response),
                response.contains(expectedContains)
        );
    }

}