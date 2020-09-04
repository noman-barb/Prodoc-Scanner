package com.aaindia.prodocscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.aaindia.prodocscanner.wrappers.BitmapMat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class BitmapUtils {


    public static final String IMAGE_HEIGHT = "hei";
    public static final String IMAGE_WIDTH = "wid";
    public static final String IMAGE_ORIENTATION_ROTATED = "ori";

    public static final String BITMAP_HEIGHT = "bhe";
    public static final String BITMAP_WIDTH = "bwi";

    public interface OnOptimalBitmapLoad {


        void onLoad(boolean error, Bitmap resource, HashMap<String, Integer> map);
    }


    public static void matToBitmap(Mat mat, Bitmap bitmap) {


        bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(mat, bitmap);

    }

    public static void rotateMatDegrees(Mat mat, int rotateDegrees) {


        switch (rotateDegrees) {

            case 90:

                Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE);
                break;


            case 180:

                Core.rotate(mat, mat, Core.ROTATE_180);
                break;


            case 270:

                Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE);
                break;


            case -90:

                Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE);
                break;

            case -180:

                Core.rotate(mat, mat, Core.ROTATE_180);
                break;


            case -270:

                Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE);
                break;
        }


    }


    public static int exifRotationDegrees(String path) {


        try {
            ExifInterface exif = new ExifInterface(path);

            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            return exifRotationToDegrees(rotation);


        } catch (IOException e) {
            return 0;
        }


    }


    public static void setExifRotationDegrees(String path, int rotationDegrees) {



        try {
            ExifInterface exif = new ExifInterface(path);

            int exifRotationFromDegrees = exifRotationFromDegrees(rotationDegrees);
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exifRotationFromDegrees));

            exif.saveAttributes();


        } catch (IOException e) {

        }

    }

    public static int exifRotationFromDegrees(int rotationDegrees){


        if (rotationDegrees == 90){

            return ExifInterface.ORIENTATION_ROTATE_90;
        }
        else if (rotationDegrees == 180){

            return ExifInterface.ORIENTATION_ROTATE_180;
        }
        else if (rotationDegrees == 270){

            return ExifInterface.ORIENTATION_ROTATE_270;
        }
        else if (rotationDegrees == -90){
            return ExifInterface.ORIENTATION_ROTATE_270;
        }
        else if (rotationDegrees == -180){
            return ExifInterface.ORIENTATION_ROTATE_180;
        }
        else if (rotationDegrees == -270){
            return ExifInterface.ORIENTATION_ROTATE_90;
        }

        return ExifInterface.ORIENTATION_NORMAL;


    }

    public static int exifRotationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public static boolean writeBitmapToFile(Bitmap bmp, File outputFile, int orientation) {

        boolean done = false;

        try (FileOutputStream out = new FileOutputStream(outputFile)) {

            bmp.compress(Bitmap.CompressFormat.JPEG, 30, out);

            ExifInterface newExif = new ExifInterface(outputFile.getAbsolutePath());


            switch (orientation) {

                case 0:

                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                    break;


                case 360:

                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                    break;

                case 90:

                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                    break;


                case 180:

                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_180));
                    break;
                case 270:

                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
                    break;


            }


            newExif.saveAttributes();


            done = true;
        } catch (IOException e) {
            e.printStackTrace();
        }


        return done;
    }

    public static HashMap<String, Integer> exifImage(String path) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;


        ExifInterface ei = null;
        try {
            ei = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }


        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        int imageOreintationRotated = 0;

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {

            int temp = imageHeight;

            imageHeight = imageWidth;
            imageWidth = temp;

            imageOreintationRotated = 1;
        }


        HashMap<String, Integer> map = new HashMap<>();
        map.put(IMAGE_HEIGHT, imageHeight);
        map.put(IMAGE_WIDTH, imageWidth);
        map.put(IMAGE_ORIENTATION_ROTATED, imageOreintationRotated);

        return map;

    }


    public static Bitmap imageProxyToBitmap(ImageProxy image) {


        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        return bitmap;
    }


    public static Size getReducedBitmapSize(Size matSize, int viewportWidth, int viewportHeight) {


        int imageWidth = (int) matSize.width;
        int imageHeight = (int) matSize.height;


        int newViewportWidth = viewportHeight * imageWidth / imageHeight;

        if (newViewportWidth > viewportWidth) {
            viewportHeight = viewportWidth * imageHeight / imageWidth;
        } else {
            viewportWidth = newViewportWidth;
        }


        return new Size(viewportWidth, viewportHeight);

        // return  Bitmap.createScaledBitmap(originalImage,viewportWidth, viewportHeight, true);


    }

//

    public static Point[] sortMatofPoints2f(MatOfPoint2f approx) {
        //calculate the center of mass of our contour image using moments
        Moments moment = Imgproc.moments(approx);


        int x = (int) (moment.get_m10() / moment.get_m00());
        int y = (int) (moment.get_m01() / moment.get_m00());

        Point[] sortedPoints = new Point[4];

        double[] data;
        int count = 0;
        for (int i = 0; i < approx.rows(); i++) {
            data = approx.get(i, 0);
            double datax = data[0];
            double datay = data[1];
            if (datax < x && datay < y) {
                sortedPoints[0] = new Point(datax, datay);
                count++;
            } else if (datax > x && datay < y) {
                sortedPoints[1] = new Point(datax, datay);
                count++;
            } else if (datax < x && datay > y) {
                sortedPoints[2] = new Point(datax, datay);
                count++;
            } else if (datax > x && datay > y) {
                sortedPoints[3] = new Point(datax, datay);
                count++;
            }
        }

        return sortedPoints;
    }


    public static PointF getPointFromMatofPoints2f(MatOfPoint2f matOfPoint2f, int row) {

        float x = (float) matOfPoint2f.get(row, 0)[0];

        float y = (float) matOfPoint2f.get(row, 0)[1];

        return new PointF(x, y);
    }


    public static Bitmap loadThumb(String imagePath) {

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true; // obtain the size of the image, without loading it in memory
        BitmapFactory.decodeFile(imagePath, bitmapOptions);

// find the best scaling factor for the desired dimensions
        int desiredWidth = 400;
        int desiredHeight = 300;
        float widthScale = (float) bitmapOptions.outWidth / desiredWidth;
        float heightScale = (float) bitmapOptions.outHeight / desiredHeight;
        float scale = Math.min(widthScale, heightScale);

        int sampleSize = 1;
        while (sampleSize < scale) {
            sampleSize *= 2;
        }

        bitmapOptions.inSampleSize = sampleSize; // this value must be a power of 2,
        // this is why you can not have an image scaled as you would like
        bitmapOptions.inJustDecodeBounds = false; // now we want to load the image

// Let's load just the part of the image necessary for creating the thumbnail, not the whole image
        Bitmap thumbnail = BitmapFactory.decodeFile(imagePath, bitmapOptions);


        return thumbnail;

    }


//    public static void loadOptBmpFile(Activity context, String path, ImageView view, OnOptimalBitmapLoad onOptimalBitmapLoad) {
//
//
//        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
//
//
//        ViewUtils.viewOnReady(view, new Utils.ViewOnReady() {
//            @Override
//            public void onReady(ViewGroup viewGroup) {
//
//
//            }
//
//            @Override
//            public void onReady(View viewGroup) {
//
//
//            }
//        });
//
//
//    }
//
//    private static void    loadOptBmpIntoIV(Activity context, String path, ImageView view, ViewGroup.LayoutParams params, OnOptimalBitmapLoad onOptimalBitmapLoad) {
//
//
//        HashMap<String, Integer> map = exifImage(path);
//
//
//        int finalImageWidth = map.get(IMAGE_WIDTH);
//        int finalImageHeight = map.get(IMAGE_HEIGHT);
//
//
//        int newParamWidth = params.height * finalImageWidth / finalImageHeight;
//
//        if (newParamWidth > params.width) {
//            params.height = params.width * finalImageHeight / finalImageWidth;
//        } else {
//            params.width = newParamWidth;
//        }
//
//        view.setLayoutParams(params);
//
//        map.put(BITMAP_HEIGHT, params.height);
//        map.put(BITMAP_WIDTH, params.width);
//
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Glide.with(context)
//                        .asBitmap()
//                        .load(path)
//                        .skipMemoryCache(true)
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .into(new CustomTarget<Bitmap>(params.width, params.height) {
//                            @Override
//                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
//
//
//                                context.runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//
//                                        onOptimalBitmapLoad.onLoad(false, resource, map);
//
//                                    }
//                                });
//                            }
//
//                            @Override
//                            public void onLoadCleared(@Nullable Drawable placeholder) {
//
//                            }
//                        });
//            }
//        }).start();
//
//
//    }



//    public static HashMap<Integer, PointF> rotateCorners90(HashMap<Integer, PointF> map, boolean clockwise){
//
//
//        HashMap<Integer, PointF> returnMap = new HashMap<>();
//
//        if (clockwise){
//
//            returnMap.put(0, map.get(2));
//
//            returnMap.put(1, map.get(0));
//
//            returnMap.put(2, map.get(3));
//
//            returnMap.put(3, map.get(1));
//        }
//        else {
//
//
//            returnMap.put(0, map.get(1));
//
//            returnMap.put(1, map.get(3));
//
//            returnMap.put(2, map.get(0));
//
//            returnMap.put(3, map.get(2));
//
//        }
//
//        return returnMap;
//
//
//
//    }


}
