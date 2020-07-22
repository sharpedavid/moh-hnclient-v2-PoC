package ca.bc.gov.hlth.mockhnsecure;

import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {

    private static final String responseMessage = "MSH|^~\\&|RAIGT-PRSN-DMGR|BC00002041|HNWeb|BC01000030|20200206123841|train96|R03|1819924|D|2.4^M\n" +
            "MSA|AA|20200206123840|HJMB001ISUCCESSFULLY COMPLETED^M\n" +
            "ERR|^^^HJMB001I&SUCCESSFULLY COMPLETED^M\n" +
            "PID||123456789^^^BC^PH^MOH|||||19840225|M^M\n" +
            "ZIA|||||||||||||||BRANTON^DARREN^S^^^^L|912 YATES ST^^^^^^^^^^^^^^^^^^^VICTORIA^BC^V8V3M2^CAN^H^^^^N|^PRN^PH^^^250^6611681";

    @Override
    public void configure() {
        from("jetty:http://{{hostname}}:{{port}}/{{endpoint}}")
            .log("HNSecure received a request")
            .process(new ValidateAccessToken())
            .setBody(simple(responseMessage));
    }
}
