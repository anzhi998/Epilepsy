package com.xjk.epilepsy.Fragments;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.xjk.epilepsy.DetailActivity;
import com.xjk.epilepsy.R;
import com.xjk.epilepsy.Utils.BaseFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Collections;

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
    @Override
    protected View initView() {
        View view = View.inflate(mContext, R.layout.fragment_linechart,null);
        return view;
    }

    private ArrayList<PointValue> generateData(ArrayList<Double> point){
        int length=point.size();
        Log.i("点数据","长度："+String.valueOf(length));
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


    @Override
    public void onPointChanged(ArrayList<ArrayList<Double>> point) {
        ArrayList<PointValue> v1data=generateData(point.get(1));
        ArrayList<PointValue> v2data=generateData(point.get(2));
        ArrayList<PointValue> v3data=generateData(point.get(3));
        Line line1 = new Line(v1data).setColor(Color.RED);//声明线并设置颜色
        Line line2= new Line(v2data).setColor(Color.RED);//声明线并设置颜色
        Line line3= new Line(v3data).setColor(Color.RED);//声明线并设置颜色
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
            V1Line.setZoomLevel(0,Collections.min(point.get(1)).intValue() ,5);

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
            axisX.setHasLines(true);
            axisY.setHasLines(true);
            data3.setAxisXBottom(axisX);
            data3.setAxisYLeft(axisY);
            data3.setLines(lines3);
            V3Line.setLineChartData(data3);
            int midY=Collections.max(point.get(3)).intValue()+Collections.min(point.get(3)).intValue();
            midY=(int)midY/2;
            V3Line.setZoomLevel(0, midY,5);
        }
    }
}