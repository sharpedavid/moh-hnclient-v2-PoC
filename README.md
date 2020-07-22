[![Build Status](https://travis-ci.org/bcgov/moh-hnclient-v2.svg?branch=master)](https://travis-ci.org/bcgov/moh-hnclient-v2)

# moh-hnclient-v2

The HNClient V2 application will recieve an HL7v2 message over plain TCP and forward it to a secure endpoint over HTTPS with an OAuth2 access token (retreived using a client credential grant)

This project also includes code that mocks out:
 - a point of service application that sends an HL7v2 message over MLLP, and
 - a resource endpoint that will receive the secure message and validate the access token.
