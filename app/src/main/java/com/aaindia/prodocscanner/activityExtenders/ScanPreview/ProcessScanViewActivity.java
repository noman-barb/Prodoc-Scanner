package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class ProcessScanViewActivity extends ScanViewActivity {


    private Thread displayImageProcessThread;


    private SavedImageDetails imageDetails;

    private String lastPreparedFilename = null;

    ActivityManager am;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageDetails = getImageDetails();
        am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    }


    HashSet<Integer> currentProcessing = new HashSet<>();


    @Override
    public void processImage(ScanPreviewAdapter.ViewHolder holder, int position, boolean colorOnly) {


        if (currentProcessing.contains(position))
            return;

        currentProcessing.add(position);


        getBinding().protector.setVisibility(View.VISIBLE);
        holder.processing.setVisibility(View.VISIBLE);
        Effects effects = imageDetails.getEffects(imageDetails.getAt(position));


        int colorCode = effects.color;
        boolean colorGray = effects.isGray;
        int colorTune = effects.colorTune;


        new Thread(new Runnable() {
            @Override
            public void run() {


                Mat processedMat = new Mat();
                float widthScaleFactor = holder.originalMat.width() * 1.0f / holder.displayBitmap.getWidth();
                float heightScaleFactor = holder.originalMat.height() * 1.0f / holder.displayBitmap.getHeight();


                Map<Integer, PointF> cropBoundsOriginalMap = new HashMap<>();
                Map<Integer, PointF> cropBoundsMap = holder.polygonView.getPoints();


                if (!colorOnly) {

                    for (int i = 0; i < 4; i++) {

                        PointF pointF = cropBoundsMap.get(i);


                        pointF.x *= widthScaleFactor;
                        pointF.y *= heightScaleFactor;

                        cropBoundsOriginalMap.put(i, new PointF(pointF.x, pointF.y));


                    }

                } else {
                    cropBoundsOriginalMap = imageDetails.getCorners(imageDetails.getAt(position));
                    cropBoundsMap = cropBoundsOriginalMap;
                }


                Point point1 = new Point(cropBoundsMap.get(0).x, cropBoundsMap.get(0).y);
                Point point2 = new Point(cropBoundsMap.get(1).x, cropBoundsMap.get(1).y);
                Point point3 = new Point(cropBoundsMap.get(3).x, cropBoundsMap.get(3).y);
                Point point4 = new Point(cropBoundsMap.get(2).x, cropBoundsMap.get(2).y);

                int diffWidth = (int) ((Math.sqrt((point1.x - point2.x) * (point1.x - point2.x) + (point1.y - point2.y) * (point1.y - point2.y)) / 2) + (Math.sqrt((point3.x - point4.x) * (point3.x - point4.x) + (point3.y - point4.y) * (point3.y - point4.y)) / 2));
                int diffHeight = (int) ((Math.sqrt((point2.x - point3.x) * (point2.x - point3.x) + (point2.y - point3.y) * (point2.y - point3.y)) / 2) + (Math.sqrt((point2.x - point3.x) * (point2.x - point3.x) + (point2.y - point3.y) * (point2.y - point3.y)) / 2));


                Mat src = new MatOfPoint2f(new Point(cropBoundsMap.get(0).x, cropBoundsMap.get(0).y), new Point(cropBoundsMap.get(1).x, cropBoundsMap.get(1).y), new Point(cropBoundsMap.get(3).x, cropBoundsMap.get(3).y), new Point(cropBoundsMap.get(2).x, cropBoundsMap.get(2).y));
                Mat dst = new MatOfPoint2f(new Point(0, 0), new Point(holder.originalMat.width() - 1, 0), new Point(holder.originalMat.width() - 1, holder.originalMat.height() - 1), new Point(0, holder.originalMat.height() - 1));


                Mat transform = Imgproc.getPerspectiveTransform(src, dst);


                Imgproc.warpPerspective(holder.originalMat, processedMat, transform, holder.originalMat.size());


                Imgproc.resize(processedMat, processedMat, new Size(diffWidth, diffHeight));


                setColorTuneListen(false);
                getBinding().colorTuneSK.setProgress(colorTune);
                getBinding().colorGrayCheck.setChecked(colorGray);
                setColorTuneListen(true);

                MatFilter.colorize(processedMat, colorCode, colorTune, colorGray);


                imageDetails.getEffects(imageDetails.getAt(position)).corners = (HashMap<Integer, PointF>) cropBoundsOriginalMap;


                String processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), imageDetails.getOrdering().get(position)).getAbsolutePath();


                if (imageDetails.sync()) {


                    int rot = imageDetails.getRotation(imageDetails.getAt(position));


                    int[] parameters = {Imgcodecs.IMWRITE_JPEG_QUALITY, 90};


                    BitmapUtils.rotateMatDegrees(processedMat, rot);
                    Imgcodecs.imwrite(processedImageFilepath, processedMat, new MatOfInt(parameters));

                }


                processedMat.release();

                System.gc();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        currentProcessing.remove(position);

                        getAutoCroppedSet().add(imageDetails.getAt(position));

                        holder.polygonView.setVisibility(View.GONE);
                        holder.processing.setVisibility(View.GONE);
                        getRecyclerView().getAdapter().notifyDataSetChanged();

                        zoomageEnableDisable(holder, true);
                        setDocumentChanged(true);


                        getBinding().protector.setVisibility(View.GONE);

                    }
                });

            }
        }).start();


    }

    @Override
    public void autocropThis(String path, SavedImageDetails imageDetails) {

        Mat originalMat = Imgcodecs.imread(path);

        if (originalMat.channels() == 4)
            Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGRA2BGR);

        MatOfPoint2f cropBoundsMat = new MatOfPoint2f();


        MatFilter.cropV1(originalMat.getNativeObjAddr(), cropBoundsMat.getNativeObjAddr());


        HashMap<Integer, PointF> cropBoundsMap = new HashMap<>();
        Point[] sortedPoints = BitmapUtils.sortMatofPoints2f(cropBoundsMat);


        for (int i = 0; i < 4; i++) {


            if (sortedPoints[i] == null) {

                sortedPoints[0] = new Point();
                sortedPoints[1] = new Point();
                sortedPoints[2] = new Point();
                sortedPoints[3] = new Point();


                sortedPoints[0].x = 0;
                sortedPoints[0].y = 0;

                sortedPoints[1].x = originalMat.width();
                sortedPoints[1].y = 0;

                sortedPoints[2].x = 0;
                sortedPoints[2].y = originalMat.height();

                sortedPoints[3].x = originalMat.width();
                sortedPoints[3].y = originalMat.height();


            }

            cropBoundsMap.put(i, new PointF((float) sortedPoints[i].x, (float) sortedPoints[i].y));
        }


        Effects effects1 = new Effects(cropBoundsMap, MatFilter.DEFAULT_COLOR_CODE, false, 0, 0);

        getImageDetails().putEffects(new File(path).getName(), effects1);


        originalMat.release();
        cropBoundsMat.release();

    }


    public void prepareMat(ScanPreviewAdapter.ViewHolder holder, int position) {


        if (holder.originalMat != null && holder.originalMat.width() > 0) {

            if (holder.matPosition == position)
                return;

        }


        if (holder.originalMat != null) {
            holder.originalMat.release();
        }


        Mat displayMat = new Mat();

        holder.originalMat = Imgcodecs.imread(getOriginalFilepaths().get(position));


        if (holder.originalMat.channels() == 4)
            Imgproc.cvtColor(holder.originalMat, holder.originalMat, Imgproc.COLOR_BGRA2BGR);


        if (wd == -1) {
            wd = holder.imageView.getMeasuredWidth();
            ht = holder.imageView.getMeasuredHeight();
        }

        Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(holder.originalMat.width(), holder.originalMat.height()), wd, ht);


        Imgproc.resize(holder.originalMat, displayMat, optimalImageSizeForDisplay);

        if (displayMat.channels() == 3)
            Imgproc.cvtColor(displayMat, displayMat, Imgproc.COLOR_BGR2RGB);
        if (displayMat.channels() == 4)
            Imgproc.cvtColor(displayMat, displayMat, Imgproc.COLOR_BGRA2RGB);

        if (holder.displayBitmap != null)
            holder.displayBitmap.recycle();

        holder.displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);


        org.opencv.android.Utils.matToBitmap(displayMat, holder.displayBitmap);


        lastPreparedFilename = getImageDetails().getAt(position);

        displayMat.release();
        holder.matPosition = position;


    }

}