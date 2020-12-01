package com.aaindia.prodocscanner.activity;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.adapters.HorizontalDocumentChooserAdapter;
import com.aaindia.prodocscanner.constants.Constants;
import com.aaindia.prodocscanner.databinding.ActivityCameraScanBinding;
import com.aaindia.prodocscanner.databinding.ActivityMainBinding;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.MyLinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.io.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;


public class CameraPreviewActivity extends AppCompatActivity implements View.OnClickListener {



    private static final int PERMISION_REQUEST_CODE = 313;
    private ActivityCameraScanBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture = null;
    private Camera camera;
    private Preview preview;
    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;
    private boolean torchEnabled = false;

    private int focusDrawableMarginOffset = 0;

    private int targetWidth = -1;
    private int targetHeight = -1;
    private int imageCaptureRotation = -1;

    private boolean requestPermissionEnabled = true;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    private String documentType = Constants.DEFAULT_DOCUMENT_TYPE;


    public String getDocumentType() {
        return documentType;
    }

    public void setRequestPermission(boolean requestPermissionEnabled) {
        this.requestPermissionEnabled = requestPermissionEnabled;
    }


    @Override
    public void onResume() {
        super.onResume();

        binding.cameraCapture.setAlpha(1.0f);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED && cameraProviderFuture == null) {


            finish();


        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorSecondaryDark));
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera_scan);

        setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());


        try {

            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

            String[] cameraList = cameraManager.getCameraIdList();


            long maxRes = -1;

            Size[] cameraSize = null;

            for (final String cameraId : cameraList) {

                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);


                for (Size size : sizes) {
                    long res = size.getHeight() * size.getWidth() * 1l;

                    if (res > maxRes) {
                        maxRes = res;
                        cameraSize = sizes;
                    }

                }

            }



            // nearest resolution search

            long targetResolution = 4000l * 3000l;

            // find which is nearest

            Size targetSize = null;

            long error = 9999999999999999l;

            for (Size size : cameraSize) {




                long res = size.getHeight() * size.getWidth() * 1l;



                long absError = Math.abs(res - targetResolution);

                if (absError < error) {
                    targetSize = size;
                    error = absError;
                }
            }


            targetHeight = targetSize.getHeight();
            targetWidth = targetSize.getWidth();


            float aspectRatio = targetHeight>targetWidth? targetHeight*1.0f/targetWidth : targetWidth*1.0f/targetHeight;

            if (targetWidth * targetHeight < (2500 * 3200) || aspectRatio<1.1 || aspectRatio>1.5) {
                targetHeight = -1;
                targetWidth = -1;
            }

        } catch (Exception e) {

        }


        checkCameraPermission();


        binding.cameraCapture.setOnClickListener(this);
        binding.flashIV.setOnClickListener(this);
        binding.scanModeIV.setOnClickListener(this);
        binding.importIV.setOnClickListener(this::onClick);
        binding.next.setOnClickListener(this::onClick);
        binding.backArrow.setOnClickListener(this::onClick);
        binding.torchIV.setOnClickListener(this::onClick);
        binding.flipCamera.setOnClickListener(this::onClick);

        setViews();

        Drawable drawable = getResources().getDrawable(R.drawable.baseline_crop_free_white_48);

        focusDrawableMarginOffset = drawable.getIntrinsicHeight() / 2;


        documentTypeChooser();

    }

    private void checkCameraPermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {


                startCamera();

            } else {
                requestCameraPermision();
            }

        } else {

            startCamera();

        }
    }

    boolean shallVibrate = false;

    private void documentTypeChooser() {


        int width = getResources().getDisplayMetrics().widthPixels;

        binding.horizontalPicker.setPadding(width / 3, 0, width / 3, 0);
        binding.horizontalPicker.requestLayout();

        binding.horizontalPicker.setLayoutManager(new MyLinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false));


        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(binding.horizontalPicker);


        binding.horizontalPicker.addOnScrollListener(new RecyclerView.OnScrollListener() {


            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);


                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(binding.horizontalPicker.getLayoutManager());
                    int pos = binding.horizontalPicker.getLayoutManager().getPosition(centerView);


                    documentType = Constants.documentImageTypes().get(pos);


                    String text = Constants.documentImageTypes().get(pos);
                    documentType = text;


                    try {


                        for (int i = 0; i < Constants.documentImageTypes().size(); i++) {

                            HorizontalDocumentChooserAdapter.Viewholder viewholder = (HorizontalDocumentChooserAdapter.Viewholder) binding.horizontalPicker.findViewHolderForLayoutPosition(i);

                            if (viewholder != null) {

                                if (pos != i) {
                                    viewholder.documentItem.setTextColor(Color.GRAY);
                                } else {
                                    viewholder.documentItem.setTextColor(Color.WHITE);
                                }
                                if (shallVibrate)
                                    Utils.vibrate(CameraPreviewActivity.this, 20);
                            }
                        }


                    } catch (Exception e) {
                    }


                }
            }
        });


        HorizontalDocumentChooserAdapter horizontalDocumentChooserAdapter = new HorizontalDocumentChooserAdapter(this

                , new HorizontalDocumentChooserAdapter.OnItemTouchListener() {
            @Override
            public void onTouch(HorizontalDocumentChooserAdapter.Viewholder viewholder, int position) {

                binding.horizontalPicker.smoothScrollToPosition(position);
                shallVibrate = true;
                if (shallVibrate)
                    Utils.vibrate(CameraPreviewActivity.this, 20);
            }
        }
        );

        binding.horizontalPicker.setAdapter(horizontalDocumentChooserAdapter);

        binding.horizontalPicker.smoothScrollToPosition(1);


    }

    private void startCamera() {


        showCase();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showCase() {


        if (!Prefs.firstTimeSeenScreen(CameraPreviewActivity.this, "camera_preview")) {
            new GuideView.Builder(this)
                    .setTitle("Scan Mode")
                    .setContentSpan((Spannable) Html.fromHtml("Switch between <b>batch</b> mode and <b>single</b> mode."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(binding.scanModeIV)

                    .setGuideListener(new GuideListener() {
                        @Override
                        public void onDismiss(View view) {


                            new GuideView.Builder(CameraPreviewActivity.this)
                                    .setTitle("Select scan type")

                                    .setContentSpan((Spannable) Html.fromHtml("<b>Pre-select filter</b> based on the <b>scan type</b>."))
                                    .setGravity(Gravity.auto) //optional
                                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                    .setTargetView(binding.horizontalPicker)
                                    .setGuideListener(new GuideListener() {
                                        @Override
                                        public void onDismiss(View view) {
                                            new GuideView.Builder(CameraPreviewActivity.this)
                                                    .setTitle("Import images")

                                                    .setContentSpan((Spannable) Html.fromHtml("<b>Tap</b> here to <b>import images</b> from the <b>device</b>."))
                                                    .setGravity(Gravity.auto) //optional
                                                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                                    .setTargetView(binding.importIV)


                                                    .build()
                                                    .show();
                                        }
                                    })

                                    .build()
                                    .show();
                        }
                    })
                    .build()
                    .show();
        }
    }

    private void requestCameraPermision() {

        if (!requestPermissionEnabled)
            return;


        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {


            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Permission Needed");
            builder.setMessage("Camera permission is required to capture images");
            builder.setCancelable(false);


            builder.setPositiveButton("OK", (dialog, which) -> {


                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISION_REQUEST_CODE);

                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    //
                }
            });


            builder.create();
            builder.show();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISION_REQUEST_CODE);
        }
    }


    Bitmap bitmap = null;

    private void setViews() {


        int flashMode = Prefs.UserSettingsCaptureImage.getFlash(this);
        int scanMode = Prefs.UserSettingsCaptureImage.getScanMode(this);


        switch (flashMode) {

            case ImageCapture.FLASH_MODE_AUTO:
                binding.flashIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_flash_auto_white_24));
                break;

            case ImageCapture.FLASH_MODE_ON:
                binding.flashIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_flash_on_white_24));

                break;

            case ImageCapture.FLASH_MODE_OFF:

                binding.flashIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_flash_off_white_24));

                break;
        }

        if (scanMode == Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH) {
            binding.scanModeIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_style_white_24));
            binding.scanModeTV.setText("Batch Mode");
        } else {
            binding.scanModeIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_crop_portrait_white_24));

            binding.scanModeTV.setText("Single Mode");
        }
    }


    private void bindPreview(ProcessCameraProvider cameraProvider) {


        if (targetWidth > targetHeight) {

            int temp = targetWidth;

            targetWidth = targetHeight;
            targetHeight = temp;
        }


        Preview.Builder previewBuilder = new Preview.Builder();

        if (targetWidth > 0 || targetHeight > 0) {

            previewBuilder.setTargetResolution(new Size(targetWidth, targetHeight));

            //   Toast.makeText(getApplicationContext(),"Target Resolution\nWidth "+targetWidth+"\nHeight "+targetHeight,Toast.LENGTH_LONG).show();
        }


        preview = previewBuilder.build();


        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();


        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder()
                .setFlashMode(Prefs.UserSettingsCaptureImage.getFlash(getApplicationContext()))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(Prefs.UserSettingsCaptureImage.getFlash(getApplicationContext()))

                // .setTargetResolution(new Size(2568,3424))

                .setTargetRotation(CameraPreviewActivity.this.getWindowManager().getDefaultDisplay().getRotation());


        if (targetWidth > 0 || targetHeight > 0) {

            imageCaptureBuilder.setTargetResolution(new Size(targetWidth, targetHeight));
        }


        imageCapture = imageCaptureBuilder.build();


        preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());


        try {
            camera = cameraProvider.bindToLifecycle(CameraPreviewActivity.this, cameraSelector, preview, imageCapture);

        } catch (Exception e) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Seems like there is a problem with your camera.\nPlease try again", Toast.LENGTH_LONG).show();


                }


            });

            return;
        } catch (Error e2) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Seems like there is a problem with your camera.\nPlease try again", Toast.LENGTH_LONG).show();
                }
            });

            return;

        }


        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);

        animator.setDuration(1200);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                binding.cameraAlphaAnimationRL.setAlpha((Float) valueAnimator.getAnimatedValue());

                if ((float) valueAnimator.getAnimatedValue() == 0) {
                    shallVibrate = true;
                    binding.cameraAlphaAnimationRL.setVisibility(View.GONE);

                }
            }
        });

        animator.setRepeatCount(0);
        animator.start();


        binding.cameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {


                int x = (int) motionEvent.getX() - focusDrawableMarginOffset;
                int y = (int) motionEvent.getY() - focusDrawableMarginOffset;

                binding.focusIV.setVisibility(View.VISIBLE);


                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.focusIV.getLayoutParams();

                params.topMargin = y;
                params.leftMargin = x;

                binding.focusIV.requestLayout();

                int animTime = 700;


                if (camera == null)
                    return false;

                MeteringPoint meteringPoint = new DisplayOrientedMeteringPointFactory(binding.cameraPreview.getDisplay(), camera.getCameraInfo(), binding.cameraPreview.getWidth(), binding.cameraPreview.getHeight()).createPoint(motionEvent.getX(), motionEvent.getY());

                FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();


                camera.getCameraControl().startFocusAndMetering(action).addListener(new Runnable() {
                    @Override
                    public void run() {


                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                binding.focusIV.setVisibility(View.GONE);

                            }
                        }, 400);

                    }
                }, ContextCompat.getMainExecutor(CameraPreviewActivity.this));

                return false;
            }
        });


        //camera.getCameraControl().enableTorch(true);


        binding.cameraCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                captureImage(((ImageView) view));
            }
        });

    }


    @Override
    public void onClick(View view) {

        int id = view.getId();


        if (camera == null) {

            if (id != R.id.backArrow)
                return;

        }


        if (id == R.id.flipCamera) {
            flipCamera();
        }

        if (id == R.id.flashIV) {

            flashModeChange(((ImageView) view));
        } else if (id == R.id.scanModeIV) {

            scanModeChange(((ImageView) view));

        } else if (id == R.id.next) {

            next((Button) view);
        } else if (id == R.id.backArrow) {
            onBackArrowPressed(((ImageView) view));
        } else if (id == R.id.importIV) {
            importImages();
        } else if (id == R.id.torchIV) {


            if (lensFacing == CameraSelector.LENS_FACING_FRONT){
                Toast.makeText(getApplicationContext(),"Cannot enable torch while using front camera",Toast.LENGTH_LONG).show();
                return;
            }
            torchEnabled = !torchEnabled;
            camera.getCameraControl().enableTorch(torchEnabled);

            if (torchEnabled) {
                binding.torchIV.setAlpha(1.0f);

            } else {
                binding.torchIV.setAlpha(0.0f);
            }

        }
    }


    boolean isFlipping = false;

    private void flipCamera() {


        if (cameraProviderFuture != null) {

            if (isFlipping)
                return;

            isFlipping = true;


            try {
                cameraProviderFuture.get().unbindAll();


                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                    binding.flipCamera.setRotation(90);


                } else {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                    binding.flipCamera.setRotation(0);


                }

                torchEnabled = false;
                camera.getCameraControl().enableTorch(torchEnabled);
                binding.torchIV.setAlpha(0.0f);

                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();


                try {

                    camera = cameraProviderFuture.get().bindToLifecycle(CameraPreviewActivity.this, cameraSelector, preview, imageCapture);


                    isFlipping = false;

                } catch (Exception e2) {

                    isFlipping = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Seems like there is a problem with your camera.\nPlease try again", Toast.LENGTH_LONG).show();
                        }
                    });

                    return;

                } catch (Error e3) {

                    isFlipping = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Seems like there is a problem with your camera.\nPlease try again", Toast.LENGTH_LONG).show();
                        }
                    });

                    return;

                }


            } catch (ExecutionException e) {
                isFlipping = false;

            } catch (InterruptedException e) {
                isFlipping = false;

            }


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode) {
            case PERMISION_REQUEST_CODE:

                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {

                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
                    builder.setTitle("Permission not granted");
                    builder.setMessage("Cannot capture images as permission to use the camera was denied");

                    builder.setCancelable(false);

                    builder.setPositiveButton("OK", (dialog, which) -> {

                        finish();


                        try {
                            dialog.dismiss();
                        } catch (Exception e) {

                        }
                    });


                    builder.create();
                    builder.show();
                }
                return;
        }
    }

    public void importImages() {
    }


    public ImageCapture getImageCapture() {
        return imageCapture;
    }

    public ActivityCameraScanBinding getBinding() {
        return binding;
    }

    public void onBackArrowPressed(ImageView view) {

        onBackPressed();

    }

    public void next(Button view) {
    }

    public void scanModeChange(ImageView view) {
    }

    public void flashModeChange(ImageView view) {

    }

    public void captureImage(ImageView view) {


        if (imageCapture == null)
            return;


        MainActivity.listingModified = true;


    }

}