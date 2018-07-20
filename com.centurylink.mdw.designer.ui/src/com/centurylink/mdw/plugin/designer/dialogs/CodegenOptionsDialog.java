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
package com.centurylink.mdw.plugin.designer.dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.designer.pages.ExportHelper;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class CodegenOptionsDialog extends TrayDialog {
    private Text packageNameTextField;
    private Text configTextField;
    private Text swaggerUrlTextField;
    private Button apiCheckbox;
    private Button modelCheckbox;
    private Button docsCheckbox;

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private String pacakgeName;

    public String getPackageName() {
        return pacakgeName;
    }

    private String configDir;

    public String getConfigDir() {
        return configDir;
    }

    private String swaggerUrl;

    public String getSwaggerUrl() {
        return swaggerUrl;
    }

    private List<String> generateOptions = new ArrayList<>();

    public List<String> getGenerateOptions() {
        return generateOptions;
    }

    public CodegenOptionsDialog(Shell shell, WorkflowProcess process) {
        super(shell);
        this.process = process;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Codegen Options");

        new Label(composite, SWT.NONE).setText("Swagger Url:");
        swaggerUrlTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = 460;
        swaggerUrlTextField.setLayoutData(gd);
        swaggerUrl = process.getProject().getServiceUrl() + "/api-docs/"
                + process.getPackage().getName().replace('.', '/')
                + process.getAttribute("requestPath") + ".json";
        swaggerUrlTextField.setText(swaggerUrl);
        swaggerUrlTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String url = swaggerUrlTextField.getText().trim();
                if (!checkUrl(url)) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    WarningTray tray = getWarningTray();
                    tray.setMessage("Swagger url is not valid/accessible");
                    tray.open();
                }
                else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    getWarningTray().close();
                }
                swaggerUrl = url;
            }
        });

        new Label(composite, SWT.NONE).setText("Package Name:");
        packageNameTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 300;
        packageNameTextField.setLayoutData(gd);
        pacakgeName = process.getPackage().getName();
        packageNameTextField.setText(pacakgeName);
        packageNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String name = packageNameTextField.getText().trim();
                if (name.length() == 0) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    return;
                }
                pacakgeName = name;
                getButton(IDialogConstants.OK_ID).setEnabled(true);
                }
        });

        new Label(composite, SWT.NONE).setText("Config Dir:");
        configTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 300;
        configTextField.setLayoutData(gd);
        configDir = process.getProject().getProjectDirWithFwdSlashes() + "/config";
        configTextField.setText(configDir);
        configTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String confDir = configTextField.getText().trim();
                if (confDir.length() == 0) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    return;
                }
                configDir = confDir;
                getButton(IDialogConstants.OK_ID).setEnabled(true);
                }
        });

        new Label(composite, SWT.NONE);
        Group optionsGroup = new Group(composite, SWT.NONE);
        optionsGroup.setText("Generate Options");
        GridLayout gl = new GridLayout();
        gl.numColumns = 6;
        optionsGroup.setLayout(gl);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        optionsGroup.setLayoutData(gd);

        apiCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        apiCheckbox.setLayoutData(gd);
        apiCheckbox.setText("APIs");
        apiCheckbox.setSelection(true);

        modelCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        modelCheckbox.setLayoutData(gd);
        modelCheckbox.setText("Models");
        modelCheckbox.setSelection(false);


        docsCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        docsCheckbox.setLayoutData(gd);
        docsCheckbox.setText("Docs");
        docsCheckbox.setSelection(false);

        return composite;
    }

    private WarningTray warningTray;

    public WarningTray getWarningTray() {
        if (warningTray == null)
            warningTray = new WarningTray(this);
        return warningTray;
    }

    private boolean checkUrl(String urlString) {
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();
            is = connection.getInputStream();
        }
        catch (IOException ex) {
            return false;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {
                }
            }
        }
        return true;
    }

    @Override
    protected void okPressed() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                if (!apiCheckbox.getSelection() && !modelCheckbox.getSelection()
                        && !docsCheckbox.getSelection()) {
                    WarningTray tray = getWarningTray();
                    tray.setMessage("Please select generate options");
                    tray.open();
                }
                else {
                    getWarningTray().close();
                    addGenerateOptions();
                    setReturnCode(OK);
                    close();
                }
            }
        });
    }

    private void addGenerateOptions() {
        if (!modelCheckbox.getSelection()) {
            generateOptions.add("-Dapis=");
        }
        else if (!apiCheckbox.getSelection()) {
            generateOptions.add("-Dmodels=");
        }
        if (!docsCheckbox.getSelection()) {
            generateOptions.add("-DapiDocs=false");
            generateOptions.add("-DmodelDocs=false");
        }
    }
}