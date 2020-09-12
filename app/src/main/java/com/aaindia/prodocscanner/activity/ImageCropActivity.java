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
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
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

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.databinding.ActivityImageCropBinding;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.ViewUtils;
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
import com.xw.repo.BubbleSeekBar;

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

public class ImageCropActivity extends AppCompatActivity implements View.OnClickListener {


    public static final String GLOBAL_ROTATION = "global_rotation";
    public static final String COLOR_TUNE = "color_tune";
    ActivityImageCropBinding binding;


    public static final String COLOR_CODE = "color_code";
    public static final String COLOR_IS_GRAY = "color_is_gray";
    public static final String CORNERS = "corners";

    public static Bitmap originalBitmap = null;
    private Bitmap displayBitmap = null;


    private Mat originalMat = null; // store original image
    private Mat processedMat = new Mat(); // store after complete processing
    private Mat displayMat = null;  // mat used for display only
    private Mat lastCroppedMat = new Mat();

    public static int rotationDegrees = 0;


    private int colorCode = MatFilter.DEFAULT_COLOR_CODE;

    private int colorTune = MatFilter.getDefaultTune(colorCode);

    private boolean colorGray = true;
    private boolean cropStart = false;


    HashMap<Integer, PointF> cropBoundsOriginalMap = null;

    private boolean processedImageThreadStop = false;
    private boolean processedDisplayImageThreadStop = false;
    private Thread processThread;
    private String originalImageFilename;
    private String processedImageFilename;
    private Thread displayImageProcessThread;


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        originalImageFilename = getIntent().getExtras().getString(FileNav.ORIGINAL_IMAGE_FILE);
        processedImageFilename = getIntent().getExtras().getString(FileNav.PROCESSED_IMAGE_FILE);

        if (originalBitmap == null || originalImageFilename == null) {

            Intent intent = new Intent(ImageCropActivity.this, MainActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);

        }


        globalRotation = 0;

        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_crop);

        binding.colorTuneSK.setProgress(colorTune);
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


        binding.colorTuneSK.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

                colorTune = (int) progress;

                processDisplayImage();
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {


            }
        });


        binding.colorGrayCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                colorGray = b;


                processDisplayImage();
            }
        });


    }


    private void loadOptimalImage() {

        binding.processing.setVisibility(View.VISIBLE);


        // set image view margin beforehand
        int margin = (int) (binding.polygonView.ballSize * 2 / 2);
        ((RelativeLayout.LayoutParams) binding.theImage.getLayoutParams()).setMargins(margin, margin, margin, margin);
        binding.theImage.requestLayout();

        new Thread(new Runnable() {
            @Override
            public void run() {

                // copy original bitmap to "originalMat" and recycle the bitmap later on
                originalMat = new Mat();
                Utils.bitmapToMat(originalBitmap, originalMat);

                if (originalMat.channels() == 4)
                    Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGRA2BGR);


                //get optimal imageview size
                Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(originalBitmap.getWidth(), originalBitmap.getHeight()), binding.theImage.getMeasuredWidth(), binding.theImage.getMeasuredHeight());
                originalBitmap.recycle(); // original bitmap recycled

                displayMat = new Mat();
                Imgproc.resize(originalMat, displayMat, optimalImageSizeForDisplay);

                if (displayBitmap != null)
                    displayBitmap.recycle();

                displayBitmap = Bitmap.createBitmap(displayMat.width(), displayMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(displayMat, displayBitmap);

                Thread thread = Thread.currentThread();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        binding.theImage.getLayoutParams().width = (int) displayBitmap.getWidth();
                        binding.theImage.getLayoutParams().height = (int) displayBitmap.getHeight();
                        binding.theImage.setImageBitmap(displayBitmap);
                        binding.theImage.requestLayout();

                        initAutoCrop();


                    }
                });

            }
        }).start();


    }

    private void initAutoCrop() {

        nextCropEnableDisable(true);
        cropStart = true;


        if (!binding.polygonView.autoCropped) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    MatOfPoint2f cropBoundsMat = new MatOfPoint2f();
                    MatFilter.cropV1(displayMat.getNativeObjAddr(), cropBoundsMat.getNativeObjAddr());

                    HashMap<Integer, PointF> cropBoundsMap = new HashMap<>();
                    Point[] sortedPoints = BitmapUtils.sortMatofPoints2f(cropBoundsMat);

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
                            binding.processing.setVisibility(View.GONE);  binding.protector.setVisibility(View.GONE);


                        }
                    });

                }
            }).start();
        } else {
            nextCropEnableDisable(true);
        }


    }


    private void processDisplayImage() {


        binding.processing.setVisibility(View.VISIBLE);

        binding.colorTuneSK.setProgress(colorTune);

        binding.colorGrayCheck.setChecked(colorGray);


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


                if (cropStart) {


                    float widthScaleFactor = originalMat.width() * 1.0f / displayMat.width();
                    float heightScaleFactor = originalMat.height() * 1.0f / displayMat.height();


                    Map<Integer, PointF> cropBoundsMap = binding.polygonView.getPoints();


                    if (cropBoundsOriginalMap == null)
                        cropBoundsOriginalMap = new HashMap<>();

                    for (int i = 0; i < 4; i++) {

                        PointF pointF = cropBoundsMap.get(i);


                        pointF.x *= widthScaleFactor;
                        pointF.y *= heightScaleFactor;

                        cropBoundsOriginalMap.put(i, new PointF(pointF.x, pointF.y));


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

                    processedMat.copyTo(lastCroppedMat);

                    if (processedDisplayImageThreadStop)
                        return;


                } else {

                    if (processedDisplayImageThreadStop)
                        return;
                    lastCroppedMat.copyTo(processedMat);

                    if (processedDisplayImageThreadStop)
                        return;

                }


                if (processedDisplayImageThreadStop)
                    return;


                MatFilter.colorize(processedMat, colorCode, colorTune, colorGray);

                if (processedDisplayImageThreadStop)
                    return;


                Size optimalImageSizeForDisplay = BitmapUtils.getReducedBitmapSize(new Size(processedMat.width(), processedMat.height()), binding.theImage.getMeasuredWidth(), binding.theImage.getMeasuredHeight());

                Imgproc.resize(processedMat, displayMat, optimalImageSizeForDisplay);

                displayBitmap.recycle();

                displayBitmap = Bitmap.createBitmap((int) optimalImageSizeForDisplay.width, (int) optimalImageSizeForDisplay.height, Bitmap.Config.ARGB_8888);


                if (processedDisplayImageThreadStop)
                    return;
                Utils.matToBitmap(displayMat, displayBitmap);

                if (processedDisplayImageThreadStop)
                    return;


                cropStart = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        binding.processing.setVisibility(View.GONE);
                        binding.polygonView.setVisibility(View.GONE);
                        binding.theImage.setImageBitmap(displayBitmap);

                        nextCropEnableDisable(false);


                        zoomageEnableDisable(true);

                    }
                });

            }
        });

        displayImageProcessThread.start();


    }

    private void zoomageEnableDisable(boolean enable){



    }

    private synchronized void processImage() {


        if (processedImageThreadStop)
            return;

        BitmapUtils.rotateMatDegrees(processedMat, globalRotation);


        if (processedImageThreadStop)
            return;

        if (originalMat.channels() == 3)
            Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGR2RGB);

        if (processedImageThreadStop)
            return;


        //BitmapUtils.setExifRotationDegrees(originalImageFilename, globalRotation);

        if (processedImageThreadStop)
            return;

        if (processedMat.channels() == 3)
            Imgproc.cvtColor(processedMat, processedMat, Imgproc.COLOR_BGR2RGB);

        if (processedImageThreadStop)
            return;


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
        SpannableString contrast = new SpannableString("Contrast");
        SpannableString paperStyle = new SpannableString("Paper ");
        SpannableString whiteBoardStyle = new SpannableString("Clean Text");

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

                if (colorCode == MatFilter.COLOR_WHITEBOARD || colorCode == MatFilter.COLOR_PAPER) {
                    colorGray = true;
                    colorTune = MatFilter.getDefaultTune(id);
                } else {
                    colorGray = false;

                }


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


    private boolean nextClickec = false;

    private void next() {

        if (nextClickec)
            return;
        ;

        nextClickec = true;
        binding.processing.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
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
                        finish();
                    }
                });

            }
        }).start();


    }

    @Override
    public void onClick(View view) {

        int id = view.getId();


        switch (id) {


            case R.id.noCropRL:



                zoomageEnableDisable(false);
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


                processDisplayImage();
                break;

            case R.id.nextIB:

                if (cropStart)
                    return;

                next();

                break;

            case R.id.CropRL:


                zoomageEnableDisable(false);

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

                if (cropStart)
                    return;

                chooseColor();

                break;


            case R.id.retakeRL:


                onBackPressed();

                break;

            case R.id.rotateRL:


                if (cropStart)
                    return;

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

        int width = binding.theImageParent.getWidth();
        int height = binding.theImageParent.getHeight();


        double scale = 1.0;


        if (Math.abs(globalRotation) == 90 || Math.abs(globalRotation) == 270) {


            int maxWidth = binding.theImageParentParent.getMeasuredWidth();
            int maxHeight = binding.theImageParentParent.getMeasuredHeight();

            int currentWidth = displayMat.height();
            int currentHeight = displayMat.width();


            // try scaling width

            int newWidth = currentWidth * maxHeight / maxWidth;
            int newHeight = newWidth * currentHeight / currentWidth;


            if (newWidth > maxWidth) {
                newHeight = currentHeight * maxWidth / maxHeight;
                newWidth = newHeight * currentWidth / currentHeight;
            }

            scale = newHeight * 1.0 / newWidth;


        }


        ValueAnimator anim = ValueAnimator.ofFloat(binding.theImageParent.getRotation(), globalRotation);


        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();

//                binding.theImageParent.setRotation(val);
//
//                if (val >= 350)
//                    binding.theImageParent.setRotation(0);


            }
        });

        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(300);
        anim.start();

        binding.theImageParent.setRotation(globalRotation);


        binding.theImageParent.setScaleX((float) scale);
        binding.theImageParent.setScaleY((float) scale);


    }


    private native void cropV1(long nativeObjAddr, long nativeObjAddr1);


}