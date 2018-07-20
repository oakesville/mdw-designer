/**
 * Copyright (c) 2018 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Repository {

    private String provider;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String branch;

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    private String commit;

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public Repository() {

    }

    public Repository(JSONObject json) {
        try {
            if (json.has("provider"))
                this.setProvider(json.getString("provider"));
            if (json.has("url"))
                this.setUrl(json.getString("url"));
            if (json.has("branch"))
                this.setBranch(json.getString("branch"));
            if (json.has("commit"))
                this.setCommit(json.getString("commit"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
