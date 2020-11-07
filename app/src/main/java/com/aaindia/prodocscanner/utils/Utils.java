package com.aaindia.prodocscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.OpenableColumns;
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
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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

    public static void copyNotProcessedOriginals(HashSet<String> masks, SavedImageDetails imageDetails, String scanDir, OnUpdateCopy updateCopy) {

        Iterator i = imageDetails.getOrdering().iterator();

        int total = imageDetails.getOrdering().size();

        int current = 1;

        Mat m;
        while (i.hasNext()) {


            String filename = (String) i.next();

            if (masks != null)
                if (!masks.contains(filename))
                    continue;


            Effects effects = imageDetails.getEffects(filename);


            File originalFile = new File(FileNav.originalScanDirFromScanDir(new File(scanDir)), filename);

            File processedImageFilepath = FileNav.getProcessedFileFromName(scanDir, filename);

            if (!processedImageFilepath.exists()) {


                updateCopy.showDialog();

                try {
                    //FileUtils.copyFile(originalFile, processedImageFilepath);

                    m = Imgcodecs.imread(originalFile.getAbsolutePath());

                    effects.corners.put(0, new PointF(0, 0));
                    effects.corners.put(1, new PointF(m.width(), 0));
                    effects.corners.put(2, new PointF(0, m.height()));
                    effects.corners.put(3, new PointF(m.width(), m.height()));

                    effects.color = MatFilter.COLOR_ORIGINAL;
                    effects.colorTune = MatFilter.getDefaultTune(effects.color);

                    Imgcodecs.imwrite(processedImageFilepath.getAbsolutePath(), m);

                    m.release();


                } catch (Exception e) {

                }


            }
        }

        imageDetails.sync();

    }


    public static String getFileNameFromUri(Uri uri, Activity activity) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    public static void checkOpenCV() {


        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initDebug();
            System.loadLibrary("native-lib");
        }

        MatFilter.loadLibrary();

    }

    public static void checkOpenCV(Activity activity) {


        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initDebug();
            System.loadLibrary("native-lib");
        }


        MatFilter.loadLibrary();

    }


    public static void vibrate(Activity context, long millis) {


        millis = Math.max(20, millis);
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!v.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(millis);
            }
        } catch (Exception e) {
        }


    }


    public interface OnUpdateCopy {


        void showDialog();
    }


}
