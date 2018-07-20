package com.centurylink.mdw.designer.model;

import org.json.JSONException;
import org.json.JSONObject;

public class DbInfo {
    private String jdbcUrl;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    private String user;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public DbInfo()
    {

    }

    public DbInfo(JSONObject json) {
        try {
            if (json.has("jdbcUrl"))
                this.setJdbcUrl(json.getString("jdbcUrl"));
            if (json.has("user"))
                this.setUser(json.getString("user"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}