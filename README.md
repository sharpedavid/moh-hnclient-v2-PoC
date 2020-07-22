[![Build Status](https://travis-ci.org/bcgov/moh-hnclient-v2.svg?branch=master)](https://travis-ci.org/bcgov/moh-hnclient-v2)

# moh-hnclient-v2

The HNClient V2 application will receive an HL7v2 message over plain TCP and forward it to a secure endpoint over HTTPS with an OAuth2 access token (retrieved using the OAuth Client Credential Grant).

This project also includes applications that mock out dependencies:
 - `mock-point-of-service`: a point of service application that sends an HL7v2 message over MLLP.
 - `mock-hnsecure`: a resource endpoint that will receive the secure message and validate the access token.

# Set-up

Prerequisites:
- Apache Maven 3.6.1+
- Java 11

## Set the Keycloak client secret as an OS environment variable

`hnclient-v2` requires the client secret for Keycloak client `moh-hnclient`. Lookup the client secret at https://common-logon-dev.hlth.gov.bc.ca. Keycloak credentials are on KeePass.

Once you have the secret, set an OS environment variable:

`MOH_HNCLIENT_SECRET=SECRET`

## Add the Keycloak certificate to the Java TrustStore

In order for `hnclient-v2` to get Access Tokens from Keycloak, it needs to trust the Keycloak installation. Download the certificate from https://common-logon-dev.hlth.gov.bc.ca and add it to Java's truststore (e.g. "C:\Dev\AdoptOpenJDK11\lib\security\cacerts")

# Run the applications

Prerequisites:
- Apache Maven 3.6.1+
- Java 11

hnclient-v2 and mock-hnsecure can be run from the command line:

```
cd hnclient-v2
mvn compile camel:run
```

```
cd mock-hnsecure
mvn compile camel:run
```

After hnclient-v2 and mock-hnsecure are running, you can send a message:

```
cd mock-point-of-service
mvn compile java:exec
```

On a Windows machine you can run `startcamel.bat` to run the above commands.