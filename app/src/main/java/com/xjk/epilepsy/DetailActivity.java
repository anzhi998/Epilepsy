package com.xjk.epilepsy;

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

import com.xjk.epilepsy.Fragments.LinechartFragment;
import com.xjk.epilepsy.Fragments.breFragment;
import com.xjk.epilepsy.Utils.BaseFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.xjk.epilepsy.Utils.GlobalBleDevice;
import com.xjk.epilepsy.Utils.StringParse;

public class DetailActivity extends FragmentActivity {
    private final String HEAD="00AA00CC";
    private boolean isStartSend=false;
    private final String TAG="详细页面";
    private final String TRUETYPE="05";
    private final int DRAW=198;
    private final int GETPPOINTS=199;
    //线程池   采用线程池，不用一个个开线程
    private ScheduledThreadPoolExecutor mThreadPool;
    private RadioGroup mRg_main;
    private List<BaseFragment> mBaseFragment;
    private List<BaseFragment> interList;
    public ArrayList<Double> BRE;
    public ArrayList<Double> V1;
    public ArrayList<Double> V2;
    public ArrayList<Double> V3;
    private BaseFragment VFragment;
    private BaseFragment BREFragment;
    private BaseFragment SPDFragment;
    private DataService service;
    private StringBuffer dataBuff;
    private StringBuffer purePointBuff;
    private ContentReceiver mDataReceiver ;
    private DataService.MyBinder myBinder;  //代理人
    private MyConn conn;
    private void doRegusterReceiver(){
        mDataReceiver=new ContentReceiver();
        IntentFilter filter= new IntentFilter("com.xjk.servicecallback.content");
        registerReceiver(mDataReceiver,filter);
    }
    public static  interface  myInterface{
        void onPointChanged(ArrayList<ArrayList<Double>> point);
    }
    public final class MyConn implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            myBinder = (DataService.MyBinder) iBinder;
            service=myBinder.getService();
            SystemClock.sleep(100);
            myBinder.connectSoc(); //连接TCP服务器
            SystemClock.sleep(100);
            BluetoothDevice target=((GlobalBleDevice)getApplication()).getGlobalBlueDevice();
            myBinder.connectDev(target); //连接蓝牙设备
            service.setCallBack(new DataService.CallBack() {
                @Override
                public void onStateChanged(boolean socState, boolean bleState) {
                    Log.e(TAG,"收到tcp状态："+String.valueOf(socState));
                    Log.e(TAG,"收到ble状态："+String.valueOf(bleState));
                    if (bleState == true) {
                        btn_ble.setBackground(getDrawable(R.drawable.circle_green));
                    } else {
                        btn_ble.setBackground(getDrawable(R.drawable.circle_red));
                    }
                    if (socState==true){
                        btn_socket.setBackground(getDrawable(R.drawable.circle_green));
                    }else {
                        btn_socket.setBackground(getDrawable(R.drawable.circle_red));
                    }
                }
            });
        }
        //当服务失去连接时调用的方法
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            service=null;
        }
    };

    /**
     * 选中的Fragment的对应的位置
     */
    private int position;
    public static Button btn_ble;
    public static Button btn_socket;
    /**
     * 上次切换的Fragment
     */
    private Fragment mContent;

    @Override
    protected void onStart() {
        super.onStart();
        conn =new MyConn();
        boolean ans=bindService(new Intent(DetailActivity.this,DataService.class),conn,BIND_AUTO_CREATE);
        Log.e(TAG,"绑定服务的结果："+String.valueOf(ans));

    }
    Runnable task=new Runnable() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = DRAW;             //触发 handle UI更新线程
            handler.sendMessage(message);
            dataBuff.delete(0,dataBuff.length());
        }
    };
    Runnable task_deal=new Runnable() {
        @Override
        public void run() {
            Message me=new Message();
            me.what=GETPPOINTS;
            handler.sendMessage(me);
        }
    };
    Runnable task_clean=new Runnable() {
        @Override
        public void run() {
            dataBuff.delete(0,dataBuff.length());
            Log.i(TAG,"从缓冲区删除数据");
        }
    };
    private Handler handler = new Handler(new Handler.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case DRAW:
                    doUpdatePoint();
                    break;
                case GETPPOINTS:
                    String pcg10s=getPCG();
                    purePointBuff.append(pcg10s);
                    break;
                default:
                    break;
            }
            return true;  //false
        }
    });
    private String getPCG(){
        String PCGStr_10point="";
        if(dataBuff.length()<500){

            return "";
        }
        String dataString=dataBuff.substring(0,500);
        try {
            int startIndex=dataString.indexOf(HEAD);
            dataBuff.delete(0,startIndex+16);
            String type=dataBuff.substring(0,2);
            if(type.equals(TRUETYPE)){
                dataBuff.delete(0,6);
                PCGStr_10point=dataBuff.substring(0,2*132);
                dataBuff.delete(0,2*132);
            }else{
                Log.e(TAG,"收到了其他类型的数据，数据类型:"+type);
            }
        }catch (Exception e){
            Log.e(TAG,"从缓冲区中寻找ecg信号出错");
        }

        return PCGStr_10point;
    }
    private void doUpdatePoint(){
        if(VFragment!=null){
            String data2Daw=purePointBuff.toString();
            if (data2Daw.length()>2*132*100){
                data2Daw=purePointBuff.substring(0,2*132*100);
                purePointBuff.delete(0,2*132*100);
                ArrayList<ArrayList<Double>> point=StringParse.string2Point(data2Daw);
                VFragment.onPointChanged(point);
                Log.i(TAG,"重绘折线图");
                BREFragment.onPointChanged(point);
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化线程池
        mThreadPool = new ScheduledThreadPoolExecutor(3);
        setContentView(R.layout.activity_detail);
        BRE=new ArrayList<Double>();
        V1=new ArrayList<Double>();
        V2=new ArrayList<Double>();
        V3=new ArrayList<Double>();
        dataBuff=new StringBuffer();
        purePointBuff=new StringBuffer();
        //初始化View
        initView();
        //初始化Fragment
        initFragment();
        //设置RadioGroup的监听
        setListener();
        btn_ble=(Button)findViewById(R.id.btn_ble);
        btn_socket=(Button)findViewById(R.id.btn_socket);
        doRegusterReceiver();
        mThreadPool.scheduleAtFixedRate(task,3,3,TimeUnit.SECONDS);
        //mThreadPool.scheduleAtFixedRate(task_clean,5,5,TimeUnit.SECONDS);
        mThreadPool.scheduleAtFixedRate(task_deal,2,20,TimeUnit.MILLISECONDS);
    }

    private BaseFragment getFragment() {
         return mBaseFragment.get(position);
    }
    private void setListener() {
        mRg_main.setOnCheckedChangeListener(new MyOnCheckedChangeListener());
        //设置默认选中
        mRg_main.check(R.id.btn_v1);
    }
    class MyOnCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.btn_v1:
                    position = 0;
                    break;
                case R.id.btn_v2:
                    position = 1;
                    break;
                case R.id.btn_v3:
                    position = 2;
                    break;
                default:
                    position = 0;
                    break;
            }

            //根据位置得到对应的Fragment
            //替换
            switchFrament(mContent, getFragment());

        }
        private void switchFrament(Fragment from,Fragment to) {
            if(from != to){
                mContent = to;
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                //才切换
                //判断有没有被添加
                if(!to.isAdded()){
                    //to没有被添加
                    //from隐藏
                    if(from != null){
                        ft.hide(from);
                    }
                    //添加to
                    if(to != null){
                        ft.add(R.id.fl_content,to).commit();
                    }
                }else{
                    //to已经被添加
                    // from隐藏
                    if(from != null){
                        ft.hide(from);
                    }
                    //显示to
                    if(to != null){
                        ft.show(to).commit();
                    }
                }
            }

        }
    }

    private void initFragment() {
        mBaseFragment = new ArrayList<>();
        interList   =   new ArrayList<>();
        VFragment=LinechartFragment.getInstance();
        BREFragment= breFragment.getInstance();
        SPDFragment=LinechartFragment.getInstance();
        mBaseFragment.add(VFragment);
        mBaseFragment.add(BREFragment);
        mBaseFragment.add(SPDFragment);
        interList.add(VFragment);
        interList.add(BREFragment);
        interList.add(SPDFragment);
    }

    private void initView() {
        mRg_main = (RadioGroup) findViewById(R.id.rg_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mDataReceiver);
        unbindService(conn);

    }
    public void onBackPressed() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("提醒：");
        builder.setMessage("确认断开连接？");
        builder.setPositiveButton("是的", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                myBinder.disconnectSoc();
                myBinder.disconnectDev();
                mThreadPool.shutdown();
                DetailActivity.this.setResult(99);
                DetailActivity.this.finish();
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
    public class ContentReceiver extends BroadcastReceiver {
        //处理点数据的接收器
        @Override
        public void onReceive(Context context, Intent intent) {
            String temp=intent.getStringExtra("data");
            dataBuff.append(temp);
            Log.i(TAG,"缓冲区添加数据，缓冲区长度为："+String.valueOf(dataBuff.length()));
        }
    };
}