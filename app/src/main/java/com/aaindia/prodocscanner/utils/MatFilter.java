package com.aaindia.prodocscanner.utils;

import android.util.Log;

import com.aaindia.prodocscanner.constants.Constants;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;

public class MatFilter {


    public static final int COLOR_ORIGINAL = 1;
    public static final int COLOR_CONTRAST = 2;
    public static final int COLOR_PAPER = 3;
    public static final int COLOR_WHITEBOARD = 4;


    public static final int DEFAULT_TUNE_COLOR_ORIGINAL = 0;
    public static final int DEFAULT_TUNE_COLOR_CONTRAST = 10;
    public static final int DEFAULT_TUNE_COLOR_PAPER = 20;
    public static final int DEFAULT_TUNE_COLOR_WHITEBOARD = 55;





    public static final int DEFAULT_COLOR_CODE = COLOR_WHITEBOARD;
    public static final boolean DEFAULT_IS_GRAY = false;


    private static native void brightnessContrastNative(long nativeObjAddr, long percentage);

    private static native void paperizeNative(long nativeObjAddr, float percentage);

    private static native void cleanTextNative(long nativeObjAddr, float percentage);

    public static native void cropV1Native(long nativeObjAddr, long nativeObjAddr1);

    private static native void doNothing();


    private static void brightnessContrast(long nativeObjAddr, long percentage) {
        MatFilter.loadLibrary();
        brightnessContrastNative(nativeObjAddr, percentage);
    }

    private static void paperize(long nativeObjAddr, float percentage) {
        MatFilter.loadLibrary();
        paperizeNative(nativeObjAddr, percentage);

    }


    private static void cleanText(long nativeObjAddr, float percentage) {
        MatFilter.loadLibrary();
        cleanTextNative(nativeObjAddr, percentage);


    }

    public static void cropV1(long nativeObjAddr, long nativeObjAddr1) {

        loadLibrary();

        cropV1Native(nativeObjAddr, nativeObjAddr1);

    }


    public static void loadLibrary() {

        try {
            doNothing();
        } catch (Error error) {
             System.loadLibrary("native-lib");
        }
        catch (Exception e){
            System.loadLibrary("native-lib");
        }


    }


    private static void whiteboard(Mat mat, float control) {

        cleanText(mat.getNativeObjAddr(), control);


    }


    public static void original(Mat mat, int tune) {

        brightnessContrast(mat.getNativeObjAddr(), tune);

    }

    public static void contrast(Mat mat, int tune) {

        brightnessContrast(mat.getNativeObjAddr(), tune);

    }


    public static int getDefaultTune(int colorCode) {

        int defaultTune = 0;

        switch (colorCode) {

            case COLOR_ORIGINAL:
                defaultTune = DEFAULT_TUNE_COLOR_ORIGINAL;
                break;

            case COLOR_CONTRAST:
                defaultTune = DEFAULT_TUNE_COLOR_CONTRAST;
                break;

            case COLOR_PAPER:
                defaultTune = DEFAULT_TUNE_COLOR_PAPER;
                break;

            case COLOR_WHITEBOARD:
                defaultTune = DEFAULT_TUNE_COLOR_WHITEBOARD;


                break;

        }

        return defaultTune;
    }

    public static void colorize(Mat mat, int color, int tune, boolean gray) {


        if (gray) {


            if (mat.channels() == 4)
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2GRAY);
            else if (mat.channels() == 3)
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        }


        switch (color) {

            case COLOR_ORIGINAL:

                original(mat, tune);

                break;


            case COLOR_CONTRAST:

                contrast(mat, tune);

                break;

            case COLOR_PAPER:

                paperize(mat.getNativeObjAddr(), tune);

                break;

            case COLOR_WHITEBOARD:


                whiteboard(mat, tune);

        }


    }

    public static int colorCodeFromDocumentType(String documentType) {


        int colorCode = DEFAULT_COLOR_CODE;

        if (documentType == null) {

            return colorCode;
        }

        if (documentType.equals(Constants.DOCUMENT_TYPE_NOTE))
            colorCode = COLOR_PAPER;

        else if (documentType.equals(Constants.DOCUMENT_TYPE_DOCUMENT))
            colorCode = COLOR_WHITEBOARD;

        else if (documentType.equals(Constants.DOCUMENT_TYPE_PHOTO))
            colorCode = COLOR_CONTRAST;

        return colorCode;


    }


}
