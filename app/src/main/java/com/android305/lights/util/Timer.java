package com.android305.lights.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Time;

public class Timer implements Serializable {
    private int id;
    private Time start;
    private Time end;
    private boolean sunday = true;
    private boolean monday = true;
    private boolean tuesday = true;
    private boolean wednesday = true;
    private boolean thursday = true;
    private boolean friday = true;
    private boolean saturday = true;
    private int status = 0;
    private String RGB = null;
    private Group group;
    private int internalGroupId;

    public Timer() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Time getStart() {
        return start;
    }

    public void setStart(Time start) {
        this.start = start;
    }

    public Time getEnd() {
        return end;
    }

    public void setEnd(Time end) {
        this.end = end;
    }

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public boolean isMonday() {
        return monday;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getRGB() {
        return RGB;
    }

    public void setRGB(String RGB) {
        this.RGB = RGB;
    }

    public Group getGroup() throws SQLException {
        //TODO: get group if not retrieved
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public int getInternalGroupId() {
        return internalGroupId;
    }

    public void setInternalGroupId(int internalGroupId) {
        this.internalGroupId = internalGroupId;
    }

    public JSONObject getParsed() {
        try {
            JSONObject timer = new JSONObject();
            timer.put("id", id);
            timer.put("start", start);
            timer.put("end", end);
            timer.put("sunday", sunday);
            timer.put("monday", monday);
            timer.put("tuesday", tuesday);
            timer.put("wednesday", wednesday);
            timer.put("thursday", thursday);
            timer.put("friday", friday);
            timer.put("saturday", saturday);
            timer.put("status", status);
            timer.put("group", internalGroupId);
            return timer;
        } catch (JSONException e) {
            throw new RuntimeException("Programming error");
        }
    }

    public static Timer getTimer(JSONObject parsed) {
        try {
            Timer timer = new Timer();
            if (parsed.has("id"))
                timer.setId(parsed.getInt("id"));
            if (parsed.has("start"))
                timer.setStart(Time.valueOf(parsed.getString("start")));
            if (parsed.has("end"))
                timer.setEnd(Time.valueOf(parsed.getString("end")));
            if (parsed.has("sunday"))
                timer.setSunday(parsed.getBoolean("sunday"));
            if (parsed.has("monday"))
                timer.setMonday(parsed.getBoolean("monday"));
            if (parsed.has("tuesday"))
                timer.setTuesday(parsed.getBoolean("tuesday"));
            if (parsed.has("wednesday"))
                timer.setWednesday(parsed.getBoolean("wednesday"));
            if (parsed.has("thursday"))
                timer.setThursday(parsed.getBoolean("thursday"));
            if (parsed.has("friday"))
                timer.setFriday(parsed.getBoolean("friday"));
            if (parsed.has("saturday"))
                timer.setSaturday(parsed.getBoolean("saturday"));
            if (parsed.has("status"))
                timer.setStatus(parsed.getInt("status"));
            if (parsed.has("group"))
                timer.setInternalGroupId(parsed.getInt("group"));
            return timer;
        } catch (JSONException e) {
            throw new RuntimeException("Programming error");
        }
    }

    @Override
    public String toString() {
        return "Timer{" +
                "id=" + id +
                ", start=" + start +
                ", end=" + end +
                ", sunday=" + sunday +
                ", monday=" + monday +
                ", tuesday=" + tuesday +
                ", wednesday=" + wednesday +
                ", thursday=" + thursday +
                ", friday=" + friday +
                ", saturday=" + saturday +
                ", status=" + status +
                ", RGB='" + RGB + '\'' +
                ", internalGroupId=" + internalGroupId +
                '}';
    }
}
