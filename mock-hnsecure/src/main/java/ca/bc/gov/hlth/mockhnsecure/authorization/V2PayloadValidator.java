package ca.bc.gov.hlth.mockhnsecure.authorization;

import org.apache.camel.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class V2PayloadValidator {

    private static final Logger logger = LoggerFactory.getLogger(V2PayloadValidator.class);

    private static Set<String> validV2MessageTypes;

    public V2PayloadValidator(AuthorizationProperties authorizationProperties) {
        validV2MessageTypes = authorizationProperties.getValidV2MessageTypes();
    }

    /**
     * Validates the Hl7V2 transaction type (MSH.8) against the list of valid transaction type (valid-v2-message-types)
     * @param v2Message the hl7v2 message to validate
     * @throws Exception message is not valid hl7v2 or is not an accepted v2 transaction type
     */
    @Handler
    public static void validate(String v2Message) throws Exception {

        int mshIndex = v2Message.indexOf("MSH|");
        if (mshIndex != 8) {
            throw new Exception("Message doesn't start with MSH and is an invalid v2 message");
        }
        logger.info("Message is valid v2 beginning with MSH");

        String transactionType = v2Message.split("\\|")[8];
        logger.info("v2 transaction type is " + transactionType);

        if (!validV2MessageTypes.stream().anyMatch(transactionType::equalsIgnoreCase)) {
            throw new Exception("v2 Transaction type " + transactionType + " is not valid for this service");
        }

    }
}
