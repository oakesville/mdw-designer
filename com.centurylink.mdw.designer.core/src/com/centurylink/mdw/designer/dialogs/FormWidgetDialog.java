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
package com.centurylink.mdw.designer.dialogs;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.designer.utils.JTablePlus;
import com.centurylink.mdw.designer.utils.SwingFormGenerator;
import com.centurylink.mdw.designer.utils.SwingFormGenerator.MenuButton;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengNode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.MaskFormatter;

public class FormWidgetDialog extends JDialog
        implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String ACTION_PROMPT_CHANGE = "prompt";

    private Map<String,String> validatorMasks;

    private JTextField widgetId;
    private JCheckBox widgetIdAuto = null;
    private JTextField widgetData = null;
    private JTextField widgetLabel;
    private JTextField widgetTip = null;;
    private JTextField widgetRequired = null;
    private JCheckBox widgetCanTypeIn = null;
    private JCheckBox widgetIsStatic = null;
    private JTextField widgetImage = null;
    private JTextField widgetDatePattern = null;
    private JTextField displayCondition = null;
    private JTextField editableCondition = null;
    private List<JTextField> validatorEditors = null;
    private List<JCheckBox> validatorSelectors = null;
    private JTextField autoValue = null;
    private JComboBox commandAction = null;
    private JTextField paginator = null;
    private JTextField choiceListSource = null;
    private JPanel validatorTable = null;
    private JComboBox validatorTypes;
    private JTextField validatorMessage = null;
    private JCheckBox radioVertical = null;
    private JCheckBox widgetIsSortable = null;
    private JCheckBox validateData = null;
    private JCheckBox showBusy = null;
    private JComboBox prompt = null;
    private JTextField promptMessage = null;
    private JTextField promptOptions = null;
    private JComboBox tableStyles;
    private JComboBox tabbingStyles;
    private JComboBox columnEditable = null;
    private JTextField columnStyle = null;

    private SwingFormGenerator generator;
    private String labelOld;

    private Font inputFont;
    private boolean readonly;

    private boolean useMask = false;

    private MbengNode node;

    private int yGap = 5;
    private int labelX = 20;
    private int yOff;

    public FormWidgetDialog(JFrame frame, MbengNode node, SwingFormGenerator generator, boolean readonly) {
        super(frame);
        setModal(true);
        setSize(700,400);
        setLocationRelativeTo(frame);
        this.generator = generator;
        this.node = node;
        this.readonly = readonly;
        init();
    }

    public FormWidgetDialog(JDialog owner, MbengNode node, SwingFormGenerator generator, boolean readonly) {
        super(owner);
        setModal(true);
        setSize(700,400);
        setLocationRelativeTo(owner);
        this.generator = generator;
        this.node = node;
        this.readonly = readonly;
        init();
    }

    private void init() {
        labelOld = node.getAttribute(FormConstants.FORMATTR_LABEL);
        setTitle(node.getName() + " " + node.getAttribute(FormConstants.FORMATTR_ID));

		JPanel panel = new JPanel(null);
        panel.setBounds(200,200,100,100);
        inputFont = new Font("Monospaced", Font.PLAIN, 12);
        validatorMasks = new HashMap<>();
        validatorMasks.put("mask", "mask('_99LLAAXX_')");
        validatorMasks.put("length", "length(_min_,_max_)");
        validatorMasks.put("range", "range(_low_,_high_)");
        validatorMasks.put("int", "int()");
        String v;
        yOff = 20;

        // widget label and ID
        JLabel label;

        widgetLabel = initTextField(panel, "Label",
        		FormConstants.FORMATTR_LABEL, labelX, 120, 200, false);
        widgetId = initTextField(panel, "ID",
        		FormConstants.FORMATTR_ID, 380, 30, 100, false);
        v = node.getAttribute(FormConstants.FORMATTR_ID);
        boolean autoid = v==null||v.length()==0||v.startsWith("_id_");
        if (autoid)
            widgetId.setEditable(false);

        widgetIdAuto = new JCheckBox();
        widgetIdAuto.setBounds(520, yOff, 20, 20);
        widgetIdAuto.setSelected(autoid);
        widgetIdAuto.addActionListener(this);
        widgetIdAuto.setEnabled(!readonly);
        widgetIdAuto.setActionCommand(Constants.ACTION_RESET);
        panel.add(widgetIdAuto);
        label = new JLabel("Auto generated");
        label.setBounds(545, yOff, 100, 20);
        panel.add(label);

        yOff += 20 + yGap;

        // widget data association
        if (node.getName().equals(FormConstants.WIDGET_TEXT)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_LIST)
                || node.getName().equals(FormConstants.WIDGET_LISTPICKER)
                || node.getName().equals(FormConstants.WIDGET_TABLE)
                || node.getName().equals(FormConstants.WIDGET_COLUMN)
                || node.getName().equals(FormConstants.WIDGET_TABBEDPANE)) {
        	widgetData = initTextField(panel, "Data",
            		FormConstants.FORMATTR_DATA, 120, 320);
        }

        // action
        if (node.getName().equals(FormConstants.WIDGET_BUTTON)
                || node.getName().equals(FormConstants.WIDGET_COLUMN)
                || node.getName().equals(FormConstants.WIDGET_MENUITEM)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_HYPERLINK)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {
            String[] commands = {
            		"",
            		"_action_?arg1=value1&...&argn=valuen",
            		"dialog_open(_dialogFormName_)",
            		"dialog_ok(_dialogFormName_)",
            		"dialog_cancel(_dialogFormName_)",
            		"validate()",
            		"repaint(_idToRefresh_)",
            		"show_page(_formName_, _inNewWindow_)",
            		"start_process(_processName_)",
            		"task_action(_actionName_)",
            		"perform_action(_action_, _showBusy_)",
            		"ajax_action(_action_,_idToUpdate_)",
            		"ajax_action_async(_action_,_idToUpdate_,_timeoutSec_,_checkInterval_,_callback_)",
            		"hyperlink:task?name=#{this.TaskInstanceId}",
    	        	"hyperlink:form?formName=...&row=#{this.TaskInstanceId}"
            };
            String labelStr;
            if (node.getName().equals(FormConstants.WIDGET_COLUMN))
                labelStr = "Hyperlink Action";
        	else if (node.getName().equals(FormConstants.WIDGET_HYPERLINK))
        	    labelStr = "URL";
        	else labelStr = "Action";
            commandAction = initComboBox(panel, labelStr,
            		commands, FormConstants.FORMATTR_ACTION, true, 120, 520);

            // discard data changes during action
            if (node.getName().equals(FormConstants.WIDGET_BUTTON)
                    || node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
                validateData = initCheckBox(panel, "Validate",
            			FormConstants.FORMATTR_VALIDATE, 160);
            }
            // show busy dialog, confirm/input dialog during action
            if (node.getName().equals(FormConstants.WIDGET_BUTTON)
                    || node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
            	showBusy = initCheckBox(panel, "Show busy popup",
            			FormConstants.FORMATTR_SHOW_BUSY, 160);
            	label = new JLabel("Prompt");
                label.setBounds(labelX, yOff, 160, 20);
                panel.add(label);
                String[] choices = {
                		FormConstants.FORMATTRVALUE_PROMPT_NONE,
                		FormConstants.FORMATTRVALUE_PROMPT_CONFIRM,
                		FormConstants.FORMATTRVALUE_PROMPT_INPUT,
                		FormConstants.FORMATTRVALUE_PROMPT_SELECT
                };
                prompt = new JComboBox(choices);
                prompt.setBounds(140, yOff, 80, 20);
                v = node.getAttribute(FormConstants.FORMATTR_PROMPT);
                String[] promptParsed = v==null?new String[0]:v.split("\\|");
                String pType = promptParsed.length<1?FormConstants.FORMATTRVALUE_PROMPT_NONE:promptParsed[0];
                prompt.setSelectedItem(pType);
                prompt.setEnabled(!readonly);
                prompt.addActionListener(this);
                prompt.setActionCommand(ACTION_PROMPT_CHANGE);
                panel.add(prompt);
                label = new JLabel("Message");
                label.setBounds(230, yOff, 60, 20);
                panel.add(label);
                promptMessage = new JTextField();
                promptMessage.setBounds(290, yOff, 160, 20);
                String pMsg = promptParsed.length<2?null:promptParsed[1];
                if (pMsg != null)
                    promptMessage.setText(pMsg);
                if (readonly || pType.equals(FormConstants.FORMATTRVALUE_PROMPT_NONE))
                	promptMessage.setEditable(false);
                panel.add(promptMessage);

                label = new JLabel("Choices");
                label.setBounds(460, yOff, 60, 20);
                panel.add(label);
                promptOptions = new JTextField();
                promptOptions.setBounds(520, yOff, 160, 20);
                v = promptParsed.length<3?null:promptParsed[2];
                if (v != null)
                    promptOptions.setText(v);
                if (readonly || !pType.equals(FormConstants.FORMATTRVALUE_PROMPT_SELECT))
                	promptOptions.setEditable(false);
                panel.add(promptOptions);
                yOff += 20 + yGap;
            }
        }

        // required condition
        if (node.getName().equals(FormConstants.WIDGET_TEXT)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_LIST)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {

            label = new JLabel("Required");
            label.setBounds(labelX, yOff, 120, 20);
            panel.add(label);
            v = node.getAttribute(FormConstants.FORMATTR_REQUIRED);
            int k;
            if (v == null || v.equalsIgnoreCase("false"))
                k = 0;
            else if (v.equalsIgnoreCase("true"))
                k = 1;
            else
                k = 2;
            String[] options = { "False", "True", "Conditional" };
            JComboBox requiredCombo = new JComboBox(options);
            requiredCombo.setBounds(140, yOff, 75, 22);
            requiredCombo.setSelectedIndex(k);
            if (readonly)
                requiredCombo.setEnabled(false);
            else {
            	requiredCombo.addActionListener(this);
            	requiredCombo.setActionCommand(Constants.ACTION_COMBO);
            }
            panel.add(requiredCombo);
            widgetRequired = new JTextField();
            widgetRequired.setBounds(220, yOff, 420, 20);
            if (v != null)
                widgetRequired.setText(v);
            panel.add(widgetRequired);
            widgetRequired.setEditable(!readonly && k==2);
            yOff += 20 + yGap;

            // auto value
            autoValue = initTextField(panel, "Default Value",
        			FormConstants.FORMATTR_AUTOVALUE, 120, 520);
        }

        // display condition
        displayCondition = initTextField(panel, "Visible when",
    			FormConstants.FORMATTR_VISIBLE, 120, 520);

        // modifiable condition
        if (node.getName().equals(FormConstants.WIDGET_TEXT)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_TABBEDPANE)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)
                || node.getName().equals(FormConstants.WIDGET_BUTTON)) {
            editableCondition = initTextField(panel,
            		node.getName().equals(FormConstants.WIDGET_BUTTON)?"Enabled when":"Modifiable when",
        			FormConstants.FORMATTR_EDITABLE, 120, 520);
        }

        // widget specific stuff
        if (node.getName().equals(FormConstants.WIDGET_TEXT)) {
            label = new JLabel("Validators");
            label.setBounds(labelX, yOff, 120, 20);
            panel.add(label);
            validatorTable = new JPanel(null);
            createValidatorTable(stringToList(node.getAttribute(FormConstants.FORMATTR_VALIDATORS),';'));
            JScrollPane scrollPane = new JScrollPane(validatorTable);
            scrollPane.setBounds(140, yOff, 350, 90);
            panel.add(scrollPane);
            JButton buttonDelete = new JButton("Delete Checked");
            buttonDelete.setBounds(510, yOff+5, 130, 25);
            buttonDelete.setActionCommand(Constants.ACTION_DELETE);
            buttonDelete.setEnabled(!readonly);
            buttonDelete.addActionListener(this);
            panel.add(buttonDelete);
            JButton buttonAdd = new JButton("Add Following");
            buttonAdd.setBounds(510, yOff+35, 130, 25);
            buttonAdd.setActionCommand(Constants.ACTION_NEW);
            buttonAdd.addActionListener(this);
            buttonAdd.setEnabled(!readonly);
            panel.add(buttonAdd);
            validatorTypes = new JComboBox();
            Set<String> validatorNames = validatorMasks.keySet();
            for (String one : validatorNames) {
                validatorTypes.addItem(one);
            }
            validatorTypes.setBounds(510, yOff+65, 120, 25);
            panel.add(validatorTypes);
            yOff += 90 + yGap;

            validatorMessage = initTextField(panel, "Error Message",
        			FormConstants.FORMATTR_INVALID_MSG, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {
        	widgetCanTypeIn = initCheckBox(panel, "Can type in value",
        			FormConstants.FORMATTR_CAN_TYPE_IN, 120);
        	choiceListSource = initTextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_DATE)) {
            widgetCanTypeIn = initCheckBox(panel, "Can type in value",
        			FormConstants.FORMATTR_CAN_TYPE_IN, 120);
            widgetDatePattern = initTextField(panel, "Date Pattern",
        			FormConstants.FORMATTR_DATE_PATTERN, 120, 160);
        } else if (node.getName().equals(FormConstants.WIDGET_TEXTAREA)) {
            widgetIsStatic = initCheckBox(panel, "Is Static",
        			FormConstants.FORMATTR_IS_STATIC, 120);
        } else if (node.getName().equals(FormConstants.WIDGET_BUTTON)) {
                widgetImage = initTextField(panel, "Image",
            			FormConstants.FORMATTR_IMAGE, 120, 160);
        } else if (node.getName().equals(FormConstants.WIDGET_HYPERLINK)) {
            widgetImage = initTextField(panel, "Image",
        			FormConstants.FORMATTR_IMAGE, 120, 160);
        } else if (node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)) {
        	choiceListSource = initTextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        	radioVertical = initCheckBox(panel, "Vertical",
        			FormConstants.FORMATTR_DIRECTION, 120);
        } else if (node.getName().equals(FormConstants.WIDGET_LIST)) {
        	choiceListSource = initTextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_LISTPICKER)) {
            choiceListSource = initTextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_MENU)) {
            MenuEditor menueditor = new MenuEditor(node, (JComponent)generator.getWidget(node));
            menueditor.setBounds(labelX, yOff, 600, 240);
            panel.add(menueditor);
            yOff += 240 + yGap;
        } else if (node.getName().equals(FormConstants.WIDGET_TABLE)) {
        	String[] choices = {
        			FormConstants.FORMATTRVALUE_TABLESTYLE_SCROLLED,
        			FormConstants.FORMATTRVALUE_TABLESTYLE_PAGINATED,
        			FormConstants.FORMATTRVALUE_TABLESTYLE_SIMPLE };
        	tableStyles = initComboBox(panel, "Table Style",
        			choices, FormConstants.FORMATTR_TABLE_STYLE, false, 120, 160);
        	paginator = initTextField(panel, "Table Action Class",
        			FormConstants.FORMATTR_ACTION, 120, 360);
        } else if (node.getName().equals(FormConstants.WIDGET_TABBEDPANE)) {
        	String[] choices = {
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_CLIENT,
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_AJAX,
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_SERVER,
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_JQUERY };
        	tabbingStyles = initComboBox(panel, "Tabbing Style",
        			choices, FormConstants.FORMATTR_TABBING_STYLE, false, 120, 160);
        	paginator = initTextField(panel, "Tabbing class (not used for client tabbing style)",
        			FormConstants.FORMATTR_ACTION, 280, 360);
        } else if (node.getName().equals(FormConstants.WIDGET_COLUMN)) {
        	widgetIsSortable = initCheckBox(panel, "Sortable",
        			FormConstants.FORMATTR_SORTABLE, 120);
        	String[] choices = { "true", "false", "when new" };
        	columnEditable = initComboBox(panel, "Editable",
        			choices, FormConstants.FORMATTR_EDITABLE, false, 120, 160);
        	columnStyle = initTextField(panel, "Column Detail Style",
        			FormConstants.FORMATTR_COLUMN_STYLE, 120, 400);
        }

        // tip
        if (node.getName().equals(FormConstants.WIDGET_TEXT)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_BUTTON)
                || node.getName().equals(FormConstants.WIDGET_LIST)) {
        	widgetTip = initTextField(panel, "Tip",
        			FormConstants.FORMATTR_TIP, 120, 320);
        }

        // buttons
        JButton buttonOk = new JButton("OK");
        buttonOk.setBounds(120, 320, 120, 25);
        buttonOk.setActionCommand(Constants.ACTION_SAVE);
        buttonOk.addActionListener(this);
        buttonOk.setEnabled(!readonly);
        panel.add(buttonOk);
        JButton buttonCancel = new JButton("Cancel");
        buttonCancel.setBounds(260, 320, 120, 25);
        buttonCancel.setActionCommand(Constants.ACTION_EXIT);
        buttonCancel.addActionListener(this);
        panel.add(buttonCancel);
        JButton buttonDelete = new JButton("Delete");
        buttonDelete.setBounds(400, 320, 120, 25);
        buttonDelete.setActionCommand(Constants.ACTION_DELETE);
        buttonDelete.addActionListener(this);
        buttonDelete.setEnabled(false);
        panel.add(buttonDelete);

        getContentPane().add(panel);
    }

    private JTextField initTextField(JPanel panel,
    		String label, String attr, int lx, int lw, int vw, boolean newline) {
    	JLabel jlabel = new JLabel(label);
        jlabel.setBounds(lx, yOff, lw, 20);
        panel.add(jlabel);
        JTextField widget = new JTextField();
        widget.setBounds(lx+lw, yOff, vw, 20);
        widget.setText(node.getAttribute(attr));
        widget.setEditable(!readonly);
        panel.add(widget);
        if (newline) yOff += 20 + yGap;
        return widget;
    }

    private JTextField initTextField(JPanel panel,
    		String label, String attr, int lw, int vw) {
    	return initTextField(panel, label, attr, labelX, lw, vw, true);
    }

    private JComboBox initComboBox(JPanel panel,
    		String label, String[] choices, String attr, boolean editable, int lw, int vw) {
    	JLabel jlabel = new JLabel(label);
        jlabel.setBounds(labelX, yOff, lw, 20);
        panel.add(jlabel);
        JComboBox widget = new JComboBox(choices);
        if (editable)
            widget.setEditable(true);
        widget.setBounds(labelX+lw, yOff, vw, 25);
        widget.setEnabled(!readonly);
        String v = node.getAttribute(attr);
        if (v != null)
            widget.setSelectedItem(v);
        panel.add(widget);
        yOff += 25 + yGap;
        return widget;
    }

    private JCheckBox initCheckBox(JPanel panel,
    		String label, String attr, int lw) {
    	JLabel jlabel = new JLabel(label);
        jlabel.setBounds(labelX, yOff, lw, 20);
        panel.add(jlabel);
        JCheckBox widget = new JCheckBox();
        widget.setBounds(labelX+lw, yOff, 20, 20);
        String v = node.getAttribute(attr);
        widget.setSelected(v!=null && "true".equalsIgnoreCase(v));
        widget.setEnabled(!readonly);
        panel.add(widget);
        yOff += 20 + yGap;
        return widget;
    }

    private int createOneValidator(String value, int yOffset) {
    	JTextField ftt;
    	if (useMask) {
	        String mask = value;
	        int m = mask.length();
	        for (int j=0; j<m; j++) {
	            if (!Character.isLetter(mask.charAt(j))) {
	                mask = mask.substring(0,j);
	                break;
	            }
	        }
	        try {
	            MaskFormatter fmter = new MaskFormatter(validatorMasks.get(mask));
	            fmter.setPlaceholderCharacter('_');
	            ftt = new JFormattedTextField(fmter);
	        } catch (Exception e) {
	            ftt = new JFormattedTextField();
	        }
    	} else {
    		ftt = new JTextField();
    	}
        ftt.setText(value);
        ftt.setFont(inputFont);
        ftt.setBounds(2, yOffset, 290, 20);
        JCheckBox sel = new JCheckBox();
        sel.setBounds(300, yOffset, 20, 20);
        validatorTable.add(sel);
        int yOffs = yOffset + 22;
        validatorTable.add(ftt);
        validatorEditors.add(ftt);
        validatorSelectors.add(sel);
        return yOffs;
    }

    private void createValidatorTable(List<String> validators) {
        int yOffset = 2;
        validatorEditors = new ArrayList<>();
        validatorSelectors = new ArrayList<>();
        for (int i=0; i<validators.size(); i++) {
            yOffset = createOneValidator(validators.get(i), yOffset);
        }
        Dimension size = new Dimension(330, yOffset+2);
        validatorTable.setPreferredSize(size);
        validatorTable.setSize(size);
    }

    private void refreshValidatorTable() {
        int yOffset = 2;
        for (int i=0; i<validatorEditors.size(); i++) {
            JTextField ftt = validatorEditors.get(i);
            JCheckBox sel = validatorSelectors.get(i);
            ftt.setBounds(2, yOffset, 290, 20);
            sel.setBounds(300, yOffset, 20, 20);
            yOffset += 22;
        }
        Dimension size = new Dimension(330, yOffset+2);
        validatorTable.setPreferredSize(size);
        validatorTable.setSize(size);
        validatorTable.repaint();
    }

    private void updateWidgetLabel(MbengNode node) {
        String labelNew = node.getAttribute(FormConstants.FORMATTR_LABEL);
        if (labelNew != null && !labelNew.equals(labelOld)) {
            if (node.getName().equals(FormConstants.WIDGET_PANEL)) {
                JPanel panel = (JPanel) generator.getWidget(node);
                ((TitledBorder) panel.getBorder()).setTitle(labelNew);
            }
            else if (node.getName().equals(FormConstants.WIDGET_BUTTON)) {
                JButton button = (JButton) generator.getWidget(node);
                button.setText(labelNew);
            }
            else if (node.getName().equals(FormConstants.WIDGET_MENU)) {
                Component menu = generator.getWidget(node);
                if (menu instanceof JPopupMenu)
                    ((JPopupMenu) menu).setName(labelNew);
                else if (menu instanceof MenuButton)
                    ((MenuButton) menu).setText(labelNew);
                else
                    ((JMenu) menu).setText(labelNew);
            }
            else if (node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
                JMenuItem menuitem = (JMenuItem) generator.getWidget(node);
                menuitem.setText(labelNew);
            }
            else if (node.getName().equals(FormConstants.WIDGET_TAB)) {
                JPanel panel = (JPanel) generator.getWidget(node);

                JTabbedPane tabbedPane = (JTabbedPane) panel.getParent();
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component comp = tabbedPane.getComponent(i);
                    if (comp == panel) {
                        tabbedPane.setTitleAt(i, labelNew);
                        break;
                    }
                }
            }
            else if (node.getName().equals(FormConstants.WIDGET_COLUMN)) {
            	JTablePlus tablePlus = (JTablePlus)generator.getWidget(node.getParent());
                int c = Integer.parseInt(node.getAttribute("INDEX"));
                tablePlus.setColumnLabel(c, labelNew);
            } else {
                JLabel label = generator.getLabel(node);
                if (label!=null) {
                	label.setText(generator.formatText(labelNew));
                }
            }
        }
    }

    private void switchTextAreaWidget(MbengNode node) {
        String isStatic = node.getAttribute(FormConstants.FORMATTR_IS_STATIC);
        Component widget = generator.getWidget(node);
        JLabel label = generator.getLabel(node);
        String labelText = node.getAttribute(FormConstants.FORMATTR_LABEL);
        String data = node.getAttribute(FormConstants.FORMATTR_DATA);
        String defval = node.getAttribute(FormConstants.FORMATTR_AUTOVALUE);
        if (isStatic!=null && "true".equalsIgnoreCase(isStatic)) {
        	if (widget.isVisible()) {	// text area -> label
        		widget.setVisible(false);
        		String v = node.getAttribute(FormConstants.FORMATTR_VX);
                int vx = (v!=null)?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_VY);
                int vy = (v!=null)?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_VW);
                int vw = (v!=null)?Integer.parseInt(v):160;
                v = node.getAttribute(FormConstants.FORMATTR_VH);
                int vh = (v!=null)?Integer.parseInt(v):60;
                label.setBounds(vx, vy, vw, vh);
                if (data != null && data.length() > 0)
                    label.setText("$$." + data);
                else label.setText(generator.formatText(defval));
        	}
        } else {
        	if (!widget.isVisible()) {			// label -> text area
        		String v = node.getAttribute(FormConstants.FORMATTR_LX);
                int lx = v!=null?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_LY);
                int ly = v!=null?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_LW);
                int lw = (v!=null)?Integer.parseInt(v):60;
                v = node.getAttribute(FormConstants.FORMATTR_LH);
                int lh = (v!=null)?Integer.parseInt(v):20;
                label.setBounds(lx, ly, lw, lh);
                label.setText(generator.formatText(labelText));
                widget.setVisible(true);
        	}
        }
    }

    public void actionPerformed(ActionEvent event)
    {
	    String cmd = event.getActionCommand();
		if (cmd.equals(Constants.ACTION_SAVE)) {
			boolean auto = widgetIdAuto.isSelected();
			node.setAttribute(FormConstants.FORMATTR_ID, auto?"":widgetId.getText());
			node.setAttribute(FormConstants.FORMATTR_LABEL, widgetLabel.getText());
            if (widgetData!=null)
                node.setAttribute(FormConstants.FORMATTR_DATA, widgetData.getText());
            if (widgetRequired!=null)
                node.setAttribute(FormConstants.FORMATTR_REQUIRED, widgetRequired.getText());
            if (widgetCanTypeIn!=null)
            	node.setAttribute(FormConstants.FORMATTR_CAN_TYPE_IN, widgetCanTypeIn.isSelected()?"TRUE":"FALSE");
            if (autoValue!=null)
            	node.setAttribute(FormConstants.FORMATTR_AUTOVALUE, autoValue.getText());
            if (widgetIsStatic!=null) {
            	node.setAttribute(FormConstants.FORMATTR_IS_STATIC, widgetIsStatic.isSelected()?"TRUE":"FALSE");
            	switchTextAreaWidget(node);
            }
            if (widgetImage!=null) {
            	String v = widgetImage.getText();
            	node.setAttribute(FormConstants.FORMATTR_IMAGE, v);
            	Component widget = generator.getWidget(node);
            	if (widget!=null && widget instanceof JButton) {
            		JButton button = (JButton)widget;
            		if (v==null || v.length()==0) {
            			button.setIcon(null);
            			button.setText(widgetLabel.getText());
            		} else {
                    	try {
                    		button.setText(null);
							button.setIcon(generator.loadIcon(v));
						} catch (Exception e) {
							e.printStackTrace();
						}
            		}
            	}
            }
            if (displayCondition!=null)
                node.setAttribute(FormConstants.FORMATTR_VISIBLE, displayCondition.getText());
            if (editableCondition!=null)
                node.setAttribute(FormConstants.FORMATTR_EDITABLE, editableCondition.getText());
            else if (columnEditable!=null)
            	node.setAttribute(FormConstants.FORMATTR_EDITABLE, (String)columnEditable.getSelectedItem());
            if (validatorTable!=null) {
                List<String> validators = new ArrayList<>();
                for (JTextField ftt : validatorEditors) {
                    validators.add(ftt.getText());
                }
                node.setAttribute(FormConstants.FORMATTR_VALIDATORS,
                		listToString(validators, ';'));
            }
            if (commandAction!=null)
                node.setAttribute(FormConstants.FORMATTR_ACTION, (String)commandAction.getSelectedItem());
            if (validateData!=null)
            	node.setAttribute(FormConstants.FORMATTR_VALIDATE, validateData.isSelected()?"true":"false");
            if (showBusy!=null)
            	node.setAttribute(FormConstants.FORMATTR_SHOW_BUSY, showBusy.isSelected()?"true":"false");
            if (prompt!=null) {
            	String pType = (String)prompt.getSelectedItem();
            	String pMsg = promptMessage.getText();
            	String pOptions = promptOptions.getText();
            	String av;
            	if (pType.equals(FormConstants.FORMATTRVALUE_PROMPT_NONE))
            		av = pType;
            	else if (pType.equals(FormConstants.FORMATTRVALUE_PROMPT_CONFIRM))
            		av = pType + "|" + pMsg;
            	else if (pType.equals(FormConstants.FORMATTRVALUE_PROMPT_INPUT))
            		av = pType + "|" + pMsg;
            	else av = pType + "|" + pMsg + "|" + pOptions;
            	node.setAttribute(FormConstants.FORMATTR_PROMPT, av);
            }
            if (choiceListSource!=null)
                node.setAttribute(FormConstants.FORMATTR_CHOICES, choiceListSource.getText());
            if (radioVertical!=null) {
            	node.setAttribute(FormConstants.FORMATTR_DIRECTION, radioVertical.isSelected()?"V":"H");
            }
            if (widgetIsSortable!=null) {
            	node.setAttribute(FormConstants.FORMATTR_SORTABLE, widgetIsSortable.isSelected()?"true":"false");
            }
            if (tableStyles!=null) {
            	node.setAttribute(FormConstants.FORMATTR_TABLE_STYLE, (String)tableStyles.getSelectedItem());
            }
            if (columnStyle!=null) {
            	node.setAttribute(FormConstants.FORMATTR_COLUMN_STYLE, columnStyle.getText());
            }
            if (tabbingStyles!=null) {
            	node.setAttribute(FormConstants.FORMATTR_TABBING_STYLE, (String)tabbingStyles.getSelectedItem());
            }
            if (paginator!=null)
                node.setAttribute(FormConstants.FORMATTR_ACTION, paginator.getText());
            if (validatorMessage!=null)
            	node.setAttribute(FormConstants.FORMATTR_INVALID_MSG, validatorMessage.getText());
			if (widgetTip!=null)
				node.setAttribute(FormConstants.FORMATTR_TIP, widgetTip.getText());
			if (widgetDatePattern!=null)
            	node.setAttribute(FormConstants.FORMATTR_DATE_PATTERN, widgetDatePattern.getText());
			this.updateWidgetLabel(node);
            this.setVisible(false);
        } else if (cmd.equals(Constants.ACTION_EXIT)) {
        	 this.setVisible(false);
        } else if (cmd.equals(Constants.ACTION_DELETE)) {
            for (int i=validatorSelectors.size()-1; i>=0; i--) {
                if (validatorSelectors.get(i).isSelected()) {
                    validatorTable.remove(validatorEditors.remove(i));
                    validatorTable.remove(validatorSelectors.remove(i));
                    break;
                }
            }
            this.refreshValidatorTable();
        } else if (cmd.equals(Constants.ACTION_NEW)) {
            String validatorName = (String)validatorTypes.getSelectedItem();
            if (validatorName!=null) {
            	this.createOneValidator(validatorMasks.get(validatorName), 0);
            	this.refreshValidatorTable();
            }
        } else if (cmd.equals(Constants.ACTION_RESET)) {	// change auto ID
        	boolean auto = widgetIdAuto.isSelected();
        	widgetId.setEditable(!auto);
        	widgetId.setText("");
        }
        else if (cmd.equals(Constants.ACTION_COMBO)) {
            JComboBox combo = (JComboBox) event.getSource();
            int k = combo.getSelectedIndex();
            if (k == 0)
                widgetRequired.setText(null);
            else if (k == 1)
                widgetRequired.setText("TRUE");
            else
                widgetRequired.setText("please enter condition here");
            widgetRequired.setEditable(k == 2);
        }
        else if (cmd.equals(ACTION_PROMPT_CHANGE)) {
        	String selected = prompt.getSelectedItem().toString();
        	if (selected.equals(FormConstants.FORMATTRVALUE_PROMPT_SELECT)) {
        		promptMessage.setEditable(true);
        		promptOptions.setEditable(true);
        	} else if (selected.equals(FormConstants.FORMATTRVALUE_PROMPT_NONE)) {
        		promptMessage.setEditable(false);
        		promptOptions.setEditable(false);
        	} else {
        		promptMessage.setEditable(true);
        		promptOptions.setEditable(false);
        	}
        }
    }

    private List<String> stringToList(String inputStr, char delimiter) {
        List<String> list = new ArrayList<>();
        if (inputStr == null || inputStr.length() == 0)
            return list;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = inputStr.length();
        boolean escaped = false;
        char ch;
        while (i<n) {
            ch = inputStr.charAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
            } else if (ch=='\\') {
                escaped = true;
            } else if (ch==delimiter) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
            i++;
        }
        list.add(sb.toString());
        return list;
    }

    private String listToString(List<String> list, char delimiter) {
        if (list == null)
            return null;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String one : list) {
            if (!first)
                sb.append(delimiter);
            else
                first = false;
            int n = one.length();
            for (int i = 0; i < n; i++) {
                char ch = one.charAt(i);
                if (ch == '\\' || ch == delimiter)
                    sb.append('\\');
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private class MenuEditor extends JPanel implements ActionListener, ListSelectionListener {

        private static final long serialVersionUID = 1L;
        private static final String ACTION_ADD_MENUITEM = "___ADD_MENUITEM___";
        private static final String ACTION_ADD_SUBMENU = "___ADD_SUBMENU___";
        private static final String ACTION_DELETE_ITEM = "___DELETE_ITEM___";
        private static final String ACTION_MOVE_UP = "___MOVE_UP___";
        private static final String ACTION_MOVE_DOWN = "___MOVE_DOWN___";
        private static final String ACTION_SHOW_SUBMENU = "___SHOW_SUBMENU___";
        private static final String ACTION_ITEM_LABEL = "___ITEM_LABEL___";
        private static final String ACTION_ITEM_ACTION = "___ITEM_ACTION___";

        private MbengNode menuNode;
        private JComponent menu;    // JMenu or JPopuMenu
        private JList itemlist;
        private ArrayList<MbengNode> items;
        private JTextField itemLabel;
        private JTextField itemAction;
        private JButton buttonUp, buttonDown, buttonSubmenu, buttonDelete;

        MenuEditor(MbengNode menuNode, JComponent menu) {
            super(new BorderLayout());
            this.menuNode = menuNode;
            if (menu instanceof MenuButton)
                this.menu = ((MenuButton) menu).getMenu();
            else
                this.menu = menu;

            this.setBorder(BorderFactory.createTitledBorder("Menu Items"));

            // item attribute panel
            JPanel itemAttrArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
            itemAttrArea.setPreferredSize(new Dimension(340,300));
            itemAttrArea.add(new JLabel("Label"));
            itemLabel = new JTextField();
            itemLabel.setPreferredSize(new Dimension(160,20));
            itemLabel.addActionListener(this);
            itemLabel.setActionCommand(ACTION_ITEM_LABEL);
            itemLabel.setEditable(!readonly);
            itemAttrArea.add(itemLabel);
            itemAttrArea.add(new JLabel("Action"));
            itemAction = new JTextField();
            itemAction.setPreferredSize(new Dimension(240,20));
            itemAction.addActionListener(this);
            itemAction.setActionCommand(ACTION_ITEM_ACTION);
            itemAction.setEditable(!readonly);
            itemAttrArea.add(itemAction);
            this.add(itemAttrArea, BorderLayout.EAST);

            JPanel buttonArea = itemAttrArea;
            JButton button;
            button = new JButton("Add Menu Item");
            button.addActionListener(this);
            button.setActionCommand(ACTION_ADD_MENUITEM);
            button.setEnabled(!readonly);
            buttonArea.add(button);
            button = new JButton("Add Sub Menu");
            button.addActionListener(this);
            button.setActionCommand(ACTION_ADD_SUBMENU);
            button.setEnabled(!readonly);
            buttonArea.add(button);

            buttonDelete = new JButton("Delete Item");
            buttonDelete.addActionListener(this);
            buttonDelete.setActionCommand(ACTION_DELETE_ITEM);
            buttonDelete.setEnabled(!readonly);
            buttonArea.add(buttonDelete);
            buttonUp = new JButton("Move Up");
            buttonUp.addActionListener(this);
            buttonUp.setActionCommand(ACTION_MOVE_UP);
            buttonUp.setEnabled(!readonly);
            buttonArea.add(buttonUp);
            buttonDown = new JButton("Move Down");
            buttonDown.addActionListener(this);
            buttonDown.setActionCommand(ACTION_MOVE_DOWN);
            buttonDown.setEnabled(!readonly);
            buttonArea.add(buttonDown);
            buttonSubmenu = new JButton("Show Sub Menu");
            buttonSubmenu.addActionListener(this);
            buttonSubmenu.setActionCommand(ACTION_SHOW_SUBMENU);
            buttonArea.add(buttonSubmenu);

            items = new ArrayList<>();
            ListModel mymodel = new AbstractListModel() {
                private static final long serialVersionUID = 1L;
                @Override
                public int getSize() {
                    return items.size();
                }
                @Override
                public Object getElementAt(int index) {
                    String label = items.get(index).getAttribute(FormConstants.FORMATTR_LABEL);
                    if (items.get(index).getName().equals(FormConstants.WIDGET_MENU))
                        return label + " [menu]";
                    else
                        return label;
                }
            };

            itemlist = new JList(mymodel);
            itemlist.addListSelectionListener(this);
            JScrollPane scrollpane = new JScrollPane(itemlist);
            scrollpane.setPreferredSize(new Dimension(240,120));
            add(scrollpane, BorderLayout.CENTER);

            setLocation(100, 100);
            MbengNode mitemnode = menuNode.getFirstChild();
            items.clear();
            while (mitemnode!=null) {
                items.add(mitemnode);
                mitemnode = mitemnode.getNextSibling();
            }
            itemlist.updateUI();
            enableButtons(-1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals(ACTION_ADD_MENUITEM)) {
                MbengDocument descDoc = node.getDocument();
                MbengNode newnode = descDoc.newNode(FormConstants.WIDGET_MENUITEM,
                        null, "X", ' ');
                newnode.setAttribute(FormConstants.FORMATTR_LABEL, "New item");
                menuNode.appendChild(newnode);
                items.add(newnode);
                itemlist.updateUI();
                itemlist.setSelectedIndex(items.size()-1);
                enableButtons(items.size()-1);
                this.repaint();
                try {
                	generator.create_component(newnode, 0, 0, 0, menu);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else if (cmd.equals(ACTION_ADD_SUBMENU)) {
                MbengDocument descDoc = node.getDocument();
                MbengNode newnode = descDoc.newNode(FormConstants.WIDGET_MENU,
                        null, "X", ' ');
                newnode.setAttribute(FormConstants.FORMATTR_LABEL, "New sub menu");
                menuNode.appendChild(newnode);
                items.add(newnode);
                itemlist.updateUI();
                itemlist.setSelectedIndex(items.size()-1);
                enableButtons(items.size()-1);
                this.repaint();
                try {
                	if (menu instanceof MenuButton)
                		 generator.create_component(newnode, 0, 0, 0, ((MenuButton)menu).getMenu());
                	else generator.create_component(newnode, 0, 0, 0, menu);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else if (cmd.equals(ACTION_MOVE_UP)) {
                int k = itemlist.getSelectedIndex();
                if (k>0) {
                    MbengNode prev = items.get(k-1);
                    MbengNode mnode = items.get(k);
                    MbengNode parent = mnode.getParent();
                    parent.removeChild(mnode);
                    parent.insertChild(mnode, prev);
                    items.remove(k);
                    items.add(k-1, mnode);
                    itemlist.updateUI();
                    itemlist.setSelectedIndex(k-1);
                    enableButtons(k-1);
                    this.repaint();
                    Component comp = (menu instanceof JMenu)?((JMenu)menu).getMenuComponent(k):
                            ((JPopupMenu)menu).getComponent(k);
                    menu.remove(k);
                    menu.add(comp, k-1);
                }
            } else if (cmd.equals(ACTION_MOVE_DOWN)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()-1) {
                    MbengNode next = k<items.size()-2?items.get(k+2):null;
                    MbengNode mnode = items.get(k);
                    MbengNode parent = mnode.getParent();
                    parent.removeChild(mnode);
                    if (next != null)
                        parent.insertChild(mnode, next);
                    else parent.appendChild(mnode);
                    items.remove(k);
                    items.add(k+1, mnode);
                    itemlist.updateUI();
                    itemlist.setSelectedIndex(k+1);
                    enableButtons(k+1);
                    this.repaint();
                    Component comp = (menu instanceof JMenu)?((JMenu)menu).getMenuComponent(k):
                        ((JPopupMenu)menu).getComponent(k);
                    menu.remove(k);
                    menu.add(comp, k+1);
                }
            } else if (cmd.equals(ACTION_DELETE_ITEM)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    MbengNode mnode = items.get(k);
                    MbengNode parent = mnode.getParent();
                    parent.removeChild(mnode);
                    items.remove(k);
                    itemlist.updateUI();
                    itemlist.setSelectedIndex(-1);
                    enableButtons(-1);
                    this.repaint();
                    generator.deleteNode(parent, mnode);
                }
            } else if (cmd.equals(ACTION_SHOW_SUBMENU)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    MbengNode mnode = items.get(k);
                    if (mnode.getName().equals(FormConstants.WIDGET_MENU)) {
                        FormWidgetDialog dialog = new FormWidgetDialog(FormWidgetDialog.this,
                        		mnode, generator, readonly);
                        dialog.setVisible(true);
                    }
                }
            } else if (cmd.equals(ACTION_ITEM_LABEL)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    String label = itemLabel.getText();
                    MbengNode mnode = items.get(k);
                    mnode.setAttribute(FormConstants.FORMATTR_LABEL, label);
                    itemlist.updateUI();
                    updateWidgetLabel(mnode);
                }
            } else if (cmd.equals(ACTION_ITEM_ACTION)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    String action = itemAction.getText();
                    MbengNode mnode = items.get(k);
                    mnode.setAttribute(FormConstants.FORMATTR_ACTION, action);
                    itemlist.updateUI();
                }
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int k = itemlist.getSelectedIndex();
            if (!e.getValueIsAdjusting()) {
                MbengNode mnode = items.get(k);
                itemLabel.setText(mnode.getAttribute(FormConstants.FORMATTR_LABEL));
                itemAction.setText(mnode.getAttribute(FormConstants.FORMATTR_ACTION));
                enableButtons(k);
            }

        }

        private void enableButtons(int k) {
            MbengNode mnode = k >= 0 ? items.get(k) : null;
            boolean isSubmenu = k >= 0 && mnode != null && FormConstants.WIDGET_MENU.equals(mnode.getName());
            buttonSubmenu.setEnabled(isSubmenu);
            buttonUp.setEnabled(k > 0 && !readonly);
            buttonDown.setEnabled(k < items.size() - 1 && !readonly);
            buttonDelete.setEnabled(k >= 0 && !readonly);
            itemLabel.setEditable(k >= 0 && !readonly);
            itemAction.setEditable(k >= 0 && !isSubmenu && !readonly);
        }
    }

}
