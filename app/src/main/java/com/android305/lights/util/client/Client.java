package com.android305.lights.util.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android305.lights.LoginActivity;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import java.net.InetSocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.sql.Timestamp;

public class Client {
    private final static String TAG = "Client";

    public final static int ERROR_UNKNOWN = 1;

    public final static int AUTH_SUCCESS = 1000;
    public final static int ERROR_FAILED_AUTHENTICATION = 1001;
    public final static int ERROR_NOT_AUTHENTICATED = 1002;

    /* Group */
    public final static int GROUP_ERROR_USAGE = 2000;
    public final static int GROUP_SQL_ERROR = 2001;
    public final static int GROUP_REFRESH = 2002;

    public final static int GROUP_ADD_SUCCESS = 2100;
    public final static int GROUP_ALREADY_EXISTS = 2101;

    public final static int GROUP_GET_SUCCESS = 2200;
    public final static int GROUP_GET_DOES_NOT_EXIST = 2201;

    public final static int GROUP_GET_ALL_SUCCESS = 2300;
    public final static int GROUP_GET_ALL_DOES_NOT_EXIST = 2301;

    /* Lamp */
    public final static int LAMP_SQL_ERROR = 3000;

    public final static int LAMP_ADD_SUCCESS = 3100;
    public final static int LAMP_ALREADY_EXISTS = 3101;

    public final static int LAMP_GET_SUCCESS = 3200;
    public final static int LAMP_GET_DOES_NOT_EXIST = 3201;

    public final static int LAMP_TOGGLE_SUCCESS = 3300;
    public final static int LAMP_TOGGLE_DOES_NOT_EXIST = 3301;

    public interface ClientInterface {
        void handshake(@Nullable Timestamp serverTime);

        void messageReceived(String msg);

        void encryptionError();

        void onDisconnect();
    }

    public static class ServerConnectException extends Exception {
        public ServerConnectException() {
            super("Connection to server could not be completed.");
        }
    }

    public final static int DEFAULT_PORT = 7123;

    private final int port;
    private final String ip;
    private final String secretKey;

    private final ClientInterface mCallback;
    private ClientSessionHandler h;
    private IoSession session;

    /**
     * This method just sets up the connection, call {@link #connect()} to create a connection
     *
     * @param port      server's port
     * @param ip        server's ip address
     * @param secretKey server's encryption key
     * @param mCallback the connection callback
     */
    public Client(int port, @NonNull String ip, @NonNull String secretKey, @NonNull ClientInterface mCallback) {
        this.port = port;
        this.ip = ip;
        this.secretKey = secretKey;
        this.mCallback = mCallback;
    }

    public void connect() throws ServerConnectException {
        final NioSocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(3000);
        TextLineCodecFactory factory = new TextLineCodecFactory(Charset.forName("UTF-8"));
        factory.setEncoderMaxLineLength(8192);
        factory.setDecoderMaxLineLength(8192);
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(factory));
        h = new ClientSessionHandler(mCallback, secretKey);
        connector.setHandler(h);
        if (LoginActivity.DEBUG)
            Log.i(TAG, "Connecting to " + ip + ":" + port);
        try {
            ConnectFuture future = connector.connect(new InetSocketAddress(ip, port));
            future.awaitUninterruptibly();
            session = future.getSession();
        } catch (RuntimeIoException | UnresolvedAddressException e) {
            if (LoginActivity.DEBUG)
                Log.e(TAG, "No connection.", e);
        }
        if (session != null) {
            new Thread() {
                public void run() {
                    session.getCloseFuture().awaitUninterruptibly();
                    connector.dispose();
                    mCallback.onDisconnect();
                    session = null;
                }
            }.start();
        } else {
            connector.dispose();
            throw new ServerConnectException();
        }
    }


    public boolean isConnected() {
        return session != null && session.isConnected() && !session.isClosing();
    }

    public IoSession getSession() {
        return session;
    }

    public void write(String message) throws InvalidKeyException {
        try {
            session.write(h.encryption.encrypt(message));
        } catch (EncryptionOperationNotPossibleException e) {
            if (LoginActivity.DEBUG)
                Log.e(TAG, "Encryption error", e);
        }
    }
}
