package com.xjk.epilepsy;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.xjk.epilepsy.Utils.ConvertUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DataService extends Service {
    public static final String TAG = "BLE转TCP中继器";
    public static  BluetoothDevice bluetoothDevice;        //目标蓝牙设备
    public static  BluetoothGatt gatt;                     //蓝牙协议栈包括GATT



    /*默认重连*/
    private static final boolean isReConnect = true;

    //线程池   采用线程池关闭，不用一个个开线程
    private ExecutorService mThreadPool;

    /*数据流*/
    OutputStream outputStream;

    /*倒计时Timer发送心跳包*/
    private Timer timer;
    private TimerTask task;
    /* 心跳周期(s)*/
    private static final int heartCycle = 30;

    private static final String UUID_SERVER = "6e4000f1-b5a3-f393-e0a9-e50e24dcca9e";    //6e400002-b5a3-f393-e0a9-e50e24dcca9e
    //    private static final String UUID_WRITE = "6e4000f2-b5a3-f393-e0a9-e50e24dcca9e";    //安卓的发送id
    private static final String UUID_READ = "6e4000f3-b5a3-f393-e0a9-e50e24dcca9e";     //安卓的接收id
    //在WRITE_NO_RESPONSE模式下 config UUID
    private static final String UUID_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    //Socket变量
    private Socket socket;
    private static final String ip = "47.100.68.201";  //阿里云 120.79.65.208
    private static final int port = 8090;
    private CallBack callBack;

    private BluetoothGattCharacteristic readCharacteristic;
    //    private BluetoothGattCharacteristic writeCharacteristic;
    private  static boolean isBLEConnect=false;         //蓝牙是否连接


    public static  interface  CallBack{
        void onStateChanged(boolean socState,boolean bleState);
    }
    public  void setCallBack(CallBack callBack){
        this.callBack=callBack;
    }
    /**
     * 服务内部的中间人
     */
    public class MyBinder extends Binder{
        /**
         * 中间人帮忙间接的调用服务的方法，启动蓝牙连接
         */
        public void connectSoc(){
            initSocket(ip,port);
        }

        public void connectDev(BluetoothDevice tempdevice){
            bluetoothDevice=tempdevice;
            connectDevice(bluetoothDevice);
        }
        public DataService getService(){
            return DataService.this;
        }

        /**
         * 断开蓝牙连接，更新UI
         */
        public void disconnectDev(){
            disconnectDevice();
        }

        /**
         * 断开socket连接，更新UI
         */
        public void disconnectSoc(){
            disconnectSocket();
        }
    }

    //ui主线程
    private Handler handler = new Handler(new Handler.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 188:
                    doUpdate();
                    break;
                default:
                    break;
            }
            return true;  //false
        }
    });

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();     //返回IBinder的中间人
    }

    @Override
    public void onCreate() {
        //初始化线程池
        mThreadPool = Executors.newCachedThreadPool();

        //初始化全局变量
//        adapter=null;               //蓝牙适配器
        //       bluetoothDevice=null;        //目标蓝牙设备
        gatt=null;                     //蓝牙协议栈包括GATT
        socket = null;
        timer=null;
        task=null;

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    //更新 状态显示（socket连接状态+蓝牙gatt连接状态）
    private void updateStatus(){
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 188;             //触发 handle UI更新线程
                handler.sendMessage(message);
            }
        });
    }

    /* 初始化 socket 线程  */
    private void initSocket(final String ip,final int port) {
        if (socket == null ) {
            //创建TCP连接
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try{
                        //创建Socket对象
                        socket = new Socket(ip, port);
                        if(socket.isConnected()){        //如果socket句柄有效
                            outputStream = socket.getOutputStream();        //输出流
                            Log.i(TAG, " ===> socket.isConnected");
                            //TODO 定时发送心跳数据
                            sendBeatData();
                        }
                    } catch (IOException e) {
                        Log.i(TAG, " ===> socket连接失败");
                    }
                }
            });
        }
    }

    /* socket定时发送心跳包；保持TCP活跃，服务器并不会回复什么 */
    private void sendBeatData() {
        if (timer == null) {          //定时器
            timer = new Timer();
        }
        if(task == null){             //任务
            task = new TimerTask() {
                @Override
                public void run() {
                    try{
                        if(outputStream == null){outputStream = socket.getOutputStream();}
                        Log.i(TAG, "发送心跳包");
                        outputStream.write(("").getBytes("UTF-8"));
                        outputStream.flush();
                    }catch (IOException e) {
                        /*发送失败说明socket断开了或者出现了其他错误*/
                        Log.e(TAG,"连接断开，正在重连");
                        /* 先释放干净，再执行socket重连 */
                        releaseSocket();
                        e.printStackTrace();
                    }finally {
                        //更新状态显示
                        //更新状态显示
                        updateStatus();
                    }
                }
            };
            timer.schedule(task,0,1000*heartCycle);
        }
    }
    // 执行状态更新
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void doUpdate(){
        //蓝牙连接
        if(socket!=null && !socket.isClosed()){
            if(callBack!=null){
                callBack.onStateChanged(true,isBLEConnect);
            }
        }else{
            if(callBack!=null){
                callBack.onStateChanged(false,isBLEConnect);
            }
        }
    }

    //释放资源，再初始化socket句柄，重连
    private void releaseSocket() {
        //关闭所有资源
        disconnectSocket();
        /*重新初始化socket*/
        if (isReConnect) {
            initSocket(ip,port);
        }
    }

    /**
     * 断开GATT连接， 没有直接触发 UI更新线程，因为 disconnectSocket 已经包含UI更新线程
     */
    private void disconnectDevice() {
        if (gatt != null) {
            gatt.close();   //  同样效果  gatt.disconnect();
            gatt=null;
        }
        bluetoothDevice = null;   //清除缓存的蓝牙设备
        isBLEConnect = false;     //蓝牙断开标志
        Log.e(TAG, "StepEnd. BlueTooth device is disconnected");
    }

    /**
     *      断开socket连接
     */
    private void disconnectSocket(){
        //关闭socket
        try{
            if(task != null){
                task.cancel();
                task = null;
            }
            if (timer != null) {
                timer.purge();
                timer.cancel();
                timer = null;
            }
            if(outputStream != null){
                outputStream.close();
                outputStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
                Log.e(TAG,"===>socket is Closed");
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
        //更新状态显示
        updateStatus();
    }


    /**
     * 建立 GATT 连接，这里Android设备作为 client 端，连接GATT Server
     * @param device
     */
    private void connectDevice(BluetoothDevice device) {
        Log.e(TAG, "Step1.5 try to connect the BLE Server!");
        //添加一个字符串缓冲（TCP专用）
        final StringBuffer tcpStrBuff = new StringBuffer();
        if(gatt!=null){
            gatt.close();
            gatt=null;
        }
        try{
            gatt = device.connectGatt(this, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    //4
                    //连接回调
                    Log.e(TAG, "Step2. onConnectionStateChang：status ==>" + newState);
                    if (newState == BluetoothGatt.STATE_CONNECTED) {  // newState = 2
                        try{
                            Thread.sleep(60);
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            gatt.discoverServices(); //执行到这里其实蓝牙已经连接成功了
                        }
                    }else if (newState == BluetoothGatt.STATE_DISCONNECTED) {   //新状态是断开连接
                        releaseGatt();     //断开连接,并走 蓝牙断开重连 程序
                    }else{
                        Log.e(TAG, "Connection state changed.  New state: " + newState);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    Log.e(TAG, "Step3. onServicesDiscovered: ");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.e(TAG, "Step3.5 Service discovery completed!");
                        //5 到这里是已经连接上设备了
                        //保存每个特性的引用。
                        BluetoothGattService service = gatt.getService(UUID.fromString(UUID_SERVER));
                        readCharacteristic = service.getCharacteristic(UUID.fromString(UUID_READ));
//                        writeCharacteristic = service.getCharacteristic(UUID.fromString(UUID_WRITE));
                        //设置RX特征变化的通知(即接收到的数据)。
                        //首先调用setCharacteristicNotification来启用通知。
                        if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
                            Log.e(TAG, "Couldn't set notifications for RX characteristic!");
                        }
                        //下一步更新RX特征的客户端描述符以启用通知。
                        if(readCharacteristic.getDescriptor(UUID.fromString(UUID_CONFIG))!=null){
                            BluetoothGattDescriptor descriptor = readCharacteristic.getDescriptor(UUID.fromString(UUID_CONFIG));
                            //6  蓝牙通信回调，然后回调通信内容会回调到onCharacteristicChanged
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);   //使能通知
                            gatt.writeDescriptor(descriptor);
                            isBLEConnect=true;      //标志位，表示蓝牙连接成功
                            //更新状态显示
                            updateStatus();
                        }else {
                            Log.e(TAG, "Step3.9 Couldn't write RX client descriptor value!");
                        }
                    }else{
                        Log.e(TAG, "Service discovery failed with status: " + status);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    //发送数据给蓝牙后，蓝牙返回数据在这里
                    //7.  获取的是 byte 整数数组
                    final byte[] values = characteristic.getValue();

                    String hex=ConvertUtils.bytesToHexString(values);
                    Log.i(TAG,"蓝牙收到数据，长度为："+String.valueOf(hex.length()));
                    sendContentBroadcast(hex);
                    //8.  将数组转成 string 用于传输
                    tcpStrBuff.append(hex);
                    String str = tcpStrBuff.toString();        // 拼接后的 str
                    int length = str.length();                 // 拼接后的 str 长度
                    if(length>2000){           //缓存2k字节数据，一次socket发送

                        // socket发送数据
                        try{
                            if(socket!=null && !socket.isClosed() && outputStream!=null){
                                outputStream.write( str.getBytes("utf-8"));    //字节流数组写入
                                outputStream.flush();
                            }
                        } catch (IOException e) { //  e.printStackTrace();
                        }
                        tcpStrBuff.delete(0,length);   //清空buff
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);

                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    //printData(characteristic.getValue());      //打印数据发送
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * 将字节打印成16进制的hex文件
     * @param b
     */
    void printData(byte[] b) {
        Log.e(TAG, b == null ? "get nothing" : ConvertUtils.bytesToHexString(b));
    }

    /** 蓝牙断开重连
     因为距离导致蓝牙断开，设置定时器 蓝牙重连
     */
    private void releaseGatt(){
        if (gatt != null) {
            gatt.close();   //  同样效果  gatt.disconnect();
            gatt=null;
        }
        isBLEConnect=false;      // 更新蓝牙连接状态
        Log.e(TAG, "StepUnexpected. BlueTooth device is lost");

        if(bluetoothDevice != null){   //说明这里蓝牙断开不是主动断开的，需要重新连接
            try{
                Thread.sleep(2000);    //2s后尝试重新连接蓝牙
                Log.e(TAG, "线程休息2s");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                Log.e(TAG, "尝试重新连接蓝牙设备");
                //连接蓝牙设备
                connectDevice(bluetoothDevice);
            }
        }
    }
    protected void sendContentBroadcast(String data){
        Intent intent=new Intent();
        intent.setAction("com.xjk.servicecallback.content");
        intent.putExtra("data",data);
        Log.e(TAG,"广播数据包："+data);
        sendBroadcast(intent);
    }

}
