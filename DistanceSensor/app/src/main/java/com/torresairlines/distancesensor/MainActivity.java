package com.torresairlines.distancesensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ListView lstvw;
    private ArrayAdapter aAdapter;
    private ArrayList<String> bluetoothDevices = new ArrayList<String>();

    private int mCalibrationOffset = 0;
    private int mCurrentSensorDistance = 0;

    private int mRandomVal = -1;
    private boolean once = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        BluetoothConnection BTConnection = new BluetoothConnection();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateCurrentDistanceText("Initializing");

        /*
            List out bluetooth devices
         */
        if(!BTConnection.IsSupported()){
            Toast.makeText(getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
        }
        else {
           // Toast.makeText(getApplicationContext(),"Listing Devices",Toast.LENGTH_SHORT).show();

            Set<BluetoothDevice> pairedDevices = BTConnection.GetBondedDevices();
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
                BTConnection.Connect(bluetoothDevices.get(i));
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

                while (!isInterrupted()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (BTConnection.isConnected()) {
                        // Read from the InputStream

                        String reqdata = ".";
                        try {
                            BTConnection.Write(reqdata.getBytes("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        int nbytes = BTConnection.Read(buffer);

                        if (nbytes>0) {
                            Log.d("DISTSENSOR: main", "reading bluetooth data: " + nbytes);
                            int parsedNum = ParseIntegerFromBytes(buffer, nbytes - 1);
                            //TODO: parsedNum validation
                            SetCurrentDistance(parsedNum);
                        } else {
                            if (once)
                            {
                                Log.d("DISTSENSOR: main", "reading blank bluetooth data");
                                once = false;
                            }
                        }
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



    private int ParseIntegerFromBytes(byte[] bytes, int len)  {
        int offset = 0;
        String strValue = new String(bytes, StandardCharsets.UTF_8);
        int iend = strValue.indexOf(".");
        if (iend != -1)
        {
            strValue= strValue.substring(0 , iend); //this will give abc
        }
        return Integer.parseInt(strValue);
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