package com.aaindia.prodocscanner.utils;

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
    public static final int DEFAULT_TUNE_COLOR_CONTRAST = 20;
    public static final int DEFAULT_TUNE_COLOR_PAPER = 20;
    public static final int DEFAULT_TUNE_COLOR_WHITEBOARD = 40;


    public static final int DEFAULT_TUNE_COLOR_ORIGINAL_GRAY = 0;
    public static final int DEFAULT_TUNE_COLOR_CONTRAST_GRAY = 20;
    public static final int DEFAULT_TUNE_COLOR_PAPER_GRAY = 60;
    public static final int DEFAULT_TUNE_COLOR_WHITEBOARD_GRAY = 60;


    public static final int DEFAULT_COLOR_CODE = COLOR_PAPER;
    public static final boolean DEFAULT_IS_GRAY = false;


    private static native void brightnessContrast(long nativeObjAddr, long percentage);

    private static native void paperize(long nativeObjAddr, float percentage);

    private static native void adjustGamma(long nativeObjAddr, float gamma);


    private static void whiteboard(Mat mat, float control) {


        if (mat.channels() > 1) {
            Mat gray = new Mat();
            Mat binary = new Mat();

            if (mat.channels() == 3)
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            else if (mat.channels() == 4)
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGRA2GRAY);

            //Imgproc.medianBlur(gray, gray, 3);

            Imgproc.blur(gray, gray, new Size(3, 3));


            Imgproc.adaptiveThreshold(gray, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 51, 10);

            Core.bitwise_not(binary, binary);

            Imgproc.dilate(binary, binary, Mat.ones(new Size(7, 7), binary.type()), new Point(-1, -1), 3);

            Core.bitwise_not(binary, binary);

            paperize(mat.getNativeObjAddr(), 0);

            LinkedList<Mat> mats = new LinkedList<>();

            Core.split(mat, mats);

            Core.bitwise_or(mats.get(0), binary, mats.get(0));
            Core.bitwise_or(mats.get(1), binary, mats.get(1));
            Core.bitwise_or(mats.get(2), binary, mats.get(2));


            Core.merge(mats, mat);

            // compute the threshold
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);

            Double thresh = Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_OTSU);


            adjustGamma(mat.getNativeObjAddr(), (float) (thresh / 255.0f) * (control / 100) * 3);


            Core.bitwise_not(binary, binary);

            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);

            mats = new LinkedList<>();

            Core.split(mat, mats);


            Core.add(mats.get(1), new Scalar(100), mats.get(1), binary);

            // mats.get(1).setTo(new Scalar(200), binary);


            Core.merge(mats, mat);

            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_HSV2BGR);


        } else {


            //Imgproc.medianBlur(mat, mat, 3);

            Imgproc.blur(mat, mat, new Size(3, 3));

            Mat binary = new Mat();

            Imgproc.adaptiveThreshold(mat, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 51, 10);


            Core.bitwise_not(binary, binary);

            Imgproc.dilate(binary, binary, Mat.ones(new Size(7, 7), binary.type()), new Point(-1, -1), 3);

            Core.bitwise_not(binary, binary);

            paperize(mat.getNativeObjAddr(), 0);

            Core.bitwise_or(mat, binary, mat);


            Double thresh = Imgproc.threshold(mat, binary, 0, 255, Imgproc.THRESH_OTSU);


            control += 1;

            control = control >= 50 ? control / 5.0f : control / 200f;


            adjustGamma(mat.getNativeObjAddr(), (float) ((thresh + 55) * 1.0f / 255) * 0.8f * control);


            Core.bitwise_not(binary, binary);

            Core.bitwise_xor(mat, mat, mat, binary);

        }


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

            if (color == COLOR_WHITEBOARD)
                tune += 20;

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

    public static native void cropV1(long nativeObjAddr, long nativeObjAddr1);
}
