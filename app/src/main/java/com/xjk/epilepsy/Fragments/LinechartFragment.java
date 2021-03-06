package com.xjk.epilepsy.Fragments;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.xjk.epilepsy.DetailActivity;
import com.xjk.epilepsy.R;
import com.xjk.epilepsy.Utils.BaseFragment;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class LinechartFragment extends BaseFragment implements DetailActivity.myInterface{


    private LineChartView V1Line;
    private LineChartView V2Line;
    private LineChartView V3Line;
    private Vector<Double> oldV1;
    private Vector<Double> oldV2;
    private Vector<Double> oldV3;
    private Vector<Double>  newV1;
    private Vector<Double> newV2;
    private Vector<Double> newV3;
    private final int updateECG=12;
    private boolean isAnanimation=false;
    private ScheduledThreadPoolExecutor upDatePool;
    @Override
    protected View initView() {
        View view = View.inflate(mContext, R.layout.fragment_linechart,null);
        return view;
    }
    Runnable task=new Runnable() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = updateECG;             //触发 handle UI更新线程
            handler.sendMessage(message);
        }
    };
    private Handler handler = new Handler(new Handler.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case updateECG:
                    updatePoints();
                    break;
                default:
                    break;
            }
            return true;  //false
        }
    });
    private void updatePoints(){
        int startIndex=oldV1.size()-newV1.size();
        if(startIndex>oldV1.size()-4){
            return;
        }
        for (int i=0;i<4;i++){
            Double temV1=newV1.get(0);
            Double temV2=newV2.get(0);
            Double temV3=newV3.get(0);
            oldV1.set(startIndex+i,temV1);
            oldV2.set(startIndex+i,temV2);
            oldV3.set(startIndex+i,temV3);
            newV1.remove(0);
            newV2.remove(0);
            newV3.remove(0);
        }
        updateLineChart();
    }
    private ArrayList<PointValue> generateData(Vector<Double> point){
        int length=point.size();
        //        point= ConvertUtils.normalize(point);
        ArrayList<PointValue> values = new ArrayList<PointValue>();//折线上的点
        for(int i=0;i<length;i++){
            Double num=point.get(i);
            String data=num.toString();
            Float ans=Float.valueOf(data);
            values.add(new PointValue(i,ans));
        }
        return values;

    }
    public static LinechartFragment getInstance(){
        LinechartFragment frag=new LinechartFragment();
        return  frag;
    }
    @Override
    public void onStart() {
        super.onStart();
        V1Line=getActivity().findViewById(R.id.V1);
        V2Line=getActivity().findViewById(R.id.V2);
        V3Line=getActivity().findViewById(R.id.V3);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void updateLineChart(){

        Line line1 = new Line(generateData(oldV1)).setColor(Color.RED);//声明线并设置颜色
        Line line2= new Line(generateData(oldV2)).setColor(Color.RED);//声明线并设置颜色
        Line line3= new Line(generateData(oldV3)).setColor(Color.RED);//声明线并设置颜色
        line1.setCubic(false);//设置是平滑的还是直的
        line1.setHasPoints(false);
        line1.setStrokeWidth(1);
        line2.setCubic(false);//设置是平滑的还是直的
        line2.setHasPoints(false);
        line2.setStrokeWidth(1);
        line3.setCubic(false);//设置是平滑的还是直的
        line3.setHasPoints(false);
        line3.setStrokeWidth(1);


        ArrayList<Line> lines=new ArrayList<Line>();
        lines.add(line1);
        if(V1Line!=null){
            V1Line.setInteractive(true);
            V1Line.setZoomType(ZoomType.VERTICAL);//设置缩放方向
            LineChartData data = new LineChartData();
            Axis axisX = new Axis();//x轴
            Axis axisY = new Axis();//y轴
            axisX.setAutoGenerated(true);
            axisY.setAutoGenerated(true);
            axisX.setHasLines(true);
            axisY.setHasLines(true);

            data.setAxisXBottom(axisX);
            data.setAxisYLeft(axisY);
            data.setLines(lines);
//            final Viewport v =new Viewport(V1Line.getMaximumViewport());
//            v.top=-4;
//            v.bottom=-7;
//            V1Line.setMaximumViewport(v);
            V1Line.setLineChartData(data);
//            int midY=Collections.max(point.get(1)).intValue()+Collections.min(point.get(1)).intValue();
//            midY=(int)midY/2;
//            V1Line.setZoomLevel(500,Collections.min(point.get(1)).intValue() ,5);

        }

        ArrayList<Line> lines2=new ArrayList<Line>();
        lines2.add(line2);
        if(V2Line!=null){
            V2Line.setInteractive(true);
            V2Line.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);//设置缩放方向
            LineChartData data2 = new LineChartData();
            Axis axisX = new Axis();//x轴
            Axis axisY = new Axis();//y轴
            axisX.setHasLines(true);
            axisY.setHasLines(true);
            axisX.setAutoGenerated(true);
            axisY.setAutoGenerated(true);
            data2.setAxisXBottom(axisX);
            data2.setAxisYLeft(axisY);
            data2.setLines(lines2);
            V2Line.setLineChartData(data2);
        }
        ArrayList<Line> lines3=new ArrayList<Line>();
        lines3.add(line3);
        if(V3Line!=null){
            V3Line.setInteractive(true);
            V3Line.setZoomType(ZoomType.VERTICAL);//设置缩放方向
            LineChartData data3 = new LineChartData();
            Axis axisX = new Axis();//x轴
            Axis axisY = new Axis();//y轴
            axisX.setAutoGenerated(true);
            axisY.setAutoGenerated(true);
            axisX.setHasLines(true);
            axisY.setHasLines(true);
            data3.setAxisXBottom(axisX);
            data3.setAxisYLeft(axisY);
            data3.setLines(lines3);
            V3Line.setLineChartData(data3);
//            int midY=Collections.max(point.get(3)).intValue()+Collections.min(point.get(3)).intValue();
//            midY=(int)midY/2;
//            //V3Line.setZoomLevel(500, midY,5);
        }
    }
    @Override
    public void onPointChanged(Vector<Vector<Double>> point) {
        newV1=point.get(1);
        newV2=point.get(2);
        newV3=point.get(3);
        if(V1Line==null||V2Line==null||V1Line==null){
            return;
        }
        if(oldV1==null&&oldV2==null&&oldV3==null){
            oldV1=newV1;
            oldV2=newV2;
            oldV3=newV3;
            updateLineChart();
        }else{
            //动态更新
            if(!isAnanimation){
                upDatePool.scheduleAtFixedRate(task,0,10, TimeUnit.MILLISECONDS);
                isAnanimation=true;
            }

        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if(!upDatePool.isShutdown()){
                upDatePool.purge();
                upDatePool.shutdown();
            }
        }catch (Exception e){}

    }

    @Override
    public void needDraw(boolean need) {
        if(need){
            upDatePool = new ScheduledThreadPoolExecutor(2);
            isAnanimation=false;
           // upDatePool.scheduleAtFixedRate(task,0,10,TimeUnit.MILLISECONDS);
        }else{
            upDatePool.purge();
            upDatePool.shutdownNow();
        }
    }
}