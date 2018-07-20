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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.LdapAuthenticator;
import com.centurylink.mdw.bpm.MDWPackage;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.common.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.MiniEncrypter;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.ProcessPersister.PersistType;
import com.centurylink.mdw.dataaccess.RemoteAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.dataaccess.UserDataAccess;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.VersionControlDummy;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.designer.model.ProcessRequest;
import com.centurylink.mdw.designer.runtime.RuntimeDataAccessRest;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.designer.utils.Server;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.centurylink.mdw.task.Attribute;
import com.centurylink.mdw.task.TaskTemplate;
import com.centurylink.mdw.task.TaskTemplatesDocument;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class DesignerDataAccess {

    enum Mode {
        THROUGH_SERVER, THROUGH_JDBC, NO_DATABASE, VCS
    }

    public static final String ERROR = "ERROR:";
    public static final String EXPORTXML = "Exporting XML";
    public static final String HTTP = "http:";
    public static final String IOEXCEPTION = "IOException";
    public static final String PROCESSID = "processId";

    private String cuid;
    private ProcessLoader loader;
    private RuntimeDataAccess rtinfo;
    private ProcessPersister persister;
    private UserDataAccess userinfo;
    private UserDataAccess userAccessServer;
    private Map<String, RemoteAccess> remoteAccess;
    private int dbSchemaVersion;
    private int dbSupportedSchemaVersion;
    private PropertyManager propertyManager = null;
    private Server currentServer;
    private List<Server> serverList;
    private List<String> peerServerList = null;
    private boolean oldNamespaces;
    private boolean remoteAssetRetrieve; // try retrieving processes remotely if
                                         // not found through loader
    private BaselineData baselineData;
    private WorkflowAccessRest workflowAccessRest;
    private boolean rolesRefreshed;

    public DesignerDataAccess(Server server, List<Server> serverList, String cuid)
            throws DataAccessException, RemoteException {
        this(server, serverList, cuid, false);
    }

    public DesignerDataAccess(Server server, List<Server> serverList, String cuid,
            boolean oldNamespaces) throws DataAccessException, RemoteException {
        this(server, serverList, cuid, null, oldNamespaces, false);
    }

    /**
     * When database is null and server_url is not null, access database through
     * the server When database is not null and server_url is null, access
     * database directly through JDBC When both database and server_url are
     * non-null, access database directly through JDBC, but server is used fetch
     * properties, launch browsers, changing runtime such as refreshing. When
     * both database and server_url are null, access database through JDBC via
     * default MDW data source. This mode can only be used internally in the MDW
     * server.
     *
     * @param database_name
     *            this can be a JDBC URL (including user/pass) or a data source
     *            name
     * @param server_url
     *            this is in the format of "iiop://<host>:<port>"
     * @param cuid
     * @param connectParams
     *            database connection parameters
     * @throws DataAccessException
     */
    public DesignerDataAccess(Server server, List<Server> serverList, String cuid,
            Map<String, String> connectParams, boolean oldNamespaces, boolean remoteRetrieve)
            throws DataAccessException, RemoteException {
        currentServer = new Server(server);
        this.serverList = serverList;
        this.cuid = cuid;
        this.oldNamespaces = oldNamespaces;
        this.remoteAssetRetrieve = remoteRetrieve;
        Mode mode;
        String compatDs = null;
        if (currentServer.getVersionControl() != null)
            mode = Mode.VCS;
        else if (currentServer.getDatabaseUrl() != null)
            mode = Mode.THROUGH_JDBC;
        else if (currentServer.getEngineUrl() != null || currentServer.getMdwWebUrl() != null)
            mode = Mode.THROUGH_SERVER;
        else if ("onServer".equals(cuid))
            mode = Mode.THROUGH_JDBC; // image servlet
        else
            mode = Mode.NO_DATABASE;
        if (mode == Mode.VCS) {
            DataAccess.currentSchemaVersion = DataAccess.supportedSchemaVersion = dbSupportedSchemaVersion = dbSchemaVersion = currentServer
                    .getSchemaVersion();
            baselineData = new MdwBaselineData();
            if (dbSchemaVersion < DataAccess.schemaVersion6) {
                // user auth access directly through db is still supported to
                // avoid confusion
                DatabaseAccess db = new DatabaseAccess(currentServer.getDatabaseUrl(),
                        connectParams);
                userinfo = DataAccess.getUserDataAccess(dbSchemaVersion, dbSupportedSchemaVersion,
                        db);
                if (!"jdbc://dummy".equals(currentServer.getDatabaseUrl()))
                    compatDs = DataAccess.isUseCompatibilityDatasource(db)
                            ? currentServer.getDatabaseUrl()
                            : null;
            }
            loader = new LoaderPersisterVcs(cuid, currentServer.getRootDirectory(),
                    currentServer.getVersionControl(), baselineData, compatDs);
            ((LoaderPersisterVcs) loader).setMdwVersion(DataAccess.currentSchemaVersion / 1000);
            persister = (ProcessPersister) loader;
            rtinfo = new RuntimeDataAccessRest((RestfulServer) server);
            userAccessServer = new UserDataAccessRest((RestfulServer) server);
            workflowAccessRest = new WorkflowAccessRest((RestfulServer) server);
        }
        else if (mode != Mode.NO_DATABASE) {
            if (mode == Mode.THROUGH_SERVER) {
                String dbinfo = getDatabaseCredentialFromServer(currentServer.getServerUrl());
                // if cannot connect to server, shall we set engine_url to null,
                // so that hasServerConnection() is false? Not here, as login
                // page may retry with
                // different database password
                currentServer.setDatabaseUrl(dbinfo);
                if (currentServer.getMdwWebUrl() == null)
                    currentServer.setMdwWebUrl(getStringProperty(PropertyNames.MDW_WEB_URL));
                currentServer.setApplicationName(getStringProperty(PropertyNames.APPLICATION_NAME));
                currentServer.setTaskManagerUrl(getStringProperty(PropertyNames.TASK_MANAGER_URL));
            }
            // exception, so using ojdbc14.jar instead
            DatabaseAccess db = new DatabaseAccess(currentServer.getDatabaseUrl(), connectParams);
            int[] versions = DataAccess.getDatabaseSchemaVersion(db);
            if (mode == Mode.THROUGH_SERVER)
                versions = getServerSchemaVersion();
            dbSchemaVersion = versions[0];
            dbSupportedSchemaVersion = versions[1];
            DataAccess.supportedSchemaVersion = dbSupportedSchemaVersion;
            // Note: need to retrieve database schema version event in server
            // mode
            // in order to check database password is set/correct
            loader = DataAccess.getProcessLoader(dbSchemaVersion, dbSupportedSchemaVersion, db,
                    null);
            rtinfo = DataAccess.getRuntimeDataAccess(dbSchemaVersion, dbSupportedSchemaVersion, db,
                    getVariableTypes());
            persister = DataAccess.getProcessPersister(dbSchemaVersion, dbSupportedSchemaVersion,
                    db, oldNamespaces ? DesignerCompatibility.getInstance() : null, null);
            userinfo = DataAccess.getUserDataAccess(dbSchemaVersion, dbSupportedSchemaVersion, db);
            propertyManager = PropertyManager
                    .initializeDesignerPropertyManager(currentServer.getDatabaseUrl());
            // ensure the property manager is loaded from the given database
            // URL, not MDWDataSource
        }
        remoteAccess = new HashMap<>();
    }

    /**
     * This constructor is used by the automated tester to clone an instance for
     * parallel accessing of test cases. Also used to get a copy for starting
     * FormPanel.
     *
     * @param copy
     * @throws NamingException
     * @throws CreateException
     * @throws RemoteException
     */
    public DesignerDataAccess(DesignerDataAccess copy) throws DataAccessException {
        if (copy.currentServer instanceof RestfulServer)
            currentServer = new RestfulServer((RestfulServer) copy.currentServer);
        else
            currentServer = new Server(copy.currentServer);
        serverList = copy.serverList;
        this.cuid = copy.cuid;

        if (currentServer.getVersionControl() != null) {
            dbSchemaVersion = DataAccess.currentSchemaVersion;
            dbSupportedSchemaVersion = DataAccess.currentSchemaVersion;
            baselineData = new MdwBaselineData();
            loader = new LoaderPersisterVcs(cuid, currentServer.getRootDirectory(),
                    currentServer.getVersionControl(), baselineData);
            persister = (ProcessPersister) loader;
            rtinfo = new RuntimeDataAccessRest((RestfulServer) copy.currentServer);
            userAccessServer = new UserDataAccessRest((RestfulServer) copy.currentServer);
            workflowAccessRest = new WorkflowAccessRest((RestfulServer) copy.currentServer);
        }
        else {
            DatabaseAccess db = new DatabaseAccess(copy.currentServer.getDatabaseUrl());
            int[] versions = DataAccess.getDatabaseSchemaVersion(db);
            dbSchemaVersion = versions[0];
            dbSupportedSchemaVersion = versions[1];
            DataAccess.supportedSchemaVersion = dbSupportedSchemaVersion;
            loader = DataAccess.getProcessLoader(dbSchemaVersion, dbSupportedSchemaVersion, db,
                    null);
            rtinfo = DataAccess.getRuntimeDataAccess(dbSchemaVersion, dbSupportedSchemaVersion, db,
                    getVariableTypes());
            persister = DataAccess.getProcessPersister(dbSchemaVersion, dbSupportedSchemaVersion,
                    db, copy.oldNamespaces ? DesignerCompatibility.getInstance() : null, null);
            userinfo = DataAccess.getUserDataAccess(dbSchemaVersion, dbSupportedSchemaVersion, db);
        }
        remoteAccess = new HashMap<>();
    }

    public boolean noDatabase() {
        return currentServer.getDatabaseUrl() == null
                && currentServer.getVersionControl() instanceof VersionControlDummy;
    }

    public int getDatabaseSchemaVersion() {
        return dbSchemaVersion;
    }

    public int getSupportedSchemaVersion() {
        return dbSupportedSchemaVersion;
    }

    public String getDatabaseCredentialFromServer() throws DataAccessException {
        return getDatabaseCredentialFromServer(currentServer.getServerUrl());
    }

    // persistence in local files (and probably vcs)
    public boolean isVcsPersist() {
        return loader instanceof LoaderPersisterVcs;
    }

    public File getVcsBase() {
        if (loader instanceof LoaderPersisterVcs) {
            return ((LoaderPersisterVcs) loader).getStorageDir();
        }
        else {
            return null;
        }
    }

    public VersionControl getVersionControl() {
        if (loader instanceof LoaderPersisterVcs) {
            return ((LoaderPersisterVcs) loader).getVersionControl();
        }
        else {
            return null;
        }
    }

    private String getDatabaseCredentialFromServer(String serverUrl) throws DataAccessException {
        try {
            String dbinfo = this.engineCall(serverUrl,
                    "<_mdw_database_credential>encrypted</_mdw_database_credential>");
            if (dbinfo == null)
                throw new PropertyException("Failed to find database info");
            if (dbinfo.startsWith("###"))
                dbinfo = MiniEncrypter.decrypt(dbinfo.substring(3));
            if (!dbinfo.startsWith("jdbc:")) {
                // we KNOW it's encrypted since we just asked it to be
                dbinfo = MiniEncrypter.decrypt(dbinfo);
                if (dbinfo == null || !dbinfo.startsWith("jdbc:"))
                    throw new PropertyException("Failed to find database info.");
            }
            return dbinfo;
        }
        catch (Exception e) {
            if (e instanceof RemoteException && e.getCause() instanceof DataAccessException) {
                // t3: connection exception
                e.printStackTrace();
                throw (DataAccessException) e.getCause();
            }
            else if (e instanceof RemoteException && e.getCause() instanceof NamingException) {
                // iiop: connection exception
                e.printStackTrace();
                throw new DataAccessException("Cannot connect to engine");
            }
            else if (e instanceof DataAccessException && e.getCause() instanceof ConnectException) {
                // http: connection exception
                e.printStackTrace();
                throw new DataAccessException("Failed to connect to server");
            }
            else if (e instanceof DataAccessException
                    && e.getCause() instanceof FileNotFoundException) {
                // http: improperty URL path exception
                e.printStackTrace();
                throw new DataAccessException("Inaccessible URL - " + e.getCause().getMessage());
            }
            else { // other unknown errors
                e.printStackTrace();
                throw new DataAccessException(-1, "Failed to find database info", e);
            }
        }
    }

    public String getSessionIdentity() {
        String name = currentServer.getName();
        if (name != null)
            return name + " - " + cuid;
        else
            return "file://" + cuid;
    }

    public List<ProcessVO> getProcessList() throws DataAccessException {
        return loader.getProcessList();
    }

    public ProcessVO getProcess(Long procId, ProcessVO procdef) throws DataAccessException {
        if (procdef != null && procdef.isRemote()) {
            ProcessLoader procLoader = remoteAccess.get(procdef.getRemoteServer()).getLoader();
            return procLoader.loadProcess(procId, true);
        }
        else {
            ProcessVO process = loader.loadProcess(procId, true);
            if (process == null && remoteAssetRetrieve)
                process = remoteRetrieveProcess(procId);
            if (process != null) {
                if (isVcsPersist()) {
                    if (remoteAssetRetrieve) {
                        try {
                            Map<String, String> overrideAttrs = workflowAccessRest
                                    .getAttributes(OwnerType.PROCESS, procId);
                            process.applyOverrideAttributes(overrideAttrs);
                        }
                        catch (IOException ex) {
                            System.err.println("Server not running: " + ex);
                        }
                    }
                }
                else {
                    if (process.isInRuleSet())
                        process.applyOverrideAttributes(getAttributes(OwnerType.PROCESS, procId));
                }
            }
            return process;
        }
    }

    public void updateVariableInstanceInDb(VariableInstanceInfo varInstInfo, String newValue,
            boolean isDocument) throws DataAccessException {
        // needs a separate db connection for some reason?
        DatabaseAccess db = new DatabaseAccess(currentServer.getDatabaseUrl());
        try {
            RuntimeDataAccess runtimeDao = DataAccess.getRuntimeDataAccess(dbSchemaVersion,
                    dbSupportedSchemaVersion, db, getVariableTypes());
            db.openConnection();
            if (isDocument) {
                DocumentReference docRef = (DocumentReference) varInstInfo.getData();
                runtimeDao.updateDocumentContent(docRef.getDocumentId(), newValue);
            }
            else {
                varInstInfo.setStringValue(newValue);
                runtimeDao.updateVariableInstance(varInstInfo);
            }
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
        auditLog(Action.Change, Entity.VariableInstance, varInstInfo.getInstanceId(),
                varInstInfo.getName());
    }

    public void updateVariableInstanceThruServer(VariableInstanceInfo var, String value,
            boolean isDocument) throws RemoteException, DataAccessException {
        if (this.dbSchemaVersion >= DataAccess.schemaVersion61) {
            try {
                String url = currentServer.getMdwWebUrl() + "/Services/Processes/"
                        + var.getStringValue() + "/values";
                JSONObject json = new JSONObject();
                if (value.indexOf("ActionRequest") > -1)
                    value = "com.centurylink.mdw.common.service.types.ActionRequestMessage\n"
                            + value;
                json.put(var.getName(), value);
                HttpHelper httpHelper = currentServer.getHttpHelper(url);
                httpHelper.setConnectTimeout(getConnectTimeout());
                httpHelper.setReadTimeout(getReadTimeout());
                httpHelper.put(json.toString());
            }
            catch (IOException | JSONException ex) {
                throw new DataAccessException(0, IOEXCEPTION, ex);
            }
        }
        else if (this.dbSchemaVersion < 5000 && isDocument) {
            // temporary work-around by updating database directly
            // need to enhance 4.5 code to handle document variables
            DocumentReference docref = new DocumentReference(var.getStringValue());
            rtinfo.updateDocumentContent(docref.getDocumentId(), value);
        }
        else {
            DomDocument domdoc = new DomDocument();
            FormatDom fmter = new FormatDom();
            domdoc.getRootNode().setName("_mdw_update_variable");
            MbengNode node = domdoc.newNode("var_value", value, "", ' ');
            domdoc.getRootNode().appendChild(node);
            if (var.getInstanceId() == null) {
                node = domdoc.newNode("var_name", var.getName(), "", ' ');
                domdoc.getRootNode().appendChild(node);
                node = domdoc.newNode("proc_inst_id", var.getStringValue(), "", ' ');
                domdoc.getRootNode().appendChild(node);
                String res = this.engineCall(fmter.format(domdoc));
                if (res.startsWith("OK:")) {
                    String[] vs = res.split(":");
                    var.setInstanceId(new Long(vs[1]));
                    if (vs.length == 3) {
                        DocumentReference docref = new DocumentReference(new Long(vs[2]), null);
                        var.setStringValue(docref.toString());
                    }
                }
                else
                    throw new DataAccessException(res);
            }
            else {
                node = domdoc.newNode("var_inst_id", var.getInstanceId().toString(), "", ' ');
                domdoc.getRootNode().appendChild(node);
                String res = this.engineCall(fmter.format(domdoc));
                if (!"OK".equals(res))
                    throw new DataAccessException(res);
            }
        }
        auditLog(Action.Create, Entity.VariableInstance, var.getInstanceId(), var.getName());
    }

    public byte[] getClass(String classname) throws DataAccessException, IOException {
        String response = this.engineCall("<_mdw_load_class>" + classname + "</_mdw_load_class>");
        if (response.startsWith(ERROR))
            throw new DataAccessException(response);
        return MiniEncrypter.decodeAlpha(response);
    }

    public ProcessInstanceVO getProcessInstanceBase(Long processInstanceId, String remoteServer)
            throws DataAccessException {
        if (remoteServer != null) {
            RuntimeDataAccess runTimeInfo = remoteAccess.get(remoteServer).getRuntimeDataAccess();
            return runTimeInfo.getProcessInstanceBase(processInstanceId);
        }
        else {
            return rtinfo.getProcessInstanceBase(processInstanceId);
        }
    }

    public ProcessInstanceVO getProcessInstanceAll(Long processInstanceId, ProcessVO procdef)
            throws DataAccessException {
        if (procdef != null && procdef.isRemote()) {
            RuntimeDataAccess runTimeInfo = remoteAccess.get(procdef.getRemoteServer())
                    .getRuntimeDataAccess();
            return runTimeInfo.getProcessInstanceAll(processInstanceId);
        }
        else {
            return rtinfo.getProcessInstanceAll(processInstanceId);
        }
    }

    public ProcessList getProcessInstanceList(Long procId, Map<String, String> criteria,
            Map<String, String> variables, int pageIndex, int pageSize, String orderBy)
            throws DataAccessException {
        criteria.put(PROCESSID, procId.toString());
        return rtinfo.getProcessInstanceList(criteria, variables, pageIndex, pageSize, orderBy);
    }

    public ProcessList getProcessInstanceList(String procName, Map<String, String> criteria,
            Map<String, String> variables, int pageIndex, int pageSize, String orderBy)
            throws DataAccessException {
        criteria.put("processName", procName);
        return rtinfo.getProcessInstanceList(criteria, variables, pageIndex, pageSize, orderBy);
    }

    public ProcessList getProcessInstanceList(Map<String, String> pMap, int pageIndex, int pageSize,
            ProcessVO procdef, String orderBy) throws DataAccessException {
        if (procdef != null && procdef.isRemote()) {
            RuntimeDataAccess runTimeInfo = remoteAccess.get(procdef.getRemoteServer())
                    .getRuntimeDataAccess();
            ProcessList ret = runTimeInfo.getProcessInstanceList(pMap, pageIndex, pageSize,
                    orderBy);
            for (ProcessInstanceVO one : ret.getProcesses()) {
                one.setRemoteServer(procdef.getRemoteServer());
            }
            return ret;
        }
        else {
            return rtinfo.getProcessInstanceList(pMap, pageIndex, pageSize, orderBy);
        }
    }

    public LinkedProcessInstance getProcessInstanceCallHierarchy(ProcessInstanceVO instance)
            throws DataAccessException {
        return rtinfo.getProcessInstanceCallHierarchy(instance.getId());
    }

    public ProcessVO getProcessDefinition(Long procId) throws DataAccessException {
        ProcessVO process = loader.getProcessBase(procId);
        if (process == null && remoteAssetRetrieve)
            process = remoteRetrieveProcess(procId);
        return process;
    }

    private RemoteAccess createRemoteAccess(String logicalServerName)
            throws DataAccessException, RemoteException {
        // use local setting first, as database entries may not contain remote
        // database spec
        // and hence requires the remote server to be running
        for (Server s : serverList) {
            if (logicalServerName.equals(s.getApplicationName())) {
                String curenv = currentServer.getEnvironment();
                String senv = s.getEnvironment();
                if (curenv == null)
                    curenv = "";
                if (senv == null)
                    senv = "";
                if (curenv.equals(senv)) {
                    String remoteDbInfo = s.getDatabaseUrl();
                    if (remoteDbInfo == null)
                        remoteDbInfo = getDatabaseCredentialFromServer(s.getServerUrl());
                    return new RemoteAccess(logicalServerName, remoteDbInfo);
                }
            }
        }
        if (currentServer.getServerUrl() != null) {
            String remoteServerUrl = this
                    .getStringProperty(PropertyNames.MDW_REMOTE_SERVER + "." + logicalServerName);
            if (remoteServerUrl == null)
                throw new DataAccessException(
                        "Cannot find translation for logical server name " + logicalServerName);
            String remoteDbInfo = this.getDatabaseCredentialFromServer(remoteServerUrl);
            return new RemoteAccess(logicalServerName, remoteDbInfo);
        }
        throw new DataAccessException(
                "Cannot find translation for logical server name " + logicalServerName);
    }

    public ProcessVO getProcessDefinition(String procname, int version)
            throws RemoteException, DataAccessException {
        if (procname.indexOf(RemoteAccess.REMOTE_NAME_DELIMITER) > -1) {
            int k = procname.indexOf(RemoteAccess.REMOTE_NAME_DELIMITER);
            String properProcname = procname.substring(0, k);
            String logicalServerName = procname.substring(k + 1);
            RemoteAccess rao = remoteAccess.get(logicalServerName);
            if (rao == null) {
                rao = createRemoteAccess(logicalServerName);
                remoteAccess.put(logicalServerName, rao);
            }
            ProcessLoader procLoader = rao.getLoader();
            ProcessVO procdef = procLoader.getProcessBase(properProcname, version);
            procdef.setRemoteServer(logicalServerName);
            return procdef;
        }
        else {
            return loader.getProcessBase(procname, version);
        }
    }

    public List<VariableTypeVO> getVariableTypes() throws DataAccessException {
        if (isVcsPersist() && workflowAccessRest.isOnline()) {
            try {
                return workflowAccessRest.getVariableTypes();
            }
            catch (Exception ex) {
                // service may not be available
                return loader.getVariableTypes();
            }
        }
        return loader.getVariableTypes();
    }

    public List<TaskCategory> getTaskCategories() throws DataAccessException {
        if (isVcsPersist() && workflowAccessRest.isOnline()) {
            try {
                return workflowAccessRest.getTaskCategories();
            }
            catch (Exception ex) {
                ex.printStackTrace(); // service may not be available
                return loader.getTaskCategories();
            }
        }
        return loader.getTaskCategories();
    }

    /**
     * Does not work for embedded subprocs in asset processes.
     */
    public List<ProcessInstanceVO> getChildProcessInstance(Long processInstanceId,
            ProcessVO childProcess, ProcessVO parentProcess) throws DataAccessException {
        Map<String, String> pMap = new HashMap<>();
        if (childProcess.isRemote()) {
            RuntimeDataAccess runTimeInfo = remoteAccess.get(childProcess.getRemoteServer())
                    .getRuntimeDataAccess();
            List<ProcessInstanceVO> ret;
            String ownerType = OwnerType.PROCESS_INSTANCE;
            if (parentProcess.isRemote()) {
                if (!parentProcess.getRemoteServer().equals(childProcess.getRemoteServer()))
                    ownerType = parentProcess.getRemoteServer();
            }
            else {
                ownerType = currentServer.getApplicationName();
            }
            pMap.put("owner", ownerType);
            pMap.put("ownerId", processInstanceId.toString());
            pMap.put(PROCESSID, childProcess.getProcessId().toString());
            ProcessList procList = runTimeInfo.getProcessInstanceList(pMap, 0,
                    QueryRequest.ALL_ROWS, null);
            ret = procList.getProcesses();
            for (ProcessInstanceVO one : ret) {
                one.setRemoteServer(childProcess.getRemoteServer());
            }
            return ret;
        }
        else {
            pMap.put("owner", OwnerType.PROCESS_INSTANCE);
            pMap.put("ownerId", processInstanceId.toString());
            pMap.put(PROCESSID, childProcess.getProcessId().toString());
            return getProcessInstanceList(pMap, 0, QueryRequest.ALL_ROWS, childProcess, null)
                    .getProcesses();
        }
    }

    public List<TaskInstanceVO> getTaskInstancesForProcessInstance(Long processInstanceId)
            throws DataAccessException {
        return rtinfo.getTaskInstancesForProcessInstance(processInstanceId);
    }

    /**
     * Returns a map relating activityId to taskInstances for a processInstance.
     * Requires a list of task activities which have instances as a parameter.
     */
    public Map<Long, List<TaskInstanceVO>> getTaskInstances(ProcessVO process,
            ProcessInstanceVO processInstance, List<Long> taskActivityIds)
            throws DataAccessException {
        Map<Long, List<TaskInstanceVO>> taskInstances = new HashMap<>();

        List<TaskInstanceVO> allTaskInstances = getTaskInstancesForProcessInstance(
                processInstance.getId());

        for (Long actId : taskActivityIds) {
            List<TaskInstanceVO> actTaskInstances = getTaskInstances(process, processInstance,
                    actId, allTaskInstances);
            if (!actTaskInstances.isEmpty()) {
                List<TaskInstanceVO> subTaskInstances = new ArrayList<>();
                for (TaskInstanceVO taskInstance : allTaskInstances) {
                    String secondaryOwner = taskInstance.getSecondaryOwnerType();
                    if (OwnerType.TASK_INSTANCE.equals(secondaryOwner)) {
                        for (TaskInstanceVO masterInstance : actTaskInstances) {
                            if (masterInstance.getTaskInstanceId()
                                    .equals(taskInstance.getSecondaryOwnerId()))
                                subTaskInstances.add(taskInstance);
                        }
                    }
                }
                actTaskInstances.addAll(subTaskInstances);
                taskInstances.put(actId, actTaskInstances);
            }
        }

        return taskInstances;
    }

    private List<TaskInstanceVO> getTaskInstances(ProcessVO process,
            ProcessInstanceVO processInstance, Long activityId,
            List<TaskInstanceVO> allTaskInstances) throws DataAccessException {
        List<TaskInstanceVO> taskInstances = new ArrayList<>();
        for (TaskInstanceVO taskInstance : allTaskInstances) {
            String secondaryOwner = taskInstance.getSecondaryOwnerType();
            if (OwnerType.DOCUMENT.equals(secondaryOwner)
                    || "EXTERNAL_EVENT_INSTANCE".equals(secondaryOwner)) {
                String formDataString = null;
                if (OwnerType.DOCUMENT.equals(secondaryOwner))
                    formDataString = rtinfo.getDocument(taskInstance.getSecondaryOwnerId())
                            .getContent();
                else if ("EXTERNAL_EVENT_INSTANCE".equals(secondaryOwner))
                    formDataString = rtinfo
                            .getExternalEventDetails(taskInstance.getSecondaryOwnerId());
                FormDataDocument formDataDoc = new FormDataDocument();
                try {
                    formDataDoc.load(formDataString);
                    for (ActivityInstanceVO activityInstance : processInstance
                            .getActivityInstances(activityId)) {
                        if (activityInstance.getId().equals(formDataDoc.getActivityInstanceId()))
                            taskInstances.add(taskInstance);
                    }
                }
                catch (MbengException ex) {
                    throw new DataAccessException(-1, ex.getMessage(), ex);
                }
            }
            else {
                // task instance secondary owner is work transition instance
                Long workTransInstId = taskInstance.getSecondaryOwnerId();
                for (WorkTransitionInstanceVO transitionInstance : processInstance
                        .getTransitions()) {
                    if (transitionInstance.getTransitionInstanceID().equals(workTransInstId)) {
                        Long transitionId = transitionInstance.getTransitionID();
                        WorkTransitionVO workTrans = process.getWorkTransition(transitionId);
                        if (workTrans == null && process.getSubProcesses() != null) {
                            for (ProcessVO subproc : process.getSubProcesses()) {
                                workTrans = subproc.getWorkTransition(transitionId);
                                if (workTrans != null)
                                    break;
                            }
                        }
                        if (workTrans != null && workTrans.getToWorkId().equals(activityId))
                            taskInstances.add(taskInstance);
                    }

                }
            }
        }

        return taskInstances;
    }

    public List<TaskInstanceVO> getTaskInstances(ProcessVO processVO,
            ProcessInstanceVO processInstance, Long activityId) throws DataAccessException {
        return getTaskInstances(processVO, processInstance, activityId,
                getTaskInstancesForProcessInstance(processInstance.getId()));
    }

    public String getDocumentContent(DocumentReference docref, String type)
            throws RemoteException, DataAccessException {
        if (type.equals(Object.class.getName())) {
            if (currentServer.getServerUrl() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("<_mdw_document_content>");
                sb.append("<document_id>").append(docref.getDocumentId().toString())
                        .append("</document_id>");
                sb.append("<type>").append(type).append("</type>");
                sb.append("</_mdw_document_content>");
                String result = this.engineCall(sb.toString());
                if (result.startsWith(ERROR))
                    throw new DataAccessException(result);
                return result;
            }
            else {
                return rtinfo.getDocument(docref.getDocumentId()).getContent();
            }
        }
        else {
            if (docref.getServer() != null) {
                RemoteAccess rao = remoteAccess.get(docref.getServer());
                if (rao == null) {
                    rao = createRemoteAccess(docref.getServer());
                    remoteAccess.put(docref.getServer(), rao);
                }
                return rao.getRuntimeDataAccess().getDocument(docref.getDocumentId()).getContent();
            }
            else {
                return rtinfo.getDocument(docref.getDocumentId()).getContent();
            }
        }
    }

    public DocumentVO getDocument(Long docId) throws DataAccessException {
        return rtinfo.getDocument(docId);
    }

    public List<ActivityImplementorVO> getActivityImplementors() throws DataAccessException {
        return loader.getActivityImplementors();
    }

    public void removeActivityImplementor(ActivityImplementorVO implementor,
            boolean deleteActivities) throws DataAccessException {
        if (deleteActivities) {
            persister.deleteActivitiesForImplementor(implementor);
            auditLog(Action.Delete, Entity.Activity, Long.valueOf(0), implementor.getLabel());
        }
        persister.deleteActivityImplementor(implementor.getImplementorId());
        auditLog(Action.Delete, Entity.ActivityImplementor, implementor.getImplementorId(),
                implementor.getLabel());
    }

    public String verifyClass(ActivityImplementorVO implementor, String packageName)
            throws RemoteException, DataAccessException {
        StringBuilder sb = new StringBuilder();
        sb.append("<_mdw_check_implementor>");
        sb.append("<class_name>").append(implementor.getImplementorClassName())
                .append("</class_name>");
        if (packageName != null)
            sb.append("<package_name>").append(packageName).append("</package_name>");
        sb.append("</_mdw_check_implementor>");
        String result = this.engineCall(sb.toString());
        if (!result.startsWith("<PAGELET"))
            throw new DataAccessException(result);
        return result;
    }

    public Long createActivityImplementor(ActivityImplementorVO implementor, boolean verifyClass,
            String packageName) throws RemoteException, DataAccessException {
        if (verifyClass)
            verifyClass(implementor, packageName);
        Long id = persister.createActivityImplementor(implementor);
        auditLog(Action.Create, Entity.ActivityImplementor, id, implementor.getLabel());
        return id;
    }

    public void updateActivityImplementor(ActivityImplementorVO implementor, boolean verifyClass,
            String packageName) throws RemoteException, DataAccessException {
        if (verifyClass)
            verifyClass(implementor, packageName);
        persister.updateActivityImplementor(implementor);
        auditLog(Action.Change, Entity.ActivityImplementor, implementor.getImplementorId(),
                implementor.getLabel());
    }

    public String createProcess(ProcessVO processVO) throws DataAccessException, XmlException {
        Long procId = persister.persistProcess(processVO, ProcessPersister.PersistType.CREATE);
        auditLog(Action.Create, Entity.Process, procId, processVO.getLabel());
        return procId.toString();
    }

    public String updateProcess(ProcessVO processVO, int version, boolean lock)
            throws DataAccessException, XmlException {
        Long procid = processVO.getProcessId();
        processVO.setModifyingUser(version < 0 || lock ? cuid : null);
        if (version < 0) {
            persister.persistProcess(processVO, ProcessPersister.PersistType.SAVE);
        }
        else if (version == 0) {
            persister.persistProcess(processVO, ProcessPersister.PersistType.UPDATE);
        }
        else {
            processVO.setVersion(version);
            procid = persister.persistProcess(processVO, ProcessPersister.PersistType.NEW_VERSION);
        }
        auditLog(Action.Change, Entity.Process, procid, processVO.getLabel());
        return procid.toString();
    }

    public boolean hasProcessInstances(Long processId) throws DataAccessException {
        return rtinfo.hasProcessInstances(processId);
    }

    public void removeProcess(ProcessVO processVO, boolean deleteInstances)
            throws DataAccessException {
        if (deleteInstances) {
            rtinfo.deleteProcessInstancesForProcess(processVO.getProcessId());
            auditLog(Action.Delete, Entity.ProcessInstance, Long.valueOf(0), processVO.getLabel());
        }
        persister.deleteProcess(processVO);
        auditLog(Action.Delete, Entity.Process, processVO.getProcessId(), processVO.getLabel());
    }

    public long renameProcess(Long processId, String newName, int newVersion)
            throws DataAccessException {
        long id = persister.renameProcess(processId, newName, newVersion);
        auditLog(Action.Rename, Entity.Process, processId, newName);
        return id;
    }

    public void deleteProcessInstances(List<ProcessInstanceVO> processInstances)
            throws DataAccessException {
        if (processInstances.isEmpty())
            return;
        List<Long> ids = new ArrayList<>();
        for (ProcessInstanceVO instance : processInstances)
            ids.add(instance.getId());
        rtinfo.deleteProcessInstances(ids);
        auditLog(Action.Delete, Entity.ProcessInstance, 0L, ids.get(0) + " ...");
    }

    public String exportProcess(Long pProcessId, boolean oldNamespaces)
            throws DataAccessException, XmlException {
        ProcessVO processVO = loader.loadProcess(pProcessId, true);
        new ProcessWorker().convert_to_designer(processVO);
        ProcessExporter exporter = DataAccess.getProcessExporter(dbSchemaVersion,
                oldNamespaces ? DesignerCompatibility.getInstance() : null);
        return exporter.exportProcess(processVO, dbSchemaVersion, new ArrayList<ExternalEventVO>());
    }

    public PackageVO loadPackage(Long packageId, boolean deep) throws DataAccessException {
        return loader.loadPackage(packageId, deep);
    }

    public String exportPackages(List<PackageVO> packages, int schemaVersion, boolean exportJson,
            boolean includeTaskTemplates, ProgressMonitor monitor)
            throws DataAccessException, ActionCancelledException, JSONException, XmlException {

        if (monitor != null)
            monitor.subTask("Loading package(s)...");

        List<PackageVO> loadedPackages = new ArrayList<>();
        for (PackageVO pkg : packages) {
            if (monitor != null)
                monitor.subTask("Loading " + pkg.getLabel() + "...");
            PackageVO packageVO = loadPackage(pkg.getId(), true);
            if (monitor != null)
                monitor.progress(30 / packages.size());
            if (monitor != null) {
                if (monitor.isCanceled())
                    throw new ActionCancelledException();
                else
                    monitor.subTask("Sorting processes and implementors");
            }

            if (packageVO.getRuleSets() != null) {
                Map<String, CustomAttributeVO> customAttrs = new HashMap<>();
                for (RuleSetVO ruleSet : packageVO.getRuleSets()) {
                    if (ruleSet.getLanguage() != null
                            && !customAttrs.containsKey(ruleSet.getLanguage())) {
                        CustomAttributeVO custAttrVO = getCustomAttribute("RULE_SET",
                                ruleSet.getLanguage());
                        if (custAttrVO != null)
                            customAttrs.put(ruleSet.getLanguage(), custAttrVO);
                    }
                }
                List<CustomAttributeVO> customAttributes = new ArrayList<>();
                for (CustomAttributeVO customAttr : customAttrs.values())
                    customAttributes.add(customAttr);
                packageVO.setCustomAttributes(customAttributes);
            }

            loadedPackages.add(packageVO);
        }

        if (monitor != null && monitor.isCanceled())
            throw new ActionCancelledException();

        if (monitor != null)
            monitor.progress(5);

        if (monitor != null)
            monitor.subTask(EXPORTXML);

        String export;
        if (exportJson) {
            ImporterExporterJson exporter = new ImporterExporterJson();
            export = exporter.exportPackages(loadedPackages);
        }
        else {
            ProcessExporter exporter = DataAccess.getProcessExporter(schemaVersion,
                    oldNamespaces ? DesignerCompatibility.getInstance() : null);
            export = exporter.exportPackages(loadedPackages, includeTaskTemplates);
        }

        if (monitor != null)
            monitor.progress(25);

        for (PackageVO packageVO : loadedPackages) {
            packageVO.setExported(true);
            persister.persistPackage(packageVO, PersistType.UPDATE);
        }

        if (monitor != null)
            monitor.progress(15);

        return export;
    }

    public String exportPackage(Long packageId, int schemaVersion, boolean includeTaskTemplates,
            ProgressMonitor monitor)
            throws DataAccessException, ActionCancelledException, XmlException {

        if (monitor != null)
            monitor.subTask("Loading package...");

        PackageVO packageVO = loadPackage(packageId, true);
        if (monitor != null)
            monitor.progress(30);

        if (monitor != null) {
            if (monitor.isCanceled())
                throw new ActionCancelledException();
            else
                monitor.subTask("Sorting processes and implementors");
        }

        // adding same logic as in PopupHandler export to get same result
        for (ProcessVO processVO : packageVO.getProcesses())
            new ProcessWorker().convert_to_designer(processVO);

        if (packageVO.getRuleSets() != null) {
            Map<String, CustomAttributeVO> customAttrs = new HashMap<>();
            for (RuleSetVO ruleSet : packageVO.getRuleSets()) {
                if (ruleSet.getLanguage() != null
                        && !customAttrs.containsKey(ruleSet.getLanguage())) {
                    CustomAttributeVO custAttrVO = getCustomAttribute("RULE_SET",
                            ruleSet.getLanguage());
                    if (custAttrVO != null)
                        customAttrs.put(ruleSet.getLanguage(), custAttrVO);
                }
            }
            List<CustomAttributeVO> customAttributes = new ArrayList<>();
            for (CustomAttributeVO customAttr : customAttrs.values())
                customAttributes.add(customAttr);
            packageVO.setCustomAttributes(customAttributes);
        }

        if (monitor != null && monitor.isCanceled())
            throw new ActionCancelledException();

        if (monitor != null)
            monitor.progress(5);

        if (monitor != null)
            monitor.subTask(EXPORTXML);

        ProcessExporter exporter = DataAccess.getProcessExporter(schemaVersion,
                oldNamespaces ? DesignerCompatibility.getInstance() : null);
        String xml = exporter.exportPackage(packageVO, includeTaskTemplates);

        if (monitor != null)
            monitor.progress(25);

        packageVO.setExported(true);
        persister.persistPackage(packageVO, PersistType.UPDATE);

        if (monitor != null)
            monitor.progress(15);

        return xml;
    }

    public String exportAttributes(String prefix, Long artifactId, int schemaVersion,
            ProgressMonitor monitor, String exportArtifactType)
            throws DataAccessException, ActionCancelledException, XmlException {
        monitor.subTask("Loading attributes...");
        PackageVO packageVO = null;
        ProcessVO processVO = null;
        if (exportArtifactType.equals(OwnerType.PACKAGE)) {
            packageVO = loadPackage(artifactId, true);
            if (isVcsPersist()) {
                try {
                    for (ProcessVO process : packageVO.getProcesses()) {
                        Map<String, String> overrideAttrs = workflowAccessRest
                                .getAttributes(OwnerType.PROCESS, process.getId());
                        process.applyOverrideAttributes(overrideAttrs);
                    }
                }
                catch (IOException ex) {
                    throw new DataAccessOfflineException("Server does not appear to be running.",
                            ex);
                }
            }
            else {
                for (ProcessVO process : packageVO.getProcesses()) {
                    if (process.isInRuleSet()) {
                        Map<String, String> overrideAttrs = getAttributes(OwnerType.PROCESS,
                                process.getId());
                        process.applyOverrideAttributes(overrideAttrs);
                    }
                }
            }
        }
        else {
            processVO = getProcess(artifactId, null);
            if (isVcsPersist()) {
                try {
                    // need to make sure attributes are retrieved
                    Map<String, String> overrideAttrs = workflowAccessRest
                            .getAttributes(OwnerType.PROCESS, processVO.getId());
                    processVO.applyOverrideAttributes(overrideAttrs);
                }
                catch (IOException ex) {
                    throw new DataAccessOfflineException("Server does not appear to be running.",
                            ex);
                }
            }
        }

        monitor.progress(30);
        if (monitor.isCanceled())
            throw new ActionCancelledException();
        if (packageVO != null) {
            // -- subprocesses must come after their containing parent processes
            Collections.sort(packageVO.getProcesses(), new Comparator<ProcessVO>() {
                public int compare(ProcessVO pVO1, ProcessVO pVO2) {
                    boolean pVO1HasSubProcs = pVO1.getSubProcesses() != null
                            && !pVO1.getSubProcesses().isEmpty();
                    boolean pVO2HasSubProcs = pVO2.getSubProcesses() != null
                            && !pVO2.getSubProcesses().isEmpty();
                    if (pVO1HasSubProcs == pVO2HasSubProcs) {
                        // sort by label
                        return pVO1.getLabel().compareToIgnoreCase(pVO2.getLabel());
                    }
                    else if (pVO1HasSubProcs)
                        return -1;
                    else
                        return 1;
                }
            });
        }

        if (monitor.isCanceled())
            throw new ActionCancelledException();

        monitor.progress(5);

        monitor.subTask(EXPORTXML);

        ProcessExporter exporter = DataAccess.getProcessExporter(schemaVersion,
                oldNamespaces ? DesignerCompatibility.getInstance() : null);
        String xml;
        if (packageVO != null)
            xml = exporter.exportOverrideAttributes(prefix, packageVO);
        else
            xml = exporter.exportOverrideAttributes(prefix, processVO, schemaVersion);

        monitor.progress(40);
        return xml;
    }

    public String exportTaskTemplates(Long packageId, ProgressMonitor monitor)
            throws DataAccessException, ActionCancelledException {

        monitor.subTask("Loading package");
        PackageVO packageVO = loadPackage(packageId, true);

        monitor.progress(30);
        if (monitor.isCanceled())
            throw new ActionCancelledException();

        if (monitor != null)
            monitor.progress(5);

        if (monitor != null)
            monitor.subTask(EXPORTXML);

        PackageDocument pkgDoc = PackageDocument.Factory.newInstance();
        MDWPackage pkg = pkgDoc.addNewPackage();
        pkg.setName(packageVO.getName());
        pkg.setVersion(PackageVO.formatVersion(packageVO.getVersion()));
        TaskTemplatesDocument.TaskTemplates templates = pkg.addNewTaskTemplates();
        if (packageVO.getTaskTemplates() != null) {
            for (TaskVO taskVO : packageVO.getTaskTemplates()) {
                if (taskVO.getVersion() > 0) {
                    TaskTemplate templateDef = templates.addNewTask();
                    templateDef.setLogicalId(taskVO.getLogicalId());
                    templateDef.setVersion(RuleSetVO.formatVersion(taskVO.getVersion()));
                    templateDef.setAssetName(taskVO.getName());
                    templateDef.setName(taskVO.getTaskName());
                    if (taskVO.getTaskCategory() != null)
                        templateDef.setCategory(taskVO.getTaskCategory());
                    if (taskVO.getComment() != null)
                        templateDef.setDescription(taskVO.getComment());
                    if (taskVO.getAttributes() != null) {
                        for (AttributeVO attrVO : taskVO.getAttributes()) {
                            if (!"TaskDescription".equals(attrVO.getAttributeName())) {
                                Attribute attr = templateDef.addNewAttribute();
                                attr.setName(attrVO.getAttributeName());
                                attr.setStringValue(attrVO.getAttributeValue());
                            }
                        }
                    }
                }
            }
        }
        String xml = pkgDoc
                .xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));

        if (monitor != null && monitor.isCanceled())
            throw new ActionCancelledException();

        if (monitor != null)
            monitor.progress(40);

        return xml;
    }

    public List<String> getRoleNames() throws DataAccessException {
        if (userAccessServer != null && userAccessServer.isOnline())
            return userAccessServer.getRoleNames();
        else if (userinfo != null && userinfo.isOnline())
            return userinfo.getRoleNames();
        else
            return baselineData.getUserRoles();
    }

    public UserVO getUser(String cuid) throws DataAccessException {
        if (userAccessServer != null && userAccessServer.isOnline())
            return userAccessServer.getUser(cuid);
        else if (userinfo != null && userinfo.isOnline())
            return userinfo.getUser(cuid);
        else
            return new UserVO(cuid);
    }

    public ProcessInstanceVO getCauseForTaskInstance(Long pTaskInstanceId)
            throws DataAccessException {
        return rtinfo.getCauseForTaskInstance(pTaskInstanceId);
    }

    public ProcessInstanceVO getProcessInstanceForSecondary(String pSecOwner, Long pSecOwnerId)
            throws DataAccessException {
        return rtinfo.getProcessInstanceForSecondary(pSecOwner, pSecOwnerId);
    }

    public List<ExternalEventVO> getExternalEvents() throws DataAccessException {
        return loader.loadExternalEvents();
    }

    public List<TaskVO> getTaskTemplates() throws DataAccessException {
        return loader.getTaskTemplates();
    }

    public void updateExternalEvent(ExternalEventVO event) throws DataAccessException {
        persister.updateExternalEvent(event);
        auditLog(Action.Change, Entity.ExternalEvent, event.getId(), event.getEventName());
    }

    public void removeExternalEvent(ExternalEventVO event) throws DataAccessException {
        persister.deleteExternalEvent(event.getId());
        auditLog(Action.Delete, Entity.ExternalEvent, event.getId(), event.getEventName());
    }

    public void createExternalEvent(ExternalEventVO event) throws DataAccessException {
        persister.createExternalEvent(event);
        auditLog(Action.Create, Entity.ExternalEvent, event.getId(), event.getEventName());
    }

    public void refreshServerCaches(String cacheName) throws RemoteException, DataAccessException {
        // when cacheName is null, refresh all caches
        if (cacheName == null)
            this.engineCall("<_mdw_refresh></_mdw_refresh>");
        else
            this.engineCall("<_mdw_refresh>" + cacheName + "</_mdw_refresh>");
    }

    public String getStringProperty(String property) throws RemoteException, DataAccessException {
        if (currentServer.getServerUrl() != null) {
            return this.engineCall("<_mdw_property>" + property + "</_mdw_property>");
        }
        else {
            try {
                return propertyManager.getStringProperty(property);
            }
            catch (Exception e) {
                e.printStackTrace();
                throw new DataAccessException(-1, "Failed to get property manager", e);
            }
        }
    }

    public List<UserGroupVO> getAllGroups() throws DataAccessException {
        if (userAccessServer != null && userAccessServer.isOnline()) {
            rolesRefreshed = true;
            return userAccessServer.getAllGroups(false);
        }
        else if (userinfo != null && userinfo.isOnline())
            return userinfo.getAllGroups(false);
        else {
            // baseline workgroups
            List<UserGroupVO> groups = new ArrayList<>();
            for (String groupName : baselineData.getWorkgroups()) {
                groups.add(new UserGroupVO(groupName));
            }
            return groups;
        }
    }

    public ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId,
            Long eventInstId) throws DataAccessException {
        return rtinfo.getExternalMessage(activityId, activityInstId, eventInstId);
    }

    public void retryActivity(Long activityId, Long activityInstId)
            throws DataAccessException, XmlException, IOException {
        if (currentServer.isSchemaVersion61()) {
            ((RestfulServer) currentServer).retryActivityInstance(activityInstId,
                    ActivityResultCodeConstant.RESULT_RETRY);
        }
        else {
            String request = currentServer.buildRetryActivityInstanceRequest(activityId,
                    activityInstId, false);
            String response = this.engineCall(request);
            try {
                String result = currentServer.getErrorMessageFromResponse(response);
                if (result == null || result.length() > 0)
                    throw new RemoteException(result);
                auditLog(Action.Retry, Entity.ActivityInstance, activityInstId, null);
            }
            catch (XmlException e) {
                throw new DataAccessException("Response is not an MDWStatusMessage");
            }
        }
    }

    public void skipActivity(Long activityId, Long activityInstId, String compCode)
            throws DataAccessException, XmlException, IOException {
        if (currentServer.isSchemaVersion61()) {
            ((RestfulServer) currentServer).skipActivityInstance(activityInstId,
                    ActivityResultCodeConstant.RESULT_PROCEED, compCode);
        }
        else {
            String request = currentServer.buildSkipActivityInstanceRequest(activityId,
                    activityInstId, compCode, false);
            String response = this.engineCall(request);
            try {
                String result = currentServer.getErrorMessageFromResponse(response);
                if (result == null || result.length() > 0)
                    throw new RemoteException(result);
                auditLog(Action.Proceed, Entity.ActivityInstance, activityInstId, compCode);
            }
            catch (XmlException e) {
                throw new DataAccessException("Response is not an MDWStatusMessage");
            }
        }
    }

    public Long savePackage(PackageVO pPackageVO) throws DataAccessException {
        PersistType persistType = pPackageVO.getVersion() == 0 || pPackageVO.isExported()
                ? PersistType.NEW_VERSION
                : PersistType.UPDATE;
        return savePackage(pPackageVO, persistType);
    }

    public Long savePackage(PackageVO pPackageVO, PersistType persistType)
            throws DataAccessException {
        Long id = persister.persistPackage(pPackageVO, persistType);
        Action action = pPackageVO.getVersion() == 0 ? Action.Create : Action.Change;
        auditLog(action, Entity.Package, id, pPackageVO.getLabel());
        return id;
    }

    /**
     * Temporary methods allowing non-Git VCS projects to import without
     * throwing exceptions due to failed audit log.
     */
    public Long savePackageNoAudit(PackageVO pPackageVO, PersistType persistType)
            throws DataAccessException {
        return persister.persistPackage(pPackageVO, persistType);
    }

    public Long savePackageNoAudit(PackageVO pPackageVO) throws DataAccessException {
        PersistType persistType = pPackageVO.getVersion() == 0 || pPackageVO.isExported()
                ? PersistType.NEW_VERSION
                : PersistType.UPDATE;
        return persister.persistPackage(pPackageVO, persistType);
    }

    public long renamePackage(Long packageId, String newName, int newVersion)
            throws DataAccessException {
        long id = persister.renamePackage(packageId, newName, newVersion);
        auditLog(Action.Rename, Entity.Package, packageId, newName);
        return id;
    }

    public void deletePackage(PackageVO pPackageVO) throws DataAccessException {
        persister.deletePackage(pPackageVO.getPackageId());
        auditLog(Action.Delete, Entity.Package, pPackageVO.getPackageId(), pPackageVO.getLabel());
    }

    public List<PackageVO> getAllPackages(ProgressMonitor progressMonitor)
            throws DataAccessException {
        return loader.getPackageList(false, progressMonitor);
    }

    public List<PackageVO> getPackageList(boolean deep) throws DataAccessException {
        return loader.getPackageList(deep, null);
    }

    public List<PackageVO> getPackageList(boolean deep, ProgressMonitor progressMonitor)
            throws DataAccessException {
        return loader.getPackageList(deep, progressMonitor);
    }

    public long addProcessToPackage(ProcessVO processVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        return persister.addProcessToPackage(processVO.getProcessId(), packageId);
    }

    public void removeProcessFromPackage(ProcessVO processVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        persister.removeProcessFromPackage(processVO.getProcessId(), packageId);
    }

    public long addExternalEventToPackage(ExternalEventVO externalEventVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        return persister.addExternalEventToPackage(externalEventVO.getId(), packageId);
    }

    public void removeExternalEventFromPackage(ExternalEventVO externalEventVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        persister.removeExternalEventFromPackage(externalEventVO.getId(), packageId);
    }

    public long addActivityImplToPackage(ActivityImplementorVO activityImplVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        return persister.addActivityImplToPackage(activityImplVO.getImplementorId(), packageId);
    }

    public void removeActivityImplFromPackage(ActivityImplementorVO activityImplVO,
            PackageVO packageVO) throws DataAccessException {
        Long packageId = savePackage(packageVO, PersistType.UPDATE);
        persister.removeActivityImplFromPackage(activityImplVO.getImplementorId(), packageId);
    }

    public long addRuleSetToPackage(RuleSetVO ruleSetVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        return persister.addRuleSetToPackage(ruleSetVO.getId(), packageId);
    }

    public void removeRuleSetFromPackage(RuleSetVO ruleSetVO, PackageVO packageVO)
            throws DataAccessException {
        Long packageId = savePackage(packageVO);
        persister.removeRuleSetFromPackage(ruleSetVO.getId(), packageId);
    }

    public List<ActivityImplementorVO> getReferencedImplementors(PackageVO packageVO)
            throws DataAccessException {
        return loader.getReferencedImplementors(packageVO);
    }

    public List<RuleSetVO> getRuleSets() throws DataAccessException {
        return loader.getRuleSets();
    }

    public RuleSetVO getRuleSet(Long id) throws DataAccessException {
        return loader.getRuleSet(id);
    }

    public RuleSetVO getRuleSet(String name, String language, int version)
            throws DataAccessException {
        return loader.getRuleSet(name, language, version);
    }

    public RuleSetVO getRuleSet(Long packageId, String name) throws DataAccessException {
        return loader.getRuleSet(packageId, name);
    }

    public Long saveRuleSet(RuleSetVO ruleset) throws DataAccessException {
        Long id = ruleset.getId();
        Action action = id <= 0 ? Action.Create : Action.Change;
        if (id <= 0)
            id = persister.createRuleSet(ruleset);
        else
            persister.updateRuleSet(ruleset);
        auditLog(action, Entity.RuleSet, id, ruleset.getDescription());
        return id;
    }

    public void renameRuleSet(RuleSetVO ruleset, String newName) throws DataAccessException {
        persister.renameRuleSet(ruleset, newName);
        auditLog(Action.Change, Entity.RuleSet, ruleset.getId(), ruleset.getDescription());
    }

    public void deleteRuleSet(RuleSetVO ruleset) throws DataAccessException {
        persister.deleteRuleSet(ruleset.getId());
        auditLog(Action.Delete, Entity.RuleSet, ruleset.getId(), ruleset.getDescription());
    }

    public void createTaskTemplate(TaskVO taskTemplate) throws DataAccessException {
        persister.createTaskTemplate(taskTemplate);
    }

    public void deleteTaskTemplate(TaskVO taskTemplate) throws DataAccessException {
        persister.deleteTaskTemplate(taskTemplate.getId());
    }

    public List<ProcessVO> findCallingProcesses(ProcessVO subproc) throws DataAccessException {
        return loader.findCallingProcesses(subproc);
    }

    public List<ProcessVO> findCalledProcesses(ProcessVO mainproc) throws DataAccessException {
        return loader.findCalledProcesses(mainproc);
    }

    public String lockUnlockProcess(Long processId, String cuid, boolean lock)
            throws DataAccessException {
        return persister.lockUnlockProcess(processId, cuid, lock);
    }

    public String lockUnlockRuleSet(Long rulesetId, String cuid, boolean lock)
            throws DataAccessException {
        return persister.lockUnlockRuleSet(rulesetId, cuid, lock);
    }

    public String readFile(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            FileInputStream in = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            in.read(bytes);
            in.close();
            return new String(bytes);
        }
        else
            throw new IOException("File does not exist - " + filename);
    }

    public void launchBrowser(String url) throws DataAccessException, RemoteException {
        String vUrl = url;
        if (vUrl.startsWith("prop:")) {
            String property = vUrl.substring(5);
            vUrl = getStringProperty(property);
        }
        else if (vUrl.startsWith("/")) {
            if (currentServer.getServerUrl() == null)
                throw new DataAccessException("No server specification to translate relative URL");
            if (vUrl.startsWith("/MDWWeb")) {
                if (currentServer.getMdwWebUrl() != null)
                    vUrl = currentServer.getMdwWebUrl() + vUrl.substring(7);
            }
            else if (vUrl.startsWith("/MDWTaskManagerWeb")) {
                if (currentServer.getTaskManagerUrl() != null)
                    vUrl = currentServer.getTaskManagerUrl() + vUrl.substring(18);
            }
            else {
                String serverurl = currentServer.getServerUrl();
                int i1 = serverurl.indexOf("//");
                int i2 = serverurl.indexOf('/', i1 + 2);
                if (i2 < 0)
                    serverurl = HTTP + serverurl.substring(i1);
                else
                    serverurl = HTTP + serverurl.substring(i1, i2);
                vUrl = serverurl + vUrl;
            }
        }
        else if (!vUrl.startsWith("http")) {
            File file = new File(vUrl);
            if (!file.exists())
                throw new DataAccessException("File does not exist: " + vUrl);
        }
        try {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + vUrl);
        }
        catch (Exception ex) {
            throw new DataAccessException(0, "cannot launch browser:\n" + ex.getLocalizedMessage(),
                    ex);
        }
    }

    public String launchProcess(ProcessVO procdef, String masterRequestId, Long activityId,
            Map<String, String> parameters, boolean isServiceProc, boolean oldNamespaces)
            throws DataAccessException, XmlException, JSONException, IOException {
        String serverUrl = currentServer.getServerUrl();
        if (serverUrl == null)
            throw new DataAccessException("Need server connection for starting process");
        if (currentServer.isSchemaVersion61()) {
            Map<VariableVO, String> variables = new HashMap<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                VariableVO varVO = procdef.getVariable(entry.getKey());
                if (varVO != null)
                    variables.put(varVO, entry.getValue());
            }
            return ((RestfulServer) currentServer).launchProcess(procdef.getProcessId(),
                    masterRequestId, procdef.getOwnerType(), procdef.getOwnerId(), variables);
        }
        else {
            String request = currentServer.buildLaunchProcessRequest(procdef, masterRequestId,
                    activityId, parameters, isServiceProc, oldNamespaces);
            return this.engineCall(request);
        }
    }

    private String sendRestfulMessage(String mdwWebUrl, String message, Map<String, String> headers)
            throws DataAccessException {
        try {
            HttpHelper httpHelper = currentServer.getHttpHelper(mdwWebUrl + "/Services/REST");
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            httpHelper.setHeaders(headers);
            String res = httpHelper.post(message);
            return res == null ? null : res.trim();
        }
        catch (IOException ex) {
            throw new DataAccessException(0, IOEXCEPTION, ex);
        }
    }

    private String sendSoapMessage(String mdwWebUrl, String message, Map<String, String> headers)
            throws DataAccessException {
        try {
            HttpHelper httpHelper = currentServer.getHttpHelper(mdwWebUrl + "/Services/SOAP");
            httpHelper.setHeaders(headers);
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            String res = httpHelper.post(message);
            return res == null ? null : res.trim();
        }
        catch (IOException ex) {
            throw new DataAccessException(0, IOEXCEPTION, ex);
        }
    }

    public String sendMessage(String protocol, String message, Map<String, String> headers)
            throws RemoteException, DataAccessException {
        if (protocol.equals(Listener.METAINFO_PROTOCOL_RESTFUL_WEBSERVICE)) {
            return sendRestfulMessage(currentServer.getMdwWebUrl(), message, headers);
        }
        else if (protocol.equals(Listener.METAINFO_PROTOCOL_EJB)) {
            return this.sendEjbMessage(currentServer.getEngineUrl(), message);
        }
        else if (protocol.equals(Listener.METAINFO_PROTOCOL_SOAP)
                || protocol.equals(Listener.METAINFO_PROTOCOL_WEBSERVICE)) {
            return this.sendSoapMessage(currentServer.getMdwWebUrl(), message, headers);
        }
        else if (currentServer.getServerUrl() != null) {
            return engineCall(currentServer.getServerUrl(), message);
        }
        else {
            throw new DataAccessException("Need server connection for sending message");
        }
    }

    public String executeUnitTest(String message, Map<String, String> headers)
            throws DataAccessException {
        try {
            HttpHelper httpHelper = currentServer.getHttpHelper(currentServer.getMdwWebUrl()
                    + "/Services/com/centurylink/mdw/testing/AutomatedTests/unit");
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            httpHelper.setHeaders(headers);
            String res = httpHelper.post(message);
            return res == null ? null : res.trim();
        }
        catch (IOException ex) {
            throw new DataAccessException(0, IOEXCEPTION, ex);
        }
    }

    public String callTaskManager(String taskManagerUrl, String request)
            throws DataAccessException {
        try {
            HttpHelper httpHelper = currentServer.getHttpHelper(taskManagerUrl);
            String response = httpHelper.post(request);
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());

            return response;
        }
        catch (IOException ex) {
            throw new DataAccessException(0, IOEXCEPTION, ex);
        }
    }

    public void setCurrentServer(Server server) {
        currentServer = server;
    }

    public Server getCurrentServer() {
        return currentServer;
    }

    public int getConnectTimeout() {
        if (currentServer == null)
            return Server.DEFAULT_CONNECT_TIMEOUT;
        else
            return currentServer.getConnectTimeout();
    }

    public int getReadTimeout() {
        if (currentServer == null)
            return Server.DEFAULT_READ_TIMEOUT;
        else
            return currentServer.getReadTimeout();
    }

    public String engineCall(String request) throws RemoteException, DataAccessException {
        return engineCall(currentServer.getServerUrl(), request);
    }

    public String engineCall(String serverUrl, String request)
            throws RemoteException, DataAccessException {
        return engineCall(serverUrl, request, null);
    }

    public String engineCall(String serverUrl, String request, Map<String, String> headers)
            throws RemoteException, DataAccessException {
        String response;
        if (serverUrl == null)
            throw new DataAccessException("No server specified");
        if (serverUrl.startsWith("iiop:")) {
            response = sendEjbMessage(serverUrl, request);
        }
        else if (serverUrl.startsWith(HTTP) || serverUrl.startsWith("https:")) {
            if (request.startsWith("<env:Envelope"))
                response = sendSoapMessage(serverUrl, request, headers);
            else
                response = sendRestfulMessage(serverUrl, request, headers);
        }
        else if (serverUrl.startsWith("rmi:")) {
            response = sendRmiMessage(serverUrl, request);
        }
        else
            throw new DataAccessException("Illegal server url: " + serverUrl);
        return response;
    }

    // really send RMI message through iiop now
    private String sendEjbMessage(String engineUrl, String request) throws RemoteException {
        Context context = null;
        try {
            Hashtable<String, String> h = new Hashtable<>();
            h.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
            h.put(Context.PROVIDER_URL, engineUrl);
            context = new InitialContext(h);
            Object obj = context.lookup(RMIListener.JNDI_NAME);
            RMIListener server;
            if (obj instanceof RMIListener)
                server = (RMIListener) obj;
            else
                server = (RMIListener) PortableRemoteObject.narrow(obj, RMIListener.class);
            return server.invoke(null, request);
        }
        catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
        finally {
            if (context != null) {
                try {
                    context.close();
                }
                catch (NamingException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }

    // really send RMI message through rmi registry
    private String sendRmiMessage(String engineUrl, String request) throws RemoteException {
        try {
            Registry registry;
            String host;
            int port;
            int k1 = engineUrl.indexOf("://");
            int k2 = engineUrl.indexOf(':', k1 + 4);
            int k3 = engineUrl.indexOf('/', k1 + 4);
            if (k2 > 0) {
                host = engineUrl.substring(k1 + 3, k2);
                if (k3 > 0)
                    port = Integer.parseInt(engineUrl.substring(k2 + 1, k3));
                else
                    port = Integer.parseInt(engineUrl.substring(k2 + 1));
                port += RMIListener.PORT_DIFF;
            }
            else {
                port = 1099; // well-known RMI registry port
                if (k3 > 0)
                    host = engineUrl.substring(k1 + 3, k3);
                else
                    host = engineUrl.substring(k1 + 3);
            }
            registry = LocateRegistry.getRegistry(host, port);
            RMIListener server = (RMIListener) registry.lookup(RMIListener.JNDI_NAME);
            return server.invoke(null, request);
        }
        catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    /**
     * Get an authenticator instance based on the system property
     * "mdw.authentication.provider".
     */
    public static Authenticator getAuthenticator() throws MDWException {
        String authenticatorClass = System.getProperty("mdw.authentication.provider");
        if (authenticatorClass == null) {
            return new LdapAuthenticator();
        }
        else {
            try {
                return Class.forName(authenticatorClass).asSubclass(Authenticator.class)
                        .newInstance();
            }
            catch (Exception ex) {
                throw new MDWException(ex.getMessage(), ex);
            }
        }
    }

    public static boolean isArchiveEditAllowed() {
        return "true".equalsIgnoreCase(System.getProperty("mdw.archive.edit.allowed"));
    }

    public boolean ping() {
        String serverUrl = currentServer.getServerUrl();
        if (serverUrl == null)
            return false;
        try {
            String res = engineCall("<ping>OK</ping>");
            return res != null && res.contains(">OK<");
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void auditLog(Action action, Entity entity, Long entityId, String comments)
            throws DataAccessException {
        if (dbSchemaVersion < DataAccess.schemaVersion61) {
            UserActionVO userAction = new UserActionVO(cuid, action, entity, entityId, comments);
            auditLog(userAction);
        }
    }

    public void auditLog(UserActionVO userAction) throws DataAccessException {
        userAction.setSource("Designer");
        if (userAccessServer != null && userAccessServer.isOnline()
                && dbSchemaVersion < DataAccess.schemaVersion61)
            userAccessServer.auditLogUserAction(userAction);
        else if (userinfo != null && userinfo.isOnline())
            userinfo.auditLogUserAction(userAction);
    }

    public List<ProcessVO> getProcessListForImplementor(Long implementorId, String implementorClass)
            throws DataAccessException {
        return loader.getProcessListForImplementor(implementorId, implementorClass);
    }

    public String getCuid() {
        return this.cuid;
    }

    private int[] getServerSchemaVersion() throws DataAccessException, RemoteException {
        String response = this.engineCall("<_mdw_dbschema_version></_mdw_dbschema_version>");
        if (response.startsWith(ERROR))
            throw new DataAccessException("Failed to get database schema version from server");
        int k = response.indexOf(',');
        int[] versions = new int[2];
        if (k > 0) {
            versions[0] = Integer.parseInt(response.substring(0, k));
            versions[1] = Integer.parseInt(response.substring(k + 1));
        }
        else {
            versions[0] = Integer.parseInt(response);
            versions[1] = versions[0] >= DataAccess.schemaVersion52 ? versions[0]
                    : DataAccess.schemaVersion4;
        }
        return versions;
    }

    public List<String> getPeerServerList() {
        if (peerServerList == null) {
            peerServerList = new ArrayList<>();
            if (currentServer.getServerUrl() != null) {
                try {
                    String response = this
                            .engineCall("<_mdw_peer_server_list></_mdw_peer_server_list>");
                    if (response.startsWith(ERROR))
                        throw new Exception("Failed to load peer server list");
                    String[] serverArray = response.split(",");
                    for (String one : serverArray) {
                        peerServerList.add(one);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return peerServerList;
    }

    public String getPeerServerUrl(String hostport) {
        String serverUrl = currentServer.getServerUrl();
        int k1 = serverUrl.indexOf("://");
        int k2 = serverUrl.indexOf('/', k1 + 4);
        String peerServerUrl = serverUrl.substring(0, k1 + 3) + hostport;
        if (k2 > 0)
            peerServerUrl += serverUrl.substring(k2);
        return peerServerUrl;
    }

    /**
     * Retrieves the latest package.
     */
    public PackageVO getPackage(String name) throws DataAccessException {
        return loader.getPackage(name);
    }

    public boolean checkServerOnline() throws DataAccessException {
        if (!userAccessServer.isOnline())
            throw userAccessServer.getDataAccessOfflineException();
        else
            return true;
    }

    public ProcessVO remoteRetrieveProcess(long processId) throws DataAccessException {
        return workflowAccessRest.getProcess(processId,
                getDatabaseSchemaVersion() >= DataAccess.schemaVersion6);
    }

    public ProcessVO remoteRetrieveProcess(String name, int version) throws DataAccessException {
        try {
            String path = "Processes?name=" + name;
            if (version != 0)
                path += "&version=" + RuleSetVO.formatVersion(version);
            String pkgXml = ((RestfulServer) currentServer).invokeResourceService(path);
            ProcessImporter importer = DataAccess.getProcessImporter(getDatabaseSchemaVersion());
            PackageVO pkg = importer.importPackage(pkgXml);
            ProcessVO process = pkg.getProcesses().get(0);
            process.setPackageName(pkg.getName());
            process.setPackageVersion(pkg.getVersionString());
            if (isVcsPersist())
                process.setId(currentServer.getVersionControl().getId(getLogicalFile(process)));
            return process;
        }
        catch (IOException ex) {
            throw new DataAccessException(
                    "Error retrieving process: " + name + " v" + RuleSetVO.formatVersion(version),
                    ex);
        }
    }

    /**
     * Replicates logic in LoaderPersisterVCS.
     */
    private File getLogicalFile(RuleSetVO asset) {
        return new File("/" + asset.getPackageName() + " v" + asset.getPackageVersion() + "/"
                + asset.getName() + " v" + asset.getVersionString());
    }

    public String getServerResourceRest(String asset) throws DataAccessException, IOException {
        return ((RestfulServer) currentServer)
                .invokeResourceService("WorkflowAsset?name=" + asset + "&format=text");
    }

    public void setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue)
            throws DataAccessException {
        Long existingId = persister.setAttribute(ownerType, ownerId, attrname, attrvalue);
        Action action = existingId == null ? Action.Create : Action.Change;
        auditLog(action, Entity.Attribute, existingId == null ? Long.valueOf(0) : existingId,
                attrname);
    }

    public Map<String, String> getAttributes(String ownerType, Long ownerId)
            throws DataAccessException {
        if (isVcsPersist()) {
            try {
                return workflowAccessRest.getAttributes(ownerType, ownerId);
            }
            catch (IOException ex) {
                throw new DataAccessOfflineException(ex.getMessage(), ex);
            }
        }
        else {
            return loader.getAttributes(ownerType, ownerId);
        }
    }

    public void setAttributes(String ownerType, Long ownerId, Map<String, String> attrs)
            throws DataAccessException {
        if (isVcsPersist()) {
            workflowAccessRest.updateAttributes(ownerType, ownerId, attrs);
            // audit logging is on the server
        }
        else {
            persister.setAttributes(ownerType, ownerId, attrs);
            auditLog(Action.Change, Entity.Attribute, ownerId, ownerType);
        }
    }

    public void setOverrideAttributes(String prefix, String ownerType, Long ownerId, String subType,
            String subId, Map<String, String> attributes) throws DataAccessException {

        Map<String, String> existAttrs = getAttributes(ownerType, ownerId);
        Map<String, String> setAttrs = new HashMap<>();
        if (existAttrs != null) {
            // retain existing attrs not related to this subType/subId/prefix
            for (Map.Entry<String, String> entry : existAttrs.entrySet()) {
                String existName = entry.getKey();
                if (!WorkAttributeConstant.isFullAttrNameFor(existName, subType, subId, prefix))
                    setAttrs.put(existName, entry.getValue());
            }
        }

        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet())
                setAttrs.put(WorkAttributeConstant.getOverrideAttributeName(entry.getKey(), subType,
                        subId), entry.getValue());
        }

        setAttributes(ownerType, ownerId, setAttrs);
    }

    public void setOverrideAttributes(ProcessVO processVO) throws DataAccessException {
        Map<String, String> overrideAttributes = processVO.getOverrideAttributes();
        setAttributes(OwnerType.PROCESS, processVO.getId(), overrideAttributes);
    }

    public void setOverrideAttributes(String prefix, String ownerType, Long ownerId,
            Map<String, String> attributes) throws DataAccessException {

        Map<String, String> existAttrs = getAttributes(ownerType, ownerId);
        Map<String, String> setAttrs = new HashMap<>();
        if (existAttrs != null) {
            // retain attributes not related to this prefix
            for (Map.Entry<String, String> entry : existAttrs.entrySet()) {
                String existName = entry.getKey();
                if (!WorkAttributeConstant.isAttrNameFor(existName, prefix))
                    setAttrs.put(existName, entry.getValue());
            }
        }

        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet())
                setAttrs.put(entry.getKey(), entry.getValue());
        }

        setAttributes(ownerType, ownerId, setAttrs);
    }

    public CustomAttributeVO getCustomAttribute(String ownerType, String categorizer)
            throws DataAccessException {
        return loader.getCustomAttribute(ownerType, categorizer);
    }

    public void setCustomAttribute(CustomAttributeVO customAttrVO) throws DataAccessException {
        Long existingId = persister.setCustomAttribute(customAttrVO);
        Action action = existingId == null ? Action.Create : Action.Change;
        auditLog(action, Entity.Attribute, existingId == null ? Long.valueOf(0) : existingId,
                "Custom attributes for: " + customAttrVO.getDefinitionAttrOwner());
    }

    public void importFromVcs(String mdwWebUrl) throws DataAccessException {
        try {
            HttpHelper httpHelper = currentServer.getHttpHelper(mdwWebUrl + "/Services/GitVcs/*");
            Map<String, String> hdrs = new HashMap<>();
            hdrs.put("request-query-string", "gitAction=pull");
            httpHelper.setHeaders(hdrs);
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            httpHelper.post("{}");
        }
        catch (IOException ex) {
            throw new DataAccessException(0, IOEXCEPTION, ex);
        }
    }

    public boolean isRolesRefreshed() {
        return rolesRefreshed;
    }

    public void setRolesRefreshed(boolean refreshed) {
        rolesRefreshed = refreshed;
    }
}
