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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
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
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;

/**
 * JDBC DataSource page of the MDW workflow project wizard.
 */
public class DataSourcePage extends WizardPage implements IFacetWizardPage {
    public static final String PAGE_TITLE = "JDBC DataSource Settings";

    private Combo driverComboBox;
    private Text jdbcUrlTextField;
    private Text dbUserTextField;
    private Text dbPasswordTextField;
    private Text schemaOwnerTextField;

    public DataSourcePage() {
        setTitle(PAGE_TITLE);
        setDescription("Enter your MDW DataSource information.\n"
                + "This will be written to your local settings.");
    }

    @Override
    public void initValues() {
        String prevDriver = "";
        String prevUrl = "";
        String prevUser = "";
        if (!getProject().isRemote()) {
            String prefix = "MDW" + getProject().getMdwVersion();
            prevDriver = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_DB_DRIVER);
            prevUrl = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_DB_URL);
            prevUser = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_DB_USER);
        }

        boolean isMdw6 = getProject().checkRequiredVersion(6, 0);
        JdbcDataSource dataSource = getDataSource();

        if (dataSource != null) {
            if (isMdw6)
                dataSource.setDriver(JdbcDataSource.DEFAULT_DRIVER_MDW6);
            else if (prevDriver.length() > 0)
                dataSource.setDriver(prevDriver);
            else
                dataSource.setDriver(JdbcDataSource.DEFAULT_DRIVER);
            driverComboBox.setText(dataSource.getDriver());

            if (isMdw6)
                dataSource.setJdbcUrl(JdbcDataSource.DEFAULT_JDBC_URL_MDW6);
            else if (prevUrl.length() > 0)
                dataSource.setJdbcUrl(prevUrl);
            else
                dataSource.setJdbcUrl(JdbcDataSource.DEFAULT_JDBC_URL);
            jdbcUrlTextField.setText(dataSource.getJdbcUrl());

            if (isMdw6) {
                dataSource.setDbUser(JdbcDataSource.DEFAULT_DB_USER_OLD);
                dataSource.setDbPassword(JdbcDataSource.DEFAULT_DB_PASSWORD_OLD);
            }
            else if (prevUser.length() > 0) {
                dataSource.setDbUser(prevUser);
            }
            else {
                dataSource.setDbUser(JdbcDataSource.DEFAULT_DB_USER);
                dataSource.setDbPassword(JdbcDataSource.DEFAULT_DB_PASSWORD);
                if (!getProject().isRemote() && !getProject().checkRequiredVersion(5, 5)) {
                    // triggers server call to retrieve mdw version -- not for
                    // remote

                    dataSource.setDbUser(JdbcDataSource.DEFAULT_DB_USER_OLD);
                    dataSource.setDbPassword(JdbcDataSource.DEFAULT_DB_PASSWORD_OLD);

                }
            }

            dbUserTextField.setText(dataSource.getDbUser());

            if (prevUser.length() == 0)
                dbPasswordTextField.setText(dataSource.getDbPassword());
        }
    }

    private JdbcDataSource getDataSource() {
        if (getProject() == null)
            return null;
        return getProject().getMdwDataSource();
    }

    /**
     * draw the widgets using a grid layout
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

        createDriverControls(composite, ncol);
        createJdbcUrlControls(composite, ncol);
        createDbUserControls(composite, ncol);
        createDbPasswordControls(composite, ncol);
        createSpacer(composite, ncol);

        setControl(composite);
    }

    /**
     * @see WizardPage#getStatuses()
     */
    @Override
    public IStatus[] getStatuses() {
        if (isPageComplete())
            return null;
        JdbcDataSource dataSource = getDataSource();
        String msg = null;
        if (dataSource != null) {
            if (containsWhitespace(dataSource.getName()))
                msg = "Invalid value for DataSource Name";
            else if (containsWhitespace(dataSource.getDriver()))
                msg = "Invalid value for JDBC Driver";
            else if (containsWhitespace(dataSource.getJdbcUrl()))
                msg = "Invalid value for JDBC URL";
            else if (containsWhitespace(dataSource.getDbUser()))
                msg = "Invalid value for DB User";
            else if (containsWhitespace(dataSource.getDbPassword()))
                msg = "Invalid value for DB Password";
        }

        if (msg == null)
            return null;

        return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    /**
     * sets the completed field on the wizard class when all the information on
     * the page is entered
     */
    public boolean isPageComplete() {
        JdbcDataSource dataSource = getDataSource();
        return dataSource != null && checkStringNoWhitespace(dataSource.getName())
                && checkStringNoWhitespace(dataSource.getDriver())
                && checkStringNoWhitespace(dataSource.getJdbcUrl())
                && checkStringNoWhitespace(dataSource.getDbUser())
                && checkStringNoWhitespace(dataSource.getDbPassword())
                && (schemaOwnerTextField == null || (!schemaOwnerTextField.isEnabled()
                        || checkStringNoWhitespace(dataSource.getSchemaOwner())));
    }

    private void createDriverControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Driver:");
        driverComboBox = new Combo(parent, SWT.DROP_DOWN);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 200;
        driverComboBox.setLayoutData(gd);

        driverComboBox.removeAll();
        for (int i = 0; i < JdbcDataSource.JDBC_DRIVERS.length; i++)
            driverComboBox.add(JdbcDataSource.JDBC_DRIVERS[i]);

        driverComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String driver = driverComboBox.getText();
                JdbcDataSource dataSource = getDataSource();
                if (dataSource != null)
                    dataSource.setDriver(driver);
                handleFieldChanged();
            }
        });
    }

    private void createJdbcUrlControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("JDBC URL:");

        jdbcUrlTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 400;
        gd.horizontalSpan = ncol - 1;
        jdbcUrlTextField.setLayoutData(gd);

        jdbcUrlTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                JdbcDataSource dataSource = getDataSource();
                if (dataSource != null)
                    dataSource.setJdbcUrl(jdbcUrlTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    private void createDbUserControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("DB User:");

        dbUserTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 1;
        dbUserTextField.setLayoutData(gd);
        dbUserTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                JdbcDataSource dataSource = getDataSource();
                if (dataSource != null)
                    dataSource.setDbUser(dbUserTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    private void createDbPasswordControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("DB Password:");

        dbPasswordTextField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 1;
        dbPasswordTextField.setLayoutData(gd);
        dbPasswordTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                JdbcDataSource dataSource = getDataSource();
                if (dataSource != null)
                    dataSource.setDbPassword(dbPasswordTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    public void setWizardContext(IWizardContext context) {
        // do nothing
    }

    public void transferStateToConfig() {
        // do nothing
    }

    @Override
    public IWizardPage getNextPage() {
        if (getProject().isRemote() || getProject().isOsgi()
                || !getProject().checkRequiredVersion(5, 1)) {
            return null;
        }
        else {
            for (IWizardPage page : getWizard().getPages()) {
                if (page.getTitle().equals(ExtensionModulesWizardPage.PAGE_TITLE))
                    return page;
            }
            return super.getNextPage();
        }
    }
}
