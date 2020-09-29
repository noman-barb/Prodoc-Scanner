package com.aaindia.prodocscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.io.FileUtils;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class Utils {

    public interface ViewOnReady {

        public void onReady(ViewGroup viewGroup);

        public void onReady(View viewGroup);

    }

    public static JsonArray insert(int index, JsonElement val, JsonArray currentArray) {
        JsonArray newArray = new JsonArray();
        for (int i = 0; i < index; i++) {
            newArray.add(currentArray.get(i));
        }
        newArray.add(val);

        for (int i = index; i < currentArray.size(); i++) {
            newArray.add(currentArray.get(i));
        }
        return newArray;
    }


    public static void setSpanActionColor(Context context, SpannableString s, int compare1, int compare2) {

        if (compare1 == compare2)
            s.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.colorSecondary)), 0, s.length(), 0);
    }

    public static void log(String msg) {
        Log.d("aaaaaaaaaaaaaaaaaaaa", msg);
    }

    public static void copyNotProcessedOriginals(SavedImageDetails imageDetails, String scanDir) {

        Iterator i = imageDetails.getOrdering().iterator();
        while (i.hasNext()) {


            String filename = (String) i.next();


            Effects effects = imageDetails.getEffects(filename);


            File originalFile = new File(FileNav.originalScanDirFromScanDir(new File(scanDir)), filename);

            File processedImageFilepath = FileNav.getProcessedFileFromName(scanDir, filename);

            if (!processedImageFilepath.exists()) {


                try {
                    FileUtils.copyFile(originalFile, processedImageFilepath);
                } catch (IOException e) {

                }


            }
        }

    }


    public static void checkOpenCV(Activity activity){




        if (!OpenCVLoader.initDebug()) {
           OpenCVLoader.initDebug();
        }

        MatFilter.loadLibrary();

    }



    public static void vibrate(Activity context, long millis) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }
    }


}
