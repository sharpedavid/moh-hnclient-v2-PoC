package com.cgi.hl7v2sendmessage;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author trevor.schiavone
 */
public class HL7v2SendMessage {

    public static void main(String[] args) throws Exception {
        try (Socket sock = new Socket("127.0.0.1", 8080)) {
            String output;
//            String msg = readInput(args[0]);
            String msg = "This is a test message from a mock Point of Service";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //baos.write(0x0b); //header byte
            //baos.write(msg.length()); //length... does this need to be padded?
            baos.write(msg.getBytes());
            baos.write(0x1c); //trailing bytes
            baos.write(0x0d);
            byte[] message = baos.toByteArray();

            sock.getOutputStream().write(message);
            sock.getOutputStream().flush();
            byte[] response = readStream(sock.getInputStream());
            output = new String(response, "UTF-8");
            Scanner s = new Scanner(output);
            while (s.hasNextLine()) {
                System.out.println(s.nextLine());
            }
        }
    }

    private static String readInput(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        return new String(readStream(fis));
    }

    private static byte[] readStream(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        int len = is.read(buff);
        while (len > 0) {
            baos.write(buff, 0, len);
            if (len == buff.length) {
                len = is.read(buff);
            } else {
                len = 0;
            }
        }
        return baos.toByteArray();
    }

}
