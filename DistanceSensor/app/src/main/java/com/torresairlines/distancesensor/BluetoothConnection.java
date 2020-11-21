package com.torresairlines.distancesensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnection {

    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btsocket = null;
    private InputStream btInputStream = null;
    private OutputStream btOutputStream = null;
    private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8615-00AB8089E4BA");
    private static String initMessage = "wakeupmrpi";

    public boolean IsSupported()
    {
        return (bAdapter != null);
    }

    public boolean isConnected()
    {
        return (btsocket != null);
    }

    public Set<BluetoothDevice> GetBondedDevices()
    {
        return bAdapter.getBondedDevices();
    }

    public void Connect(String deviceMAC) {

        Log.d("DISTSENSOR: bluetooth", "Connect called with MAC: " + deviceMAC);

        // If there are paired devices
        Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();

        ArrayList list = new ArrayList();

        if(pairedDevices.size()>0) {
            for(BluetoothDevice device: pairedDevices){

                String macAddress = device.getAddress();

                if (deviceMAC.equals(macAddress)) {

                    BluetoothSocket tmp = null;
                    Log.d("DISTSENSOR: bluetooth", "Attempting to connect to " + macAddress);

                    try {
                        //Insecure
                         Method createRfcommSocket = device.getClass().getMethod("createRfcommSocket", int.class);
                         tmp = (BluetoothSocket) createRfcommSocket.invoke(device, 1);
                          //Secure
                          //tmp = device.createRfcommSocketToServiceRecord( SerialPortServiceClass_UUID );

                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (tmp != null) {
                        try {
                            tmp.connect();
                        } catch (IOException e1) {

                            Log.d("DISTSENSOR: bluetooth", "IOException on connect");
                             try {
                                tmp.close();
                             } catch (IOException e2) { }

                            continue;
                        }
                    }

                    btsocket = tmp;

                    if (btsocket != null) {
                        try {
                            btInputStream = btsocket.getInputStream();
                            btOutputStream = btsocket.getOutputStream();
                        } catch (IOException e) {
                            Log.d("DISTSENSOR: bluetooth", "IOException creating I/O streams");
                        }

                        byte[] initstring = null;

                        try {
                            initstring = initMessage.getBytes("UTF-8");

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            initstring = initMessage.getBytes();
                        }
                        Write(initstring);
                        Log.d("DISTSENSOR: bluetooth", "Writing out string to bluetooth: " + initstring);

                    }

                }
            }
        }
    }

    public int Write(byte[] bytes) {
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

    public int Read(byte[] buffer) {
        int n = 0;

        if (btInputStream != null) {
            try {
                n = btInputStream.read(buffer, 0, buffer.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return n;
    }

}
