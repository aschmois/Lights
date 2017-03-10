package com.android305.lights.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android305.lights.LoginActivity;
import com.android305.lights.util.Group;
import com.android305.lights.util.client.Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.sql.Timestamp;
import java.util.HashMap;
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
    public final static int GROUPS_NEEDS_REFRESH = 3;
    public final static String FILTER = "com.android305.lights.ACTIVITY_UPDATE";

    protected Client client;

    private boolean handshake = false;
    private boolean handshakeFailed = false;
    private boolean invalidKey = false;
    private String mHost;
    private String mPassword;
    private String mSecretKey;
    private boolean authenticated = true;

    protected HashMap<Integer, JSONObject> response = new HashMap<>();

    public ClientService() {
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ClientService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ClientService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
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
                    ServiceUtils.sleep(100);
                if (invalidKey)
                    return ERROR_KEY_INVALID;
                if (handshakeFailed)
                    return ERROR_UNKNOWN;
                if (!client.isConnected())
                    return ERROR_HOST_INVALID;
                int actionId = getActionId();
                JSONObject write = new JSONObject();
                write.put("action_id", actionId);
                write.put("action", "authenticate");
                write.put("password", password);
                client.write(write.toString());
                while (!response.containsKey(actionId) && client.isConnected())
                    ServiceUtils.sleep(10);
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
        int auth = authenticate(mHost, mSecretKey, mPassword);
        if (auth == SUCCESS) {
            Intent i = new Intent(FILTER);
            i.putExtra(COMMAND, GROUPS_NEEDS_REFRESH);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
        return auth;
    }

    private int getActionId() {
        int actionId = ServiceUtils.randInt(ServiceUtils.DEFAULT);
        if (response.containsKey(actionId))
            return getActionId();
        return actionId;
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
                        JSONObject data = json.getJSONObject("data");
                        if (data.has("group")) {
                            i.putExtra(GROUP_EXTRA, Group.getGroup(data.getJSONObject("group")));
                        }
                        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                        break;
                }
            }
            if (LoginActivity.DEBUG) {
                boolean error = json.getBoolean("error");
                String message = json.getString("message");
                StringBuilder sb = new StringBuilder("\n");
                sb.append("Action ID: ");
                sb.append(actionId);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "Service is being destroyed");
        try {
            if (client != null) {
                client.getSession().close(true);
            }
        } catch (Exception ignored) {
        }
    }
}
