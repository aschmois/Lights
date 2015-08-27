package com.android305.lights.util;

import java.io.Serializable;
import java.util.Arrays;

public class Group implements Serializable {
    private int id;
    private String name;

    private Lamp[] lamps;

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

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lamps=" + Arrays.toString(lamps) +
                '}';
    }
}
