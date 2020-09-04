package com.aaindia.prodocscanner.wrappers;


import android.graphics.PointF;

import com.aaindia.prodocscanner.utils.MatFilter;

import java.util.HashMap;

public class Effects {


    public HashMap<Integer, PointF> corners;
    public int color = MatFilter.DEFAULT_COLOR_CODE;
    public boolean isGray = true;
    public int rotation = 0;
    public int colorTune = MatFilter.getDefaultTune(color);


    public Effects(HashMap<Integer, PointF> corners, int color, boolean isGray, int rotation, int colorTune) {
        this.corners = corners;
        this.color = color;
        this.isGray = isGray;
        this.rotation = rotation;
        this.colorTune = colorTune;

    }


}
