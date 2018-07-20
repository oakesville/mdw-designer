package com.centurylink.mdw.plugin.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.centurylink.mdw.cli.Convert;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ConvertPropertiesToYaml implements IObjectActionDelegate {
    private ISelection selection;

    public ISelection getSelection() {
        return selection;
    }

    private Shell shell;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    @SuppressWarnings("restriction")
    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            if (structuredSelection.getFirstElement() instanceof IFile) {
                final IFile inputFile = (IFile) structuredSelection.getFirstElement();
                final WorkflowProject project = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(inputFile.getProject().getName());
                BusyIndicator.showWhile(
                        MdwPlugin.getActiveWorkbenchWindow().getShell().getDisplay(),
                    new Runnable() {
                        public void run() {
                            try {
                                    Convert convert = new Convert(inputFile.getRawLocation().makeAbsolute()
                                            .toFile());
                                    convert.setConfigLoc(inputFile.getParent().getRawLocation().makeAbsolute()
                                            .toFile().getAbsolutePath());
                                    convert.run();
                                    inputFile.getParent().refreshLocal(IResource.DEPTH_ONE,
                                            new NullProgressMonitor());
                            } catch (Exception ex) {
                                PluginMessages.uiError(shell, ex,
                                        "Convert Application Properties", project);
                            }
                        }
                    });
            }
        }
    }
}
