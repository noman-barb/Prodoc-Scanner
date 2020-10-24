package com.aaindia.prodocscanner.wrappers;

import android.graphics.PointF;

import com.aaindia.prodocscanner.utils.MatFilter;

import java.util.HashMap;

public class CompleteEffectHolder {


    public HashMap<Integer, PointF> corners;
    public int color = MatFilter.DEFAULT_COLOR_CODE;
    public boolean isGray = true;
    public int rotation = 0;
    public int colorTune = MatFilter.getDefaultTune(color);
    public String originalPath = null;
    public String processedPath = null;
    public String scanDirPath = null;
    public String imageFileName = null;

    public CompleteEffectHolder(HashMap<Integer, PointF> corners, int color, boolean isGray, int rotation, int colorTune, String originalPath, String processedPath, String scanDirPath, String imageFileName) {
        this.corners = corners;
        this.color = color;
        this.isGray = isGray;
        this.rotation = rotation;
        this.colorTune = colorTune;
        this.originalPath = originalPath;
        this.processedPath = processedPath;
        this.scanDirPath = scanDirPath;
        this.imageFileName = imageFileName;

    }

}