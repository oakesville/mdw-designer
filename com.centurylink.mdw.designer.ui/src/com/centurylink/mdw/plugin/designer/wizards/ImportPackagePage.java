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
package com.centurylink.mdw.plugin.designer.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWActivity;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.Discoverer;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.preferences.UrlsPreferencePage;

public class ImportPackagePage extends WizardPage {
    private static final String ASSETS = "/assets";
    private static final String PKG_EXISTS = "Package already exists in version control: ";
    private Button importFileRadio;
    private Button distDiscoverRadio;
    private Button mavenDiscoverRadio;
    private Text filePathText;
    private Button browseImportFileButton;
    private Text discoveryUrlText;
    private Text groupIdText;
    private Button latestVersionsCheckbox;

    private Folder packageFolder;
    private WorkflowElement preselected;
    private boolean is6;

    public ImportPackagePage() {
        setTitle("Import MDW Workflow Package(s)");
        setDescription("Import design assets into your workflow project.");
    }

    @Override
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 3;
        gl.numColumns = ncol;
        composite.setLayout(gl);


        createWorkflowProjectControls(composite, ncol, true);
        createSpacer(composite, ncol);

        is6 = getProject().checkRequiredVersion(6, 0);
        createImportControls(composite, ncol);

        // TODO option to launch directly to Discovery mode
        enableDistDiscoveryControls(isDistributedDiscovery());
        enableFileControls(!isDistributedDiscovery() && !isMavenDiscovery());
        enableMavenDiscoveryControls(isMavenDiscovery());

        setControl(composite);

        filePathText.forceFocus();
    }

    private void createImportControls(Composite parent, int ncol) {
        Group radioGroup = new Group(parent, SWT.NONE);
        radioGroup.setText("Import Type");
        GridLayout gl = new GridLayout();
        gl.numColumns = 3;
        radioGroup.setLayout(gl);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        radioGroup.setLayoutData(gd);

        importFileRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 3;
        importFileRadio.setLayoutData(gd);
        importFileRadio.setSelection(true);
        importFileRadio.setText("From File");
        importFileRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selected = importFileRadio.getSelection();
                distDiscoverRadio.setSelection(!selected);
                enableDistDiscoveryControls(!selected);
                enableFileControls(selected);
                if (is6) {
                    mavenDiscoverRadio.setSelection(!selected);
                    enableMavenDiscoveryControls(!selected);
                }
                handleFieldChanged();
            }
        });

        Label label = new Label(radioGroup, SWT.NONE);
        label.setText("Package File:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
        label.setLayoutData(gd);

        filePathText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        filePathText.setLayoutData(gd);
        filePathText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleFieldChanged();
            }
        });

        browseImportFileButton = new Button(radioGroup, SWT.PUSH);
        browseImportFileButton.setText("Browse...");
        browseImportFileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                dlg.setFilterExtensions(getFileExtensions());
                String res = dlg.open();
                if (res != null)
                    filePathText.setText(res);
            }
        });

        if (is6) {
            mavenDiscoverRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
            gd = new GridData(GridData.BEGINNING);
            gd.horizontalSpan = 3;
            mavenDiscoverRadio.setLayoutData(gd);
            mavenDiscoverRadio.setSelection(false);
            mavenDiscoverRadio.setText("Central Discovery");
            mavenDiscoverRadio.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean selected = mavenDiscoverRadio.getSelection();
                    importFileRadio.setSelection(!selected);
                    distDiscoverRadio.setSelection(!selected);
                    enableFileControls(!selected);
                    enableDistDiscoveryControls(!selected);
                    enableMavenDiscoveryControls(selected);
                    handleFieldChanged();
                }
            });

            label = new Label(radioGroup, SWT.NONE);
            label.setText("Group Id:");
            gd = new GridData(GridData.BEGINNING);
            gd.horizontalIndent = 25;
            label.setLayoutData(gd);

            groupIdText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
            gd = new GridData(GridData.BEGINNING);
            gd.widthHint = 350;
            gd.horizontalSpan = 2;
            groupIdText.setLayoutData(gd);
            groupIdText.setText("com.centurylink.mdw.assets");
            groupIdText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    handleFieldChanged();
                }
            });

            createSpacer(radioGroup, 1);
            latestVersionsCheckbox = new Button(radioGroup, SWT.CHECK | SWT.LEFT);
            gd = new GridData(GridData.BEGINNING);
            gd.horizontalSpan = 2;
            latestVersionsCheckbox.setLayoutData(gd);
            latestVersionsCheckbox.setText("Show only latest released versions");
            latestVersionsCheckbox.setSelection(true);
        }

        String text = "Discovery";
        if (is6)
            text = "Distributed Discovery";
        distDiscoverRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 3;
        distDiscoverRadio.setLayoutData(gd);
        distDiscoverRadio.setSelection(false);
        distDiscoverRadio.setText(text);
        distDiscoverRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selected = distDiscoverRadio.getSelection();
                importFileRadio.setSelection(!selected);
                enableFileControls(!selected);
                enableDistDiscoveryControls(selected);
                if (is6) {
                    enableMavenDiscoveryControls(!selected);
                    mavenDiscoverRadio.setSelection(!selected);
                }
                handleFieldChanged();
            }
        });

        label = new Label(radioGroup, SWT.NONE);
        label.setText("Asset Discovery URL:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
        label.setLayoutData(gd);

        discoveryUrlText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        gd.horizontalSpan = 2;
        discoveryUrlText.setLayoutData(gd);
        if (!getProject().checkRequiredVersion(6))
            discoveryUrlText.setText(MdwPlugin.getSettings().getProjectDiscoveryUrl() + ASSETS);
        discoveryUrlText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleFieldChanged();
            }
        });

        createSpacer(radioGroup, 3);

        if (!getProject().checkRequiredVersion(6, 0)) {
            createSpacer(radioGroup, 1);
            latestVersionsCheckbox = new Button(radioGroup, SWT.CHECK | SWT.LEFT);
            gd = new GridData(GridData.BEGINNING);
            gd.horizontalSpan = 2;
            latestVersionsCheckbox.setLayoutData(gd);
            latestVersionsCheckbox.setText("Show only latest released versions");
            latestVersionsCheckbox.setSelection(true);
        }

        new Label(radioGroup, SWT.NONE);
        Link link = new Link(radioGroup, SWT.SINGLE);
        link.setText(" Configure <A>Default Discovery URL</A>");
        link.setLayoutData(new GridData(GridData.END));
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(getShell(),
                        UrlsPreferencePage.PREF_PAGE_ID, null, null);
                if (pref != null) {
                    pref.open();
                    String discUrl = MdwPlugin.getSettings().getProjectDiscoveryUrl();
                    discoveryUrlText.setText(
                            discUrl.endsWith("/") ? discUrl + "Assets" : discUrl + ASSETS);
                }
            }
        });
    }

    private void enableFileControls(boolean enabled) {
        if (filePathText.isEnabled() != enabled) {
            filePathText.setEnabled(enabled);
            browseImportFileButton.setEnabled(enabled);
        }
    }

    private void enableDistDiscoveryControls(boolean enabled) {
        if (discoveryUrlText.isEnabled() != enabled)
            discoveryUrlText.setEnabled(enabled);
        if (latestVersionsCheckbox != null && latestVersionsCheckbox.isEnabled() != enabled)
            latestVersionsCheckbox.setEnabled(enabled);
    }

    private void enableMavenDiscoveryControls(boolean enabled) {
        if (groupIdText != null && groupIdText.isEnabled() != enabled)
            groupIdText.setEnabled(enabled);
        if (latestVersionsCheckbox != null && latestVersionsCheckbox.isEnabled() != enabled)
            latestVersionsCheckbox.setEnabled(enabled);
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete() && !isZipFormat();
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        if (getProject() == null)
            return false;
        if (!getProject().isFilePersist() && !getProject().getDesignerDataModel()
                .userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN))
            return false;
        if (isDistributedDiscovery()) {
            return discoveryUrlText != null && checkUrl(discoveryUrlText.getText());
        } else if (isMavenDiscovery()) {
            return groupIdText != null && checkString(groupIdText.getText());
        }
        else {
            return filePathText != null && checkFile(filePathText.getText());
        }
    }

    boolean isDistributedDiscovery() {
        return distDiscoverRadio != null && distDiscoverRadio.getSelection();
    }

    boolean isMavenDiscovery() {
        return mavenDiscoverRadio != null && mavenDiscoverRadio.getSelection();
    }

    @Override
    public IStatus[] getStatuses() {
        String msg = null;
        if (getProject() == null)
            msg = "Please select a valid workflow project";
        else if (!getProject().isFilePersist() && !getProject().getDesignerDataModel()
                .userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN))
            msg = "You're not authorized to import into this workflow project.";
        else {
            if (isDistributedDiscovery()) {
                if (!checkUrl(discoveryUrlText.getText()))
                    msg = "Please enter a valid Discovery URL.";
            }
            else if (isMavenDiscovery()) {
                if (!checkString(groupIdText.getText()))
                    msg = "Please enter group id.";
            }
            else {
                if (!checkFile(filePathText.getText()))
                    msg = "Please enter a valid file path.";
            }
        }

        if (msg == null)
            return null;

        return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    @Override
    public WorkflowElement getElement() {
        return ((ImportPackageWizard) getWizard()).getTopFolder();
    }

    @Override
    public IWizardPage getNextPage() {
        ImportPackageWizard wiz = (ImportPackageWizard) getWizard();
        final boolean distributeDisc = isDistributedDiscovery();

        if (distributeDisc || isMavenDiscovery()) {
            preselected = null;
            ((ImportPackageWizard) getWizard()).setHasOldImplementors(false);

            final boolean latestVersionsOnly;
            if (latestVersionsCheckbox != null)
                latestVersionsOnly = latestVersionsCheckbox.getSelection();
            else
                latestVersionsOnly = true;

            final String url = discoveryUrlText.getText().trim();

            final String groupId;
            if (groupIdText != null)
                groupId = groupIdText.getText().trim();
            else
                groupId = null;
            // display a progress dialog since this can take a while
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(getShell());
            try {
                pmDialog.run(true, true, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        monitor.beginTask("Crawling for workflow assets...", 100);
                        try {
                            if (distributeDisc) {
                                packageFolder = populateTopFolder(url, null, latestVersionsOnly, monitor);
                            } else {
                                packageFolder = populateTopFolder(null, groupId, latestVersionsOnly, monitor);
                            }
                            monitor.done();
                        }
                        catch (InterruptedException ex) {
                            throw ex;
                        }
                        catch (Exception ex) {
                            throw new InvocationTargetException(ex);
                        }
                    }
                });
            }
            catch (InterruptedException iex) {
            }
            catch (Exception ex) {
                PluginMessages.uiError(getShell(), ex, "Discover Packages", getProject());
                return null;
            }
        }
        else {
            BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                public void run() {
                    try {
                        packageFolder = populateTopFolder(null, null, false, null);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(getShell(), ex, "Import Packages", getProject());
                    }
                }
            });
        }

        if (packageFolder == null) {
            return null;
        }
        else {
            wiz.setFolder(packageFolder);
            wiz.initializePackageSelectPage(preselected);
            return ((ImportPackageWizard) getWizard()).getImportPackageSelectPage();
        }
    }

    public Folder populateTopFolder(String discoveryUrl, String groupId, boolean latestVersionsOnly,
            IProgressMonitor progressMonitor) throws Exception {
        Folder folder = null;
        getImportPackageWizard().getImportPackageSelectPage().clear();
        if (discoveryUrl != null || groupId != null) {
            if (discoveryUrl != null) {
                if (getProject().checkRequiredVersion(6, 0, 13))
                    folder = new Discoverer(new URL(discoveryUrl), getProject().getHttpHelper(discoveryUrl)).getAssetTopFolder(latestVersionsOnly,
                        progressMonitor);
                else
                    folder = new Discoverer(new URL(discoveryUrl)).getAssetTopFolder(latestVersionsOnly,
                            progressMonitor);
            }
            if (groupId != null)
                folder = new Discoverer(groupId).getAssetTopFolder(latestVersionsOnly,
                        progressMonitor);

            if (getProject().isRemote() && getProject().isGitVcs()) {
                List<Folder> emptyFolders = removeGitVersionedPackages(folder);
                List<Folder> emptyParents = new ArrayList<>();
                for (Folder emptyFolder : emptyFolders) {
                    if (emptyFolder.getParent() instanceof Folder) {
                        Folder parent = emptyFolder.getParent();
                        parent.getChildren().remove(emptyFolder);
                        // go one more level up
                        if (parent.getChildren().isEmpty() && !emptyParents.contains(parent))
                            emptyParents.add(parent);
                    }
                }
                for (Folder emptyParent : emptyParents) {
                    if (emptyParent.getParent() instanceof Folder)
                        (emptyParent.getParent()).getChildren().remove(emptyParent);
                }
            }
        }
        else {
            String filepath = filePathText.getText().trim();
            String contents = FileHelper.getFileContents(filepath);
            folder = new Folder(filepath);
            boolean hasOldImpls = false;
            if (contents.trim().startsWith("{")) {
                ImporterExporterJson importer = new ImporterExporterJson();
                List<PackageVO> packages = importer.importPackages(contents);
                for (PackageVO pkg : packages) {
                    if (getProject().isRemote() && getProject().isGitVcs()) {
                        for (WorkflowPackage existingVcs : getProject().getTopLevelPackages()) {
                            if (existingVcs.getName().equals(pkg.getName()))
                                getImportPackageWizard().getImportPackageSelectPage()
                                        .setError(PKG_EXISTS + pkg.getName());
                        }
                    }
                    File aFile = new File(folder, pkg.getName() + " v" + pkg.getVersionString());
                    ImporterExporterJson jsonExporter = new ImporterExporterJson();
                    List<PackageVO> pkgs = new ArrayList<>();
                    pkgs.add(pkg);
                    JSONObject pkgJson = new JSONObject(jsonExporter.exportPackages(pkgs));
                    pkgJson.put("name", pkg.getName());
                    aFile.setContent(pkgJson.toString(2));
                    folder.addChild(aFile);
                }
                preselected = folder;
            }
            else {
                try {
                    // try and parse as multiple packages
                    PackageDocument pkgDoc = PackageDocument.Factory.parse(contents);
                    QName docElement = new QName("http://mdw.centurylink.com/bpm",
                            "processDefinition");
                    for (MDWProcessDefinition pkgDef : pkgDoc.getPackage()
                            .getProcessDefinitionList()) {
                        if (getProject().isRemote() && getProject().isGitVcs()) {
                            for (WorkflowPackage existingVcs : getProject().getTopLevelPackages()) {
                                if (existingVcs.getName().equals(pkgDef.getPackageName()))
                                    getImportPackageWizard().getImportPackageSelectPage()
                                            .setError(PKG_EXISTS + pkgDef.getPackageName());
                            }
                        }
                        if (!hasOldImpls && getProject().isFilePersist()
                                && !getProject().isRemote())
                            hasOldImpls = checkForOldImplementors(pkgDef);
                        File aFile = new File(folder,
                                pkgDef.getPackageName() + " v" + pkgDef.getPackageVersion());
                        aFile.setContent(pkgDef.xmlText(new XmlOptions().setSaveOuter()
                                .setSaveSyntheticDocumentElement(docElement)));
                        folder.addChild(aFile);
                    }
                    preselected = folder;
                }
                catch (XmlException ex) {
                    // unparseable -- assume single package
                    if (getProject().isRemote() && getProject().isGitVcs()) {
                        MDWProcessDefinition procDef = ProcessDefinitionDocument.Factory
                                .parse(contents, Compatibility.namespaceOptions())
                                .getProcessDefinition();
                        for (WorkflowPackage existingVcs : getProject().getTopLevelPackages()) {
                            if (existingVcs.getName().equals(procDef.getPackageName()))
                                getImportPackageWizard().getImportPackageSelectPage()
                                        .setError(PKG_EXISTS + procDef.getPackageName());
                        }
                    }
                    if (getProject().isFilePersist() && !getProject().isRemote())
                        hasOldImpls = checkForOldImplementors(ProcessDefinitionDocument.Factory
                                .parse(contents, Compatibility.namespaceOptions())
                                .getProcessDefinition());
                    File file = new File(folder, filepath);
                    file.setContent(contents);
                    folder.addChild(file);
                    preselected = file;
                }
            }
            getImportPackageWizard().setHasOldImplementors(hasOldImpls);
        }

        return folder;
    }

    private ImportPackageWizard getImportPackageWizard() {
        return ((ImportPackageWizard) getWizard());
    }

    /**
     * Not foolproof since it relies on asset XML naming convention. returns
     * emptyFolders to be pruned.
     */
    private List<Folder> removeGitVersionedPackages(Folder folder) {
        List<Folder> emptyFolders = new ArrayList<>();
        Map<File, Folder> toRemove = new HashMap<>();
        for (WorkflowElement child : folder.getChildren()) {
            if (child instanceof Folder) {
                for (Folder emptyFolder : removeGitVersionedPackages((Folder) child)) {
                    if (!emptyFolders.contains(emptyFolder))
                        emptyFolders.add(emptyFolder);
                }
            }
            else if (child instanceof File) {
                File file = (File) child;
                String pkgName = file.getName();
                if (file.getParent() instanceof Folder && pkgName.endsWith(".xml")) {
                    pkgName = pkgName.substring(0, pkgName.length() - 3);
                    int lastDash = pkgName.lastIndexOf('-');
                    if (lastDash > 0) {
                        pkgName = pkgName.substring(0, lastDash);
                        for (WorkflowPackage gitPackage : getProject().getTopLevelPackages()) {
                            if (pkgName.equals(gitPackage.getName())) {
                                PluginMessages.log("Import excludes VCS package: " + pkgName);
                                toRemove.put(file, (Folder) file.getParent());
                            }
                        }
                    }
                }
            }
        }
        if (!toRemove.isEmpty()) {
            getImportPackageWizard().getImportPackageSelectPage().setInfo(
                    "Some packages are not displayed since they exist in version control.");
            for (Map.Entry<File,Folder> file : toRemove.entrySet()) {
                Folder removeFrom = file.getValue();
                removeFrom.getChildren().remove(file.getKey());
                if (removeFrom.getChildren().isEmpty())
                    emptyFolders.add(removeFrom);
            }
        }
        return emptyFolders;
    }

    private boolean checkForOldImplementors(MDWProcessDefinition pkgDef) {
        try {
            for (MDWProcess proc : pkgDef.getProcessList()) {
                for (MDWActivity act : proc.getActivityList()) {
                    if (Compatibility.isOldImplementor(act.getImplementation()))
                        return true;
                }
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex); // silently fail and return false
        }
        return false;
    }

    protected String[] getFileExtensions() {
        String[] extns = new String[] { "*.json", "*.zip" };
        if (!getProject().checkRequiredVersion(6))
            extns = new String[] { "*.json", "*.xml" };
        return extns;
    }

    protected boolean isZipFormat() {
        return filePathText.getText().indexOf("zip") > -1;
    }

    public java.io.File getZipFile() {
        return new java.io.File(filePathText.getText());
    }

    protected String getDiscoveryUrl() {
        String url = discoveryUrlText.getText().trim();
        if (!url.isEmpty()) {
            int index = url.indexOf("/services/");
            if (index == -1)
                index = url.indexOf("/Services/");
            if (index == -1)
                index = url.indexOf(ASSETS);
            if (index > 0)
                return url.substring(0, index);
        }
        return null;
    }

    protected String getGroupId() {
        if (groupIdText != null)
            return groupIdText.getText();
        return null;
    }
}