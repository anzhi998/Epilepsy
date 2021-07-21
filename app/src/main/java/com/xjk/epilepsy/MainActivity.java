package com.xjk.epilepsy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xjk.epilepsy.Utils.StatusBarUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.xjk.epilepsy.Utils.StringParse;

import static com.xjk.epilepsy.Utils.StringParse.string2Point;

public class MainActivity extends Activity implements View.OnClickListener {
    public static final String action="jason.broadcast.action";
    private  static int REQUEST_ENABLE_BLUETOOTH = 1;
    private  boolean  mIsBound=false;
    private DataService.MyBinder myBinder;  //代理人
    private DataService dataService;
    BluetoothAdapter bluetoothAdapter;
    private TextView scanDevices;
    private LinearLayout loadingLay;
    private RecyclerView rv;//蓝牙设备展示列表
    private BluetoothReceiver bluetoothReceiver;//蓝牙广播接收器
    private final String TAG="主线程";
    private RxPermissions rxPermissions;//权限请求
    DeviceAdapter mAdapter;//蓝牙设备适配器
    List<BluetoothDevice> deviceList = new ArrayList<>();//储存蓝牙设备
    private  TextView scanBtn;
    private  String data2draw;
    private boolean isConOK=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatusBarUtil.StatusBarLightMode(this);//状态栏黑色字体


        initView();//初始化控件
        checkVersion();//检查版本

        scanBtn=(TextView)findViewById(R.id.scan_devices);

    }
    @Override
    protected void onStart() {

        super.onStart();
        //绑定服务获取中间人
        Intent intent = new Intent(MainActivity.this, DataService.class);
        //conn 通讯频道， BIND_AUTO_CREATE如果服务不存在，会把服务创建出来
        boolean ans=bindService(intent,conn,BIND_AUTO_CREATE);
        Log.e(TAG,"绑定服务的结果："+String.valueOf(ans));
    }




    /**
     * 初始化控件
     */
    private void initView() {
        loadingLay = findViewById(R.id.loading_lay);
        scanDevices = findViewById(R.id.scan_devices);
        rv = findViewById(R.id.rv);
        scanDevices.setOnClickListener(this);
    }
    /**
     * 检查Android版本
     */
    private void checkVersion() {
        if (Build.VERSION.SDK_INT >= 23) {//6.0或6.0以上
            permissionsRequest();//动态权限申请
        } else {//6.0以下
            initBlueTooth();//初始化蓝牙配置
        }
    }
    /**
     * 动态权限申请
     */
    private void permissionsRequest() {//使用这个框架使用了Lambda表达式，设置JDK版本为 1.8或者更高
        rxPermissions = new RxPermissions(this);//实例化这个权限请求框架，否则会报错
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(granted -> {
                    if (granted) {//申请成功
                        initBlueTooth();//初始化蓝牙配置
                    } else {//申请失败
                        showMsg("权限未开启");
                    }
                });
    }
    /**
     * 初始化蓝牙配置
     */
    private void initBlueTooth() {
        IntentFilter intentFilter = new IntentFilter();//创建一个IntentFilter对象
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);//获得扫描结果
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//绑定状态变化
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描结束
        bluetoothReceiver = new BluetoothReceiver();//实例化广播接收器
        registerReceiver(bluetoothReceiver, intentFilter);//注册广播接收器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//获取蓝牙适配器
    }
    /**
     * 消息提示
     *
     * @param msg 消息内容
     */
    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId()==R.id.scan_devices){
            if(bluetoothAdapter.isEnabled()){
                if(mAdapter !=null){
                    deviceList.clear();
                    mAdapter.notifyDataSetChanged();
                }
                bluetoothAdapter.startDiscovery();
                v.setClickable(false);
            }else {
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
        }else {
            showMsg("该设备不支持蓝牙");
        }
    }

    /**
     * 结果返回
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                showMsg("蓝牙打开成功");
            } else {
                showMsg("蓝牙打开失败");
            }
        }
        else if(requestCode==99){
            if(resultCode==99)
            {doDisCon();}
        }
    }
    private  ServiceConnection conn = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // 在Activity得到了服务返回的代理人对象，IBinder
            myBinder = (DataService.MyBinder) iBinder;
            dataService=myBinder.getService();
            mIsBound=true;
            Log.e(TAG,"绑定服务成功");
            dataService.setCallBack(new DataService.CallBack() {

                @Override
                public void onStateChanged(boolean socState,boolean bleState) {
                    Log.e(TAG,"收到tcp状态："+String.valueOf(socState));
                    Log.e(TAG,"收到ble状态："+String.valueOf(bleState));
                    if(bleState==true){
                        isConOK=true;
                    }else{
                        isConOK=false;
                    }
                    Intent intent=new Intent(action);
                    intent.putExtra("ble",String.valueOf(isConOK));
                    intent.putExtra("soc",String.valueOf(socState));
                    sendBroadcast(intent);
                }
            });
        }
        //当服务失去连接时调用的方法
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mIsBound=false;
            Log.e(TAG,"失去服务");
        }
    };
    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND://扫描到设备
                    showDevicesData(context, intent);//数据展示
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED://设备绑定状态发生改变
                    mAdapter.changeBondDevice();//刷新适配器
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED://开始扫描
                    loadingLay.setVisibility(View.VISIBLE);//显示加载布局
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED://扫描结束
                    loadingLay.setVisibility(View.GONE);//隐藏加载布局
                    scanBtn.setClickable(true);
                    break;
            }
        }

    }
    private void showDevicesData(Context context, Intent intent) {
        getBondedDevice();//获取已绑定的设备
        //获取周围蓝牙设备
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (deviceList.indexOf(device) == -1) {//防止重复添加
            if (device.getName() != null) {//过滤掉设备名称为null的设备
                deviceList.add(device);
            }
        }
        mAdapter = new DeviceAdapter(R.layout.item, deviceList);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(mAdapter);

        mAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                //点击时获取状态，如果已经配对过了就不需要在配对
                bluetoothAdapter.cancelDiscovery();//停止搜索
                //单独开一个线程连接蓝牙设备
                //连接的过程中是否需要开一个等待的dialog？
                //连接成功后跳转到详情界面
                final BluetoothDevice tempdevice=deviceList.get(position);//先获取到当前选中的设备
                if(tempdevice.getName().contains("Bio")){
                    Log.i(TAG,"想要连接的蓝牙装置是"+tempdevice.getName());
                    createOrRemoveBond(1,tempdevice);
                    //TODO,初始化TCP连接
                    //蓝牙连接设备
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(100);
                            myBinder.connectSoc(); //连接TCP服务器
                            SystemClock.sleep(100);
                            myBinder.connectDev(tempdevice); //连接蓝牙设备
                        }
                    }).start();
                    Intent in = new Intent(MainActivity.this,DetailActivity.class);
                    startActivityForResult(in,99);
                }
                else{
                    showMsg("不支持该设备");
                }
            }
        });
    }
    private void getBondedDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {//如果获取的结果大于0，则开始逐个解析
            for (BluetoothDevice device : pairedDevices) {
                if (deviceList.indexOf(device) == -1) {//防止重复添加
                    if (device.getName() != null) {//过滤掉设备名称为null的设备
                        deviceList.add(device);
                    }
                }
            }
        }
    }
    private void createOrRemoveBond(int type, BluetoothDevice device) {
        Method method = null;
        try {
            switch (type) {
                case 1://开始匹配
                    method = BluetoothDevice.class.getMethod("createBond");
                    method.invoke(device);
                    break;
                case 2://取消匹配
                    method = BluetoothDevice.class.getMethod("removeBond");
                    method.invoke(device);
                    deviceList.remove(device);//清除列表中已经取消了配对的设备
                    break;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * 弹窗
     *
     * @param dialogTitle     标题
     * @param onClickListener 按钮的点击事件
     */
    private void showDialog(String dialogTitle, @NonNull DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogTitle);
        builder.setPositiveButton("确定", onClickListener);
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }


    /**
     * 销毁
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //卸载广播接收器
        unregisterReceiver(bluetoothReceiver);
//        myBinder.disconnectDev();
//        myBinder.disconnectSoc();
//        unbindService(conn);

    }

    @Override
    public void onBackPressed() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("提醒：");
        builder.setMessage("确认退出程序？");
        builder.setPositiveButton("是的", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                doDisCon();
                Intent intent = new Intent(MainActivity.this, DataService.class);
                stopService(intent);       //停止服务
                MainActivity.this.finish();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }
    private void doDisCon(){
        if(myBinder!=null){
            myBinder.disconnectSoc();
            myBinder.disconnectDev();
        }
        else return;
    }

}