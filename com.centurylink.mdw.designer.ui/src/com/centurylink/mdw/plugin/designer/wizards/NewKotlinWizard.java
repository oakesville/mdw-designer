package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.Kotlin;

public class NewKotlinWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.kotlin";

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new Kotlin());
    }
}
