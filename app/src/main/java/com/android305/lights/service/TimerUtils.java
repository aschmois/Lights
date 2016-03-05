package com.android305.lights.service;

import android.util.Log;

import com.android305.lights.util.Timer;
import com.android305.lights.util.client.Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;

public class TimerUtils {
    private final static String TAG = TimerUtils.class.getSimpleName();

    private static int getActionId(ClientService service) {
        int actionId = ServiceUtils.randInt(ServiceUtils.TIMER);
        if (service.response.containsKey(actionId))
            return getActionId(service);
        return actionId;
    }

    private static JSONObject request(int actionId, String secondaryAction, Timer timer) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("action_id", actionId);
        request.put("action", "timer");
        request.put("secondary_action", secondaryAction);
        request.put("timer", timer.getParsed());
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

    public static ClientResponse addTimer(ClientService service, Timer timer) {
        return updateAdd(service, timer, "add");
    }

    public static ClientResponse editTimer(ClientService service, Timer timer) {
        return updateAdd(service, timer, "edit");
    }

    private static ClientResponse updateAdd(ClientService service, Timer timer, String param) {
        try {
            int actionId = getActionId(service);
            JSONObject request = request(actionId, param, timer);
            if (connectAndWait(service, actionId, request)) {
                JSONObject obj = service.response.get(actionId);
                int code = obj.getInt("code");
                String msg = obj.getString("message");
                switch (code) {
                    case Client.TIMER_EDIT_SUCCESS:
                    case Client.TIMER_ADD_SUCCESS:
                        JSONObject parsed = obj.getJSONObject("data").getJSONObject("timer");
                        Timer retrievedTimer = Timer.getTimer(parsed);
                        service.response.remove(actionId);
                        return new ClientResponse(code, msg, retrievedTimer);
                    default:
                        return new ClientResponse(code, msg, timer);
                }
            }
        } catch (InvalidKeyException e) {
            Log.e(TAG, "updateAdd() error", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "updateAdd() error", e);
            return null;
        }
        return null;
    }

    public static boolean deleteTimer(ClientService service, Timer timer) {
        try {
            int actionId = getActionId(service);
            JSONObject request = request(actionId, "delete", timer);
            if (connectAndWait(service, actionId, request)) {
                JSONObject obj = service.response.get(actionId);
                switch (obj.getInt("code")) {
                    case Client.TIMER_DELETE_SUCCESS:
                        return true;
                    default:
                        return false;
                }
            }
        } catch (InvalidKeyException | JSONException e) {
            Log.e(TAG, "deleteTimer() error", e);
            return false;
        }
        return false;
    }
}
