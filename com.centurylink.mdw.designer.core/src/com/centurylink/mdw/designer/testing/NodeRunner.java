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


import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * Runs NodeJS test assets (through runner.js).
 */
public class NodeRunner {
    static final String NODE_PKG = "com.centurylink.mdw.node";
    static final String PARSER = "parser.js";
    static final String RUNNER = "testRunner.js";

    NodeRunner(){

    }

    public void run(final TestCase testCase, DesignerDataAccess dao, boolean verbose, boolean createReplace) throws ServiceException {

        PackageVO pkgVo = null;
        NodeJS nodeJS = NodeJS.createNodeJS();

        try {
            pkgVo = dao.getPackage(NODE_PKG);
        }
        catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }

        final V8Object fileObj = new V8Object(nodeJS.getRuntime()).add("file", pkgVo.getRuleSet(RUNNER).getRawFile().getAbsolutePath());
        JavaCallback callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return fileObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getRunner");


        final Result parseResult = new Result();
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                V8Object resultObj = parameters.getObject(0);
                parseResult.status = resultObj.getString("status");
                parseResult.message = resultObj.getString("message");
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setParseResult");

        nodeJS.exec(pkgVo.getRuleSet(PARSER).getRawFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        fileObj.release();
        nodeJS.release();

        if (!parseResult.status.equals("OK"))
            throw new ServiceException(PARSER + parseResult);

        nodeJS = NodeJS.createNodeJS();

        final V8Object testObj = new V8Object(nodeJS.getRuntime());
        testObj.add("file", testCase.getCaseFile().getAbsolutePath());
        V8Array valueFiles = new V8Array(nodeJS.getRuntime());
        valueFiles.push("localhost.env"); // TODO hardcoded
        testObj.add("valueFiles", valueFiles);
        valueFiles.release();
        testObj.add("resultDir", testCase.getResultDirectory().getAbsolutePath());
        final Map<String,TestCaseItem> testCaseItems = new HashMap<>();
        final V8Array testItems = new V8Array(nodeJS.getRuntime());
        for (TestCaseItem item : testCase.getItems()) {
            String itemId = item.getName();
            V8Object itemObj = new V8Object(nodeJS.getRuntime()).add("name", item.getName());
            try {
                if (item.getObject().has("request")) {
                    JSONObject request = item.getObject().getJSONObject("request");
                    if (request.has("method")) {
                        itemObj.add("method", request.getString("method"));
                        itemId = request.getString("method") + ":" + itemId;
                    }
                }
                JSONObject options = item.getOptions() == null ? new JSONObject() : item.getOptions();
                if (verbose && !options.has("debug"))
                    options.put("debug", "true");
                if (createReplace && !options.has("overwriteExpected"))
                    options.put("overwriteExpected", "true");
                options.put("qualifyLocations", false);
                if (JSONObject.getNames(options) != null) {
                    V8Object json = nodeJS.getRuntime().getObject("JSON");
                    V8Array params = new V8Array(nodeJS.getRuntime()).push(options.toString());
                    V8Object jsonObj = json.executeObjectFunction("parse", params);
                    itemObj.add("options", jsonObj);
                    params.release();
                    json.release();
                    jsonObj.release();
                }
                if (item.getValues() != null) {
                    V8Object json = nodeJS.getRuntime().getObject("JSON");
                    V8Array params = new V8Array(nodeJS.getRuntime()).push(item.getValues().toString());
                    V8Object jsonObj = json.executeObjectFunction("parse", params);
                    itemObj.add("values", jsonObj);
                    params.release();
                    json.release();
                    jsonObj.release();
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            testItems.push(itemObj);
            testCaseItems.put(itemId, item);
            itemObj.release();
        }
        testObj.add("items", testItems);
        testItems.release();

        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return testObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getTestCase");

        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                String itemId = parameters.getString(0);
                V8Object resultObj =parameters.getObject(1);
                if (itemId == null) {
                    for (TestCaseItem item : testCase.getItems()) {
                        updateItem(item, resultObj);
                    }
                }
                TestCaseItem item = testCaseItems.get(itemId);
                if (item != null) {
                    updateItem(item, resultObj);
                }
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setTestResult");

        final V8 v8 = nodeJS.getRuntime();
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                String itemId = parameters.getString(0);
                V8Object responseObj = parameters.getObject(1);
                TestCaseItem item = testCaseItems.get(itemId);
                if (item != null) {
                    V8Object json = v8.getObject("JSON");
                    V8Array params = new V8Array(v8).push(responseObj);
                    String jsonStr = json.executeStringFunction("stringify", params);
                    params.release();
                    json.release();
                    try {
                        item.setResponse(new JSONObject(jsonStr));
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                responseObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setTestResponse");

        nodeJS.exec(pkgVo.getRuleSet(RUNNER).getRawFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        testObj.release();
        nodeJS.release();
    }

    private class Result {
        String status;
        String message;
        public String toString() {
            return status + ": " + message;
        }
    }

    private void updateItem(TestCaseItem item, V8Object resultObj) {
        item.setStatus(resultObj.getString("status"));
        item.setMessage(resultObj.getString("message"));
    }
}
