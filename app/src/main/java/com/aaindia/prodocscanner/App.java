package com.aaindia.prodocscanner;

import android.app.Application;
import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.widget.ImageView;

import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.CompleteEffectHolder;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.MobileAds;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.util.HashMap;
import java.util.LinkedList;



public class App extends Application {




    @Override
    public void onCreate() {
        super.onCreate();


        MobileAds.initialize(this);

        PDFBoxResourceLoader.init(getApplicationContext());
        Utils.checkOpenCV();
    }




    public static LinkedList<CompleteEffectHolder> CLIPBOARD = new LinkedList<>();




}
