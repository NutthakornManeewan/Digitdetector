package com.example.antenna.digitdetector;

import android.util.Log;

/**
 * Created by Antenna on 7/3/2016.
 */
public class KNNClassifier {
    String TAG         = "Result";
    int[]    index_arr = null;
    double[] dista_arr = null;

    public KNNClassifier (int[] idx_arr, double[] dist_arr) {
        this.index_arr = idx_arr;
        this.dista_arr = dist_arr;
    }

    public void SortDistance() {
        // *******************************************************
        // ********** Sort distance array and its index **********
        // *******************************************************
        double tmpDist = 0.0;
        int    tmpIdx  = 0;
        boolean flag   = true;
        while (flag) {
            flag = false;
            for (int j = 0; j < dista_arr.length-1; j++) {
                if (dista_arr[j+1] < dista_arr[j]) {
                    tmpDist = dista_arr[j];
                    tmpIdx  = index_arr[j];
                    dista_arr[j]   = dista_arr[j+1];
                    dista_arr[j+1] = tmpDist;
                    index_arr[j]   = index_arr[j+1];
                    index_arr[j+1] = tmpIdx;
                    flag = true;
                }
            }
        }
    }

    public int Candidate () {
        // **********************************************************************
        // ********** Candidate and classify number with HOGs features **********
        // **********************************************************************
        int[] chopIdx = new int[5];
        int[] count   = {0,0,0,0,0,0,0,0,0,0};
        int max       = 0;
        for (int i=0; i<5; i++) {
            chopIdx[i] = (int)(index_arr[i]/22);
            count[chopIdx[i]]++;
        }
        if (count[2]!=0 && count[8]!=0) { max = 20; }
        else if ((count[5]!=0 && count[6]!=0) && (count[5]>=count[6])) { max = 50; }
        else if (count[1]!=0 && count[4]!=0) { max = 40; }
        else {
            for (int j=0; j<count.length; j++) {
                if (count[j] > max)
                    max = j;
            }
            max = max * 10;
        }

        Log.d(TAG, "Can:["+count[0]+count[1]+count[2]+count[3]+count[4]+count[5]+count[6]+count[7]+count[8]+count[9]+"]");
        Log.d(TAG, "max:" + max);
        return max;
    }
}
