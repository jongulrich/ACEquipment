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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ListView lstvw;
    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<String> bluetoothDevices = new ArrayList<String>();
    private BluetoothSocket btsocket = null;
    private InputStream btInputStream = null;
    private OutputStream btOutputStream = null;
    private int mCalibrationOffset = 0;
    private int mCurrentSensorDistance = 0;
    //for secure connections// private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8615-00AB8089E4BA");
    private static String initMessage = "wakeupmrpi";
    private int mRandomVal = -1;


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
           // Toast.makeText(getApplicationContext(),"Listing Devices",Toast.LENGTH_SHORT).show();

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
                //Toast.makeText(getApplicationContext(),"Selected : " + item,Toast.LENGTH_SHORT).show();
                BTConnect(bluetoothDevices.get(i));
            }
        });


        /*
            Calibration Buttons
         */

        final Button btnCalibrate = findViewById(R.id.btnCalibrate);
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ZeroizeCurrentDistance();
            }
        });
        final Button btnResetCalibration = findViewById(R.id.btnResetCalibration);
        btnResetCalibration.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SetCalibrationOffset(0);
            }
        });

        /*
            Update the GUI
         */

        Thread thread = new Thread() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Keep listening to the InputStream while connected
                    if (btsocket != null && btInputStream != null) {
                        try {
                            // Read from the InputStream
                            bytes = btInputStream.read(buffer);

                            int parsedNum = ParseIntegerFromBytes(buffer, bytes);
                            //TODO: parsedNum validation
                            SetCurrentDistance(parsedNum);

                        } catch (IOException e) {
                            connectionLost();
                        }
                    } else {
                        mRandomVal--;
                        SetCurrentDistance(-999+mRandomVal);
                    }
                    updateCurrentDistanceText(Integer.toString(GetCurrentDistance()));
                }
            }
        };

        thread.start();

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

                    if (btsocket != null) {
                        try {
                            btInputStream = btsocket.getInputStream();
                            btOutputStream = btsocket.getOutputStream();
                        } catch (IOException e) {
                        }

                        byte[] initstring = null;

                        try {
                            initstring = initMessage.getBytes("US-ASCII");
                            BTWrite(initstring);

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            initstring = initMessage.getBytes();
                        }
                        BTWrite(initstring);


                    }

                }
            }
        }
    }

    private int BTWrite(byte[] bytes) {
        int n = 0;

        if (btOutputStream != null && bytes.length > 0)
        {
            try {
                btOutputStream.write(bytes);
                n = bytes.length;
            } catch (IOException e) {
            }
        }
        return n;
    }

    private int ParseIntegerFromBytes(byte[] bytes, int len)  {
        int offset = 0;
        ByteBuffer wrapped = ByteBuffer.wrap(bytes,offset,len);
        return wrapped.getInt();
    }

    private void connectionLost() {
        //Toast.makeText(getApplicationContext(),"Connection Lost" ,Toast.LENGTH_SHORT).show();
    }

    private void ZeroizeCurrentDistance()
    {
        SetCalibrationOffset(-1 * mCurrentSensorDistance);
    }

    private void SetCalibrationOffset(int offset)
    {
       // Toast.makeText(getApplicationContext(),"Calibration offset set to " + offset,Toast.LENGTH_SHORT).show();
        mCalibrationOffset = offset;

        //Update GUI
        TextView textView = (TextView) findViewById(R.id.tvCalibrationOffset);
        textView.setText(Integer.toString(offset));
        textView.setTextColor(Color.BLACK);

    }
    private void SetCurrentDistance(int sensorDistance)
    {
        mCurrentSensorDistance = sensorDistance;
    }
    private int GetCurrentDistance()
    {
        int adjustedDistance = (mCurrentSensorDistance + mCalibrationOffset);
        return adjustedDistance;
    }
}