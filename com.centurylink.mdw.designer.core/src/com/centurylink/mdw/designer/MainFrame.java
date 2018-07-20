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
package com.centurylink.mdw.designer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;

import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.designer.utils.CustomOptionPane;
import com.centurylink.mdw.designer.utils.Server;

public class MainFrame extends JFrame implements WindowListener {

	// start page names
	public static final String PROCESS_LIST = "PROCESS_LIST";
	public static final String PROCESS_INSTANCE = "PROCESS_INSTANCE";
	public static final String MILESTONE_INSTANCE = "MILESTONE_INSTANCE";
	public static final String FORM_MAIN = "FORM_MAIN";

	private DesignerPage currentPage;

	private DesignerDataModel model;
	public DesignerDataAccess dao = null;
	private String titleBase = "MDW Designer";
	private PrintStream log;
	private SimpleDateFormat df;
	public String errmsg;
	public Color colorCanvas;
	public Color colorReadonlyCanvas;
	public Font fontTextfield;
	private String cuid;
	private CustomOptionPane optionPane;
	private String startPage;
	private HashMap<Class<?>,DesignerPage> pages;
	private boolean isAdditonalMainFrame;

	// the following are shared among all windows (MainFrame instances)
	private static List<Server> serverList;

	private IconFactory iconFactory;
	public IconFactory getIconFactory() { return iconFactory; }

    public MainFrame(String title) {
 		super(title);

 		this.iconFactory = new IconFactory();
		colorCanvas = Color.white;
		colorReadonlyCanvas = new Color(235,240,245);
		setSize(900, 720);
		addWindowListener(this);
		currentPage = null;
		rootPane.setDoubleBuffered(false);
		fontTextfield = new Font("courier", Font.PLAIN, 12);
		dao = null;
		log = System.out;
		df = new SimpleDateFormat("HH:mm:ss");
		setDefaultLookAndFeelDecorated(true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		errmsg = null;
		startPage = null;
	}

	public void setPage(DesignerPage page) {
		Container contentPane = getContentPane();
		JMenuBar menubar = null;

		if (currentPage!=null) {
			if (!currentPage.canleave()) return;
			contentPane.remove(currentPage);
		}
		Runtime.getRuntime().gc();
		contentPane.add(page);
		if (startPage==null) {
			menubar = page.getMenuBar();
			if (menubar!=null) setJMenuBar(menubar);
			else {
				menubar = this.getJMenuBar();
				if (menubar!=null) setJMenuBar(null);
				// this is for AK's change to remove menu bar for log in page
			}
		}
		page.setVisible(true);
		this.setTitle(page.getTitle());
		currentPage = page;
		repaint();
		setVisible(true);
		if (errmsg!=null) {
			optionPane.showError(this, errmsg);
			errmsg = null;
		}
	}

	public void windowActivated(WindowEvent arg0) {
	    // do nothing
	}

	public void windowClosed(WindowEvent arg0) {
	    // do nothing
	}

	public DesignerDataModel getDataModel() {
		return model;
	}

	public void setOptionPane(CustomOptionPane pane) {
		optionPane = pane;
	}

	public CustomOptionPane getOptionPane() {
		return optionPane;
	}

	public String getCuid() {
		return cuid;
	}

	public void windowClosing(WindowEvent arg0) {
	    // do nothing
	}

	public void windowDeactivated(WindowEvent arg0) {
	    // do nothing
	}

	public void windowDeiconified(WindowEvent arg0) {
	    // do nothing
	}

	public void windowIconified(WindowEvent arg0) {
	    // do nothing
	}

	public void windowOpened(WindowEvent arg0) {
	    // do nothing
	}

	public void setNewServer() {
		if (dao==null) return;
	}

	public void log(String msg) {
		String line = "[" + df.format(new Date()) + "][designer]" + msg;
		log.println(line);
	}

	public List<Server> getServerList() {
		return serverList;
	}

	public String getDesignerTitle(){
		return titleBase;
	}

    /**
     * Uses 50% of progressMonitor.
     */
    public void startSession(String cuid, Server server, ProgressMonitor progressMonitor,
            Map<String, String> connectParams, boolean oldNamespaces, boolean remoteRetrieve)
    throws RemoteException, DataAccessException {
	    this.cuid = cuid;
		dao = new DesignerDataAccess(server, serverList, this.cuid, connectParams, oldNamespaces, remoteRetrieve);
		model = new DesignerDataModel();
		pages = new HashMap<>();
		iconFactory.setDesignerDataAccess(dao);
		model.setDatabaseSchemaVersion(dao.getDatabaseSchemaVersion());
		if (progressMonitor != null) {
		    progressMonitor.progress(5);
		    progressMonitor.subTask("Loading reference information");
		}
		model.reloadVariableTypes(dao);
		model.reloadRoleNames(dao);
		model.reloadTaskCategories(dao);
		// privileges must be loaded before loading groups/process/resource
		model.reloadPriviledges(dao, cuid);
		model.reloadGroups(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(5);
		// resources must be loaded before processes
		if (progressMonitor != null)
		    progressMonitor.subTask("Loading workflow assets");
		model.reloadRuleSets(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(15);
		// activity implementors must be loaded before processes
		if (progressMonitor != null)
		    progressMonitor.subTask("Loading activity implementors");
		model.reloadActivityImplementors(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(5);
		if (progressMonitor != null)
		    progressMonitor.subTask("Loading event handlers");
		model.reloadExternalEvents(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(5);
        if (progressMonitor != null)
            progressMonitor.subTask("Loading task templates");
        model.reloadTaskTemplates(dao);
        if (progressMonitor != null)
            progressMonitor.progress(5);
		if (startPage==null || PROCESS_LIST.equals(startPage)) {
			if (progressMonitor != null)
			    progressMonitor.subTask("Loading process list");
			model.reloadProcesses(dao);	// must be after loading rule sets
			if (progressMonitor != null)
			    progressMonitor.progress(15);
		}
		VariableTypeCache.loadCache(model.getVariableTypes());
		titleBase = "MDW Designer (" + dao.getSessionIdentity() + ")";
	}

	public boolean isInEclipse() {
		return !this.isVisible() && startPage==null && !isAdditonalMainFrame;
	}

	public DesignerPage getPage(Class<?> cls) {
		return getPage(cls, false);
	}

	public DesignerPage getPage(Class<?> cls, boolean forceNew) {
		try {
			DesignerPage page = pages.get(cls);
			if (page==null || forceNew) {
				page = (DesignerPage)cls.getConstructor(MainFrame.class).newInstance(this);
				pages.put(cls, page);
			}
			return page;
		} catch (Exception e) {
			e.printStackTrace();
			getOptionPane().showError(this, "Failed to create page " + cls.getName());
			return currentPage;
		}
	}

	public String getStartPage() {
		return startPage;
	}

	public void setPage(Class<?> cls) {
		DesignerPage page = getPage(cls);
		this.setPage(page);
	}

	public void removePage(Class<?> cls) {
		pages.remove(cls);
	}

	public DesignerPage getCurrentPage() {
		return currentPage;
	}
}
