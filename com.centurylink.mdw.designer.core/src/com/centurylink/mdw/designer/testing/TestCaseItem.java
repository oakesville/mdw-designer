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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an item within a test case (eg: in a postman collection).
 */
public class TestCaseItem {

    public TestCaseItem(String name) {
        this.object = new JSONObject();
        try {
            this.object.put("name", name);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String msg) {
        this.message = msg;
    }

    /**
     * Must have a 'name' property.
     */
    private JSONObject object;

    public JSONObject getObject() {
        return object;
    }

    public void setObject(JSONObject object) {
        this.object = object;
    }

    /**
     * Test case runner options. The caseName option indicates running without
     * verification as part of a large test script execution. In this case
     * responseObject will be populated.
     */
    private JSONObject options;

    public JSONObject getOptions() {
        return options;
    }

    public void setOptions(JSONObject options) {
        this.options = options;
    }

    public String getOption(String name) {
        if (options == null || !options.has(name))
            return null;
        try {
            return options.getString(name);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setOption(String name, String value) {
        if (options == null)
            options = new JSONObject();
        try {
            options.put(name, value);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runtime values for results comparisons.
     */
    private JSONObject values;

    public JSONObject getValues() {
        return values;
    }

    public void setValues(JSONObject values) {
        this.values = values;
    }

    private JSONObject response;

    public JSONObject getResponse() {
        return response;
    }

    public void setResponse(JSONObject object) {
        this.response = object;
    }

    public String getName() {
        return object == null ? null : object.optString("name");
    }
}
