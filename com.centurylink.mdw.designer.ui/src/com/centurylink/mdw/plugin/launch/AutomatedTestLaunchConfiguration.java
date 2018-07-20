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
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.views.AutomatedTestView;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AutomatedTestLaunchConfiguration extends LaunchConfigurationDelegate {
    public static final String WORKFLOW_PROJECT = "workflowProject";
    public static final String WORKFLOW_PACKAGE = "workflowPackage";
    public static final String IS_LEGACY_LAUNCH = "isLegacyLaunch";
    public static final String RESULTS_PATH = "resultsPath";
    public static final String THREAD_COUNT = "threadCount";
    public static final String THREAD_INTERVAL = "threadInterval";
    public static final String RUN_COUNT = "runCount";
    public static final String RUN_INTERVAL = "runInterval";
    public static final String TEST_CASES = "testCases";
    public static final String VERBOSE = "verbose";
    public static final String STUBBING = "stubbing";
    public static final String SINGLE_SERVER = "singleServer";
    public static final String CREATE_REPLACE_RESULTS = "createReplaceResults";
    public static final String IS_LOAD_TEST = "isLoadTest";
    public static final String TESTCASE_COUNTS_MAP = "testCaseCountsMap";

    private AutomatedTestSuite testSuite;

    public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                .getWorkflowProject(launchConfig.getAttribute(WORKFLOW_PROJECT, ""));

        boolean isLoadTest = launchConfig.getAttribute(IS_LOAD_TEST, false);
        String attrPrefix = isLoadTest ? AutomatedTestCase.LOAD_TEST
                : AutomatedTestCase.FUNCTION_TEST;

        String resultsPath = launchConfig.getAttribute(attrPrefix + "_" + RESULTS_PATH, "");
        if (resultsPath.isEmpty())
            resultsPath = WorkflowProject.DEFAULT_TEST_RESULTS_PATH;
        workflowProject.setTestResultsPath(attrPrefix, resultsPath);

        int threadCount = launchConfig.getAttribute(attrPrefix + "_" + THREAD_COUNT, 5);

        int runCount = launchConfig.getAttribute(attrPrefix + "_" + RUN_COUNT, 100);
        int threadInterval = launchConfig.getAttribute(attrPrefix + "_" + THREAD_INTERVAL, 2);
        int runInterval = launchConfig.getAttribute(attrPrefix + "_" + RUN_INTERVAL, 2);
        boolean verbose = launchConfig.getAttribute(attrPrefix + "_" + VERBOSE, true);
        boolean stubbing = launchConfig.getAttribute(attrPrefix + "_" + STUBBING, false);
        boolean singleServer = launchConfig.getAttribute(attrPrefix + "_" + SINGLE_SERVER, true);
        boolean createReplace = launchConfig.getAttribute(attrPrefix + "_" + CREATE_REPLACE_RESULTS,
                false);
        boolean postmanTestsExists = false;
        HashMap<String, Boolean> testCaseOrder = null;

        testSuite = new AutomatedTestSuite(workflowProject);
        testSuite.setLoadTest(isLoadTest);
        testSuite.setThreadCount(threadCount);
        testSuite.setRunCount(runCount);
        if (isLoadTest)
            testSuite.setThreadInterval(runInterval);
        else
            testSuite.setThreadInterval(threadInterval);
        testSuite.setVerbose(verbose);
        testSuite.setStubbing(stubbing);
        testSuite.setSingleServer(singleServer);
        testSuite.setCreateReplaceResults(createReplace);

        List<AutomatedTestCase> testCases = new ArrayList<>();
        List<String> testCasesStr = launchConfig.getAttribute(attrPrefix + "_" + TEST_CASES,
                new ArrayList<String>());
        for (String testCaseStr : testCasesStr) {
            AutomatedTestCase autoTestCase;
            if (testCaseStr.startsWith("Legacy/"))
                autoTestCase = workflowProject.getLegacyTestSuite().getTestCase(testCaseStr);
            else if (testCaseStr.indexOf("postman") > -1) {
                String[] pathArray = testCaseStr.split("/");
                String method = pathArray[2];
                AutomatedTestCase cloneTestCase = (AutomatedTestCase) workflowProject
                        .getAsset(pathArray[0] + "/" + pathArray[1]);
                autoTestCase = new AutomatedTestCase(cloneTestCase);
                autoTestCase.setId(cloneTestCase.getId());
                try {
                    StringBuilder itemName = new StringBuilder(pathArray[3]);
                    for (int i = 4; i < pathArray.length; i++)
                        itemName = itemName.append("/" + pathArray[i]);
                    autoTestCase.setItemName(itemName.toString());
                    autoTestCase.setMethod(method);
                    autoTestCase.getTestCase().resetItems();
                    autoTestCase.getTestCase().addItem(cloneTestCase.getItem(itemName.toString(), method));
                    postmanTestsExists = true;
                }
                catch (Exception e) {
                    PluginMessages.log(e);
                }

            }
            else
                autoTestCase = (AutomatedTestCase) workflowProject.getAsset(testCaseStr);

            autoTestCase.setTestSuite(testSuite);
            if (!autoTestCase.getResultsDir().exists())
                autoTestCase.getResultsDir().mkdirs();
            testCases.add(autoTestCase);
        }
        testSuite.setTestCases(testCases);

        if (postmanTestsExists) {
            testCaseOrder = new HashMap<>();
            List<AutomatedTestCase> cases = new ArrayList<>();
            for (AutomatedTestCase testCase : testSuite.getTestCases()) {
                if (!testCase.isPostman())
                    cases.add(testCase);
                else {
                    if (testCaseOrder.get(testCase.getName()) == null) {
                        List<AutomatedTestCase> postmanCases = testSuite
                                .getPostmanTestCasesByName(testCase.getName());
                        testCaseOrder.put(testCase.getName(), true);
                        AutomatedTestCase postmanCase = testSuite
                                .getPostmanTestCaseByMethod(postmanCases, "GET");
                        if (postmanCase != null)
                            cases.add(postmanCase);
                        postmanCase = testSuite.getPostmanTestCaseByMethod(postmanCases, "DELETE");
                        if (postmanCase != null)
                            cases.add(postmanCase);
                        postmanCase = testSuite.getPostmanTestCaseByMethod(postmanCases, "POST");
                        if (postmanCase != null)
                            cases.add(postmanCase);
                        postmanCase = testSuite.getPostmanTestCaseByMethod(postmanCases, "PUT");
                        if (postmanCase != null)
                            cases.add(postmanCase);
                    }
                }
            }
            testSuite.setTestCases(cases);
        }

        if (isLoadTest) {
            Map<String, String> testCaseCounts = launchConfig.getAttribute(
                    attrPrefix + "_" + TESTCASE_COUNTS_MAP, new HashMap<String, String>());
            for (Map.Entry<String,String> entry : testCaseCounts.entrySet()) {
                int count = Integer.parseInt(entry.getValue());
                testSuite.getTestCase(entry.getKey()).setRunCount(count);
            }
        }

        if (!testSuite.getResultsDir().exists())
            testSuite.getResultsDir().mkdirs();

        testSuite.setDebug(ILaunchManager.DEBUG_MODE.equals(mode));
        showResultsView();
    }

    private void showResultsView() {
        MdwPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
                try {
                    IViewPart viewPart = page.showView("mdw.views.launch.automatedTest");
                    if (viewPart != null) {
                        AutomatedTestView testView = (AutomatedTestView) viewPart;
                        if (testView.isLocked()) {
                            MessageDialog.openError(page.getActivePart().getSite().getShell(),
                                    "Test Exec",
                                    "A test appears to be already running. Please stop the current test or wait for it to complete before launching another one.");
                            return;
                        }
                        testView.setTestSuite(testSuite);
                        if (testSuite.isLoadTest())
                            testView.runLoadTests();
                        else
                            testView.runTests();
                    }
                }
                catch (PartInitException ex) {
                    PluginMessages.uiError(ex, "Test Results", testSuite.getProject());
                }
            }
        });
    }
}
