package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

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
import java.util.Map;


public class ProcessScanViewActivity extends ScanViewActivity {


    private Bitmap displayBitmap = null;

    private Mat originalMat = new Mat(); // store original image
    private Mat processedMat = new Mat(); // store after complete processing
    private Mat displayMat = new Mat();  // mat used for display only

    private Thread displayImageProcessThread;
    private boolean processedDisplayImageThreadStop = false;

    private SavedImageDetails imageDetails;

    private String lastPreparedFilename = null;


    public Mat getProcessedMat() {
        return processedMat;
    }

    public Mat getDisplayMat() {
        return displayMat;
    }

    public Bitmap getDisplayBitmap() {
        return displayBitmap;
    }


    public Mat getOriginalMat() {
        return originalMat;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageDetails = getImageDetails();
    }


    @Override
    public void processImage(ScanPreviewAdapter.ViewHolder holder, int position, boolean colorOnly) {




        getBinding().protector.setVisibility(View.VISIBLE);

        holder.processing.setVisibility(View.VISIBLE);
        Effects effects = imageDetails.getEffects(imageDetails.getAt(position));



        int colorCode = effects.color;
        boolean colorGray = effects.isGray;
        int colorTune = effects.colorTune;


        if (displayImageProcessThread != null) {
            processedDisplayImageThreadStop = true;


            try {
                displayImageProcessThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            processedDisplayImageThreadStop = false;

        }

        displayImageProcessThread = new Thread(new Runnable() {
            @Override
            public void run() {


                float widthScaleFactor = originalMat.width() * 1.0f / displayMat.width();
                float heightScaleFactor = originalMat.height() * 1.0f / displayMat.height();


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


                if (processedDisplayImageThreadStop)
                    return;


                Point point1 = new Point(cropBoundsMap.get(0).x, cropBoundsMap.get(0).y);
                Point point2 = new Point(cropBoundsMap.get(1).x, cropBoundsMap.get(1).y);
                Point point3 = new Point(cropBoundsMap.get(3).x, cropBoundsMap.get(3).y);
                Point point4 = new Point(cropBoundsMap.get(2).x, cropBoundsMap.get(2).y);

                int diffWidth = (int) ((Math.sqrt((point1.x - point2.x) * (point1.x - point2.x) + (point1.y - point2.y) * (point1.y - point2.y)) / 2) + (Math.sqrt((point3.x - point4.x) * (point3.x - point4.x) + (point3.y - point4.y) * (point3.y - point4.y)) / 2));
                int diffHeight = (int) ((Math.sqrt((point2.x - point3.x) * (point2.x - point3.x) + (point2.y - point3.y) * (point2.y - point3.y)) / 2) + (Math.sqrt((point2.x - point3.x) * (point2.x - point3.x) + (point2.y - point3.y) * (point2.y - point3.y)) / 2));


                if (processedDisplayImageThreadStop)
                    return;


                Mat src = new MatOfPoint2f(new Point(cropBoundsMap.get(0).x, cropBoundsMap.get(0).y), new Point(cropBoundsMap.get(1).x, cropBoundsMap.get(1).y), new Point(cropBoundsMap.get(3).x, cropBoundsMap.get(3).y), new Point(cropBoundsMap.get(2).x, cropBoundsMap.get(2).y));
                Mat dst = new MatOfPoint2f(new Point(0, 0), new Point(originalMat.width() - 1, 0), new Point(originalMat.width() - 1, originalMat.height() - 1), new Point(0, originalMat.height() - 1));


                if (processedDisplayImageThreadStop)
                    return;

                Mat transform = Imgproc.getPerspectiveTransform(src, dst);

                if (processedDisplayImageThreadStop)
                    return;
                Imgproc.warpPerspective(originalMat, processedMat, transform, originalMat.size());

                if (processedDisplayImageThreadStop)
                    return;
                Imgproc.resize(processedMat, processedMat, new Size(diffWidth, diffHeight));

                if (processedDisplayImageThreadStop)
                    return;


                MatFilter.colorize(processedMat, colorCode, colorTune, colorGray);

                if (processedDisplayImageThreadStop)
                    return;


                imageDetails.getEffects(imageDetails.getAt(position)).corners = (HashMap<Integer, PointF>) cropBoundsOriginalMap;


                String processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), imageDetails.getOrdering().get(position)).getAbsolutePath();


                if (imageDetails.sync()){
                    int rot = imageDetails.getRotation(imageDetails.getAt(position));


                    int[] parameters = {Imgcodecs.IMWRITE_JPEG_QUALITY,90};


                    BitmapUtils.rotateMatDegrees(processedMat, rot);
                    Imgcodecs.imwrite(processedImageFilepath, processedMat, new MatOfInt(parameters));

                }




                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        getAutoCroppedSet().add(imageDetails.getAt(position));

                        holder.polygonView.setVisibility(View.GONE);
                        holder.processing.setVisibility(View.GONE);
                        getRecyclerView().getAdapter().notifyDataSetChanged();
                        getBinding().protector.setVisibility(View.GONE);
                        setDocumentChanged(true);

                    }
                });

            }
        });

        displayImageProcessThread.start();


    }


    public void prepareMats(ScanPreviewAdapter.ViewHolder holder, int position) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                getBinding().protector.setVisibility(View.VISIBLE);
            }
        });

        if (lastPreparedFilename!=null){

            String currentFilename = getImageDetails().getAt(position);

            if (lastPreparedFilename.equals(currentFilename)) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        getBinding().protector.setVisibility(View.GONE);
                    }
                });
                return;
            }
        }

        originalMat = Imgcodecs.imread(getOriginalFilepaths().get(position));


        if (originalMat.channels() == 4)
            Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGRA2BGR);
//        else if(originalMat.channels()==3)
//            Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGR2RGB);


        Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(originalMat.width(), originalMat.height()), holder.imageView.getMeasuredWidth(), holder.imageView.getMeasuredHeight());

        Imgproc.resize(originalMat, displayMat, optimalImageSizeForDisplay);

        if (displayMat.channels()==3)
            Imgproc.cvtColor(displayMat, displayMat, Imgproc.COLOR_BGR2RGB);

        displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);


        org.opencv.android.Utils.matToBitmap(displayMat, displayBitmap);


        lastPreparedFilename = getImageDetails().getAt(position);



        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                getBinding().protector.setVisibility(View.GONE);
            }
        });

    }

}
