package com.xjk.epilepsy.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class StringParse {
    public static final Vector<Vector<Double>> string2Point(String data) {
        if(data.length()<264)
        {
            return null;
        }
        List<String> ecgstrList=getStrList(data,264);
        Vector<Double> V1=new Vector<>();
        Vector<Double> V2=new Vector<>();
        Vector<Double> V3=new Vector<>();
        Vector<Double> BRE=new Vector<>();
        Vector<Double> ACCx=new Vector<>();
        Vector<Double> ACCy=new Vector<>();
        Vector<Double> ACCz=new Vector<>();

        Vector<Vector<Double>> ans=new Vector<Vector<Double>>();
        for(String ecgStr : ecgstrList){
            if(ecgStr.length()<264)
            {
                break;
            }
            String ecg4Channel=ecgStr.substring(0,240);
            String accelerateSpeed=ecgStr.substring(240,252);
            String x=accelerateSpeed.substring(0,4);
            String y=accelerateSpeed.substring(4,8);
            String z=accelerateSpeed.substring(8,12);
            ACCx.add(str2acc(x));
            ACCy.add(str2acc(y));
            ACCz.add(str2acc(z));
            Vector<Double> dataArray=new Vector<>();
            int index=0;
            for(int i=0;i<40;i++){
                String point=ecg4Channel.substring(index,index+6);
                point=point+"00";
                Long longstr=Long.parseLong(point,16);
                int num=new Integer(longstr.intValue());
                //BigInteger bi = new BigInteger(point, 16);
                //bi=bi.shiftLeft(8);
                double value=num/10307920.2816;
                dataArray.add(value);
                index+=6;
            }
            for (int j=0;j<40;j+=4){
                BRE.add(dataArray.get(j));
                V1.add(dataArray.get(j+1));
                V2.add(dataArray.get(j+2));
                V3.add(dataArray.get(j+3));
            }

        }
        BRE=ConvertUtils.normalize(BRE);
        V1=ConvertUtils.normalize(V1);
        V2=ConvertUtils.normalize(V2);
        V3=ConvertUtils.normalize(V3);
//        ACCx=ConvertUtils.normalize(ACCx);
//        ACCy=ConvertUtils.normalize(ACCy);
//        ACCz=ConvertUtils.normalize(ACCz);

        ans.add(BRE);
        ans.add(V1);
        ans.add(V2);
        ans.add(V3);
        ans.add(ACCx);
        ans.add(ACCy);
        ans.add(ACCz);
        return ans;
    }

    private static List<String> getStrList(String inputString, int length) {
        int size = inputString.length() / length;
        if (inputString.length() % length != 0) {
            size += 1;
        }
        return getStrList(inputString, length, size);
    }
    /**
     * 把原始字符串分割成指定长度的字符串列表
     *
     * @param inputString
     *            原始字符串
     * @param length
     *            指定长度
     * @param size
     *            指定列表大小
     * @return
     */
    private static List<String> getStrList(String inputString, int length,
                                          int size) {
        List<String> list = new ArrayList<String>();
        for (int index = 0; index < size; index++) {
            String childStr = substring(inputString, index * length,
                    (index + 1) * length);
            list.add(childStr);
        }
        return list;
    }
    /**
     * 分割字符串，如果开始位置大于字符串长度，返回空
     *
     * @param str
     *            原始字符串
     * @param f
     *            开始位置
     * @param t
     *            结束位置
     * @return
     */
    private static String substring(String str, int f, int t) {
        if (f > str.length())
            return null;
        if (t > str.length()) {
            return str.substring(f, str.length());
        } else {
            return str.substring(f, t);
        }
    }
    private static Double str2acc(String s){
        String temp=s+"0000";
        Long longstr=Long.parseLong(temp,16);
        int num=new Integer(longstr.intValue());
        double value=(double)(num>>16)*32/65535;
        return value;

    }
    private static String strTo16(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = (int) s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }
}
