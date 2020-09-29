package com.aaindia.prodocscanner.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.camera.core.ImageCapture;

import java.util.HashMap;

public class Prefs {


    public class UserSettingsCaptureImageWrapper {


        public static final int FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO;
        public static final int FLASH_TORCH = 84;
        public static final int FLASH_ON = ImageCapture.FLASH_MODE_ON;
        public static final int FLASH_OFF = ImageCapture.FLASH_MODE_OFF;

        public static final int SCAN_MODE_BATCH = 0;
        public static final int SCAN_MODE_SINGLE = 1;

        public static final int GRID_ON = 0;
        public static final int GRID_OFF = 1;

        public int flash, scanMode, grid;


        public UserSettingsCaptureImageWrapper(int flash, int scanMode, int grid) {

            this.flash = flash;
            this.scanMode = scanMode;
            this.grid = grid;

        }


    }


    public static class OCRPreference {

        public static final String OCR_PREFS = "ocr_prefs";

        public static final String OCR_LANG = "ocr_lang";

        public static String getLang(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(OCR_PREFS, context.MODE_PRIVATE);

            return prefs.getString(OCR_LANG, null);


        }


        public static void setLang(Context context, String language) {

            SharedPreferences.Editor editor = context.getSharedPreferences(OCR_PREFS, context.MODE_PRIVATE).edit();
            editor.putString(OCR_LANG, language);


            editor.commit();
        }


    }


    public static class CameraPreference {

        public static final String CAMERA_PREFS = "camera_prefs";

        public static final String CAMERA_CAPTURE_ROTATION = "camera_capture_rotation";
        public static final String CAMERA_CAPTURE_WIDTH = "camera_capture_width";
        public static final String CAMERA_CAPTURE_HEIGHT = "camera_capture_height";

        public static HashMap<String, Integer> getSimpleCameraPref(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(CAMERA_PREFS, context.MODE_PRIVATE);
            int rotation = prefs.getInt(CAMERA_CAPTURE_ROTATION, -1);
            int width = prefs.getInt(CAMERA_CAPTURE_WIDTH, -1);
            int height = prefs.getInt(CAMERA_CAPTURE_HEIGHT, -1);

            HashMap<String, Integer> map = new HashMap<>();

            map.put(CAMERA_CAPTURE_ROTATION, rotation);
            map.put(CAMERA_CAPTURE_HEIGHT, height);
            map.put(CAMERA_CAPTURE_WIDTH, width);

            return map;
        }


        public static void setSimpleCameraPrefs(Context context, HashMap<String, Integer> map) {

            SharedPreferences.Editor editor = context.getSharedPreferences(CAMERA_PREFS, context.MODE_PRIVATE).edit();
            editor.putInt(CAMERA_CAPTURE_HEIGHT, map.get(CAMERA_CAPTURE_HEIGHT));

            editor.putInt(CAMERA_CAPTURE_WIDTH, map.get(CAMERA_CAPTURE_WIDTH));


            editor.putInt(CAMERA_CAPTURE_ROTATION, map.get(CAMERA_CAPTURE_ROTATION));


            editor.commit();
        }


    }

    public static class UserSettingsCaptureImage {

        public static final String USER_SETTINGS_CAPTURE_IMAGE_PREFS = "flash_scan_prefs";

        public static final String FLASH_KEY = "flash_key";

        public static final String SCAN_MODE_KEY = "scan_mode_key";
        public static final String GRID_KEY = "grid_key";


        public static int getFlash(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(USER_SETTINGS_CAPTURE_IMAGE_PREFS, context.MODE_PRIVATE);
            return prefs.getInt(FLASH_KEY, UserSettingsCaptureImageWrapper.FLASH_AUTO);
        }


        public static int getScanMode(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(USER_SETTINGS_CAPTURE_IMAGE_PREFS, context.MODE_PRIVATE);
            return prefs.getInt(SCAN_MODE_KEY, UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH);
        }

        public static int getGrid(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(USER_SETTINGS_CAPTURE_IMAGE_PREFS, context.MODE_PRIVATE);
            return prefs.getInt(GRID_KEY, UserSettingsCaptureImageWrapper.GRID_OFF);
        }

        public static void setFlash(Context context, int value) {

            SharedPreferences.Editor editor = context.getSharedPreferences(USER_SETTINGS_CAPTURE_IMAGE_PREFS, context.MODE_PRIVATE).edit();
            editor.putInt(FLASH_KEY, value);
            editor.commit();
        }

        public static void setScanMode(Context context, int value) {

            SharedPreferences.Editor editor = context.getSharedPreferences(USER_SETTINGS_CAPTURE_IMAGE_PREFS, context.MODE_PRIVATE).edit();
            editor.putInt(SCAN_MODE_KEY, value);
            editor.commit();
        }

        public static void setGrid(Context context, int value) {

            SharedPreferences.Editor editor = context.getSharedPreferences(USER_SETTINGS_CAPTURE_IMAGE_PREFS, context.MODE_PRIVATE).edit();
            editor.putInt(GRID_KEY, value);
            editor.commit();
        }


    }


    public static class displayPrefs {

        public static final String SCAN_DISPLAY_PREFS = "scan_display_prefs";

        public static String DISPLAY_STYLE_KEY = "display_style_key";
        public static int GROUPPED = 0;
        public static int ALL_DOCS = 1;

        public static String SORT_ORDER_KEY = "sort_order_key";
        public static int NEWEST_FIRST = -1;
        public static int OLDEST_FIRST = 1;


        public static int getSort(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(SCAN_DISPLAY_PREFS, context.MODE_PRIVATE);
            return prefs.getInt(SORT_ORDER_KEY, NEWEST_FIRST);
        }

        public static int getDisplayStyle(Context context) {

            SharedPreferences prefs = context.getSharedPreferences(SCAN_DISPLAY_PREFS, context.MODE_PRIVATE);
            return prefs.getInt(DISPLAY_STYLE_KEY, GROUPPED);
        }


        public static void setSort(Context context, int sortOrderValue) {

            SharedPreferences.Editor editor = context.getSharedPreferences(SCAN_DISPLAY_PREFS, context.MODE_PRIVATE).edit();
            editor.putInt(SORT_ORDER_KEY, sortOrderValue);
            editor.commit();
        }

        public static void setDisplayStyle(Context context, int displayStyleValue) {

            SharedPreferences.Editor editor = context.getSharedPreferences(SCAN_DISPLAY_PREFS, context.MODE_PRIVATE).edit();
            editor.putInt(DISPLAY_STYLE_KEY, displayStyleValue);
            editor.commit();
        }


    }


}
