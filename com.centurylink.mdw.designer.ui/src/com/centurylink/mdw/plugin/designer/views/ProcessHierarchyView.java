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
package com.centurylink.mdw.plugin.designer.views;

import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.plugin.designer.WorkflowSelectionProvider;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.views.ProcessHierarchyContentProvider.LinkedProcess;

public class ProcessHierarchyView extends ViewPart
        implements ITabbedPropertySheetPageContributor, IMenuListener {
    public static final String VIEW_ID = "mdw.views.designer.processes.hierarchy";

    private TreeViewer treeViewer;
    private ProcessHierarchyActionGroup actionGroup;
    private WorkflowProcess processVersion; // starting pv

    public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent);
        treeViewer.setLabelProvider(new ProcessHierarchyLabelProvider());

        // action group
        actionGroup = new ProcessHierarchyActionGroup(this);
        actionGroup.fillActionBars(getViewSite().getActionBars());

        treeViewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                for (Object item : getSelection().toList()) {
                    if (item instanceof LinkedProcess)
                        actionGroup.getActionHandler().open(((LinkedProcess) item).getProcess());
                    if (item instanceof LinkedProcessInstance) {
                        ProcessInstanceVO procInst = ((LinkedProcessInstance) item)
                                .getProcessInstance();
                        WorkflowProcess pv = processVersion.getProject()
                                .getProcess(procInst.getProcessId());
                        if (pv != null) {
                            WorkflowProcess toOpen = new WorkflowProcess(pv);
                            toOpen.setProcessInstance(procInst);
                            actionGroup.getActionHandler().open(toOpen);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        WorkflowSelectionProvider selectionProvider = new WorkflowSelectionProvider(null);
        site.setSelectionProvider(selectionProvider);
    }

    public void setProcess(WorkflowProcess pv) {
        this.processVersion = pv;
        if (pv.hasInstanceInfo()) {
            ProcessInstanceHierarchyContentProvider contentProvider = new ProcessInstanceHierarchyContentProvider();
            treeViewer.setContentProvider(contentProvider);
            treeViewer.setInput(pv);
            treeViewer.expandToLevel(pv, TreeViewer.ALL_LEVELS);
            treeViewer.setSelection(contentProvider.getInitialSelection());
        }
        else {
            ProcessHierarchyContentProvider contentProvider = new ProcessHierarchyContentProvider();
            treeViewer.setContentProvider(contentProvider);
            treeViewer.setInput(pv);
            treeViewer.expandToLevel(pv, TreeViewer.ALL_LEVELS);
            treeViewer.setSelection(contentProvider.getInitialSelection());
        }
    }

    public void setProcessInstance(ProcessInstanceVO procInst) {
        // do nothing
    }

    public String getContributorId() {
        return "mdw.tabbedprops.contributor"; // see plugin.xml
    }

    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    private IStructuredSelection getSelection() {
        return (IStructuredSelection) treeViewer.getSelection();
    }

    protected void handleSelectionChanged(IStructuredSelection selection) {
        List<?> list = selection.toList();
        if (list.isEmpty())
            return;

        ActionContext actionContext = new ActionContext(selection);
        actionGroup.setContext(actionContext);

        // TODO property tabs
    }

    public void menuAboutToShow(IMenuManager menuManager) {
        actionGroup.fillContextMenu(menuManager);
    }

}
