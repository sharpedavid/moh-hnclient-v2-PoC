package ca.bc.gov.hlth.mockhnsecure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ValidateAccessToken implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ValidateAccessToken.class);

    private Properties applicationProperties;

    /**
     * Default Constructor
     */
    public ValidateAccessToken() {
        // TODO we end up loading application properties both here and in the route - refactor to only load once
        applicationProperties = new Properties();
        try {
            applicationProperties.load(this.getClass().getResourceAsStream("/application.properties"));
        } catch (IOException ex) {
            logger.error("Properties file could not be loaded: ", ex);
        }
    }

    @Override
    public void process(Exchange exchange)
            throws MalformedURLException, ParseException, JOSEException, BadJOSEException {

        String accessToken = exchange.getIn().getHeader("Authorization").toString();
        logger.info(String.format("Access token: %s", accessToken));

        // Create a JWT processor for the access tokens
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier(new JOSEObjectType("JWT")));

        // The public RSA keys to validate the signatures
        // The RemoteJWKSet caches the retrieved keys to speed up subsequent look-ups
        // TODO this should be moved into the constructor to make use of the JWK caching
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(
                new URL("https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/openid-connect/certs"),
                // Overrides the DefaultResourceRetriever to up the timeouts to 5 seconds
                new DefaultResourceRetriever(5000, 5000, 51200)
        );

        // Configure the JWT processor with a key selector to feed matching public
        // RSA keys sourced from the JWK set URL
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);

        // Set the required JWT claims - these must all be available in the token payload
        jwtProcessor.setJWTClaimsSetVerifier(
                new CustomJWTClaimsVerifier(
                        // TODO might be better to just get these during init so we're not doing it for every message
                        // Accepted Audience -> aud
                        getPropertyAsSet("audience"),
                        // Accepted Authorized Parties -> azp
                        getPropertyAsSet("authorized-parties"),
                        // Accepted Scopes -> scope
                        getPropertyAsSet("scopes"),
                        // Exact Match Claims -> iss
                        new JWTClaimsSet.Builder()
                                .issuer("https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications")
                                .build(),
                        // Required Claims -> azp, scope, iat, exp, jti
                        new HashSet<>(Arrays.asList("azp", "scope", "iat", "exp", "jti")),
                        // Prohibited Claims
                        null
                )
        );

        // Process the token
        JWTClaimsSet claimsSet = jwtProcessor.process(accessToken, null);

        // Print out the token claims set
        logger.info("TOKEN PAYLOAD: " + claimsSet.toJSONObject());
    }

    /**
     * Return a list of values from a comma delimited property
     *
     * @param key
     * @return List
     */
    private List<String> getPropertyAsList(String key) {
        String property = applicationProperties.getProperty(key);
        List<String> propertyList = Collections.emptyList();
        if (property != null && !property.isBlank()) {
            propertyList = Arrays.asList(property.split("\\s*,\\s*"));
        }
        return propertyList;
    }

    /**
     * Return a set of values from a comma delimited property
     *
     * @param key
     * @return Set
     */
    private Set<String> getPropertyAsSet(String key) {
        return new HashSet<>(getPropertyAsList(key));
    }
}
