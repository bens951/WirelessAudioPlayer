package com.example.bluetooth;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainFragment extends Fragment implements OnItemClickListener {

    private static final boolean DEBUG = true;

    public static final int REQUEST_CODE_ENABLE_BT = 100001;

    private static final int MENU_MAKE_DEVICE_BT_DISCOVERABLE = 1;
    private static final int MENU_START_DISCOVERY             = 2;

    private BluetoothAdapter mBtAdaper;
    private ArrayList<BluetoothDevice> mPairedDevices = new ArrayList<BluetoothDevice>();
    private StateChangedReceiver mStateChangedReceiver;
    private ListView mListView;
    private PairedDeviceAdapter mDataAdapter;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            }
        }
        
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Utils.TAG, "MainFragment onCreate");
        mBtAdaper = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdaper == null) {
            Toast.makeText(getActivity(), "Device doesn't support bluetooth", Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        setHasOptionsMenu(true);
        Log.d(Utils.TAG, "bt enable = " + mBtAdaper.isEnabled());
        if (!mBtAdaper.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BT);
        }
        getActivity().registerReceiver(mBtFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        mListView = (ListView) v.findViewById(R.id.listview);
        if (mDataAdapter == null) {
            mDataAdapter = new PairedDeviceAdapter();
        }
        if (mListView != null) {
            mListView.setAdapter(mDataAdapter);
            mListView.setOnItemClickListener(this);
        }
        return v;
    }

    private void registerStateChangedReceiver() {
        if (mStateChangedReceiver == null) {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            mStateChangedReceiver = new StateChangedReceiver();
            getActivity().registerReceiver(mStateChangedReceiver, filter);
        }
    }

    private void unregisterStateChangedReceiver() {
        if (mStateChangedReceiver != null) {
            getActivity().unregisterReceiver(mStateChangedReceiver);
            mStateChangedReceiver = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(0, MENU_MAKE_DEVICE_BT_DISCOVERABLE, 0, "make discoverable");
        menu.add(0, MENU_START_DISCOVERY, 0, "start discovery");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_MAKE_DEVICE_BT_DISCOVERABLE:
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
                break;
            case MENU_START_DISCOVERY:
                if (mBtAdaper != null) {
                    mBtAdaper.startDiscovery();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class StateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Utils.TAG, "StateChangedReceiver onReceive " +
                    "previous state = " + intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF) +
                    ", current state = " + intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Utils.TAG, "onActivityResult requestCode = " + requestCode + ", resultCode = " + resultCode);
        switch (requestCode) {
            case REQUEST_CODE_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    // check result code
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBtAdaper != null) {
            mPairedDevices = new ArrayList<BluetoothDevice>(mBtAdaper.getBondedDevices());
            if (DEBUG) {
                for (BluetoothDevice device : mPairedDevices) {
                    Log.d(Utils.TAG, "device = " + device);
                }
            }
        }
    }

    private class PairedDeviceAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public PairedDeviceAdapter() {
            mInflater = getActivity().getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mPairedDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mPairedDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.item_device, null);
                holder = new ViewHolder();
                holder.name = (TextView) view.findViewById(R.id.textView_name);
                holder.address = (TextView) view.findViewById(R.id.textView_addr);
                holder.status = (TextView) view.findViewById(R.id.textView_status);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = (BluetoothDevice) getItem(position);
            holder.name.setText("Name: " + device.getName());
            holder.address.setText("Address: " + device.getAddress());
            holder.status.setText("Status: " + device.getBondState());
            return view;
        }

        private class ViewHolder {
            public TextView name, address, status;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerStateChangedReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterStateChangedReceiver();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mBtFoundReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver mBtFoundReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) return;
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(Utils.TAG, "mBtFoundReceiver = " + device);
                if (!mPairedDevices.contains(device)) {
                    mPairedDevices.add(device);
                    mDataAdapter.notifyDataSetChanged();
                }
            }
        }

    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        BluetoothDevice device = mPairedDevices.get(position);
        if (device != null) {
            CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox_server);
            if (cb.isChecked()) {
                ServerThread server = new ServerThread(mBtAdaper, mHandler);
                server.run();
            } else {
                ClientThread client = new ClientThread(device, mHandler);
                client.run();
            }
        }
        
    }

}
