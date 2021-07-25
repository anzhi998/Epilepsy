package com.xjk.epilepsy.Fragments;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.xjk.epilepsy.DetailActivity;
import com.xjk.epilepsy.R;
import com.xjk.epilepsy.Utils.BaseFragment;
import com.xjk.epilepsy.Utils.ConvertUtils;

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

public class BreChartFragment extends BaseFragment implements DetailActivity.myInterface{


    private LineChartView BreLine;
    @Override
    protected View initView() {
        View view = View.inflate(mContext, R.layout.fragment_brechart,null);
        return view;
    }

    private ArrayList<PointValue> generateData(ArrayList<Double> point){
        int length=point.size();
//        point= ConvertUtils.normalize(point);
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
    public static BreChartFragment getInstance(){
        BreChartFragment frag=new BreChartFragment();
        return  frag;
    }
    @Override
    public void onStart() {
        super.onStart();
        BreLine=getActivity().findViewById(R.id.BreLine);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onPointChanged(ArrayList<ArrayList<Double>> point) {
        ArrayList<PointValue> bredata=generateData(point.get(0));
        Line line1 = new Line(bredata).setColor(Color.RED);//声明线并设置颜色
        line1.setCubic(false);//设置是平滑的还是直的
        line1.setHasPoints(false);
        line1.setStrokeWidth(1);


        ArrayList<Line> lines=new ArrayList<Line>();
        lines.add(line1);
        if(BreLine!=null){
            BreLine.setInteractive(true);
            BreLine.setZoomType(ZoomType.VERTICAL);//设置缩放方向
            LineChartData data = new LineChartData();
            Axis axisX = new Axis();//x轴
            Axis axisY = new Axis();//y轴
            axisX.setHasLines(true);
            axisY.setHasLines(true);

            data.setAxisXBottom(axisX);
            data.setAxisYLeft(axisY);
            data.setLines(lines);
            BreLine.setLineChartData(data);
            BreLine.setZoomLevel(0,Collections.min(point.get(1)).intValue() ,5);

        }



    }
}