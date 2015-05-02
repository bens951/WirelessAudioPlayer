package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ClientThread extends Thread {
    private final BluetoothSocket mSocket;
    private final Handler mHandler;
    public Handler mIncomingHandler;

    public ClientThread(BluetoothDevice device, Handler handler) {
        BluetoothSocket tempSocket = null;
        this.mHandler = handler;

        try {
            tempSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(Constants.UUID_STRING));
        } catch (Exception e) {
            Log.e(Utils.TAG, e.toString());
        }
        this.mSocket = tempSocket;
    }

    public void run() {
        try {
            Log.v(Utils.TAG, "Opening client socket");
            mSocket.connect();
            Log.v(Utils.TAG, "Connection established");

        } catch (IOException ioe) {
            mHandler.sendEmptyMessage(MessageType.COULD_NOT_CONNECT);
            Log.e(Utils.TAG, ioe.toString());
            try {
                mSocket.close();
            } catch (IOException ce) {
                Log.e(Utils.TAG, "Socket close exception: " + ce.toString());
            }
        }

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mIncomingHandler = new Handler(){
            @Override
            public void handleMessage(Message message)
            {
                if (message.obj != null)
                {
                    Log.v(Utils.TAG, "Handle received data to send");
                    byte[] payload = (byte[])message.obj;

                    try {
                        mHandler.sendEmptyMessage(MessageType.SENDING_DATA);
                        OutputStream outputStream = mSocket.getOutputStream();

                        // Send the header control first
                        outputStream.write(Constants.HEADER_MSB);
                        outputStream.write(Constants.HEADER_LSB);

                        // write size
                        outputStream.write(Utils.intToByteArray(payload.length));

                        // write digest
                        byte[] digest = Utils.getDigest(payload);
                        outputStream.write(digest);

                        // now write the data
                        outputStream.write(payload);
                        outputStream.flush();

                        Log.v(Utils.TAG, "Data sent.  Waiting for return digest as confirmation");
                        InputStream inputStream = mSocket.getInputStream();
                        byte[] incomingDigest = new byte[16];
                        int incomingIndex = 0;

                        try {
                            while (true) {
                                byte[] header = new byte[1];
                                inputStream.read(header, 0, 1);
                                incomingDigest[incomingIndex++] = header[0];
                                if (incomingIndex == 16) {
                                    if (Utils.digestMatch(payload, incomingDigest)) {
                                        Log.v(Utils.TAG, "Digest matched OK.  Data was received OK.");
                                        ClientThread.this.mHandler.sendEmptyMessage(MessageType.DATA_SENT_OK);
                                    } else {
                                        Log.e(Utils.TAG, "Digest did not match.  Might want to resend.");
                                        ClientThread.this.mHandler.sendEmptyMessage(MessageType.DIGEST_DID_NOT_MATCH);
                                    }

                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            Log.e(Utils.TAG, ex.toString());
                        }

                        Log.v(Utils.TAG, "Closing the client socket.");
                        mSocket.close();

                    } catch (Exception e) {
                        Log.e(Utils.TAG, e.toString());
                    }

                }
            }
        };

        mHandler.sendEmptyMessage(MessageType.READY_FOR_DATA);
        Looper.loop();
    }

    public void sendData(byte[] data) {
        Message m = new Message();
        m.obj = data;
        mIncomingHandler.sendMessage(m);
    }

    public void cancel() {
        try {
            if (mSocket.isConnected()) {
                mSocket.close();
            }
        } catch (Exception e) {
            Log.e(Utils.TAG, e.toString());
        }
    }
}