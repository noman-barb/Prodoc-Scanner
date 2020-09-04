package com.aaindia.prodocscanner.wrappers;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

public class BitmapMat{

    public Bitmap bitmap;
    public Mat mat;

    public BitmapMat(Bitmap bitmap, Mat mat){
        this.bitmap = bitmap;
        this.mat = mat;

    }

}