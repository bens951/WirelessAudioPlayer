package com.example.bluetooth;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BlueToothPairedDeviceController {
    private static BlueToothPairedDeviceController sInstance;
    private static ArrayList<BluetoothDevice> mDevices;

    public static BlueToothPairedDeviceController getInstance() {
        if (sInstance == null) {
            mDevices = new ArrayList<BluetoothDevice>();
            sInstance = new BlueToothPairedDeviceController();
        }
        return sInstance;
    }

    public ArrayList<BluetoothDevice> getList() {
        return mDevices;
    }

    public int getListSize() {
        return mDevices.size();
    }

    public boolean addDevice(BluetoothDevice device) {
        if (!mDevices.contains(device)) {
            Log.d(Utils.TAG, "[BlueToothPairedDeviceController] addDevice device = " + device);
            return mDevices.add(device);
        }
        return false;
    }

    public boolean removeDevice(BluetoothDevice device) {
        if (mDevices.contains(device)) {
            Log.d(Utils.TAG, "[BlueToothPairedDeviceController] removeDevice device = " + device);
            return mDevices.remove(device);
        }
        return false;
    }

}
