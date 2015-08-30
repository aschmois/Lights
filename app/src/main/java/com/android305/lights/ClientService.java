package com.android305.lights;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;

import com.android305.lights.util.Group;
import com.android305.lights.util.Lamp;
import com.android305.lights.util.client.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientService extends Service implements Client.ClientInterface {
    private final static String TAG = "ClientService";
    private final static String hostRegex = "(?:(\\d+\\.\\d+\\.\\d+\\.\\d+)(?::(\\d+))?)?(?:([A-z0-9.]+)(?::(\\d+))?)?";

    public final static int ERROR_UNKNOWN = -1;
    public final static int SUCCESS = 0;
    public final static int ERROR_KEY_INVALID = 1;
    public final static int ERROR_PASSWORD_INVALID = 2;
    public final static int ERROR_HOST_INVALID = 3;

    public static final String GROUP_EXTRA = "lamp_extra";
    public final static String COMMAND = "COMMAND";
    public final static int LOST_CONNECTION = 1;
    public final static int GROUP_NEEDS_REFRESH = 2;
    public final static String FILTER = "com.android305.lights.ACTIVITY_UPDATE";

    private Client client;

    private boolean handshake = false;
    private boolean handshakeFailed = false;
    private boolean invalidKey = false;
    private String mHost;
    private String mPassword;
    private String mSecretKey;
    private boolean authenticated = true;

    private HashMap<Integer, JSONObject> response = new HashMap<>();

    public ClientService() {
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ClientService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ClientService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int authenticate(String host, String sKey, String password) {
        handshake = false;
        handshakeFailed = false;
        invalidKey = false;
        Pattern pattern = Pattern.compile(hostRegex);
        Matcher matcher = pattern.matcher(host);
        String connectTo = null;
        int port = 0;
        if (matcher.matches()) {
            String ip = matcher.group(1);
            String p = matcher.group(2);
            String domain = matcher.group(3);
            String domainPort = matcher.group(4);
            if (ip != null) {
                connectTo = ip;
                if (p != null)
                    port = Integer.parseInt(p);
            } else if (domain != null) {
                connectTo = domain;
                if (domainPort != null)
                    port = Integer.parseInt(domainPort);
            }
        }
        if (connectTo != null) {
            if (port == 0)
                port = Client.DEFAULT_PORT;
            client = new Client(port, connectTo, sKey, this);
            try {
                client.connect();
                while (!handshake && !handshakeFailed && !invalidKey && client.isConnected())
                    sleep(100);
                if (!client.isConnected())
                    return ERROR_HOST_INVALID;
                if (invalidKey)
                    return ERROR_KEY_INVALID;
                if (handshakeFailed)
                    return ERROR_UNKNOWN;
                int actionId = getActionId();
                JSONObject write = new JSONObject();
                write.put("action_id", actionId);
                write.put("action", "authenticate");
                write.put("password", password);
                client.write(write.toString());
                while (!response.containsKey(actionId) && client.isConnected())
                    sleep(10);
                if (!client.isConnected())
                    return ERROR_HOST_INVALID;
                int code = response.get(actionId).getInt("code");
                response.remove(actionId);
                switch (code) {
                    case Client.AUTH_SUCCESS:
                        authenticated = true;
                        break;
                    case Client.ERROR_FAILED_AUTHENTICATION:
                        return ERROR_PASSWORD_INVALID;
                }
                mHost = host;
                mPassword = password;
                mSecretKey = sKey;
                return SUCCESS;
            } catch (Client.ServerConnectException e) {
                if (LoginActivity.DEBUG)
                    Log.e(TAG, "Server Error", e);
                return ERROR_HOST_INVALID;
            } catch (JSONException e) {
                return ERROR_UNKNOWN;
            } catch (InvalidKeyException e) {
                return ERROR_KEY_INVALID;
            }
        }
        return ERROR_UNKNOWN;
    }

    public int reconnect() {
        if (mHost == null)
            return ERROR_HOST_INVALID;
        if (mPassword == null)
            return ERROR_PASSWORD_INVALID;
        if (mSecretKey == null)
            return ERROR_KEY_INVALID;
        return authenticate(mHost, mSecretKey, mPassword);
    }

    private int getActionId() {
        int actionId = new Random().nextInt(5000);
        if (response.containsKey(actionId))
            return getActionId();
        return actionId;
    }

    public SparseArray<Group> getGroups() {
        try {
            int actionId = getActionId();
            JSONObject write = new JSONObject();
            write.put("action_id", actionId);
            write.put("action", "group");
            write.put("secondary_action", "get-all");
            client.write(write.toString());
            while (!response.containsKey(actionId) && client.isConnected())
                sleep(10);
            if (!client.isConnected())
                return null;
            switch (response.get(actionId).getInt("code")) {
                case Client.GROUP_GET_ALL_SUCCESS:
                    SparseArray<Group> groups = new SparseArray<>();
                    JSONArray array = response.get(actionId).getJSONObject("data").getJSONArray("groups");
                    response.remove(actionId);
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
        } catch (InvalidKeyException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "getGroups() error", e);
            return null;
        }
        return new SparseArray<>();
    }

    public Lamp toggleLamp(Lamp lamp) {
        try {
            int actionId = getActionId();
            JSONObject write = new JSONObject();
            write.put("action_id", actionId);
            write.put("action", "lamp");
            write.put("secondary_action", "toggle");
            write.put("lamp", lamp.getParsed());
            client.write(write.toString());
            while (!response.containsKey(actionId) && client.isConnected())
                sleep(10);
            if (!client.isConnected())
                return null;
            switch (response.get(actionId).getInt("code")) {
                case Client.LAMP_TOGGLE_SUCCESS:
                    JSONObject parsed = response.get(actionId).getJSONObject("data").getJSONObject("lamp");
                    Lamp retrievedLamp = Lamp.getLamp(parsed);
                    response.remove(actionId);
                    return retrievedLamp;
                case Client.LAMP_TOGGLE_DOES_NOT_EXIST:
                    return null;
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

    public boolean isConnected() {
        return authenticated && client != null && client.isConnected();
    }

    @Override
    public void encryptionError() {
        invalidKey = true;
    }

    @Override
    public void handshake(@Nullable Timestamp serverTime) {
        if (serverTime != null) {
            handshake = true;
            handshakeFailed = false;
        } else {
            handshake = false;
            handshakeFailed = true;
        }
    }

    @Override
    public void messageReceived(String msg) {
        try {
            JSONObject json = new JSONObject(msg);
            int actionId = json.getInt("action_id");
            int code = json.getInt("code");
            if (actionId != -1) {
                response.put(actionId, json);
            } else {
                switch (code) {
                    case Client.GROUP_REFRESH:
                        Intent i = new Intent(FILTER);
                        i.putExtra(COMMAND, GROUP_NEEDS_REFRESH);
                        i.putExtra(GROUP_EXTRA, Group.getGroup(json.getJSONObject("group")));
                        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                        break;
                }
            }
            if (LoginActivity.DEBUG) {
                boolean error = json.getBoolean("error");
                String message = json.getString("message");
                String original = json.getString("original");
                StringBuilder sb = new StringBuilder("\n");
                sb.append("Action ID: ");
                sb.append(actionId);
                sb.append("\n");
                sb.append("Original: ");
                sb.append(original);
                sb.append("\n");
                sb.append("Error: ");
                sb.append(error);
                sb.append(". Code: ");
                sb.append(code);
                sb.append("\n");
                sb.append("Message: ");
                sb.append(message);
                Log.v(TAG, sb.toString());
            }
        } catch (JSONException e) {
            if (LoginActivity.DEBUG)
                Log.e(TAG, "Error retrieving message: " + msg, e);
        }
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "Broadcasting disconnect message");
        Intent intent = new Intent(FILTER);
        intent.putExtra(COMMAND, LOST_CONNECTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
