package com.aaindia.prodocscanner.utils;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.io.FileUtils;

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


}
