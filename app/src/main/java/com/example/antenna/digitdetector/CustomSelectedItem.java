package com.example.antenna.digitdetector;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

/**
 * Created by wwwNa on 12/13/2016.
 */

public class CustomSelectedItem implements AdapterView.OnItemSelectedListener {
    public static int index=0;
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        index = position + 5;
        Log.i("Excel", "Order index : " + index);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
