package com.android305.lights.util.client;

import android.util.Log;

import com.android305.lights.LoginActivity;
import com.android305.lights.util.encryption.Encryption;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ClientSessionHandler extends IoHandlerAdapter {
    private final static String TAG = "ClientSessionHandler";
    Client.ClientInterface mCallback;

    private boolean handshake = false;
    private boolean pong = false;
    protected final Encryption encryption;

    public ClientSessionHandler(Client.ClientInterface mCallback, String secretKey) {
        this.mCallback = mCallback;
        this.encryption = new Encryption(secretKey);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        if (LoginActivity.DEBUG)
            Log.e(TAG, "Client exception caught", cause);
        session.close(true);
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        try {
            String msg = dec(message.toString());
            if (!handshake) {
                if (LoginActivity.DEBUG)
                    Log.d(TAG, msg);
                try {
                    DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.US);
                    mCallback.handshake(new Timestamp(format.parse(msg).getTime()));
                    if (LoginActivity.DEBUG)
                        Log.d(TAG, "Handshake Successful");
                    handshake = true;
                    keepAliveThread(session);
                } catch (ParseException e) {
                    if (LoginActivity.DEBUG)
                        Log.e(TAG, "Handshake Failed", e);
                    mCallback.handshake(null);
                    session.close(true);
                }
            } else {
                if (!msg.equals("pong")) {
                    if (LoginActivity.DEBUG)
                        Log.d(TAG, msg);
                    mCallback.messageReceived(msg);
                } else {
                    pong = true;
                }
            }
        } catch (EncryptionOperationNotPossibleException e) {
            if (LoginActivity.DEBUG)
                Log.e(TAG, "Encryption error", e);
            mCallback.encryptionError();
            session.close(true);
        }
    }

    private String dec(String msg) throws EncryptionOperationNotPossibleException {
        return encryption.decrypt(msg);
    }

    private String enc(String msg) throws EncryptionOperationNotPossibleException {
        return encryption.encrypt(msg);
    }

    @Override
    public void sessionOpened(IoSession session) {
        if (LoginActivity.DEBUG)
            Log.d(TAG, "Connection established");
    }

    private void keepAliveThread(final IoSession session) {
        new Thread() {
            public void run() {
                while (session != null && session.isConnected() && !session.isClosing()) {
                    try {
                        WriteFuture future = session.write(enc("ping"));
                        future.awaitUninterruptibly(1000);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                        if (pong) {
                            pong = false;
                        } else {
                            break;
                        }
                    } catch (EncryptionOperationNotPossibleException e) {
                        if (LoginActivity.DEBUG)
                            Log.e(TAG, "Encryption error", e);
                        break;
                    }
                }
                if (session != null)
                    session.close(true);
                if (LoginActivity.DEBUG)
                    Log.d(TAG, "Lost connection to server");
            }
        }.start();
    }
}