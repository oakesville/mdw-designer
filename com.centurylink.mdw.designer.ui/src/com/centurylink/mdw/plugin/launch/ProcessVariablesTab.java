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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.VariableValue;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.variables.VariableValuesTableContainer;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class ProcessVariablesTab extends AbstractLaunchConfigurationTab
        implements DirtyStateListener {
    private WorkflowProject workflowProject;

    private VariableValuesTableContainer tableContainer;
    private List<VariableValue> variableValues;

    private Image image = MdwPlugin.getImageDescriptor("icons/variable.gif").createImage();

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        composite.setLayout(topLayout);

        createProcessVariablesSection(composite);
    }

    public String getName() {
        return "Variables";
    }

    @Override
    public Image getImage() {
        return image;
    }

    public void initializeFrom(ILaunchConfiguration launchConfig) {
        try {
            String wfProject = launchConfig
                    .getAttribute(ProcessLaunchConfiguration.WORKFLOW_PROJECT, "");
            workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(wfProject);
            if (workflowProject != null) {
                String procName = launchConfig.getAttribute(ProcessLaunchConfiguration.PROCESS_NAME,
                        "");
                String procVer = launchConfig
                        .getAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, "");
                WorkflowProcess processVersion = workflowProject.getProcess(procName, procVer);
                if (processVersion == null) {
                    // handle condition: obsolete version no longer in project
                    // list, but not yet in archive
                    processVersion = new WorkflowProcess(workflowProject,
                            workflowProject.getDesignerProxy().getProcessVO(procName, procVer));
                }

                Map<String, String> variables = launchConfig.getAttribute(
                        ProcessLaunchConfiguration.VARIABLE_VALUES, new HashMap<String, String>());

                variableValues = new ArrayList<>();
                List<VariableVO> variableVOs;
                String activityId = launchConfig
                        .getAttribute(ActivityLaunchConfiguration.ACTIVITY_ID, "");
                if (activityId == null || activityId.isEmpty())
                    variableVOs = processVersion.getInputVariables();
                else
                    variableVOs = processVersion.getVariables();

                for (VariableVO variableVO : variableVOs) {
                    String varName = variableVO.getVariableName();
                    VariableTypeVO varType = workflowProject.getDataAccess()
                            .getVariableType(variableVO.getVariableType());
                    variableValues
                            .add(new VariableValue(variableVO, varType, variables.get(varName)));
                }
                tableContainer.setInput(variableValues);
            }

            validatePage();
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Launch Init", workflowProject);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy launchConfig) {
        Map<String, String> varVals = new HashMap<>();
        if (variableValues != null) {
            for (VariableValue variableValue : variableValues) {
                varVals.put(variableValue.getName(), variableValue.getValue());
            }
        }
        launchConfig.setAttribute(ProcessLaunchConfiguration.VARIABLE_VALUES, varVals);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig) {
        // do nothing
    }

    private void createProcessVariablesSection(Composite parent) {
        tableContainer = new VariableValuesTableContainer();
        tableContainer.getColumnSpecs().get(0).label = "Input Variable";
        tableContainer.create(parent);
        tableContainer.addDirtyStateListener(this);
    }

    private void validatePage() {
        setErrorMessage(null);
        setMessage(null);

        if (variableValues != null) {
            StringBuilder missingVars = new StringBuilder("");
            for (VariableValue variableValue : variableValues) {
                if (variableValue.getValue() == null || variableValue.getValue().length() == 0) {
                    if (missingVars.length() > 0)
                        missingVars.append(", ");
                    missingVars.append(variableValue.getName());
                }
            }
            if (missingVars.length() > 0) {
                setMessage("Input variables missing values:\n" + missingVars);
                updateLaunchConfigurationDialog();
                return;
            }
        }

        updateLaunchConfigurationDialog();
    }

    @Override
    public boolean canSave() {
        return getErrorMessage() == null;
    }

    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        return canSave();
    }

    @Override
    public void dispose() {
        super.dispose();
        image.dispose();
        if (tableContainer != null)
            tableContainer.removeDirtyStateListener(this);
    }

    public void dirtyStateChanged(boolean dirty) {
        setDirty(dirty);
        validatePage();
    }
}
