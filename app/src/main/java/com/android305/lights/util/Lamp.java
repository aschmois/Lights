package com.android305.lights.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Lamp implements Serializable {
    private int id;
    private String name;
    private String ipAddress;
    private boolean status;
    private boolean invert;
    private String error = null;
    private Group group;

    private int internalGroupId;

    public Lamp() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Group getGroup() {
        //TODO: retrieve group if not yet loaded
        return group;
    }

    public void setInternalGroupId(int internalGroupId) {
        this.internalGroupId = internalGroupId;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "Lamp{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", status=" + status +
                ", invert=" + invert +
                ", error='" + error + '\'' +
                ", internalGroupId=" + internalGroupId +
                '}';
    }

    public JSONObject getParsed() {
        try {
            JSONObject lamp = new JSONObject();
            lamp.put("id", id);
            lamp.put("name", name);
            lamp.put("ip", ipAddress);
            lamp.put("status", status);
            lamp.put("invert", invert);
            lamp.put("error", error);
            lamp.put("group", internalGroupId);
            return lamp;
        } catch (JSONException e) {
            throw new RuntimeException("Programming error");
        }
    }

    public static Lamp getLamp(JSONObject parsed) {
        try {
            Lamp lamp = new Lamp();
            if (parsed.has("id"))
                lamp.setId(parsed.getInt("id"));
            if (parsed.has("name"))
                lamp.setName(parsed.getString("name"));
            if (parsed.has("ip"))
                lamp.setIpAddress(parsed.getString("ip"));
            if (parsed.has("status"))
                lamp.setStatus(parsed.getBoolean("status"));
            if (parsed.has("invert"))
                lamp.setInvert(parsed.getBoolean("invert"));
            if (parsed.has("error"))
                lamp.setError(parsed.getString("error"));
            if (parsed.has("group"))
                lamp.setInternalGroupId(parsed.getInt("group"));
            return lamp;
        } catch (JSONException e) {
            throw new RuntimeException("Programming error");
        }
    }
}
