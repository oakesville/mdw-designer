/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.designer.testing;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.utilities.JsonUtil;

public class ApiResponse implements Verifiable{

    public ApiResponse(JSONObject json) {
        try {
            if (json.has("status")) {
                JSONObject jsonObj = json.getJSONObject("status");
                this.setStatus(new Status(jsonObj));
            }
            if (json.has("body"))
                this.setBody(json.getString("body"));
            if (json.has("time"))
                this.setTime(Integer.parseInt(json.getString("time")));
            if (json.has("headers")) {
                JSONObject headersJson = json.getJSONObject("headers");
                this.setHeaders(JsonUtil.getMap(headersJson));
            }
            if (json.has("options")) {
                JSONObject optionsJson = json.getJSONObject("options");
                this.setOptions(JsonUtil.getJsonObjects(optionsJson));
            }
            if (json.has("values")) {
                JSONObject valuesJson = json.getJSONObject("values");
                this.setValues(JsonUtil.getJsonObjects(valuesJson));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    private Map<String, String> headers;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    private int time;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Case-insensitive lookup.  Returns the value with it's original capitalization.
     */
    public String getHeader(String name) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase(name))
                return headers.get(key);
        }
        return null;
    }

    /**
     * Verifies that a header matches the expected value. Both name and value
     * are case-insensitive.
     */
    public boolean checkHeader(String name, String value) {
        String val = getHeader(name);
        return val == null ? value == null : val.equalsIgnoreCase(value);
    }

    /**
     * Verifier options.
     */
    private Map<String, JSONObject> options;

    public Map<String, JSONObject> getOptions() {
        return options;
    }

    public void setOptions(Map<String, JSONObject> options) {
        this.options = options;
    }

    /**
     * Verifier values.
     */
    private Map<String, JSONObject> values;

    public Map<String, JSONObject> getValues() {
        return values;
    }

    public void setValues(Map<String, JSONObject> values) {
        this.values = values;
    }

    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
