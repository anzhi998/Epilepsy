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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xjk.epilepsy.Utils.GlobalBleDevice;
import com.xjk.epilepsy.Utils.StatusBarUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener {
    public static final String action="jason.broadcast.action";
    private  static final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final int DEFAULT_VIEW = 66;
    private static final int REQUEST_CODE_SCAN = 67;
    private static final int REQUEST_CODE_ACTION= 99;
    private String targetDeviceMac="";
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatusBarUtil.StatusBarLightMode(this);//状态栏黑色字体


        initView();//初始化控件
        checkVersion();//检查版本
        ShowBondedDevices();
        scanBtn=(TextView)findViewById(R.id.scan_devices);
    }
    @Override
    protected void onStart() {

        super.onStart();

    }
    private void delDeviceInfo(String address){
        SharedPreferences Info = getSharedPreferences("deviceInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = Info.edit();//获取Editor
        editor.remove(address);
        editor.commit();
        Log.i(TAG, "删除设备信息成功");
    }
    private void saveDeviceInfo(){
        SharedPreferences Info = getSharedPreferences("deviceInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = Info.edit();//获取Editor
        //得到Editor后，写入需要保存的数据
        editor.putString(targetDeviceMac, targetDeviceMac);
        editor.commit();//提交修改
        Log.i(TAG, "保存设备信息成功");
    }
    private void ShowBondedDevices(){
        deviceList.clear();
        SharedPreferences deviceInfo = getSharedPreferences("deviceInfo", MODE_PRIVATE);
        Map<String,?> deviceL=deviceInfo.getAll();
        if(deviceL.size()!=0)
            for(Object j :deviceL.values()){
                   String tempadd=j.toString();
                   BluetoothDevice tem=bluetoothAdapter.getRemoteDevice(tempadd);
                   deviceList.add(tem);
            }
        else {
                Log.e("TAG","没有绑定的设备");
        }
        mAdapter = new DeviceAdapter(R.layout.item, deviceList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mAdapter);
        mAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                //点击时获取状态，如果已经配对过了就不需要在配对
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();//停止搜索
                }
                //单独开一个线程连接蓝牙设备
                //连接的过程中是否需要开一个等待的dialog？
                //连接成功后跳转到详情界面
                try {
                    final BluetoothDevice tempdevice = deviceList.get(position);//先获取到当前选中的设备
                    ((GlobalBleDevice) getApplication()).setGlobalBlueDevice(tempdevice);
                    Log.i(TAG, "想要连接的蓝牙装置是" + tempdevice.getName());
                    //TODO,初始化TCP连接
                    Intent in = new Intent(MainActivity.this, DetailActivity.class);
                    startActivityForResult(in, REQUEST_CODE_ACTION);
                } catch (Exception e) {
                    showMsg("请重新搜索设备");
                }
            }
        });
        mAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                BluetoothDevice toDisBond=deviceList.get(position);
                delDeviceInfo(toDisBond.getAddress());
                deviceList.remove(toDisBond);
                ShowBondedDevices();
                //createOrRemoveBond(2,toDisBond);
                return true;
            }
        });
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
        mAdapter = new DeviceAdapter(R.layout.item, deviceList);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.requestPermissions(
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        DEFAULT_VIEW);
            }

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
        switch (requestCode){
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == RESULT_OK) {
                    showMsg("蓝牙打开成功");
                } else {
                    showMsg("蓝牙打开失败");
                }
                break;
            case REQUEST_CODE_ACTION:
                ShowBondedDevices();
                break;
            case REQUEST_CODE_SCAN:
                Object obj = data.getParcelableExtra(ScanUtil.RESULT);
                if (obj instanceof HmsScan) {
                    if (!TextUtils.isEmpty(((HmsScan) obj).getOriginalValue())) {
                        targetDeviceMac=((HmsScan) obj).getOriginalValue();
                        Log.i(TAG,"扫描得到的mac地址："+targetDeviceMac);
                        BluetoothDevice tempdevice=bluetoothAdapter.getRemoteDevice(targetDeviceMac);
//                        createOrRemoveBond(1,tempdevice);
                        saveDeviceInfo();//保存到本地
                        ShowBondedDevices();
//                        Intent in = new Intent(MainActivity.this,DetailActivity.class);
                        ((GlobalBleDevice) getApplication()).setGlobalBlueDevice(tempdevice);
//                        startActivityForResult(in,REQUEST_CODE_ACTION);
                    }
                    return;
                }
                break;
            default:break;
        }
    }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions == null || grantResults == null || grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (requestCode == DEFAULT_VIEW) {
            //start ScankitActivity for scanning barcode
            ScanUtil.startScan(MainActivity.this, REQUEST_CODE_SCAN, new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.ALL_SCAN_TYPE).create());
        }
    }

    private void showDevicesData(Context context, Intent intent) {
        getBondedDevice();//获取已绑定的设备
        //获取周围蓝牙设备
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (deviceList.indexOf(device) == -1) {//防止重复添加
            if (device.getAddress().equals(targetDeviceMac)) {//过滤掉设备名称为null的设备
                deviceList.add(device);
            }
        }
        mAdapter = new DeviceAdapter(R.layout.item, deviceList);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(mAdapter);

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
                    ShowBondedDevices();
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
//                Intent intent = new Intent(MainActivity.this, DataService.class);
//                stopService(intent);       //停止服务
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


}