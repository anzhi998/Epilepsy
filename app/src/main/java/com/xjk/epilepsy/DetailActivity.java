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

import com.suke.widget.SwitchButton;
import com.xjk.epilepsy.Fragments.BreChartFragment;
import com.xjk.epilepsy.Fragments.LinechartFragment;
import com.xjk.epilepsy.Fragments.SpeedFragment;
import com.xjk.epilepsy.Utils.BaseFragment;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
    private final int parseStringInterval=20;
    private final int drawLineInterval=4;
    //线程池   采用线程池，不用一个个开线程
    private ScheduledThreadPoolExecutor mThreadPool;
    private RadioGroup mRg_main;
    private List<BaseFragment> mBaseFragment;
    private List<BaseFragment> interList;
    private BaseFragment VFragment;
    private BaseFragment BREFragment;
    private BaseFragment SPDFragment;
    private DataService service;
    private StringBuffer dataBuff;
    private StringBuffer purePointBuff;
    private ContentReceiver mDataReceiver ;
    private DataService.MyBinder myBinder;  //代理人
    private com.suke.widget.SwitchButton switchButton;
    private MyConn conn;
    private TextView upload_spd;
    private TextView download_spd;
    private TextView power_text;
    private ImageView power_img;
    private void doRegusterReceiver(){
        mDataReceiver=new ContentReceiver();
        IntentFilter filter= new IntentFilter("com.xjk.servicecallback.content");
        registerReceiver(mDataReceiver,filter);
    }
    public static  interface  myInterface{
        void onPointChanged(Vector<Vector<Double>> point);
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

                @Override
                public void onSpeedChanged(double up_Spd, double down_Spd) {
                    BigDecimal up=new BigDecimal(up_Spd);
                    BigDecimal down=new BigDecimal(down_Spd);
                    double ups=up.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
                    double downs=down.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
                    upload_spd.setText(String.valueOf(ups)+" k/s");
                    download_spd.setText(String.valueOf(downs)+" k/s");

                }

                @Override
                public void onPowerChanged(int power) {
                    power_text.setText(String.valueOf(power)+"%");
                    if(80<=power&&100>=power){
                        power_img.setImageResource(R.mipmap.power5);
                    }else if(60<=power&&80>=power){
                        power_img.setImageResource(R.mipmap.power4);
                    }else if(40<=power&&60>=power){
                        power_img.setImageResource(R.mipmap.power2);
                    }else if(20<=power&&40>=power){
                        power_img.setImageResource(R.mipmap.power1);
                    }else{
                        power_img.setImageResource(R.mipmap.power0);
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
//        Log.e(TAG,"绑定服务的结果："+String.valueOf(ans));

    }
    Runnable task=new Runnable() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = DRAW;             //触发 handle UI更新线程
            handler.sendMessage(message);

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
//            Log.e(TAG,"缓冲区删除数据，删除长度为"+String.valueOf(startIndex+16));
            String type=dataBuff.substring(0,2);
            if(type.equals(TRUETYPE)){
                dataBuff.delete(0,6);
                PCGStr_10point=dataBuff.substring(0,2*132);
                dataBuff.delete(0,2*132);
                Log.e(TAG,"缓冲区找到1帧ECG信号");
            }else {
                dataBuff.delete(0,6);
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
                Vector<Vector<Double>> point=StringParse.string2Point(data2Daw);
                for(int i=0;i<interList.size();i++){
                    interList.get(i).onPointChanged(point);
                }
                Log.e(TAG,"更新折线图");
            }else {
                Log.e(TAG,"数据量不够,缓冲区数据大小:"+String.valueOf(data2Daw.length()));
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_detail);
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

        switchButton =(com.suke.widget.SwitchButton) findViewById(R.id.switch_btn);
        switchButton.toggle(true);
        switchButton.setShadowEffect(true);
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                sendContentBroadcast(isChecked);//通知service
                if(isChecked){

                    mThreadPool = new ScheduledThreadPoolExecutor(3);
                    //如果需要画图
                    mThreadPool.scheduleAtFixedRate(task,3,drawLineInterval,TimeUnit.SECONDS);
                    //mThreadPool.scheduleAtFixedRate(task_clean,5,5,TimeUnit.SECONDS);
                    mThreadPool.scheduleAtFixedRate(task_deal,2,parseStringInterval,TimeUnit.MILLISECONDS);
                }else{
                    mThreadPool.purge();
                    mThreadPool.shutdownNow();
                }
                for(int i=0;i<interList.size();i++){
                    interList.get(i).needDraw(isChecked);
                }
            }
        });
        switchButton.setChecked(false);
    }

    private BaseFragment getFragment() {
         return mBaseFragment.get(position);
    }
    private void setListener() {
        mRg_main.setOnCheckedChangeListener(new MyOnCheckedChangeListener());
        //设置默认选中
        mRg_main.check(R.id.btn_ecg);
    }
    class MyOnCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.btn_ecg:
                    position = 0;
                    break;
                case R.id.btn_bre:
                    position = 1;
                    break;
                case R.id.btn_acc:
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
    protected void sendContentBroadcast(boolean data){
        Intent intent=new Intent();
        intent.setAction("com.xjk.detailpage.content");
        intent.putExtra("draw",data);
        sendBroadcast(intent);
    }
    private void initFragment() {
        mBaseFragment = new ArrayList<>();
        interList   =   new ArrayList<>();
        VFragment=LinechartFragment.getInstance();
        BREFragment= BreChartFragment.getInstance();
        SPDFragment= SpeedFragment.getInstance();
        mBaseFragment.add(VFragment);
        mBaseFragment.add(BREFragment);
        mBaseFragment.add(SPDFragment);
        interList.add(VFragment);
        interList.add(BREFragment);
        interList.add(SPDFragment);
    }

    private void initView() {
        mRg_main = (RadioGroup) findViewById(R.id.rg_main);
        upload_spd=(TextView) findViewById(R.id.text_upspd);
        download_spd=(TextView) findViewById(R.id.text_downspd);

        power_img=(ImageView)findViewById(R.id.img_power);
        power_text=(TextView)findViewById(R.id.text_power);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mDataReceiver);
        unbindService(conn);
        super.onDestroy();
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
                try {
                    mThreadPool.shutdown();
                }catch (Exception e){}
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
            if(temp==null) return;
            dataBuff.append(temp);

        }
    };
}