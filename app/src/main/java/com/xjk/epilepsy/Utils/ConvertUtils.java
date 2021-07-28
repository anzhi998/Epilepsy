package com.xjk.epilepsy.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

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
    public static final Vector<Double> normalize(Vector<Double> arr){
        double min = Collections.min(arr);
        double max=Collections.max(arr);
        double rate=max-min;
        for(int i=0;i<arr.size();i++){
            arr.set(i,(arr.get(i)-min)/rate);
        }
        return arr;
    }
}
