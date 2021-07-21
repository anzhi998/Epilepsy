package com.xjk.epilepsy.Utils;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

public class GlobalBleDevice extends Application {
    BluetoothDevice globalBleDevice=null;
    public void setGlobalBlueDevice(BluetoothDevice globalBlueSocket){
        this.globalBleDevice = globalBlueSocket;
    }
    public BluetoothDevice getGlobalBlueDevice(){
        return globalBleDevice;
    }

}
