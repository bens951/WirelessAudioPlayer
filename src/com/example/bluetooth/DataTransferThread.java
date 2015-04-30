package com.example.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

class DataTransferThread extends Thread {
    private final BluetoothSocket mSocket;
    private Handler mHandler;

    public DataTransferThread(BluetoothSocket socket, Handler handler) {
        this.mSocket = socket;
        this.mHandler = handler;
    }

    public void run() {
        try {
            InputStream inputStream = mSocket.getInputStream();
            boolean waitingForHeader = true;
            ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
            byte[] headerBytes = new byte[22];
            byte[] digest = new byte[16];
            int headerIndex = 0;
            ProgressData progressData = new ProgressData();

            while (true) {
                if (waitingForHeader) {
                    byte[] header = new byte[1];
                    inputStream.read(header, 0, 1);
                    Log.v(Utils.TAG, "Received Header Byte: " + header[0]);
                    headerBytes[headerIndex++] = header[0];

                    if (headerIndex == 22) {
                        if ((headerBytes[0] == Constants.HEADER_MSB) && (headerBytes[1] == Constants.HEADER_LSB)) {
                            Log.v(Utils.TAG, "Header Received.  Now obtaining length");
                            byte[] dataSizeBuffer = Arrays.copyOfRange(headerBytes, 2, 6);
                            progressData.totalSize = Utils.byteArrayToInt(dataSizeBuffer);
                            progressData.remainingSize = progressData.totalSize;
                            Log.v(Utils.TAG, "Data size: " + progressData.totalSize);
                            digest = Arrays.copyOfRange(headerBytes, 6, 22);
                            waitingForHeader = false;
                            sendProgress(progressData);
                        } else {
                            Log.e(Utils.TAG, "Did not receive correct header.  Closing socket");
                            mSocket.close();
                            mHandler.sendEmptyMessage(MessageType.INVALID_HEADER);
                            break;
                        }
                    }

                } else {
                    // Read the data from the stream in chunks
                    byte[] buffer = new byte[Constants.CHUNK_SIZE];
                    Log.v(Utils.TAG, "Waiting for data.  Expecting " + progressData.remainingSize + " more bytes.");
                    int bytesRead = inputStream.read(buffer);
                    Log.v(Utils.TAG, "Read " + bytesRead + " bytes into buffer");
                    dataOutputStream.write(buffer, 0, bytesRead);
                    progressData.remainingSize -= bytesRead;
                    sendProgress(progressData);

                    if (progressData.remainingSize <= 0) {
                        Log.v(Utils.TAG, "Expected data has been received.");
                        break;
                    }
                }
            }

            // check the integrity of the data
            final byte[] data = dataOutputStream.toByteArray();

            if (Utils.digestMatch(data, digest)) {
                Log.v(Utils.TAG, "Digest matches OK.");
                Message message = new Message();
                message.obj = data;
                message.what = MessageType.DATA_RECEIVED;
                mHandler.sendMessage(message);

                // Send the digest back to the client as a confirmation
                Log.v(Utils.TAG, "Sending back digest for confirmation");
                OutputStream outputStream = mSocket.getOutputStream();
                outputStream.write(digest);

            } else {
                Log.e(Utils.TAG, "Digest did not match.  Corrupt transfer?");
                mHandler.sendEmptyMessage(MessageType.DIGEST_DID_NOT_MATCH);
            }

            Log.v(Utils.TAG, "Closing server socket");
            mSocket.close();

        } catch (Exception ex) {
            Log.d(Utils.TAG, ex.toString());
        }
    }

    private void sendProgress(ProgressData progressData) {
        Message message = new Message();
        message.obj = progressData;
        message.what = MessageType.DATA_PROGRESS_UPDATE;
        mHandler.sendMessage(message);
    }
}
