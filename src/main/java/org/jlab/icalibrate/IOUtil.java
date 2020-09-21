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
}
