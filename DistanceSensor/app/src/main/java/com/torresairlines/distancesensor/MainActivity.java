package com.torresairlines.distancesensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ListView lstvw;
    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<String> bluetoothDevices = new ArrayList<String>();
    private BluetoothSocket btsocket = null;
    private InputStream btInputStream = null;
    private OutputStream btOutputStream = null;
    private int mCurrentDistance = 0;
    //for secure connections// private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8615-00AB8089E4BA");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateCurrentDistanceText("Initializing");

        /*
            List out bluetooth devices
         */
        if(bAdapter==null){
            Toast.makeText(getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getApplicationContext(),"Listing Devices",Toast.LENGTH_SHORT).show();

            Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
            ArrayList list = new ArrayList();
            if(pairedDevices.size()>0){
                for(BluetoothDevice device: pairedDevices){
                    String devicename = device.getName();
                    String macAddress = device.getAddress();
                    list.add("Name: "+devicename+"MAC Address: "+macAddress);
                    bluetoothDevices.add(device.getAddress());
                }
                lstvw = (ListView) findViewById(R.id.deviceList);
                aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                lstvw.setAdapter(aAdapter);
            }
        }

        /*
            On Bluetooth Selection
         */

        // Set a click listener for ListView items
        lstvw = (ListView) findViewById(R.id.deviceList);
        lstvw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Get selected item text
                String item = (String) adapterView.getItemAtPosition(i);
                // Display the selected item
                Toast.makeText(getApplicationContext(),"Selected : " + item,Toast.LENGTH_SHORT).show();
                BTConnect(bluetoothDevices.get(i));
            }
        });

        /*
            Update the GUI
         */
        Handler myHandler = new Handler();
        int delay = 1000; // 1000 milliseconds == 1 second

        myHandler.postDelayed(new Runnable() {
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;

                // Keep listening to the InputStream while connected
                if(btsocket != null &&  btInputStream != null) {
                    try {
                        // Read from the InputStream
                        bytes = btInputStream.read(buffer);

                        //TODO:: Parse Message
                        mCurrentDistance = 1;

                    } catch (IOException e) {
                        connectionLost();
                    }
                }
                else
                {
                    mCurrentDistance = -999;
                }
                updateCurrentDistanceText(Integer.toString(mCurrentDistance));
            }
        }, delay);

    }

    public void updateCurrentDistanceText(String toThis) {
        TextView textView = (TextView) findViewById(R.id.tvDistanceValue);
        textView.setText(toThis);
        textView.setTextColor(Color.GREEN);
    }

    public void BTConnect(String deviceMAC) {

        // If there are paired devices
        Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();

        ArrayList list = new ArrayList();

        if(pairedDevices.size()>0) {
            for(BluetoothDevice device: pairedDevices){

                String macAddress = device.getAddress();

                if (deviceMAC.equals(macAddress)) {

                    BluetoothSocket tmp = null;

                    try {
                        Method method = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class } );
                        tmp = (BluetoothSocket) method.invoke(device, 1);

                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    try {
                        tmp.connect();
                    } catch (IOException e1) {
                        try {
                            tmp.close();
                        } catch (IOException e2) {
                        }
                    }
                    btsocket = tmp;
                    try {
                        btInputStream = btsocket.getInputStream();
                        btOutputStream = btsocket.getOutputStream();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }



    private void connectionLost() {
        Toast.makeText(getApplicationContext(),"Connection Lost" ,Toast.LENGTH_SHORT).show();
    }

}