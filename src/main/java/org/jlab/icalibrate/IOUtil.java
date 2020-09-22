package org.jlab.icalibrate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Logger;

public class IOUtil {

    private static final Logger LOGGER = Logger.getLogger(
            IOUtil.class.getName());

    private IOUtil() {
        // Can't instantiate publicly
    }

    /**
     * Reads in an InputStream fully and returns the result as a String.
     *
     * @param is The InputStream
     * @param encoding The character encoding of the String
     * @return The String representation of the data
     */
    public static String streamToString(InputStream is, String encoding) {
        String str = "";

        Scanner scan = new Scanner(is, encoding).useDelimiter("\\A");

        if (scan.hasNext()) {
            str = scan.next();
        }

        return str;
    }

    public static String doHtmlGet(String urlStr, int connectTimeout, int readTimeout) throws MalformedURLException, ProtocolException, IOException {
        URL url;
        HttpURLConnection con;
   
        url = new URL(urlStr);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);
        
        return streamToString(con.getInputStream(), "UTF-8");
    }

    /**
     * Escape XML characters.  This method aids with preparing a string to be inserted safely into HTML for example.
     *
     * @param input The unescaped string
     * @return The escaped string
     */
    public static String escapeXml(String input) {
        String output = input;

        if (input != null) {
            output = output.replace("&", "&#038;"); // Must do this one first as & within other replacements
            output = output.replace("\"", "&#034;");
            output = output.replace("'", "&#039;");
            output = output.replace("<", "&#060;");
            output = output.replace(">", "&#062;");
        }
        return output;
    }
}
