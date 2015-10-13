package com.android305.lights.service;

import com.android305.lights.util.Lamp;

public class ClientResponse {

    private int response;
    private String message;

    private Lamp lamp;

    public ClientResponse(int response, String message, Lamp lamp) {
        this.response = response;
        this.message = message;
        this.lamp = lamp;
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
}
