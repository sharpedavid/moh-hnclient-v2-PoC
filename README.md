[![Build Status](https://travis-ci.org/bcgov/moh-hnclient-v2.svg?branch=master)](https://travis-ci.org/bcgov/moh-hnclient-v2)

# MOH HNClient V2

The `hnclient-v2` application will receive an HL7v2 message over plain TCP and forward it to a secure endpoint over HTTPS with an OAuth2 access token (retrieved using the OAuth Client Credential Grant).

This project also includes applications that mock-out dependencies:
 - `mock-point-of-service`: a point of service application that sends an HL7v2 message over MLLP.
 - `mock-hnsecure`: a resource endpoint that receives a message and validates the access token.

 ![hnclientv2](https://user-images.githubusercontent.com/1767127/88949525-36f92f80-d248-11ea-9de7-1479222f1cfd.png)

# Configuration

Prerequisites:
- Apache Maven 3.6.1+
- Java 11

## Step 1: Configure client authentication

### Signed JWT (default)

By default, `hnclient-v2` and our Keycloak development server are configured to use "Signed JWT" client authentication. To use our Keycloak development server with HNClient:

1. Retrieve the `moh-hnclient` JKS file from KeePass in the IAM directory.
2. In `hnclient-v2`'s `application.properties` file, set `jks-file=JKS_FILE_LOCATION`.
3. Set `MOH_HNCLIENT_KEYSTORE_PASSWORD` as an operating system environment variable. The password is also in KeePass on the `moh-hnclient` record.
4. In the `hnclient-v2` `application.properties` file, ensure that `client-auth-type = SIGNED_JWT`.

### Client ID and Password (optional)

`hnclient-v2` also supports "Client ID and Secret" client authentication. To use it, the Keycloak server must be configured to use "Client ID and Secret".

1. Go to the Keycloak development server and look-up the client secret for `moh-hnclient`.
2. Set `MOH_HNCLIENT_SECRET` as an operating system environment variable.
3. In the `hnclient-v2` `application.properties` file, ensure that `client-auth-type = CLIENT_ID_SECRET`.

## Step 2: Add the Keycloak certificate to the Java TrustStore

In order for `hnclient-v2` to get access tokens from Keycloak, it needs to trust the Keycloak development server, which uses a self-signed certificate. Download the certificate from https://common-logon-dev.hlth.gov.bc.ca and add it to Java's truststore (e.g. "C:\Dev\AdoptOpenJDK11\lib\security\cacerts").

# Run the applications

`hnclient-v2` and `mock-hnsecure` can be run from the command line:

```
cd hnclient-v2
mvn compile camel:run
```

```
cd mock-hnsecure
mvn compile camel:run
```

After `hnclient-v2` and `mock-hnsecure` are running, you can send a message using `mock-point-of-service`:

```
cd mock-point-of-service
mvn compile exec:java
```

On a Windows machine, you can run `startcamel.bat` to run the above commands.