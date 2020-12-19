package com.aaindia.prodocscanner.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.aaindia.prodocscanner.App;
import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.EditScanViewActivity;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ScanViewActivity;
import com.aaindia.prodocscanner.constants.AdIds;
import com.aaindia.prodocscanner.databinding.ActivityImageCropBinding;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.ViewUtils;
import com.aaindia.prodocscanner.utils.ads.AdDialog;
import com.aaindia.prodocscanner.views.PolygonView;
import com.aaindia.prodocscanner.wrappers.BitmapMat;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.slider.Slider;
import com.google.firebase.analytics.FirebaseAnalytics;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;

public class ImageCropActivity extends AppCompatActivity implements View.OnClickListener {



    public static final String GLOBAL_ROTATION = "global_rotation";
    public static final String COLOR_TUNE = "color_tune";
    ActivityImageCropBinding binding;


    public static final String COLOR_CODE = "color_code";
    public static final String COLOR_IS_GRAY = "color_is_gray";
    public static final String CORNERS = "corners";

    public static final String ACTIVITY_NAME = "IMAGE_CROP_ACTIVITY";
    private Bitmap displayBitmap = null;


    private Mat originalMat = null; // store original image
    private Mat processedMat = new Mat(); // store after complete processing
    private Mat displayMat = null;  // mat used for display only
    private Mat lastCroppedMat = new Mat();

    public static int rotationDegrees = 0;


    private int colorCode = MatFilter.DEFAULT_COLOR_CODE;

    private int colorTune = MatFilter.getDefaultTune(colorCode);

    private boolean colorGray = false;
    private boolean cropStart = false;


    HashMap<Integer, PointF> cropBoundsOriginalMap = null;



    private Thread processThread;
    private String originalImageFilename;
    private String processedImageFilename;

    public static final String DOCUMENT_TYPE_KEY = "document_type";

    boolean isLoaded = false;

    boolean initialCropApplied = false;
    boolean isNextClicked = false;



    ExecutorService executorService;

    boolean isProcessing = false;

    public static int nTimesOpened=-1;
    public static boolean adNeverShown = true;
    private boolean adShown = false;

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        releaseMats();
        finish();
    }







    public void showAd(){




        if (CameraScanActivity.unifiedNativeAd ==null || adShown){
            return;
        }





        if(adNeverShown || nTimesOpened==5 || nTimesOpened==10 || (nTimesOpened>10 && nTimesOpened%10==0) )

        try {
            new AdDialog(ImageCropActivity.this)
                    .setAd(CameraScanActivity.unifiedNativeAd);
            adShown = true;
            adNeverShown = false;



        }
        catch (Exception e){}



    }






    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nTimesOpened++;



        executorService = Executors.newFixedThreadPool(2);




        originalImageFilename = getIntent().getExtras().getString(FileNav.ORIGINAL_IMAGE_FILE);
        processedImageFilename = getIntent().getExtras().getString(FileNav.PROCESSED_IMAGE_FILE);


        checkIfBitmapInMemory();


        globalRotation = 0;

        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_crop);

        binding.colorTuneSK.setValue(colorTune);
        binding.colorGrayCheck.setChecked(colorGray);

        binding.root.post(new Runnable() {
            @Override
            public void run() {
                loadOptimalImage();
            }
        });


        binding.nextIB.setOnClickListener(this);
        binding.nextCropIB.setOnClickListener(this);
        binding.CropRL.setOnClickListener(this);
        binding.noCropRL.setOnClickListener(this::onClick);
        binding.colorRL.setOnClickListener(this::onClick);
        binding.rotateRL.setOnClickListener(this::onClick);
        binding.retakeRL.setOnClickListener(this::onClick);


        binding.colorTuneSK.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {


            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {


                if (isNextClicked){
                    return;
                }

                if (!isLoaded) {
                    slider.setValue(colorTune);
                    return;
                }


                colorTune = (int) slider.getValue();

                processDisplayImage();

            }
        });


        binding.colorGrayCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


                if (isNextClicked){
                    return;
                }


                if (!compoundButton.isPressed())
                    return;


                if (isProcessing){

                    binding.colorGrayCheck.setChecked(!b);
                    return;
                }


                if (isLoaded) {
                    colorGray = b;
                    processDisplayImage();

                } else {
                    binding.colorGrayCheck.setChecked(!b);
                }
            }
        });


        colorCode = MatFilter.colorCodeFromDocumentType(getIntent().getExtras().getString(DOCUMENT_TYPE_KEY));

        colorTune = MatFilter.getDefaultTune(colorCode);


    }



    private void checkIfBitmapInMemory() {


        if (originalImageFilename == null || (onceProcessed && (processedMat.height() == 0 || displayMat.height() == 0 || displayBitmap.getHeight() == 0)) || (originalMat != null && originalMat.height() == 0)) {

//            Intent intent = new Intent(ImageCropActivity.this, MainActivity.class);
//
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//            startActivity(intent);
//            finish();

            loadOptimalImage();

        }
    }


    private void loadOptimalImage() {


        isLoaded = false;
        binding.protector.setVisibility(View.VISIBLE);

        binding.processing.setVisibility(View.VISIBLE);


        binding.polygonView.autoCropped = false;


        // set image view margin beforehand
        int margin = (int) (binding.polygonView.ballSize * 2 / 2);
        ((RelativeLayout.LayoutParams) binding.theImage.getLayoutParams()).setMargins(margin, margin, margin, margin);
        binding.theImage.requestLayout();

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                // copy original bitmap to "originalMat" and recycle the bitmap later on

                File imageFile = FileNav.getTempFile(ImageCropActivity.this, "single_mode_capture.jpg");
                originalMat = Imgcodecs.imread(imageFile.getAbsolutePath());

                if (originalMat==null || originalMat.width()==0){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Not an image file",Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });

                    return;
                }

                if (originalMat.channels() == 4)
                    Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGRA2RGB);
                else if (originalMat.channels() == 3)
                    Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGR2RGB);


                //get optimal imageview size
                Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(originalMat.width(), originalMat.height()), binding.theImage.getMeasuredWidth(), binding.theImage.getMeasuredHeight());
                // original bitmap recycled

                displayMat = new Mat();
                Imgproc.resize(originalMat, displayMat, optimalImageSizeForDisplay);

                if (displayBitmap != null)
                    displayBitmap.recycle();

                displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(displayMat, displayBitmap);

                //Thread thread = Thread.currentThread();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        binding.theImage.getLayoutParams().width = (int) displayBitmap.getWidth();
                        binding.theImage.getLayoutParams().height = (int) displayBitmap.getHeight();
                        binding.theImage.setImageBitmap(displayBitmap);
                        binding.theImage.requestLayout();

                        initAutoCrop();


                    }
                });

            }
        });


    }

    private Bitmap bitmapTemp = null;

    private void initAutoCrop() {

        zoomageEnable(false);
        nextCropEnableDisable(true);
        cropStart = true;
        initialCropApplied = false;


        if (!binding.polygonView.autoCropped) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {

                    MatOfPoint2f cropBoundsMat = new MatOfPoint2f();

                    HashMap<Integer, PointF> cropBoundsMap = new HashMap<>();
                    Point[] sortedPoints = null;

                    try {
                        MatFilter.cropV1(displayMat.getNativeObjAddr(), cropBoundsMat.getNativeObjAddr());

                       sortedPoints = BitmapUtils.sortMatofPoints2f(cropBoundsMat, new Size(displayMat.width(), displayMat.height()));


                    }

                    catch (Exception e){

                        sortedPoints = new Point[4];

                        sortedPoints[0] = new Point(0,0);
                        sortedPoints[1] = new Point(originalMat.width(),0);
                        sortedPoints[2] = new Point(0,originalMat.height());
                        sortedPoints[3] = new Point(originalMat.width(),originalMat.height());

                    }

                    catch (Error e2){

                        sortedPoints = new Point[4];

                        sortedPoints[0] = new Point(0,0);
                        sortedPoints[1] = new Point(originalMat.width(),0);
                        sortedPoints[2] = new Point(0,originalMat.height());
                        sortedPoints[3] = new Point(originalMat.width(),originalMat.height());


                    }








                    for (int i = 0; i < sortedPoints.length; i++) {
                        cropBoundsMap.put(i, new PointF((float) sortedPoints[i].x, (float) sortedPoints[i].y));
                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            nextCropEnableDisable(true);
                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.theImage.getLayoutParams();
                            binding.theImage.setLayoutParams(params);

                            RelativeLayout.LayoutParams params1 = ViewUtils.copy(params);

                            binding.polygonView.setPoints(cropBoundsMap);

                            binding.polygonView.setLayoutParams(params1);
                            binding.polygonView.getLayoutParams().height += binding.polygonView.ballSize;
                            binding.polygonView.getLayoutParams().width += binding.polygonView.ballSize;
                            binding.polygonView.requestLayout();
                            binding.polygonView.autoCropped = true;

                            rotate(rotationDegrees);
                            binding.processing.setVisibility(View.GONE);
                            binding.protector.setVisibility(View.GONE);

                            isLoaded = true;


                            binding.polygonView.pointMove = new PolygonView.OnPointMove() {
                                @Override
                                public void onMove(double x, double y) {


                                    x = x - 40;
                                    y = y - 40;

                                    int tempX, tempY;
                                    binding.roi1IV.setVisibility(View.GONE);
                                    if (bitmapTemp != null) {
                                        bitmapTemp.recycle();
                                    }

                                    if (x >= 0 && x < (displayBitmap.getWidth() - 80) && y >= 0 && y < (displayBitmap.getHeight() - 80)) {


                                        bitmapTemp = Bitmap.createBitmap(displayBitmap, (int) x, (int) y, 80, 80);

                                    } else {

                                        bitmapTemp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);

                                        for (int i = (int) x; i < (int) x + 80; i++) {


                                            if (i >= 0 && i < displayBitmap.getWidth()) {
                                                for (int j = (int) y; j < (int) y + 80; j++) {


                                                    if (j >= 0 && j < displayBitmap.getHeight()) {

                                                        tempX = (int) (i - x);
                                                        tempY = (int) (j - y);

                                                        if (tempX <= 79 && tempX <= 79)
                                                            bitmapTemp.setPixel(tempX, tempY, displayBitmap.getPixel(i, j));

                                                    }

                                                }
                                            }
                                        }
                                    }

                                    binding.roi1IV.setImageBitmap(bitmapTemp);

                                    binding.roi1IV.setVisibility(View.VISIBLE);
                                    binding.roi1IV.setRotation(binding.theImageParent.getRotation());

                                }

                                @Override
                                public void onStop() {

                                    ValueAnimator animator = ValueAnimator.ofFloat(1, 0);

                                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                            float val = (float) valueAnimator.getAnimatedValue();
                                            binding.roitRoot.setAlpha(val);
                                            if (val == 0) {
                                                binding.roitRoot.setVisibility(View.GONE);
                                            }
                                        }
                                    });

                                    animator.setDuration(200);
                                    animator.start();

                                }

                                @Override
                                public void onStart() {

                                    binding.roitRoot.setVisibility(View.VISIBLE);

                                    ValueAnimator animator = ValueAnimator.ofFloat(0, 1);

                                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                            float val = (float) valueAnimator.getAnimatedValue();
                                            binding.roitRoot.setAlpha(val);
                                        }
                                    });

                                    animator.setDuration(200);
                                    animator.start();

                                }
                            };


                        }
                    });

                }
            });
        } else {
            nextCropEnableDisable(true);
        }


    }

    private void zoomageEnable(boolean b) {
        binding.theImage.setZoomable(b);
        binding.theImage.setDoubleTapToZoom(b);
        binding.theImage.setTranslatable(b);

    }


    boolean onceProcessed = false;

    private synchronized void processDisplayImage() {


        if (isProcessing)
            return;

        isProcessing = true;

        isNextClicked = true;


        onceProcessed = true;

        binding.processing.setVisibility(View.VISIBLE);
        binding.protector.setVisibility(View.VISIBLE);

        binding.colorTuneSK.setValue(colorTune);

        binding.colorGrayCheck.setChecked(colorGray);


        executorService.execute(new Runnable() {
            @Override
            public void run() {


                if (cropStart) {


                    float widthScaleFactor = originalMat.width() * 1.0f / displayMat.width();
                    float heightScaleFactor = originalMat.height() * 1.0f / displayMat.height();


                    Map<Integer, PointF> cropBoundsMap = binding.polygonView.getPoints();


                    if (cropBoundsMap == null || cropBoundsMap.get(0) == null) {
                        cropStart = false;
                        return;
                    }

                    if (cropBoundsOriginalMap == null)
                        cropBoundsOriginalMap = new HashMap<>();

                    for (int i = 0; i < 4; i++) {

                        PointF pointF = cropBoundsMap.get(i);


                        pointF.x *= widthScaleFactor;
                        pointF.y *= heightScaleFactor;

                        cropBoundsOriginalMap.put(i, new PointF(pointF.x, pointF.y));


                    }



                    Point point1 = new Point(cropBoundsMap.get(0).x, cropBoundsMap.get(0).y);
                    Point point2 = new Point(cropBoundsMap.get(1).x, cropBoundsMap.get(1).y);
                    Point point3 = new Point(cropBoundsMap.get(3).x, cropBoundsMap.get(3).y);
                    Point point4 = new Point(cropBoundsMap.get(2).x, cropBoundsMap.get(2).y);

                    int diffWidth = (int) ((Math.sqrt((point1.x - point2.x) * (point1.x - point2.x) + (point1.y - point2.y) * (point1.y - point2.y)) / 2) + (Math.sqrt((point3.x - point4.x) * (point3.x - point4.x) + (point3.y - point4.y) * (point3.y - point4.y)) / 2));
                    int diffHeight = (int) ((Math.sqrt((point2.x - point3.x) * (point2.x - point3.x) + (point2.y - point3.y) * (point2.y - point3.y)) / 2) + (Math.sqrt((point2.x - point3.x) * (point2.x - point3.x) + (point2.y - point3.y) * (point2.y - point3.y)) / 2));




                    Mat src = new MatOfPoint2f(new Point(cropBoundsMap.get(0).x, cropBoundsMap.get(0).y), new Point(cropBoundsMap.get(1).x, cropBoundsMap.get(1).y), new Point(cropBoundsMap.get(3).x, cropBoundsMap.get(3).y), new Point(cropBoundsMap.get(2).x, cropBoundsMap.get(2).y));
                    Mat dst = new MatOfPoint2f(new Point(0, 0), new Point(originalMat.width() - 1, 0), new Point(originalMat.width() - 1, originalMat.height() - 1), new Point(0, originalMat.height() - 1));



                    Mat transform = Imgproc.getPerspectiveTransform(src, dst);




                    Imgproc.warpPerspective(originalMat, processedMat, transform, originalMat.size());


                    Imgproc.resize(processedMat, processedMat, new Size(diffWidth, diffHeight));



                    processedMat.copyTo(lastCroppedMat);




                } else {


                    lastCroppedMat.copyTo(processedMat);



                }




                MatFilter.colorize(processedMat, colorCode, colorTune, colorGray);



                Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(processedMat.width(), processedMat.height()), binding.theImage.getMeasuredWidth(), binding.theImage.getMeasuredHeight());

                Imgproc.resize(processedMat, displayMat, optimalImageSizeForDisplay);

                displayBitmap.recycle();

                displayBitmap = Bitmap.createBitmap((int) optimalImageSizeForDisplay.width, (int) optimalImageSizeForDisplay.height, Bitmap.Config.ARGB_8888);


                Utils.matToBitmap(displayMat, displayBitmap);




                cropStart = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        binding.processing.setVisibility(View.GONE);
                        binding.polygonView.setVisibility(View.GONE);
                        binding.theImage.setImageBitmap(displayBitmap);

                        nextCropEnableDisable(false);

                        zoomageEnable(true);
                        isNextClicked = false;
                        isProcessing = false;

                        binding.protector.setVisibility(View.GONE);
                    }
                });

            }
        });


    }


    private synchronized void processImage() {



        if (isProcessing)
            return;



        BitmapUtils.rotateMatDegrees(processedMat, globalRotation);



        if (originalMat.channels() == 3)
            Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGR2RGB);




        if (processedMat.channels() == 3)
            Imgproc.cvtColor(processedMat, processedMat, Imgproc.COLOR_BGR2RGB);



        int[] parameters = {Imgcodecs.IMWRITE_JPEG_QUALITY, 90};


        Imgcodecs.imwrite(originalImageFilename, originalMat, new MatOfInt(parameters));
        Imgcodecs.imwrite(processedImageFilename, processedMat, new MatOfInt(parameters));
        // BitmapUtils.setExifRotationDegrees(processedImageFilename, 0);


    }


    public void setSpanActionColor(SpannableString s, int compare1, int compare2) {

        if (compare1 == compare2)
            s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSecondary)), 0, s.length(), 0);
    }


    private void chooseColor() {

        PopupMenu popupMenu = new PopupMenu(ImageCropActivity.this, binding.colorRL);

        Menu menu = popupMenu.getMenu();

        SpannableString original = new SpannableString("Orignal");
        SpannableString contrast = new SpannableString("Photo");
        SpannableString paperStyle = new SpannableString("Note");
        SpannableString whiteBoardStyle = new SpannableString("Document");

        setSpanActionColor(original, colorCode, MatFilter.COLOR_ORIGINAL);
        setSpanActionColor(contrast, colorCode, MatFilter.COLOR_CONTRAST);
        setSpanActionColor(paperStyle, colorCode, MatFilter.COLOR_PAPER);
        setSpanActionColor(whiteBoardStyle, colorCode, MatFilter.COLOR_WHITEBOARD);

        menu.add(0, MatFilter.COLOR_ORIGINAL, 0, original);
        menu.add(0, MatFilter.COLOR_CONTRAST, 0, contrast);
        menu.add(0, MatFilter.COLOR_PAPER, 0, paperStyle);
        menu.add(0, MatFilter.COLOR_WHITEBOARD, 0, whiteBoardStyle);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int id = item.getItemId();

                colorCode = id;

                colorTune = MatFilter.getDefaultTune(id);


                processDisplayImage();

                return true;
            }
        });

        popupMenu.show();


    }


    private void nextCropEnableDisable(boolean cropEnable) {

        if (cropEnable) {
            binding.polygonView.setVisibility(View.VISIBLE);
            binding.nextCropIB.setVisibility(View.VISIBLE);
            binding.nextIB.setVisibility(View.GONE);
            binding.nextTV.setVisibility(View.GONE);
            binding.nextTVCrop.setVisibility(View.VISIBLE);
        } else {
            binding.polygonView.setVisibility(View.GONE);
            binding.nextCropIB.setVisibility(View.GONE);
            binding.nextIB.setVisibility(View.VISIBLE);
            binding.nextTV.setVisibility(View.VISIBLE);
            binding.nextTVCrop.setVisibility(View.GONE);
        }
    }




    private void next() {

        binding.processing.setVisibility(View.VISIBLE);

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                processImage();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        binding.processing.setVisibility(View.GONE);
                        Intent returnIntent = new Intent();


                        returnIntent.putExtra(FileNav.ORIGINAL_IMAGE_FILE, originalImageFilename);
                        returnIntent.putExtra(FileNav.PROCESSED_IMAGE_FILE, processedImageFilename);
                        returnIntent.putExtra(CORNERS, cropBoundsOriginalMap);
                        returnIntent.putExtra(COLOR_CODE, colorCode);
                        returnIntent.putExtra(GLOBAL_ROTATION, globalRotation);
                        returnIntent.putExtra(COLOR_IS_GRAY, colorGray);
                        returnIntent.putExtra(COLOR_TUNE, colorTune);

                        setResult(Activity.RESULT_OK, returnIntent);


                        releaseMats();

                        finish();
                    }
                });

            }
        });


    }

    private void releaseMats() {

        try {

            if (originalMat != null) {
                originalMat.release();
            }

            if (displayMat != null) {
                displayMat.release();
            }

            if (processedMat != null) {
                processedMat.release();
            }

            if (lastCroppedMat != null) {
                lastCroppedMat.release();
            }

        } catch (Exception e) {
        }


    }

    @Override
    public void onClick(View view) {

        int id = view.getId();


        if (isNextClicked){
            return;
        }


        switch (id) {


            case R.id.noCropRL:

                if (isProcessing)
                    return;

                if (!isLoaded)
                    return;

                initialCropApplied  = false;
                zoomageEnable(true);

                if (displayBitmap != null)
                    displayBitmap.recycle();


                Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(originalMat.width(), originalMat.height()), binding.theImage.getMeasuredWidth(), binding.theImage.getMeasuredHeight());


                Imgproc.resize(originalMat, displayMat, optimalImageSizeForDisplay);
                displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(displayMat, displayBitmap);
                binding.theImage.setImageBitmap(displayBitmap);


                HashMap<Integer, PointF> cropBoundsMap = new HashMap<>();

                cropBoundsMap.put(0, new PointF((float) 0, (float) 0));

                cropBoundsMap.put(1, new PointF((float) displayMat.width(), (float) 0));

                cropBoundsMap.put(2, new PointF((float) 0, (float) displayMat.height()));

                cropBoundsMap.put(3, new PointF((float) displayMat.width(), (float) displayMat.height()));

                binding.polygonView.setPoints(cropBoundsMap);


                displayBitmap.recycle();
                displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(displayMat, displayBitmap);
                binding.theImage.setImageBitmap(displayBitmap);
                nextCropEnableDisable(true);

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.theImage.getLayoutParams();
                RelativeLayout.LayoutParams params1 = ViewUtils.copy(params);

                binding.polygonView.setPoints(cropBoundsMap);

                binding.polygonView.setLayoutParams(params1);
                binding.polygonView.getLayoutParams().height += binding.polygonView.ballSize;
                binding.polygonView.getLayoutParams().width += binding.polygonView.ballSize;
                binding.polygonView.requestLayout();
                binding.polygonView.autoCropped = true;

                break;


            case R.id.nextCropIB:

                if (isProcessing)
                    return;

                if (initialCropApplied){
                    return;
                }


                if (!isLoaded)
                    return;

                initialCropApplied = true;

                showAd();
                processDisplayImage();
                break;

            case R.id.nextIB:

                if (isProcessing)
                    return;


                isNextClicked = true;

                if (!isLoaded)
                    return;

                if (cropStart)
                    return;

                next();

                break;

            case R.id.CropRL:


                if (isProcessing)
                    return;

                if (!isLoaded)
                    return;

                if (!binding.polygonView.autoCropped)
                    return;


                Size optimalImageSizeForDisplay2 = BitmapUtils.getReducedBitmapSize(new Size(originalMat.width(), originalMat.height()), binding.theImage.getMeasuredWidth(), binding.theImage.getMeasuredHeight());


                Imgproc.resize(originalMat, displayMat, optimalImageSizeForDisplay2);

                displayBitmap.recycle();
                displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(displayMat, displayBitmap);
                binding.theImage.setImageBitmap(displayBitmap);
                initAutoCrop();

                break;

            case R.id.colorRL:

                if (isProcessing)
                    return;

                if (!isLoaded)
                    return;

                if (cropStart) {


                    new GuideView.Builder(ImageCropActivity.this)
                            .setTitle("Crop")
                            .setContentSpan((Spannable) Html.fromHtml("<b>Crop</b> the image at first."))
                            .setGravity(Gravity.auto) //optional
                            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                            .setTargetView(binding.nextCropIB)
                            .build().show();
                    return;


                }

                chooseColor();

                break;


            case R.id.retakeRL:


                onBackPressed();

                break;

            case R.id.rotateRL:


                if (isProcessing)
                    return;

                if (!isLoaded)
                    return;


                if (cropStart) {


                    new GuideView.Builder(ImageCropActivity.this)
                            .setTitle("Crop")
                            .setContentSpan((Spannable) Html.fromHtml("<b>Crop</b> the image at first."))
                            .setGravity(Gravity.auto) //optional
                            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                            .setTargetView(binding.nextCropIB)
                            .build().show();
                    return;


                }

                rotate(90);

                break;


        }
    }


    private int globalRotation = 0;

    private void rotate(int rotateBy) {


        globalRotation += rotateBy;

        //globalRotation = 0;

        if (Math.abs(globalRotation) > 360)
            globalRotation = 90;


        if (globalRotation >= 90)
            binding.theImageParent.setRotation(globalRotation - 90);


        double scale = 1.0;


        if (Math.abs(globalRotation) == 90 || Math.abs(globalRotation) == 270) {


            int maxWidth = (int) ((int) (binding.theImageParentParent.getMeasuredWidth()) * 0.93);
            int maxHeight = (int) ((int) (binding.theImageParentParent.getMeasuredHeight()) * 0.93);

            int currentWidth = displayBitmap.getHeight();
            int currentHeight = displayBitmap.getWidth();


            int newWidth = maxWidth;

            int newHeight = (int) (currentHeight * (newWidth) * 1.0 / currentWidth);

            if (newHeight > maxHeight) {
                //scale height

                newHeight = maxHeight;
                newWidth = (int) (currentWidth * 1.0 * (newHeight / currentHeight));

            }


            scale = newHeight * 1.0f / currentHeight * 1.0f;


        } else if (Math.abs(globalRotation) == 0 || Math.abs(globalRotation) == 180) {


        }


        binding.theImageParent.setRotation(globalRotation);


        binding.theImageParent.setScaleX((float) scale);
        binding.theImageParent.setScaleY((float) scale);
        binding.polygonView.scaleDrawing(scale);


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();

        com.aaindia.prodocscanner.utils.Utils.checkOpenCV(this);

        checkIfBitmapInMemory();


        if (executorService == null || executorService.isTerminated() || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(2);
        }


    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {
            executorService.shutdown();

            while (!(executorService.isTerminated() || executorService.isShutdown())) {
            }
        } catch (Exception e) {
        }


    }
}