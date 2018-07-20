/**
 * Copyright (c) 2018 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.model;

import org.json.JSONException;
import org.json.JSONObject;

public class AppSummary {
    private String appId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    private String appVersion;

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    private String container;

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    private String mdwVersion;

    public String getMdwVersion() {
        return mdwVersion;
    }

    public void setMdwVersion(String mdwVersion) {
        this.mdwVersion = mdwVersion;
    }

    private String mdwBuild;

    public String getMdwBuild() {
        return mdwBuild;
    }

    public void setMdwBuild(String mdwBuild) {
        this.mdwBuild = mdwBuild;
    }

    private String mdwHubUrl;

    public String getMdwHubUrl() {
        return mdwHubUrl;
    }

    public void setMdwHubUrl(String mdwHubUrl) {
        this.mdwHubUrl = mdwHubUrl;
    }

    private String mdwWebUrl;

    public String getMdwWebUrl() {
        return mdwWebUrl;
    }

    public void setMdwWebUrl(String mdwWebUrl) {
        this.mdwWebUrl = mdwWebUrl;
    }

    private String taskManagerUrl;

    public String getTaskManagerUrl() {
        return taskManagerUrl;
    }

    public void setTaskManagerUrl(String taskManagerUrl) {
        this.taskManagerUrl = taskManagerUrl;
    }

    private String designerUrl;

    public String getDesignerUrl() {
        return designerUrl;
    }

    public void setDesignerUrl(String designerUrl) {
        this.designerUrl = designerUrl;
    }

    private String oAuthTokenUrl;

    public String getOAuthTokenUrl() {
        return oAuthTokenUrl;
    }

    public void setOAuthTokenUrl(String oAuthTokenUrl) {
        this.oAuthTokenUrl = oAuthTokenUrl;
    }

    private String authMethod;

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    private String servicesUrl;

    public String getServicesUrl() {
        return servicesUrl;
    }

    public void setServicesUrl(String servicesUrl) {
        this.servicesUrl = servicesUrl;
    }

    private Repository repository;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    private DbInfo dbInfo;

    public DbInfo getDbInfo() {
        return dbInfo;
    }

    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
    }

    public AppSummary() {

    }

    public AppSummary(JSONObject json) {
        try {
            JSONObject lJson = json;
            if (json.has("ser:ApplicationSummary"))
                lJson = json.getJSONObject("ser:ApplicationSummary");
            else if (json.has("ApplicationSummary"))
                lJson = json.getJSONObject("ApplicationSummary");
            if (lJson.has("appId"))
                this.setAppId(lJson.getString("appId"));
            else if (lJson.has("ApplicationName"))
                this.setAppId(lJson.getString("ApplicationName"));
            if (lJson.has("appVersion"))
                this.setAppVersion(lJson.getString("appVersion"));
            else if (lJson.has("Version"))
                this.setAppVersion(lJson.getString("Version"));
            if (lJson.has("mdwVersion"))
                this.setMdwVersion(lJson.getString("mdwVersion"));
            else if (lJson.has("MdwVersion"))
                this.setMdwVersion(lJson.getString("MdwVersion"));
            if (lJson.has("mdwBuild"))
                this.setMdwBuild(lJson.getString("mdwBuild"));
            else if (lJson.has("MdwBuild"))
                this.setMdwBuild(lJson.getString("MdwBuild"));
            if (lJson.has("mdwHubUrl"))
                this.setMdwHubUrl(lJson.getString("mdwHubUrl"));
            else if (lJson.has("MdwHubUrl"))
                this.setMdwHubUrl(lJson.getString("MdwHubUrl"));
            if (lJson.has("servicesUrl"))
                this.setServicesUrl(lJson.getString("servicesUrl"));
            else if (lJson.has("ServicesUrl"))
                this.setServicesUrl(lJson.getString("ServicesUrl"));
            if (lJson.has("repository"))
                this.setRepository(new Repository(lJson.getJSONObject("repository")));
            else if (lJson.has("Repository"))
                this.setRepository(new Repository(lJson.getJSONObject("Repository")));
            if (lJson.has("DbInfo"))
                this.setDbInfo(new DbInfo(lJson.getJSONObject("DbInfo")));
            if (lJson.has("MdwWebUrl"))
                this.setMdwWebUrl(lJson.getString("MdwWebUrl"));
            if (lJson.has("authMethod"))
                this.setAuthMethod(lJson.getString("authMethod"));
            if (lJson.has("Container"))
                this.setContainer(lJson.getString("Container"));
            if (lJson.has("TaskManagerUrl"))
                this.setTaskManagerUrl(lJson.getString("TaskManagerUrl"));
            if (lJson.has("DesignerUrl"))
                this.setDesignerUrl(lJson.getString("DesignerUrl"));
            if (lJson.has("OAuthTokenUrl"))
                this.setOAuthTokenUrl(lJson.getString("OAuthTokenUrl"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
