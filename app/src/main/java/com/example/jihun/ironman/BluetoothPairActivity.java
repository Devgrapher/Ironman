package com.example.jihun.ironman;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;


public class BluetoothPairActivity extends Activity {
    private static final String TAG = "ironman.bluetooth";
    private ListView listview_devices_;
    private Button device_refresh_;
    private Set<BluetoothDevice> paired_devices_;
    private BluetoothAdapter bluetooth_;
    private ArrayAdapter listview_adapter_;
    private HashMap<String, BluetoothDevice> device_name_map_;
    private ConnectThread connect_thread_;

    //private UUID uuid_ = UUID.randomUUID();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pair);

        listview_devices_ = (ListView) findViewById(R.id.listViewBluetoothDevices);
        device_refresh_ = (Button) findViewById(R.id.buttonBluetoothDeviceRefresh);
        device_refresh_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateDeviceListView();
            }
        });
        bluetooth_ = BluetoothAdapter.getDefaultAdapter();

        listview_adapter_ = new ArrayAdapter(this,android.R.layout.simple_list_item_1);
        listview_devices_.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = listview_devices_.getItemAtPosition(position).toString();
                Log.d(TAG, "Connect name : " + name);
                BluetoothDevice device = device_name_map_.get(name);
                if (device == null) {
                    Toast.makeText(getApplicationContext(),"Can't find the device!",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                connect_thread_ = new ConnectThread(device_name_map_.get(name));
                connect_thread_.start();
            }
        });

        listview_devices_.setAdapter(listview_adapter_);
        device_name_map_ = new HashMap();
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdateDeviceListView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver_);
        if (connect_thread_ != null) {
            connect_thread_.cancel();
            try {
                connect_thread_.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void ClearDeviceList() {
        listview_adapter_.clear();
        device_name_map_.clear();
    }

    public void UpdateDeviceListView() {
        if (!bluetooth_.isEnabled()) {
            Toast.makeText(getApplicationContext(),"bluetooth is not enabled",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ClearDeviceList();
        UpdateBondedDevices();
        DiscorverDevices();
    }

    private void UpdateBondedDevices() {
        paired_devices_ = bluetooth_.getBondedDevices();

        for(BluetoothDevice device : paired_devices_) {
            Log.d(TAG, "bonded devices : " + device.getName());
            AddDevice(device);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver receiver_ = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "found devices : " +  device.getName());
                // Add the name and address to an array adapter to show in a ListView
                AddDevice(device);
            }
        }
    };

    private void AddDevice(BluetoothDevice device) {
        String name = device.getName() + "\n" + device.getAddress();
        if (!device_name_map_.containsKey(name)) {
            listview_adapter_.add(name);
        }
        // Even if the map already has the device, put the object again to have the new device object
        device_name_map_.put(name, device);
    }

    private void DiscorverDevices() {
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver_, filter); // Don't forget to unregister during onDestroy

        if (!bluetooth_.startDiscovery()) {
            Log.e(TAG, "startDiscovery failed");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_pair, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket_;
        private final BluetoothDevice device_;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to socket_,
            // because socket_ is final
            BluetoothSocket tmp = null;
            device_ = device;

            // The uuid that I want to connect to.
            // This value of uuid is for Serial Communication.
            // http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord(java.util.UUID)
            // https://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                tmp = device_.createRfcommSocketToServiceRecord(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket_ = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetooth_.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d(TAG, "Connect...");
                socket_.connect();
            } catch (IOException connectException) {
                connectException.printStackTrace();
                // Unable to connect; close the socket and get out
                try {
                    socket_.close();
                } catch (IOException closeException) { }
                return;
            }

            Log.d(TAG, "Connected");

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                }
            });

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(socket_);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                socket_.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
