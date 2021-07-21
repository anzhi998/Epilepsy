package com.xjk.epilepsy;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;

import com.xjk.epilepsy.Fragments.LinechartFragment;
import com.xjk.epilepsy.Utils.BaseFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.xjk.epilepsy.Utils.StringParse;

public class DetailActivity extends FragmentActivity {
    private String data2Draw;
    /*倒计时Timer发送心跳包*/
    private Timer timer;
    private TimerTask task;
    Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what==1){
                if(StrBuff.length()>2000){
                    String data= StrBuff.toString();
                    StrBuff.delete(0,StrBuff.length());
                    ArrayList<ArrayList<Double>> points=StringParse.string2Point(data);
                    BRE=points.get(0);
                    V1=points.get(1);
                    V2=points.get(2);
                    V3=points.get(3);
                    Log.i("详细页面","准备发送数据点");
                    V1Fragment.onPointChange(V1);
                }
            }
        }
    };
    private RadioGroup mRg_main;
    private List<BaseFragment> mBaseFragment;
    private List<BaseFragment> interList;
    public ArrayList<Double> BRE;
    public ArrayList<Double> V1;
    public ArrayList<Double> V2;
    public ArrayList<Double> V3;
    private BaseFragment V1Fragment;
    private BaseFragment V2Fragment;
    private BaseFragment V3Fragment;
    private BaseFragment BREFragment;
    private DataService service;
    private StringBuffer StrBuff;
    private ContentReceiver mDataReceiver ;
    private DataService.MyBinder myBinder;  //代理人
    private MyConn conn;
    private void doRegusterReceiver(){
        mDataReceiver=new ContentReceiver();
        IntentFilter filter= new IntentFilter("com.xjk.servicecallback.content");
        registerReceiver(mDataReceiver,filter);
    }
    public final class MyConn implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            myBinder = (DataService.MyBinder) iBinder;
            service=myBinder.getService();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        BRE=new ArrayList<Double>();
        V1=new ArrayList<Double>();
        V2=new ArrayList<Double>();
        V3=new ArrayList<Double>();
        StrBuff= new StringBuffer();
        //初始化View
        initView();
        //初始化Fragment
        initFragment();
        //设置RadioGroup的监听
        setListener();
        btn_ble=(Button)findViewById(R.id.btn_ble);
        btn_socket=(Button)findViewById(R.id.btn_socket);
        IntentFilter filter=new IntentFilter(MainActivity.action);
        registerReceiver(broadcastReceiver,filter);
        doRegusterReceiver();
        conn =new MyConn();
        bindService(new Intent(DetailActivity.this,DataService.class),conn,BIND_AUTO_CREATE);
//        sendPoint();
//        ArrayList<Double> v1=new ArrayList<Double>();
//        v1.add(1.0);
//        v1.add(2.0);
//        V1Fragment.onPointChange(v1);
        timer=new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                Message me=new Message();
                me.what=1;
                handler.sendMessage(me);
            }
        };
        timer.schedule(task, 2000);
    }
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        //处理状态灯的接收器
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean bleState = Boolean.parseBoolean(intent.getExtras().getString("ble"));
            boolean socState = Boolean.parseBoolean(intent.getExtras().getString("soc"));
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
    };
    public interface ToFragmentListener {
        void onPointChange(ArrayList<Double> point);
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
                case R.id.btn_bre:
                    position=3;
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
        V1Fragment=LinechartFragment.getInstance();
        V2Fragment=LinechartFragment.getInstance();
        V3Fragment=LinechartFragment.getInstance();
        BREFragment=LinechartFragment.getInstance();
        mBaseFragment.add(V1Fragment);
        mBaseFragment.add(V2Fragment);
        mBaseFragment.add(V3Fragment);
        mBaseFragment.add(BREFragment);
        interList.add(V1Fragment);
        interList.add(V2Fragment);
        interList.add(V3Fragment);
        interList.add(BREFragment);

    }

    private void initView() {
        mRg_main = (RadioGroup) findViewById(R.id.rg_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer!=null)
        {
            timer.purge();
            timer.cancel();
            timer = null;
        }
        unregisterReceiver(broadcastReceiver);
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
            String temp = intent.getStringExtra("data");
            Log.i("详细页面","收到广播数据包:"+temp);
            if (temp != null) {
               StrBuff.append(temp);
               Log.i("缓冲区长度",String.valueOf(StrBuff.length()));
            }
        }
    };

}