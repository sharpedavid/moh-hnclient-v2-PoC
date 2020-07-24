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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;

public class ValidateAccessToken implements Processor {

    private static Logger logger = LoggerFactory.getLogger(ValidateAccessToken.class);

    public void process(Exchange exchange) throws MalformedURLException, ParseException, JOSEException, BadJOSEException {

        String accessToken = exchange.getIn().getHeader("Authorization").toString();
        logger.info(String.format("Access token: %s", accessToken));

        // Create a JWT processor for the access tokens
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier(new JOSEObjectType("JWT")));

        // The public RSA keys to validate the signatures
        // The RemoteJWKSet caches the retrieved keys to speed up subsequent look-ups
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(
                new URL("https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/openid-connect/certs")
        );

        // Configure the JWT processor with a key selector to feed matching public
        // RSA keys sourced from the JWK set URL
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);

        // Set the required JWT claims - these must all be available in the token payload
        // TODO - can this be used to validate the actual claim contents?
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier(
                //Exact match claims
                new JWTClaimsSet.Builder()
                        .issuer("https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications")
                        .build(),
                //Required claims
                new HashSet<>(Arrays.asList("sub", "iat", "exp", "scope", "clientId", "jti"))));

        // Process the token
        JWTClaimsSet claimsSet = jwtProcessor.process(accessToken, null);

        // Print out the token claims set
        logger.info("TOKEN PAYLOAD: " + claimsSet.toJSONObject());

    }
}
