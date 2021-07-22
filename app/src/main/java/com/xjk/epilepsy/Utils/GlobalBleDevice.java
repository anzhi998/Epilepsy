package com.xjk.epilepsy.Utils;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GlobalBleDevice extends Application {
    BluetoothDevice globalBleDevice=null;
    public void setGlobalBlueDevice(BluetoothDevice globalBlueSocket){
        this.globalBleDevice = globalBlueSocket;
    }
    public BluetoothDevice getGlobalBlueDevice(){
        return globalBleDevice;
    }
    ConcurrentLinkedQueue queue = null;
    public void setQueue(ConcurrentLinkedQueue<Byte> data){
        this.queue=data;
    }
    public ConcurrentLinkedQueue getQueue(){
        return this.queue;
    }
}
