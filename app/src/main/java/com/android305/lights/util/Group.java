package com.android305.lights.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Group implements Serializable {
    private int id;
    private String name;

    private Lamp[] lamps;
    private Timer[] timers;

    public Group() {
    }

    public Group(String name) {
        this.name = name;
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

    public Lamp[] getLamps() {
        return lamps;
    }

    public void setLamps(Lamp[] lamps) {
        this.lamps = lamps;
    }

    public Timer[] getTimers() {
        return timers;
    }

    public void setTimers(Timer[] timers) {
        this.timers = timers;
    }

    public static Group getGroup(JSONObject parsed) {
        try {
            JSONArray lampsParsed = null;
            JSONArray timersParsed = null;
            if (parsed.has("lamps"))
                lampsParsed = parsed.getJSONArray("lamps");
            if (parsed.has("timers"))
                timersParsed = parsed.getJSONArray("timers");
            Group group = new Group();
            group.setId(parsed.getInt("id"));
            group.setName(parsed.getString("name"));
            if (lampsParsed != null) {
                ArrayList<Lamp> lamps = new ArrayList<>();
                for (int x = 0; x < lampsParsed.length(); x++) {
                    JSONObject lampParsed = lampsParsed.getJSONObject(x);
                    Lamp lamp = Lamp.getLamp(lampParsed);
                    lamp.setGroup(group);
                    lamps.add(lamp);
                }
                group.setLamps(lamps.toArray(new Lamp[lamps.size()]));
            }
            if (timersParsed != null) {
                ArrayList<Timer> timers = new ArrayList<>();
                for (int x = 0; x < timersParsed.length(); x++) {
                    JSONObject timerParsed = timersParsed.getJSONObject(x);
                    Timer timer = Timer.getTimer(timerParsed);
                    timer.setGroup(group);
                    timers.add(timer);
                }
                group.setTimers(timers.toArray(new Timer[timers.size()]));
            }
            return group;
        } catch (JSONException e) {
            throw new RuntimeException("Programming error");
        }
    }

    @Override
    public String toString() {
        return "Group{" + "id=" + id + ", name='" + name + '\'' + ", lamps=" + Arrays.toString(lamps) + '}';
    }

    public JSONObject getParsed() {
        try {
            JSONObject group = new JSONObject();
            group.put("id", id);
            group.put("name", name);
            return group;
        } catch (JSONException e) {
            throw new RuntimeException("Programming error");
        }
    }
}
