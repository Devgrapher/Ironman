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
                connect_thread_ = new ConnectThread(device_name_map_.get(name));
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

        //ClearDeviceList();
        UpdateBondedDevices();
        DiscorverDevices();
    }

    private void UpdateBondedDevices() {
        paired_devices_ = bluetooth_.getBondedDevices();
        ArrayList list = new ArrayList();

        for(BluetoothDevice device : paired_devices_) {
            Log.d(TAG, "bonded devices : " + device.getName() + "\n" + device.getAddress());
            list.add(device.getName() + "\n" + device.getAddress());
        }
        if (!list.isEmpty()) {
            listview_adapter_.add(list);
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
                String name = device.getName() + "\n" + device.getAddress();
                Log.d(TAG, "found devices : " + name);
                // Add the name and address to an array adapter to show in a ListView
                listview_adapter_.add(name);
                device_name_map_.put(name, device);
            }
        }
    };

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

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
            } catch (IOException e) { }
            socket_ = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetooth_.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket_.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    socket_.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(socket_);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                socket_.close();
            } catch (IOException e) { }
        }
    }
}
