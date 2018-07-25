package com.example.asus.project_car;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by asus on 2018/7/12.
 * This is a Client
 */

class ConnectThread extends Thread {
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//一定要是這組，範例說的

    public ConnectThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter){
        this.device=device;
        this.bluetoothAdapter=bluetoothAdapter;
        BluetoothSocket tmp;
        try{
            tmp=device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            socket=tmp;
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        bluetoothAdapter.cancelDiscovery();
        try{
            socket.connect();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void cancel(){
        try{
            socket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
