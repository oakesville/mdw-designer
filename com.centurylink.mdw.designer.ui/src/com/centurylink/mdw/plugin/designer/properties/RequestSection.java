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
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.cli.Codegen;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.CodegenOptionsDialog;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerView;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

import io.swagger.codegen.DefaultCodegen;

public class RequestSection extends PropertySection implements IFilter {
    private String[] requiredTypes = new String[] { "true", "false" };
    private static final String REQUEST_PATH = "requestPath";

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public WorkflowProject getProject() {
        return process.getProject();
    }

    private TableEditor tableEditor;
    private List<ColumnSpec> columnSpecs;

    private PropertyEditor methodEditor;
    private PropertyEditor pathEditor;
    private PropertyEditor requestSummaryEditor;
    private ParameterContentProvider contentProvider;
    private ParameterLabelProvider labelProvider;
    private ParameterCellModifier cellModifier;
    private ParameterModelUpdater modelUpdater;
    private PropertyEditor codeGenEditor;

    @Override
    public void setSelection(WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        methodEditor.setElement(process);
        String method = process.getAttribute("requestMethod");
        if (method != null)
            for (String opt : getMethodOptions()) {
                if (opt.startsWith(method))
                    methodEditor.setValue(opt);
            }
        methodEditor
                .setEditable(!process.isReadOnly() && process.getAttribute(REQUEST_PATH) != null);

        pathEditor.setElement(process);
        pathEditor.setValue(process.getAttribute(REQUEST_PATH));
        pathEditor.setEditable(!process.isReadOnly());

        requestSummaryEditor.setElement(process);
        requestSummaryEditor.setValue(process.getAttribute("requestSummary"));
        requestSummaryEditor
                .setEditable(!process.isReadOnly() && process.getAttribute(REQUEST_PATH) != null);

        tableEditor.setElement(process);
        tableEditor.setValue(getRequestParameters());
        tableEditor.setEditable(!process.isReadOnly());

        codeGenEditor
                .setEnabled(!process.isReadOnly() && process.getAttribute(REQUEST_PATH) != null);
    }

    @Override
    public void drawWidgets(Composite composite, WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        methodEditor = new PropertyEditor(process, PropertyEditor.TYPE_COMBO);
        methodEditor.setLabel("Method");
        methodEditor.setValueOptions(getMethodOptions());
        methodEditor.setWidth(100);
        methodEditor.setReadOnly(true);
        methodEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                if (validateRequestPath())
                    process.setAttribute("requestMethod", (String) newValue);
            }
        });
        methodEditor.render(composite);

        pathEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
        pathEditor.setLabel("Path");
        pathEditor.setWidth(300);
        pathEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                process.setAttribute(REQUEST_PATH, newValue.toString());
                enableControls();
            }
        });
        pathEditor.render(composite);

        requestSummaryEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
        requestSummaryEditor.setLabel("Swagger Summary");
        requestSummaryEditor.setWidth(300);
        requestSummaryEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                if (validateRequestPath())
                    process.setAttribute("requestSummary", (String) newValue);
            }
        });
        requestSummaryEditor.render(composite);

        tableEditor = new TableEditor(process, TableEditor.TYPE_TABLE);
        if (columnSpecs == null)
            columnSpecs = createColumnSpecs();
        tableEditor.setColumnSpecs(columnSpecs);

        if (contentProvider == null)
            contentProvider = new ParameterContentProvider();
        tableEditor.setContentProvider(contentProvider);

        if (labelProvider == null)
            labelProvider = new ParameterLabelProvider();
        tableEditor.setLabelProvider(labelProvider);
        tableEditor.setLabel("Parameters");

        if (cellModifier == null)
            cellModifier = new ParameterCellModifier();
        tableEditor.setCellModifier(cellModifier);

        if (modelUpdater == null)
            modelUpdater = new ParameterModelUpdater();
        tableEditor.setModelUpdater(modelUpdater);

        tableEditor.render(composite);

        codeGenEditor = new PropertyEditor(process, PropertyEditor.TYPE_BUTTON);
        codeGenEditor.setLabel("Codegen");
        codeGenEditor.setWidth(65);
        codeGenEditor.setComment(" ");
        codeGenEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                genearateApiCode();
            }
        });
        codeGenEditor.render(composite);
    }

    private List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> colSpecs = new ArrayList<>();

        ColumnSpec nameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Parameter Name", "name");
        nameColSpec.width = 100;
        colSpecs.add(nameColSpec);

        ColumnSpec typeColSpec = new ColumnSpec(PropertyEditor.TYPE_COMBO, "Type", "type");
        typeColSpec.width = 70;
        typeColSpec.readOnly = true;
        typeColSpec.options = getParameterTypeNames();
        colSpecs.add(typeColSpec);

        ColumnSpec reqColSpec = new ColumnSpec(PropertyEditor.TYPE_CHECKBOX, "Required",
                "selected");
        reqColSpec.width = 60;
        colSpecs.add(reqColSpec);

        ColumnSpec descColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Description", "desc");
        descColSpec.width = 220;
        colSpecs.add(descColSpec);

        ColumnSpec dataTypeColSpec = new ColumnSpec(PropertyEditor.TYPE_COMBO, "Data Type",
                "dataType");
        dataTypeColSpec.width = 210;
        dataTypeColSpec.readOnly = true;
        dataTypeColSpec.options = getDataTypeNames();
        colSpecs.add(dataTypeColSpec);

        return colSpecs;
    }

    private List<String> getMethodOptions() {
        List<String> methodOptions = new ArrayList<>();
        methodOptions.add("GET");
        methodOptions.add("POST");
        methodOptions.add("PUT");
        methodOptions.add("DELETE");
        methodOptions.add("PATCH");
        return methodOptions;
    }

    private String[] getParameterTypeNames() {
        List<String> names = new ArrayList<>();
        names.add("Path");
        names.add("Body");
        names.add("Query");
        names.add("Header");
        names.add("Form");

        return names.toArray(new String[0]);
    }

    private String[] getDataTypeNames() {
        List<String> assetTypes = new ArrayList<>();
        assetTypes.add(RuleSetVO.JAVA);
        assetTypes.add(RuleSetVO.KOTLIN);
        assetTypes.add(RuleSetVO.KOTLIN_SCRIPT);
        List<WorkflowAsset> assets = getProject().getAssetList(assetTypes);
        List<String> dataTypes = new ArrayList<>();
        for (WorkflowAsset asset : assets)
            dataTypes.add(asset.getPackage().getName().trim() + "/" + asset.getLabel().trim());

        return dataTypes.toArray(new String[0]);
    }

    public List<ParameterVO> getRequestParameters() {
        List<AttributeVO> attrs = process.getAttributes();
        if (attrs != null)
            for (int i = 0; i < attrs.size(); i++) {
                AttributeVO att = attrs.get(i);
                String name = att.getAttributeName();
                String value = att.getAttributeValue();
                if (name.equals("requestParameters"))
                    return parseAttrValues(value);
            }

        return Collections.emptyList();
    }

    public List<ParameterVO> parseAttrValues(String value) {
        String[] attrValues = value.split("\\],\\[");
        if (attrValues.length == 0)
            return Collections.emptyList();
        List<ParameterVO> params = new ArrayList<>();
        for (int i = 0; i < attrValues.length; i++) {
            attrValues[i] = attrValues[i].replaceAll("\"", "").replace("[", "").replace("]", "");
            String[] reqParams = attrValues[i].split(",");
            ParameterVO parameterVo = new ParameterVO();
            parameterVo.setParameterName(reqParams[0]);
            parameterVo.setParameterType(reqParams[1]);
            parameterVo.setRequired(Boolean.valueOf(reqParams[2]));
            parameterVo.setDescription(reqParams[3]);
            parameterVo.setDataType(reqParams[4].trim() + " v" + reqParams[5].trim());
            params.add(parameterVo);
        }
        return params;
    }

    class ParameterContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<ParameterVO> rows = (List<ParameterVO>) inputElement;
            Collections.sort(rows, new Comparator<ParameterVO>() {
                public int compare(ParameterVO v1, ParameterVO v2) {
                    return v1.getParameterName().compareToIgnoreCase(v2.getParameterName());
                }
            });
            return rows.toArray(new ParameterVO[0]);
        }

        @Override
        public void dispose() {
            // do nothing
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // do nothing
        }
    }

    class ParameterLabelProvider extends LabelProvider implements ITableLabelProvider {
        private Map<ImageDescriptor, Image> imageCache = new HashMap<>();

        public Image getColumnImage(Object element, int columnIndex) {
            ParameterVO row = (ParameterVO) element;
            ColumnSpec colspec = columnSpecs.get(columnIndex);
            if (colspec.type.equals(PropertyEditor.TYPE_CHECKBOX)) {
                ImageDescriptor descriptor = null;
                Boolean value = row.getRequired();
                if (value.booleanValue()) {
                    descriptor = MdwPlugin.getImageDescriptor("icons/checked.gif");
                }
                else {
                    descriptor = MdwPlugin.getImageDescriptor("icons/unchecked.gif");
                }
                Image image = imageCache.get(descriptor);
                if (image == null) {
                    image = descriptor.createImage();
                    imageCache.put(descriptor, image);
                }
                return image;
            }
            else {
                return null;
            }
        }

        public String getColumnText(Object element, int columnIndex) {
            ParameterVO parameterVO = (ParameterVO) element;

            switch (columnIndex) {
            case 0:
                return parameterVO.getParameterName();
            case 1:
                return parameterVO.getParameterType();
            case 2:
                return null;
            case 3:
                return parameterVO.getDescription();
            case 4:
                return parameterVO.getDataType();
            default:
                return null;
            }
        }
    }

    class ParameterCellModifier extends TableEditor.DefaultCellModifier {
        ParameterCellModifier() {
            tableEditor.super();
        }

        @Override
        public Object getValue(Object element, String property) {
            ParameterVO parameterVO = (ParameterVO) element;
            int colIndex = getColumnIndex(property);
            switch (colIndex) {
            case 0:
                return parameterVO.getParameterName();
            case 1:
                String varType = parameterVO.getParameterType();
                String[] types = getParameterTypeNames();
                return getIndex(varType, types);
            case 2:
                return parameterVO.getRequired();
            case 3:
                return parameterVO.getDescription();
            case 4:
                String dataType = parameterVO.getDataType();
                String[] dataTypes = getDataTypeNames();
                return getIndex(dataType, dataTypes);
            default:
                return null;
            }
        }

        @Override
        public void modify(Object element, String property, Object value) {
            TableItem item = (TableItem) element;
            if (item != null) {
                ParameterVO parameterVO = (ParameterVO) item.getData();
                int colIndex = getColumnIndex(property);
                switch (colIndex) {
                case 0:
                    parameterVO.setParameterName(((String) value).trim());
                    break;
                case 1:
                    Integer iVal = (Integer) value;
                    parameterVO
                            .setParameterType(iVal == null ? null : getParameterTypeNames()[iVal]);
                    break;
                case 2:
                    parameterVO.setRequired(Boolean.valueOf(value.toString()));
                    break;
                case 3:
                    parameterVO.setDescription(((String) value).trim());
                    break;
                case 4:
                    Integer i = (Integer) value;
                    parameterVO.setDataType(i == null ? null : getDataTypeNames()[i]);
                    break;
                default:
                }
                tableEditor.getTableViewer().update(parameterVO, null);
                modelUpdater.updateModelValue(tableEditor.getTableValue());
                tableEditor.fireValueChanged(tableEditor.getTableValue());
            }
        }

        @Override
        public boolean canModify(Object element, String property) {
            boolean editable = super.canModify(element, property);
            if (editable) {
                return validateRequestPath();
            }
            return editable;
        }
    }

    public int getIndex(String type, String[] types) {
        int index = 0;
        for (int i = 0; i < types.length; i++) {
            if (type.equals(types[i])) {
                index = i;
                break;
            }
        }
        return index;
    }

    class ParameterModelUpdater implements TableEditor.TableModelUpdater {
        public Object create() {
            ParameterVO parameterVO = new ParameterVO();
            parameterVO.setParameterName("NewParameter");
            parameterVO.setParameterType(getParameterTypeNames()[0]);
            parameterVO.setRequired(Boolean.valueOf(requiredTypes[0]));
            parameterVO.setDescription("Description");
            parameterVO.setDataType(getDataTypeNames()[0]);
            process.fireDirtyStateChanged(true);
            return parameterVO;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void updateModelValue(List tableValue) {
            List<ParameterVO> parameterVOs = (List<ParameterVO>) tableValue;
            updateAttributes(parameterVOs);
        }
    }

    public void updateAttributes(List<ParameterVO> parameterVOs) {
        StringBuilder attrValue = new StringBuilder("[");
        for (ParameterVO vo : parameterVOs) {
            if (attrValue.toString().endsWith("]"))
                attrValue.append(",");
            String[] types = vo.getDataType().split(" v");
            attrValue.append("[").append("\"").append(vo.getParameterName()).append("\"")
                    .append(",").append("\"").append(vo.getParameterType()).append("\"").append(",")
                    .append("\"").append(vo.getRequired()).append("\"").append(",").append("\"")
                    .append(vo.getDescription()).append("\"").append(",").append("\"")
                    .append(types[0]).append("\"").append(",").append("\"")
                    .append(AssetVersionSpec.getDefaultSmartVersionSpec(types[1])).append("\"")
                    .append("]");
        }
        attrValue.append("]");
        process.setAttribute("requestParameters", attrValue.toString());
    }

    class ParameterVO {
        private String parameterName;
        private String parameterType;
        private Boolean required;
        private String description;
        private String dataType;

        /**
         * @return the parameterName
         */
        public String getParameterName() {
            return parameterName;
        }

        /**
         * @param parameterName
         *            the parameterName to set
         */
        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        /**
         * @return the parameterType
         */
        public String getParameterType() {
            return parameterType;
        }

        /**
         * @param parameterType
         *            the parameterType to set
         */
        public void setParameterType(String parameterType) {
            this.parameterType = parameterType;
        }

        /**
         * @return the required
         */
        public Boolean getRequired() {
            return required;
        }

        /**
         * @param required
         *            the required to set
         */
        public void setRequired(Boolean required) {
            this.required = required;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description
         *            the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @return the dataType
         */
        public String getDataType() {
            return dataType;
        }

        /**
         * @param dataType
         *            the dataType to set
         */
        public void setDataType(String dataType) {
            this.dataType = dataType;
        }
    }

    private void enableControls() {
        if (process.getAttribute(REQUEST_PATH) == null
                || process.getAttribute(REQUEST_PATH).isEmpty()) {
            methodEditor.setEditable(false);
            requestSummaryEditor.setEditable(false);
            codeGenEditor.setEditable(false);
        }
        else {
            methodEditor.setEditable(true);
            requestSummaryEditor.setEditable(true);
            codeGenEditor.setEditable(true);
        }
    }

    public boolean validateRequestPath() {
        boolean pathEmpty = process.getAttribute(REQUEST_PATH) == null
                || process.getAttribute(REQUEST_PATH).isEmpty();
        if (pathEmpty)
            MessageDialog.openWarning(getShell(), "Request Details", "Request path is required.");
        return !pathEmpty;
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowProcess))
            return false;

        process = (WorkflowProcess) toTest;

        if (!process.getProject().checkRequiredVersion(6, 1))
            return false;

        return !process.hasInstanceInfo();
    }

    protected void genearateApiCode() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                ProcessEditor processEditor = (ProcessEditor) MdwPlugin.getActivePage()
                        .findEditor(process);
                if (processEditor != null && processEditor.isDirty()) {
                    String msg = process.getName()
                            + " has unsaved changes. Please save changes and try.";
                    MessageDialog.openInformation(getShell(), "Request", msg);
                }
                else {
                    CodegenOptionsDialog pkgInputDialog = new CodegenOptionsDialog(getShell(),
                            process);
                    int result = pkgInputDialog.open();
                    if (result == Dialog.OK) {
                        generate(pkgInputDialog.getPackageName(), pkgInputDialog.getConfigDir(),
                                pkgInputDialog.getSwaggerUrl(), pkgInputDialog.getGenerateOptions());
                    }
                }
            }
        });
    }

    private void generate(String pkgName, String configDir, String url, List<String> systemProps) {
        try {
            Codegen codeGen = new Codegen();
            codeGen.setBasePackage('/' + process.getPackage().getName());
            codeGen.setApiPackage(pkgName);
            codeGen.setCodeType("swagger");
            codeGen.setConfigLoc(configDir);
            codeGen.setMdwVersion(getProject().getMdwVersion());
            codeGen.setAssetLoc(getProject().getAssetDir().toString());
            if (getProject().getMdwVersion().indexOf("SNAPSHOT") > -1)
                codeGen.setSnapshots(true);
            codeGen.setTemplateDir(getProject().getTempDir() + "\\templates");
            codeGen.setInputSpec(url);
            codeGen.setVmArgs(systemProps);
            codeGen.run();
            ProcessExplorerView processExplorer = (ProcessExplorerView) MdwPlugin.getActivePage()
                    .findView(ProcessExplorerView.VIEW_ID);
            if (processExplorer != null) {
                processExplorer.handleRefresh();
                processExplorer.expand(getProject());
            }
            WorkflowAsset asset = WorkflowProjectManager.getInstance().getWorkflowProject(getProject().getName())
                    .getAsset(pkgName,
                            DefaultCodegen.camelize(
                                    url.substring(url.lastIndexOf('/') + 1, url.indexOf('.')))
                                    + ".java");
            if (asset != null)
                asset.openFile(new NullProgressMonitor());
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Generate Api Code", getProject());
        }

    }
}