/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.auth;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.auth.AuthenticationException;
import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;

/**
 * <p>
 * Authenticates using JWT - normally used in the PaaS space <br/>
 * </p>
 *
 * @author aa56486
 *
 */
public class MdwAuthenticator implements Authenticator {

    private String tokenLocation;
    private String appId;
    private String jwtToken;

    public String getAppId() {
        return appId;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public MdwAuthenticator(String appId, String tokenLoc) {
        this.appId = appId;
        this.tokenLocation = tokenLoc;
    }

    /**
     * Supports existing api with just cuid and pass
     * @param cuid
     * @param pass
     */
    public void authenticate(String cuid, String pass) throws MdwSecurityException {
        jwtToken = doAuthentication(cuid, pass);
    }

    /**
     * <p>
     * Takes a cuid and pass combination and authenticates against JWT.
     * </p>
     *
     * @param cuid
     * @param pass
     * @return the JWT access token
     */
    public String doAuthentication(String cuid, String pass) throws MdwSecurityException {
        String accessToken = null;
        try {
            if (StringHelper.isEmpty(tokenLocation)) {
                throw new MdwSecurityException(
                        "Token location is empty, should point to an JWT token location endpoint."
                                + " Unable to authenticate user " + cuid + " with JWT");
            }

            JSONObject json = new JSONObject();
            json.put("user", cuid);
            json.put("password", pass);
            json.put("appId", appId);

            try {
                HttpHelper helper = new HttpHelper(new URL(tokenLocation));
                Map<String, String> hdrs = new HashMap<>();
                hdrs.put("Content-Type", "application/json; charset=utf-8");
                helper.setHeaders(hdrs);
                String response = helper.post(json.toString());
                JSONObject responseJson = new JSONObject(response);
                accessToken = responseJson.getString("mdwauth");
                if (accessToken == null || accessToken.isEmpty())
                    throw new IOException(
                            "User authentication failed with response:" + responseJson);
            }
            catch (IOException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
        catch (Exception ex) {
            String msg = "Unable to authenticate user " + cuid + " with JWT";
            throw new AuthenticationException(msg, ex);
        }
        return accessToken;
    }

    public String getKey() {
        return tokenLocation + "_" + appId;
    }
}
