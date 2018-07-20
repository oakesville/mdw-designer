package com.centurylink.mdw.designer.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.centurylink.mdw.common.utilities.HttpHelper;

public class DesignerHttpHelper extends HttpHelper {

    private String jwtToken;

    public DesignerHttpHelper(URL url) {
        super(url);
    }

    public DesignerHttpHelper(URL url, String jwtToken) {
        super(url);
        this.jwtToken = jwtToken;
    }

    public DesignerHttpHelper(URL url, String user, String password) {
        super(url, user, password);
    }

    public DesignerHttpHelper(HttpURLConnection connection) {
        super(connection);
    }

    public DesignerHttpHelper(HttpURLConnection connection, String jwtToken) {
        super(connection);
        this.jwtToken = jwtToken;
    }

    public DesignerHttpHelper(HttpURLConnection connection, String user, String password) {
        super(connection, user, password);
    }

    public DesignerHttpHelper(HttpHelper cloneFrom, URL url) {
        super(cloneFrom, url);
    }

    /**
     * Configures the connection timeout values and headers.
     */
    @Override
    protected void prepareConnection(HttpURLConnection connection) throws IOException {
        super.prepareConnection(connection);
        if (jwtToken != null) {
            connection.setRequestProperty(HTTP_BASIC_AUTH_HEADER, "Bearer " + jwtToken);
        }
    }
}
