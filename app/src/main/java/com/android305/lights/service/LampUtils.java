package com.android305.lights.service;

import android.util.Log;

import com.android305.lights.util.Lamp;
import com.android305.lights.util.client.Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;

public class LampUtils {
    private final static String TAG = "LampUtils";

    private static int getActionId(ClientService service) {
        int actionId = ServiceUtils.randInt(1000, 1999);
        if (service.response.containsKey(actionId))
            return getActionId(service);
        return actionId;
    }

    private static JSONObject request(int actionId, String secondaryAction, Lamp lamp) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("action_id", actionId);
        request.put("action", "lamp");
        request.put("secondary_action", secondaryAction);
        request.put("lamp", lamp.getParsed());
        return request;
    }

    private static boolean connectAndWait(ClientService service, int actionId, JSONObject request) throws InvalidKeyException {
        if (service.client == null || !service.client.isConnected())
            return false; //TODO: figure out how to wait for connection, if I do a while loop the thread is blocked, may need to thread this method further
        service.client.write(request.toString());
        while (!service.response.containsKey(actionId) && service.client.isConnected())
            ServiceUtils.sleep(10);
        return service.client.isConnected();
    }

    public static Lamp addLamp(ClientService service, Lamp lamp) {
        try {
            int actionId = getActionId(service);
            JSONObject request = request(actionId, "add", lamp);
            if (connectAndWait(service, actionId, request)) {
                switch (service.response.get(actionId).getInt("code")) {
                    case Client.LAMP_ADD_SUCCESS:
                        JSONObject parsed = service.response.get(actionId).getJSONObject("data").getJSONObject("lamp");
                        Lamp retrievedLamp = Lamp.getLamp(parsed);
                        service.response.remove(actionId);
                        return retrievedLamp;
                    case Client.LAMP_ALREADY_EXISTS:
                        return lamp;
                }
            }
        } catch (InvalidKeyException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        }
        return null;
    }

    public static Lamp toggleLamp(ClientService service, Lamp lamp) {
        try {
            int actionId = getActionId(service);
            JSONObject request = request(actionId, "toggle", lamp);
            if (connectAndWait(service, actionId, request)) {
                switch (service.response.get(actionId).getInt("code")) {
                    case Client.LAMP_TOGGLE_SUCCESS:
                        JSONObject parsed = service.response.get(actionId).getJSONObject("data").getJSONObject("lamp");
                        Lamp retrievedLamp = Lamp.getLamp(parsed);
                        service.response.remove(actionId);
                        return retrievedLamp;
                    case Client.LAMP_TOGGLE_DOES_NOT_EXIST:
                        return null;
                }
            }
        } catch (InvalidKeyException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        }
        return null;
    }
}
