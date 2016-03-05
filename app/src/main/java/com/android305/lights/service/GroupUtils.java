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

}
