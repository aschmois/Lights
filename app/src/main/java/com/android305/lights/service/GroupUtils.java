package com.android305.lights.service;

import android.util.Log;
import android.util.SparseArray;

import com.android305.lights.util.Group;
import com.android305.lights.util.client.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;

public class GroupUtils {
    private final static String TAG = "GroupUtils";

    private static int getActionId(ClientService service) {
        int actionId = ServiceUtils.randInt(ServiceUtils.GROUP);
        if (service.response.containsKey(actionId))
            return getActionId(service);
        return actionId;
    }

    private static JSONObject request(int actionId, String secondaryAction, Group group) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("action_id", actionId);
        request.put("action", "group");
        request.put("secondary_action", secondaryAction);
        request.put("group", group.getParsed());
        return request;
    }

    public static SparseArray<Group> getGroups(ClientService service) {
        try {
            int actionId = getActionId(service);
            JSONObject write = new JSONObject();
            write.put("action_id", actionId);
            write.put("action", "group");
            write.put("secondary_action", "get-all");
            while (service.client == null || !service.client.isConnected())
                ServiceUtils.sleep(1000);
            service.client.write(write.toString());
            while (!service.response.containsKey(actionId) && service.client.isConnected())
                ServiceUtils.sleep(10);
            if (!service.client.isConnected())
                return null;
            switch (service.response.get(actionId).getInt("code")) {
                case Client.GROUP_GET_ALL_SUCCESS:
                    SparseArray<Group> groups = new SparseArray<>();
                    JSONArray array = service.response.get(actionId).getJSONObject("data").getJSONArray("groups");
                    service.response.remove(actionId);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject groupParsed = array.getJSONObject(i);
                        Group group = Group.getGroup(groupParsed);
                        groups.put(group.getId(), group);
                    }
                    Log.i(TAG, groups.toString());
                    return groups;
                case Client.GROUP_GET_ALL_DOES_NOT_EXIST:
                    return new SparseArray<>();
            }
        } catch (InvalidKeyException | JSONException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        }
        return new SparseArray<>();
    }

    public static boolean deleteGroup(ClientService service, Group group) {
        try {
            int actionId = getActionId(service);
            JSONObject request = request(actionId, "delete", group);
            if (connectAndWait(service, actionId, request)) {
                JSONObject obj = service.response.get(actionId);
                switch (obj.getInt("code")) {
                    case Client.GROUP_DELETE_SUCCESS:
                        return true;
                    default:
                        return false;
                }
            }
        } catch (InvalidKeyException e) {
            Log.e(TAG, "deleteGroup() error", e);
            return false;
        } catch (JSONException e) {
            Log.e(TAG, "deleteGroup() error", e);
            return false;
        }
        return false;
    }

    public static ClientResponse addGroup(ClientService service, Group group) {
        return updateAdd(service, group, "add");
    }

    public static ClientResponse editGroup(ClientService service, Group group) {
        return updateAdd(service, group, "edit");
    }

    private static ClientResponse updateAdd(ClientService service, Group group, String param) {
        try {
            int actionId = getActionId(service);
            JSONObject request = request(actionId, param, group);
            if (connectAndWait(service, actionId, request)) {
                JSONObject obj = service.response.get(actionId);
                int code = obj.getInt("code");
                String msg = obj.getString("message");
                switch (code) {
                    case Client.GROUP_EDIT_SUCCESS:
                    case Client.GROUP_ADD_SUCCESS:
                        JSONObject parsed = obj.getJSONObject("data").getJSONObject("group");
                        Group retrievedGroup = Group.getGroup(parsed);
                        service.response.remove(actionId);
                        return new ClientResponse(code, msg, retrievedGroup);
                    default:
                        return new ClientResponse(code, msg, group);
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

    private static boolean connectAndWait(ClientService service, int actionId, JSONObject request) throws InvalidKeyException {
        if (service.client == null || !service.client.isConnected())
            return false; //TODO: figure out how to wait for connection, if I do a while loop the thread is blocked, may need to thread this method further
        service.client.write(request.toString());
        while (!service.response.containsKey(actionId) && service.client.isConnected())
            ServiceUtils.sleep(10);
        return service.client.isConnected();
    }



}
