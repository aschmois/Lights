package com.android305.lights.service;

import com.android305.lights.util.Lamp;
import com.android305.lights.util.Timer;

public class ClientResponse {

    private int response;
    private String message;

    private Lamp lamp;
    private Timer timer;

    public ClientResponse(int response, String message, Lamp lamp) {
        this.response = response;
        this.message = message;
        this.lamp = lamp;
    }

    public ClientResponse(int response, String message, Timer timer) {
        this.response = response;
        this.message = message;
        this.timer = timer;
    }

    public int getResponse() {
        return response;
    }

    public String getMessage() {
        return message;
    }

    public Lamp getLamp() {
        return lamp;
    }

    public Timer getTimer() {
        return timer;
    }

}
