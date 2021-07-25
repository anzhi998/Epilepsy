package com.xjk.epilepsy.Utils;

import java.util.ArrayList;
import java.util.Collections;

public class ConvertUtils {
    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }
    public static final double[] string2Arr(String data){
        double[] ydata={1.0};
        return  ydata;
    }
    public static final ArrayList<Double> normalize(ArrayList<Double> arr){
        double min = Collections.min(arr);
        double max=Collections.max(arr);
        double rate=max-min;
        for(double item : arr){
            item=(item-min)/rate;
        }
        return arr;
    }
}
