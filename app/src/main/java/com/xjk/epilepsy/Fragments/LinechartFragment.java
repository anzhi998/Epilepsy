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

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class LinechartFragment extends BaseFragment implements DetailActivity.ToFragmentListener {


    private LineChartView lineChart;
    @Override
    protected View initView() {
        View view = View.inflate(mContext, R.layout.fragment_linechart,null);
        return view;
    }

    @Override
    public void onPointChange(ArrayList<Double> point) {
        resetData(point);
    }
    private void resetData(ArrayList<Double> point){
        lineChart.clearFocus();
        int length=point.size();
        Log.e("Fragment","收到要绘制的点");
        ArrayList<PointValue> values = new ArrayList<PointValue>();//折线上的点
        for(int i=0;i<length;i++){
            Double num=point.get(i);
            String data=num.toString();
            Float ans=Float.valueOf(data);
            values.add(new PointValue(i,ans));
        }
        Line line = new Line(values).setColor(Color.BLUE);//声明线并设置颜色
        line.setCubic(false);//设置是平滑的还是直的
        ArrayList<Line> lines=new ArrayList<Line>();
        lines.add(line);
        lineChart.setInteractive(true);//设置图表是可以交互的（拖拽，缩放等效果的前提）
        lineChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);//设置缩放方向
        LineChartData data = new LineChartData();
        Axis axisX = new Axis();//x轴
        Axis axisY = new Axis();//y轴
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setLines(lines);
        lineChart.setLineChartData(data);//给图表设置数据

    }
    public static LinechartFragment getInstance(){
        LinechartFragment frag=new LinechartFragment();
        return  frag;
    }
    @Override
    public void onStart() {
        super.onStart();
        lineChart=(LineChartView) getActivity().findViewById(R.id.line_chart);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    private void generrateData(){
        ArrayList<PointValue> values = new ArrayList<PointValue>();//折线上的点
        values.add(new PointValue(0, 1));
        values.add(new PointValue(1, 1));
        values.add(new PointValue(2, 1));
        values.add(new PointValue(3, 1));
        Line line = new Line(values).setColor(Color.BLUE);//声明线并设置颜色
        line.setCubic(false);//设置是平滑的还是直的
        ArrayList<Line> lines=new ArrayList<Line>();
        lines.add(line);
        lineChart.setInteractive(true);//设置图表是可以交互的（拖拽，缩放等效果的前提）
        lineChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);//设置缩放方向
        LineChartData data = new LineChartData();
        Axis axisX = new Axis();//x轴
        Axis axisY = new Axis();//y轴
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setLines(lines);
        lineChart.setLineChartData(data);//给图表设置数据
    }
}