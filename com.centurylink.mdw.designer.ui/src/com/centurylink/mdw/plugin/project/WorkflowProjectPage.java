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
package com.centurylink.mdw.plugin.project;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.j2ee.ui.project.facet.EarProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;
import org.eclipse.wst.common.project.facet.ui.ModifyFacetedProjectWizard;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class WorkflowProjectPage extends WizardPage implements IFacetWizardPage {
    public static final String PAGE_TITLE = "MDW Workflow Project";
    // page widgets
    private Text sourceProjectNameTextField;
    private Combo mdwVersionComboBox;

    public String getSourceProjectName() {
        if (sourceProjectNameTextField == null)
            return null;
        else
            return sourceProjectNameTextField.getText();
    }

    public WorkflowProjectPage() {
        setTitle(PAGE_TITLE);
        setDescription("Enter the name of your Workflow Project.\n");
    }

    public void createControl(Composite parent) {
        drawWidgets(parent);
    }

    protected ServerSettings getServerSettings() {
        if (getProject() == null)
            return null;
        else
            return getProject().getServerSettings();
    }

    /**
     * Draw the widgets using a grid layout.
     *
     * @param parent
     *            - the parent composite
     */
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 4;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createSourceProjectControls(composite, ncol);
        if (!getProject().isRemote()) {
            createMdwVersionControls(composite, ncol);
            createAuthorControls(composite, ncol);
            getProject().setPersistType(PersistType.Git);
            getProject().getMdwVcsRepository().setProvider(VcsRepository.PROVIDER_GIT);
        }
        setControl(composite);
    }

    /**
     * @see WizardPage#getStatuses()
     */
    @Override
    public IStatus[] getStatuses() {
        if (!getProject().isRemote() && containsWhitespace(getSourceProjectName())) {
            IStatus s = new Status(IStatus.ERROR, getPluginId(), 0,
                    "Source project name cannot contain whitespace.", null);
            return new IStatus[] { s };
        }
        else if (WorkflowProjectManager.getInstance().projectNameExists(getSourceProjectName())) {
            IStatus s = new Status(IStatus.ERROR, getPluginId(), 0,
                    "Source project name already exists.", null);
            return new IStatus[] { s };
        }
        else if (getProject().isEarProject()) {
            // adding workflow facet to existing EAR project
            IJavaProject sourceProject = WorkflowProjectManager
                    .getJavaProject(getSourceProjectName());
            if (!sourceProject.exists()) {
                return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0,
                        "Source project name should refer to a Java project that exists in your workspace.",
                        null) };
            }
            else {
                return new IStatus[] { new Status(IStatus.INFO, getPluginId(), 0,
                        "Please ensure that Source project has either the 'EJB Module' facet or the 'Utility Module' facet\nand that it's included in the EAR project's deployment assembly.",
                        null) };
            }
        }
        else {
            return null;
        }
    }

    /**
     * Sets the completed field on the wizard class when all the information on
     * the page is entered.
     */
    public boolean isPageComplete() {
        boolean complete = true;
        if (getProject().isRemote()) {
            if (!checkString(getProject().getSourceProjectName()))
                complete = false;
        }
        else {
            if (!checkStringNoWhitespace(getProject().getSourceProjectName()))
                complete = false;
            else if (!checkStringNoWhitespace(getProject().getMdwVersion()))
                complete = false;
        }
        if (complete) {
            complete = !WorkflowProjectManager.getInstance()
                    .projectNameExists(getSourceProjectName());
        }
        if (complete && getProject().isEarProject()) {
            // adding workflow facet to existing EAR project
            complete = WorkflowProjectManager.getJavaProject(getSourceProjectName()).exists();
        }
        return complete;
    }

    private void createSourceProjectControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Project Name:");

        sourceProjectNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 2;
        sourceProjectNameTextField.setLayoutData(gd);
        sourceProjectNameTextField.setTextLimit(50);
        final boolean updateEarBasedOnSource = getProject().getEarProjectName() == null;
        sourceProjectNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String workflowProjectName = sourceProjectNameTextField.getText().trim();
                getProject().setSourceProjectName(workflowProjectName);
                if (updateEarBasedOnSource)
                    getProject().setEarProjectName(workflowProjectName + "Ear");
                if (getWizard() instanceof EarProjectWizard) {
                    EarProjectWizard earProjectWizard = (EarProjectWizard) getWizard();
                    earProjectWizard.getDataModel().setProperty(
                            IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME,
                            getProject().getEarProjectName());
                }
                handleFieldChanged();
            }
        });
        // when activated by adding facet to existing cloud project
        if (getProject().isCloudProject() && getProject().getEarProjectName() != null) {
            getProject().setSourceProjectName(getProject().getEarProjectName());
            sourceProjectNameTextField.setText(getProject().getSourceProjectName());
            if (getProject().isCloudProject())
                sourceProjectNameTextField.setEditable(false);
        }

        Label existing = new Label(parent, SWT.BEGINNING);
        existing.setLayoutData(
                new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL));
        // adding facet to existing EAR project
        if (getProject().isEarProject()) {
            existing.setText("  (Existing Java Project)");
            if (getProject().getEarProjectName() != null) {
                IProject proj = MdwPlugin.getWorkspaceRoot()
                        .getProject(getProject().getEarProjectName());
                if (proj != null) {
                    IJavaProject relatedJavaProj = WorkflowProjectManager.getInstance()
                            .getRelatedJavaProject(proj);
                    if (relatedJavaProj != null)
                        sourceProjectNameTextField.setText(relatedJavaProj.getProject().getName());
                }
            }
        }
    }

    private void createMdwVersionControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("MDW Version:");
        mdwVersionComboBox = new Combo(parent, SWT.DROP_DOWN);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        mdwVersionComboBox.setLayoutData(gd);

        mdwVersionComboBox.removeAll();
        List<String> mdwVersions = MdwPlugin.getSettings().getMdwVersions();
        for (int i = 0; i < mdwVersions.size(); i++)
            mdwVersionComboBox.add(mdwVersions.get(i));

        // default to latest version
        String latestVersion = MdwPlugin.getSettings().getLatestMdwVersion();
        getProject().setMdwVersion(latestVersion);

        mdwVersionComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String mdwVersion = mdwVersionComboBox.getText();
                getProject().setMdwVersion(mdwVersion);
                handleFieldChanged();
            }
        });

        mdwVersionComboBox.setText(latestVersion);
    }





    public void setWizardContext(IWizardContext wizardContext) {
        IProjectFacetVersion workflowProjectFacetVersion = null;
        boolean hasJava16Facet = false;

        for (Object selectedFacetVersion : wizardContext.getSelectedProjectFacets()) {
            IProjectFacetVersion projectFacetVersion = (IProjectFacetVersion) selectedFacetVersion;
            if (projectFacetVersion.getProjectFacet().getId().equals("mdw.workflow"))
                workflowProjectFacetVersion = projectFacetVersion;
            else if (projectFacetVersion.getProjectFacet().getId().equals("java")
                    && Float.parseFloat(projectFacetVersion.getVersionString()) >= 1.6)
                hasJava16Facet = true;
        }

        if (workflowProjectFacetVersion != null) {
            setProject((WorkflowProject) wizardContext
                    .getAction(IFacetedProject.Action.Type.INSTALL, workflowProjectFacetVersion)
                    .getConfig());
            if (hasJava16Facet) {
                getProject().setCloudProject(true);
            }
        }
    }

    public void transferStateToConfig() {
        // do nothing
    }

    @Override
    public IWizardPage getNextPage() {
        if (getProject().isRemote()) {
            RemoteWorkflowProjectWizard wizard = (RemoteWorkflowProjectWizard) getWizard();
            return wizard.getRemoteHostInfoPage();
        }
        else if (getWizard() instanceof LocalCloudProjectWizard) {
            // cloud or ear from new project wizard
            LocalCloudProjectWizard wizard = (LocalCloudProjectWizard) getWizard();
            ServerSettingsPage serverSettingsPage = wizard.getTomcatSettingsPage();
            serverSettingsPage.initValues();
            return serverSettingsPage;
        }
        else {
            // is the case when adding workflow facet to existing EAR or Java
            // project
            ModifyFacetedProjectWizard wizard = (ModifyFacetedProjectWizard) getWizard();
            ServerSettingsPage serverSettingsPage = null;

            for (IWizardPage page : wizard.getPages()) {
                if (getServerSettings().isTomcat()
                        && page.getTitle().equals(TomcatSettingsPage.PAGE_TITLE))
                    serverSettingsPage = (ServerSettingsPage) page;

                if (page.getTitle().equals(ExtensionModulesWizardPage.PAGE_TITLE))
                    ((ExtensionModulesWizardPage) page).initValues();
            }
            if (serverSettingsPage != null) {
                serverSettingsPage.initValues();
                return serverSettingsPage;
            }
            else {
                return super.getNextPage();
            }
        }
    }
}
