package com.centurylink.mdw.designer.model;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class ProcessRequest implements Jsonable {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private String masterRequestId;

    public String getMasterRequestId() {
        return masterRequestId;
    }

    public void setMasterRequestId(String s) {
        masterRequestId = s;
    }

    private Long definitionId;

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long id) {
        this.definitionId = id;
    }

    private Map<String, String> values;

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }


    private String ownerType;

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    private Long ownerId;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    private Long instanceId;

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public ProcessRequest(Long processId, String masterRequestId, String owner, Long ownerId, Map<VariableVO, String> variables) {
        this.masterRequestId = masterRequestId;
        this.definitionId = processId;
        this.ownerId = ownerId;
        this.ownerType = owner;
        this.values = new HashMap<>();
        for (Map.Entry<VariableVO, String> entry: variables.entrySet())
            values.put(entry.getKey().getVariableName(), entry.getValue());
    }

    public ProcessRequest(JSONObject json) throws JSONException {
        if (json.has("id"))
            this.id = json.getLong("id");
        if (json.has("masterRequestId"))
            this.masterRequestId = json.getString("masterRequestId");
        if (json.has("definitionId"))
            this.definitionId = json.getLong("definitionId");
        if (json.has("values")) {
            this.values = new HashMap<>();
            JSONObject valuesObj = json.getJSONObject("values");
            String[] names = JSONObject.getNames(valuesObj);
            if (names != null) {
                for (String name : names) {

                    JSONObject valObject = valuesObj.getJSONObject(name);
                    this.values.put(name, valObject.toString());

                }
            }
        }

        if (json.has("ownerType"))
            this.ownerType = json.getString("ownerType");
        if (json.has("ownerId"))
            this.ownerId = json.getLong("ownerId");
        if (json.has("instanceId"))
            this.instanceId = json.getLong("instanceId");
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null)
            json.put("id", id);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (definitionId != null)
            json.put("definitionId", definitionId);
        if (values != null) {
            JSONObject valuesJson = new JSONObject();
            for (String name : values.keySet()) {
                valuesJson.put(name, values.get(name));
            }
            json.put("values", valuesJson);
        }
        if (ownerType != null)
            json.put("ownerType", ownerType);
        if (ownerId != null)
            json.put("ownerId", ownerId);
        if (instanceId != null)
            json.put("instanceId", instanceId);
        return json;
    }

    @Override
    public String getJsonName() {
        return "Process Request";
    }

}
