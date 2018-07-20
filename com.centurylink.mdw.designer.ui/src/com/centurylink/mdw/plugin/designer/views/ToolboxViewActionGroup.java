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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ToolboxViewActionGroup extends ActionGroup {
    public static final String TOOLBOX_SUPPRESSED_IMPLS = "MdwToolboxSuppressedActivityImplementors";

    private ToolboxView view;

    private IAction sortAction;
    private IAction refreshAction;
    private IAction saveAction;

    public IAction getSaveAction() {
        return saveAction;
    }

    private IAction deleteAction;

    public IAction getDeleteAction() {
        return deleteAction;
    }

    private IAction newAction;

    public IAction getNewAction() {
        return newAction;
    }

    private IAction filterAction;

    public IAction getFilterAction() {
        return filterAction;
    }

    private IAction discoverAction;

    public IAction getDiscoverAction() {
        return discoverAction;
    }

    public ToolboxViewActionGroup(ToolboxView view) {
        this.view = view;

        sortAction = createSortAction();
        refreshAction = createRefreshAction();
        saveAction = createSaveAction();
        deleteAction = createDeleteAction();
        newAction = createNewAction();
        filterAction = createFilterAction();
        discoverAction = createDiscoverAction();
        enableToolbarActions(false);
    }

    public void enableToolbarActions(boolean enabled) {
        sortAction.setEnabled(enabled);
        refreshAction.setEnabled(enabled);
        ActivityImpl impl = view.getSelection();
        saveAction.setEnabled(enabled && view.isDirty() && impl != null
                && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
        deleteAction.setEnabled(enabled && view.isSelection() && impl != null
                && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
        newAction.setEnabled(
                enabled && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
        filterAction.setEnabled(enabled);
        discoverAction.setEnabled(
                enabled && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
    }

    private IAction createSortAction() {
        IAction lSortAction = new Action() {
            @Override
            public void run() {
                view.handleSort(isChecked());
            }
        };
        lSortAction.setText("Sort");
        ImageDescriptor sortImageDesc = MdwPlugin.getImageDescriptor("icons/sort.gif");
        lSortAction.setImageDescriptor(sortImageDesc);
        lSortAction.setChecked(MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.PREFS_SORT_TOOLBOX_A_TO_Z));
        return lSortAction;
    }

    private IAction createRefreshAction() {
        IAction lRefreshAction = new Action() {
            @Override
            public void run() {
                view.handleRefresh();
            }
        };
        lRefreshAction.setText("Refresh");
        ImageDescriptor refreshImageDesc = MdwPlugin.getImageDescriptor("icons/refresh.gif");
        lRefreshAction.setImageDescriptor(refreshImageDesc);
        return lRefreshAction;
    }

    private IAction createSaveAction() {
        IAction lSaveAction = new Action() {
            @Override
            public void run() {
                view.handleSave();
            }
        };
        lSaveAction.setText("Save");
        ImageDescriptor saveImageDesc = MdwPlugin.getImageDescriptor("icons/save.gif");
        lSaveAction.setImageDescriptor(saveImageDesc);
        ImageDescriptor saveDisabledImageDesc = MdwPlugin
                .getImageDescriptor("icons/save_disabled.gif");
        lSaveAction.setDisabledImageDescriptor(saveDisabledImageDesc);
        return lSaveAction;
    }

    private IAction createDeleteAction() {
        IAction lDeleteAction = new Action() {
            @Override
            public void run() {
                view.handleDelete();
            }
        };
        lDeleteAction.setText("Delete");
        ImageDescriptor deleteImageDesc = MdwPlugin.getImageDescriptor("icons/delete.gif");
        lDeleteAction.setImageDescriptor(deleteImageDesc);
        return lDeleteAction;
    }

    private IAction createNewAction() {
        IAction lNewAction = new Action() {
            @Override
            public void run() {
                view.handleNew();
            }
        };
        lNewAction.setText("New Implementor");
        ImageDescriptor newImageDesc = MdwPlugin.getImageDescriptor("icons/genact_wiz.gif");
        lNewAction.setImageDescriptor(newImageDesc);
        return lNewAction;
    }

    private IAction createFilterAction() {
        IAction lFilterAction = new Action() {
            @Override
            public void run() {
                view.handleFilter();
            }
        };

        lFilterAction.setText("Filter");
        ImageDescriptor filterImageDesc = MdwPlugin.getImageDescriptor("icons/filter.gif");
        lFilterAction.setImageDescriptor(filterImageDesc);
        return lFilterAction;
    }

    private IAction createDiscoverAction() {
        IAction lDiscoverAction = new Action() {
            @Override
            public void run() {
                view.handleDiscover();
            }
        };

        lDiscoverAction.setText("Discover Workflow Assets");
        ImageDescriptor discoverImageDesc = MdwPlugin.getImageDescriptor("icons/discover.gif");
        lDiscoverAction.setImageDescriptor(discoverImageDesc);
        return lDiscoverAction;
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        IToolBarManager toolbar = actionBars.getToolBarManager();
        toolbar.add(new GroupMarker("mdw.toolbox.group"));
        toolbar.add(sortAction);
        toolbar.add(filterAction);
        toolbar.add(refreshAction);
        toolbar.add(saveAction);
        toolbar.add(discoverAction);
        toolbar.add(newAction);
        toolbar.add(deleteAction);
    }
}