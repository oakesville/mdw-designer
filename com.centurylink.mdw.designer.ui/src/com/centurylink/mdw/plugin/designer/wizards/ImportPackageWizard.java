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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cli.Import;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.ZipHelper;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.designer.utils.DesignerHttpHelper;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.Importer;
import com.centurylink.mdw.plugin.designer.SwtProgressMonitor;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerView;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ImportPackageWizard extends Wizard implements IImportWizard {
    private ImportPackagePage importPackagePage;
    private ImportPackageSelectPage importPackageSelectPage;
    private boolean zipFormat;
    private java.io.File importFile;
    private String discoveryUrl;
    private boolean mavenDiscovery;
    private String groupId;

    ImportPackageSelectPage getImportPackageSelectPage() {
        return importPackageSelectPage;
    }

    private IWorkbench workbench;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    private Folder topFolder;

    public Folder getTopFolder() {
        return topFolder;
    }

    public Folder getFolder() {
        return (Folder) topFolder.getChildren().get(0);
    }

    void setFolder(Folder folder) {
        if (topFolder.getChildren() != null)
            topFolder.getChildren().clear();
        topFolder.addChild(folder);
    }

    private boolean discovery;

    public boolean isDiscovery() {
        return discovery;
    }

    void setDiscovery(boolean disc) {
        this.discovery = disc;
    }

    boolean upgradeAssets;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
        setNeedsProgressMonitor(true);
        setWindowTitle("MDW Import");

        importPackagePage = new ImportPackagePage();
        importPackageSelectPage = new ImportPackageSelectPage();

        topFolder = new Folder("assets");

        if (selection != null && selection.getFirstElement() instanceof WorkflowProject) {
            WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
            topFolder.setProject(workflowProject);
        }
        else if (selection != null && selection.getFirstElement() instanceof WorkflowPackage) {
            WorkflowPackage packageVersion = (WorkflowPackage) selection.getFirstElement();
            topFolder.setProject(packageVersion.getProject());
        }
        else {
            WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                    .findWorkflowProject(selection);
            if (workflowProject != null)
                topFolder.setProject(workflowProject);
        }
    }

    @Override
    public boolean performFinish() {
        final List<WorkflowPackage> importedPackages = new ArrayList<>();
        final List<java.io.File> includes = new ArrayList<>();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    WorkflowProject wfp = topFolder.getProject();
                    DesignerProxy designerProxy = wfp.getDesignerProxy();
                    java.io.File assetDir = wfp.getAssetDir();
                    java.io.File zipFile = null;
                    java.io.File tempDir = wfp.getTempDir();
                    monitor.beginTask("Import Packages",
                            100 * importPackageSelectPage.getSelectedPackages().size());
                    monitor.subTask("Importing selected packages...");
                    monitor.worked(10);

                    StringBuilder sb = new StringBuilder();
                    ProgressMonitor progressMonitor = new SwtProgressMonitor(
                            new SubProgressMonitor(monitor, 100));
                    for (File pkgFile : importPackageSelectPage.getSelectedPackages()) {
                        if (pkgFile.getContent() == null) { // download
                                                            // postponed
                                                            // for
                                                            // discovered
                            if (pkgFile.getUrl() != null) { // assets
                                HttpHelper httpHelper = new HttpHelper(pkgFile.getUrl());
                                httpHelper.setConnectTimeout(
                                        MdwPlugin.getSettings().getHttpConnectTimeout());
                                httpHelper.setReadTimeout(
                                        MdwPlugin.getSettings().getHttpReadTimeout());
                                pkgFile.setContent(httpHelper.get());
                            }
                            else if (mavenDiscovery)
                                importFromMaven(pkgFile.getName(), wfp, includes, monitor);
                            else {
                                getPackageNames(pkgFile.getName(), sb);
                            }
                        }
                        String pkgFileContent = pkgFile.getContent();
                        if (pkgFileContent != null) {
                            Importer importer = new Importer(designerProxy.getPluginDataAccess(),
                                    wfp.isFilePersist() && wfp.isRemote() ? null : getShell());
                            WorkflowPackage importedPackage = importer.importPackage(wfp,
                                    pkgFileContent, progressMonitor);
                            if (importedPackage == null) // canceled
                            {
                                progressMonitor.done();
                                break;
                            }
                            else {
                                if (upgradeAssets) {
                                    progressMonitor.subTask(
                                            "Upgrading activity implementors and other assets...");
                                    designerProxy.upgradeAssets(importedPackage);
                                }

                                if (wfp.isFilePersist()) // file system eclipse
                                                         // sync
                                    wfp.getSourceProject().refreshLocal(2, null);
                                // TODO refresh Archive in case existing package
                                // was
                                // moved there

                                importedPackages.add(importedPackage);
                                includes.add(new java.io.File(
                                        assetDir + "/" + importedPackage.getName().replace('.', '/')));
                            }
                            progressMonitor.done();
                        }
                    }
                    if (sb.length() > 0) {
                        if (!tempDir.exists() && !tempDir.mkdirs()) {
                            throw new IOException("Unable to create temp directory: " + tempDir);
                        }
                        zipFile = new java.io.File(tempDir + "/packages"
                                + StringHelper.filenameDateToString(new Date()) + ".zip");
                        String url = discoveryUrl
                                + "/asset/packages?packages=" + sb.toString();
                        HttpHelper httpHelper = new HttpHelper(new URL(url));
                        httpHelper.setConnectTimeout(
                                MdwPlugin.getSettings().getHttpConnectTimeout());
                        httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());
                        httpHelper.download(zipFile);
                    }

                    if (zipFormat)
                        zipFile = importFile;
                    if (!wfp.isRemote() && zipFile != null)
                        unzipToLocal(wfp, zipFile, tempDir, assetDir, importedPackages,
                                progressMonitor);
                    if (!includes.isEmpty()) {
                        if (!tempDir.exists() && !tempDir.mkdirs()) {
                            throw new IOException("Unable to create temp directory: " + tempDir);
                        }
                        zipFile = new java.io.File(tempDir + "/packages"
                                + StringHelper.filenameDateToString(new Date()) + ".zip");
                        ZipHelper.zipWith(assetDir, zipFile, includes);
                    }
                    if (zipFile != null && wfp.isRemote() && wfp.isFilePersist()) {
                        uploadToRemoteServer(wfp, zipFile);
                        if (!zipFile.delete())
                            PluginMessages.log("Unable to delete the file " + zipFile.getPath());
                        progressMonitor.done();
                    }
                    wfp.getDesignerProxy().getCacheRefresh().doRefresh(true);
                }
                catch (ActionCancelledException ex) {
                    throw new OperationCanceledException();
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                    throw new InvocationTargetException(ex);
                }
            }
        };

        try {
            boolean confirmed = true;
            if (topFolder.getProject().checkRequiredVersion(6, 0, 13)
                    && topFolder.getProject().isRemote())
                confirmed = MessageDialog.openConfirm(getShell(), "Confirm Import",
                        "This import will impact the remote environment. Are you sure you want to import?");
            if (confirmed) {
                getContainer().run(true, true, op);
                if (!importedPackages.isEmpty())
                    DesignerPerspective.promptForShowPerspective(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                            importedPackages.get(0));
                IWorkbenchPage page = MdwPlugin.getActivePage();
                ProcessExplorerView processExplorer = (ProcessExplorerView) page
                        .findView(ProcessExplorerView.VIEW_ID);
                if (processExplorer != null) {
                    processExplorer.handleRefresh();
                    processExplorer.expand(topFolder);
                }
            }
            return true;
        }
        catch (InterruptedException ex) {
            MessageDialog.openInformation(getShell(), "Import Package", "Import Cancelled");
            return true;
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Import Package",
                    importPackagePage.getProject());
            return false;
        }
    }

    @Override
    public void addPages() {
        addPage(importPackagePage);
        addPage(importPackageSelectPage);
    }

    @Override
    public boolean canFinish() {
        zipFormat = importPackagePage.isZipFormat();
        discoveryUrl = importPackagePage.getDiscoveryUrl();
        mavenDiscovery = importPackagePage.isMavenDiscovery();
        groupId = importPackagePage.getGroupId();
        if (importPackagePage.isPageComplete() && zipFormat) {
            importFile = importPackagePage.getZipFile();
            return true;
        }
        else if (importPackagePage.isPageComplete() && importPackageSelectPage.isPageComplete())
            return true;
        return false;
    }

    void initializePackageSelectPage(WorkflowElement preselected) {
        importPackageSelectPage.initialize(preselected);
    }

    void setHasOldImplementors(boolean hasOldImplementors) {
        importPackageSelectPage.showUpgradeAssetsComposite(hasOldImplementors);
    }

    void importFromMaven(String fileName, WorkflowProject wfp, List<java.io.File> includes, IProgressMonitor monitor)
            throws IOException {
        int index = fileName.indexOf(' ');
        String pkg = fileName.substring(0, index);
        String artifact = pkg.replace(groupId.replace(".assets", "."), "").replace('.', '-');
        java.io.File assetDir = wfp.getAssetDir();
        Import importer = new Import(groupId, artifact, fileName.substring(index + 2));
        importer.setAssetLoc(assetDir.getPath());
        importer.setForce(true);
        importer.run(new SwtProgressMonitor(monitor));
        if (wfp.isRemote())
            includes.add(new java.io.File(assetDir + "/" + pkg.replace('.', '/')));
    }

    void getPackageNames(String fileName, StringBuilder sb) {
        if (sb.length() > 0)
            sb.append("," + fileName.substring(0, fileName.indexOf(' ')));
        else
            sb.append(fileName.substring(0, fileName.indexOf(' ')));
    }

    void uploadToRemoteServer(WorkflowProject wfp, java.io.File zipFile)
            throws IOException, GeneralSecurityException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                InputStream is = new FileInputStream(zipFile);) {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = is.read(bytes)) != -1)
                os.write(bytes, 0, read);
            String uploadUrl;
            DesignerHttpHelper httpHelper;
            byte[] resp;
            if (wfp.checkRequiredVersion(6, 0, 13)) {
                uploadUrl = wfp.getServiceUrl() + "/asset/packages";
                Map<String, String> hdrs = new HashMap<>();
                hdrs.put("Content-Type", "application/zip");
                httpHelper = new DesignerHttpHelper(new URL(uploadUrl), wfp.getUser().getJwtToken());
                httpHelper.setHeaders(hdrs);
                resp = httpHelper.putBytes(os.toByteArray());
            }
            else {
                uploadUrl = wfp.getServiceUrl() + "/upload?overwrite=true&assetZip=true&user="
                        + importPackagePage.getProject().getUser().getUsername();
                String encryptedPassword = CryptUtil
                        .encrypt(wfp.getMdwDataSource().getDbPassword());
                httpHelper = new DesignerHttpHelper(new URL(uploadUrl),
                        wfp.getMdwDataSource().getDbUser(), encryptedPassword);
                resp = httpHelper.postBytes(os.toByteArray());
            }
            PluginMessages.log("Asset download respose: " + new String(resp));
        }
    }

    void unzipToLocal(WorkflowProject wfp, java.io.File zipFile, java.io.File tempDir,
            java.io.File assetDir, List<WorkflowPackage> importedPackages,
            ProgressMonitor progressMonitor)
            throws IOException, DataAccessException, CoreException, JSONException {
        VersionControl vcs = new VersionControlGit();
        vcs.connect(null, null, null, wfp.getAssetDir());
        progressMonitor.subTask("Archive existing assets...");
        VcsArchiver archiver = new VcsArchiver(assetDir, tempDir, vcs, progressMonitor);
        archiver.backup();
        PluginMessages.log("Unzipping " + zipFile + " into: " + assetDir);
        ZipHelper.unzip(zipFile, assetDir, null, null, true);
        archiver.archive();
        ZipHelper.unzip(zipFile, tempDir, null, null, true);
        wfp.getSourceProject().refreshLocal(2, null);
        java.io.File explodedDir = new java.io.File(tempDir + "/com");
        if (explodedDir.isDirectory()) {
            List<java.io.File> fileList = FileHelper.getFilesRecursive(explodedDir, "package.json",
                    new ArrayList<java.io.File>());
            for (java.io.File file : fileList) {
                WorkflowPackage workflowPackage = new WorkflowPackage();
                workflowPackage.setProject(wfp);
                workflowPackage.setPackageVO(
                        new PackageVO(new JSONObject(FileHelper.getFileContents(file.getPath()))));
                importedPackages.add(workflowPackage);
            }
            FileHelper.deleteRecursive(explodedDir);
        }

    }
}
