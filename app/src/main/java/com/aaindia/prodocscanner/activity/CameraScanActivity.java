package com.aaindia.prodocscanner.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ScanViewActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.databinding.ActivityCameraScanBinding;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nguyenhoanglam.imagepicker.model.Image;
import com.nguyenhoanglam.imagepicker.ui.imagepicker.ImagePicker;


import org.apache.commons.io.FileUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

public class CameraScanActivity extends CameraPreviewActivity {


    public static final String IMPORT_IMAGES = "import_images";
    private static final int PERMISION_REQUEST_CODE_STORAGE = 2420;
    public static String SCAN_PATH_KEY = "scan_path";
    public static String CURRENT_DIR_KEY = "current_dir";
    public static String NUM_PAGES = "num_pages";
    public static String INSERT_AT = "insert_at";
    public static String ADD_PAGES = "add_pages";

    public static String RETAKE_IMAGE = "retake_image";


    private ActivityCameraScanBinding binding;

    private int scanMode = -1;
    private String scanDirPath = null;
    private String currentDir = null;
    private int numPages = 0;
    private int insertAt = 0;
    private int pageInsertStartFrom = 0;


    private boolean isCapturing = false;

    private SavedImageDetails imageDetails;

    private int capturedImages = 0;

    int getScanMode = 0;
    public int CROP_ACTIVITY_CODE = 821;
    public int IMPORT_ACTIVITY_CODE = 134;


    private boolean backpressReturnToDir = true;
    private boolean isAddPages = false;
    private boolean importImages = false;
    boolean intentResult = false;

    boolean isRetakeImage = false;

    MediaPlayer cameraShutterSound = null;

    ExecutorService executorService;


    private boolean showGuide = false;

    @Override
    protected void onPause() {
//        if (imageDetails != null)
//            imageDetails.sync();

        super.onPause();
    }

    @Override
    protected void onStop() {
        recycleImageCropActivityBitmap();


        try {

            if (cameraShutterSound != null) {

                if (cameraShutterSound.isPlaying())
                    cameraShutterSound.stop();

                cameraShutterSound.release();
                cameraShutterSound = null;
            }
        } catch (Exception e) {
        }


        if (executorService != null && !executorService.isTerminated()) {


            if (!executorService.isShutdown())
                executorService.shutdown();

            while (!executorService.isTerminated()) {
            }

            if (imageDetails != null) {
                imageDetails.sync();
            }

        }

        executorService = null;
        super.onStop();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        isCapturing = false;

        if (requestCode == CROP_ACTIVITY_CODE) {
            if (resultCode == Activity.RESULT_OK) {


                String originalImageFilename = (String) data.getExtras().get(FileNav.ORIGINAL_IMAGE_FILE);
                String processedImageFilename = (String) data.getExtras().get(FileNav.PROCESSED_IMAGE_FILE);

                HashMap<Integer, PointF> cropBoundsOriginalMap = (HashMap<Integer, PointF>) data.getExtras().get(ImageCropActivity.CORNERS);


                int colorCode = (int) data.getExtras().get(ImageCropActivity.COLOR_CODE);

                int globalRotation = (int) data.getExtras().get(ImageCropActivity.GLOBAL_ROTATION);
                boolean isColorGray = (boolean) data.getExtras().get(ImageCropActivity.COLOR_IS_GRAY);

                int colorTune = (int) data.getExtras().get(ImageCropActivity.COLOR_TUNE);

                Effects effects = new Effects(cropBoundsOriginalMap, colorCode, isColorGray, globalRotation, colorTune);


                imageSaved(null, new File(originalImageFilename).getName(), effects, true);

                // newly added


            } else if (resultCode == Activity.RESULT_CANCELED) {

                isCapturing = false;
                binding.cameraCapture.setAlpha(1.0f);

            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {


            if (requestCode == IMPORT_ACTIVITY_CODE
                    && resultCode == Activity.RESULT_OK) {

                onExternalSafImport(data);

            }

        } else {
            if (ImagePicker.shouldHandleResult(requestCode, resultCode, data, IMPORT_ACTIVITY_CODE)) {

                onExternalImport(data);
            }
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode) {
            case PERMISION_REQUEST_CODE_STORAGE:

                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImagePicker();
                } else {

                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
                    builder.setTitle("Permission not granted");
                    builder.setMessage("Cannot import images as permission to access storage was denied.");

                    builder.setCancelable(false);

                    builder.setPositiveButton("OK", (dialog, which) -> {

                        if (importImages)
                            finish();


                        try {
                            dialog.dismiss();
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    });


                    builder.create();
                    builder.show();
                }
                return;
        }
    }


    private void onExternalImport(Intent data) {

        MainActivity.listingModified = true;


        ArrayList<Image> images = ImagePicker.getImages(data);

        ArrayList<Uri> uris = new ArrayList<>();

        for (Image image : images) {
            uris.add(image.getUri());
        }


        if (uris.size() > 0) {


            if (uris.size() > 1) {
                importMultipleImages(uris);
            } else {
                importSingleImage(uris.get(0));
            }
        }


    }


    private void onExternalSafImport(Intent data) {

        if (data.getClipData() != null) {

            if (data.getClipData().getItemCount() == 1) {
                importSingleImage(data.getClipData().getItemAt(0).getUri());
            } else {

                ArrayList<Uri> uris = new ArrayList<>();

                for (int i = 0; i < data.getClipData().getItemCount(); i++) {

                    uris.add(data.getClipData().getItemAt(i).getUri());
                }

                importMultipleImages(uris);
            }
        } else if (data.getData() != null) {

            importSingleImage(data.getData());
        }
    }


    private void importMultipleImages(ArrayList<Uri> uris) {


        ProgressDialog progressDialog = new ProgressDialog(CameraScanActivity.this);
        progressDialog.setMessage("Importing ");
        progressDialog.setCancelable(false);

        progressDialog.show();


        try {

            initializeDir();


            new Thread(new Runnable() {
                @Override
                public void run() {

                    int i = 0;
                    for (Uri uri : uris) {

                        int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.setMessage("Importing " + finalI + "/" + uris.size());
                            }
                        });
                        i++;

                        try {
                            InputStream in = getContentResolver().openInputStream(uri);

                            HashMap<String, File> map = FileNav.newImageFile(scanDirPath);


                            File originalFile = map.get(FileNav.ORIGINAL_IMAGE_FILE);
                            //File processedImageFileName = map.get(FileNav.PROCESSED_IMAGE_FILE);


                            OutputStream out = new FileOutputStream(originalFile);

                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            out.close();
                            in.close();

                            imageSaved(null, originalFile.getName(), null, false);

                        } catch (Exception e) {


                        }


                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {

                            }

                            // newly added

                            if (importImages) {
                                next(binding.next);
                            }
                        }
                    });

                }
            }).start();


        } catch (Exception e) {

        }
    }

    private void importSingleImage(Uri uri) {


        try {


            initializeDir();

            InputStream in = getContentResolver().openInputStream(uri);


            File temp = FileNav.getTempFile(getApplicationContext(), "single_mode_capture.jpg");
            OutputStream out = new FileOutputStream(temp);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();


            recycleImageCropActivityBitmap();

            //      ImageCropActivity.originalBitmap = BitmapFactory.decodeFile(temp.getAbsolutePath());
            ImageCropActivity.rotationDegrees = 0;


            HashMap<String, File> map = FileNav.newImageFile(scanDirPath);


            String originalFileName = map.get(FileNav.ORIGINAL_IMAGE_FILE).getCanonicalPath();
            String processedImageFileName = map.get(FileNav.PROCESSED_IMAGE_FILE).getCanonicalPath();


            Intent intent = new Intent(getApplicationContext(), ImageCropActivity.class);

            intent.putExtra(FileNav.ORIGINAL_IMAGE_FILE, originalFileName);
            intent.putExtra(FileNav.PROCESSED_IMAGE_FILE, processedImageFileName);

            intent.putExtra(ImageCropActivity.DOCUMENT_TYPE_KEY, getDocumentType());
            startActivityForResult(intent, CROP_ACTIVITY_CODE);


        } catch (Exception e) {

        }


    }

    private void recycleImageCropActivityBitmap() {

//        if (ImageCropActivity.originalBitmap != null)
//            ImageCropActivity.originalBitmap.recycle();
    }


    private void initializeDir() {

        if (scanDirPath == null)
            scanDirPath = FileNav.newScanDir(currentDir).getAbsolutePath();


        if (imageDetails == null)

            imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDirPath));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        Utils.checkOpenCV(this);

        showGuide = !Prefs.firstTimeSeenScreen(CameraScanActivity.this, "camera_scan_act");


        if (getIntent() != null && getIntent().getType() != null) {

            intentResult = true;
        }


        importImages = getIntent().getExtras().getBoolean(IMPORT_IMAGES, false);

        isRetakeImage = getIntent().getExtras().getBoolean(RETAKE_IMAGE, false);


        setRequestPermission(!(importImages || intentResult));

        super.onCreate(savedInstanceState);


        try {

            cameraShutterSound = new MediaPlayer();
            cameraShutterSound.setAudioStreamType(AudioManager.STREAM_RING);
            cameraShutterSound.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.camera_shutter));
            cameraShutterSound.prepare();
        } catch (Exception e) {

        }


        binding = getBinding();
        scanMode = Prefs.UserSettingsCaptureImage.getScanMode(this);

        fetchFromIntent(savedInstanceState);

        if (capturedImages == 0) {
            binding.next.setAlpha(0.4f);
        }


        if (importImages) {


//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.SUPPORTED_64_BIT_ABIS.length > 0) {
//                importImages();
//            } else {
//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//
//                                importImages();
//                            }
//                        });
//                    }
//                }, 300);
//            }


            importImages();

        }

        executorService = Executors.newFixedThreadPool(2);


        if (intentResult) {

            importImages = true;
            onExternalSafImport(getIntent());
        }


    }

    private void fetchFromIntent(Bundle savedInstanceState) {


        scanMode = Prefs.UserSettingsCaptureImage.getScanMode(getApplicationContext());


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentDir = extras.getString(CURRENT_DIR_KEY, FileNav.getBaseDir(this).getAbsolutePath());
            scanDirPath = extras.getString(SCAN_PATH_KEY);
            isAddPages = extras.getBoolean(ADD_PAGES, false);


            if (scanDirPath != null) {
                insertAt = extras.getInt(INSERT_AT, 0);
                numPages = extras.getInt(NUM_PAGES, 0);
                pageInsertStartFrom = insertAt;

                backpressReturnToDir = false;

                binding.numPagesTV.setText(numPages + "");
            }


        }


    }


    @Override
    public void onBackPressed() {

        if (imageDetails != null) {
            imageDetails.sync();
        }

        super.onBackPressed();


        recycleImageCropActivityBitmap();


        if (backpressReturnToDir) {


            if (capturedImages == 0 && scanDirPath != null) {

                FileUtils.deleteQuietly(new File(scanDirPath));
                MainActivity.listingModified = false;
            }
            finish();

        } else {


            if (scanDirPath != null) {
                goToDocViewer();
            }


            finish();
        }


    }


    boolean nextClicked = false;

    @Override
    public void next(Button view) {
        super.next(view);

        if (isCapturing)
            return;

        if (numPages == 0)
            return;


        if (nextClicked) {
            return;
        }

        nextClicked = true;
        isCapturing = true;
        binding.cameraCapture.setAlpha(0.5f);

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Just a moment");
        pd.setMessage("Detecting document edges");
        pd.setCancelable(false);


        if (executorService != null && !executorService.isTerminated())
            executorService.shutdown();

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (executorService != null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                pd.show();
                            } catch (Exception e) {
                            }

                        }
                    });

                    while (!executorService.isTerminated()) {

                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (imageDetails != null)
                            imageDetails.sync();

                        try {
                            pd.dismiss();
                        } catch (Exception e) {
                            //
                        }
                        goToDocViewer();
                        finish();
                    }
                });
            }
        }).start();


    }

    private void goToDocViewer() {

        int scrollTo = 0;

        if (getIntent().getExtras() != null) {

            scrollTo = getIntent().getExtras().getInt(CameraScanActivity.INSERT_AT, 0);

            if (isRetakeImage && insertAt > 0) {
                scrollTo--;
            }
        }

        Intent intent = new Intent(CameraScanActivity.this, ScanPreviewActivity.class);
        intent.putExtra(ScanPreviewActivity.SCAN_DIR_PATH, scanDirPath);
        intent.putExtra(GlobalConstants.CLASS_NAME, MainActivity.CLASS_NAME);
        intent.putExtra(ScanPreviewActivity.SCROLL_TO, scrollTo);

        intent.putExtra(ScanViewActivity.NEW_SCAN, true);


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);


    }


    @Override
    public void importImages() {
        super.importImages();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !isRetakeImage);

            intent.setType("image/*");

            startActivityForResult(intent, IMPORT_ACTIVITY_CODE);

        } else {
            checkCameraPermission();
        }


    }


    private void checkCameraPermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                startImagePicker();

                ;

            } else {
                requestStoragePermision();
            }

        } else {

            startImagePicker();

        }
    }

    private void requestStoragePermision() {


        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {


            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Permission Needed");
            builder.setMessage("Storage access permission is needed to import images.");
            builder.setCancelable(false);


            builder.setPositiveButton("OK", (dialog, which) -> {


                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISION_REQUEST_CODE_STORAGE);

                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    //
                }
            });


            builder.create();
            builder.show();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISION_REQUEST_CODE_STORAGE);
        }


    }

    private void startImagePicker() {


        ImagePicker.with(CameraScanActivity.this)
                .setFolderMode(true)

                .setShowCamera(false)
                .setAlwaysShowDoneButton(false)
                .setDoneTitle("Import")
                .setBackgroundColor("#E1E2E1")
                .setToolbarColor("#3949AB")
                .setStatusBarColor("#3949AB")
                .setProgressBarColor("#2962FF")
                .setIndicatorColor("#2962FF")

                .setMultipleMode(true)
                .setShowNumberIndicator(true)
                .setRequestCode(IMPORT_ACTIVITY_CODE)
                .start();
    }

    @Override
    public void scanModeChange(ImageView view) {
        super.scanModeChange(view);

        int state = Prefs.UserSettingsCaptureImage.getScanMode(getApplicationContext());


        if (state != Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH) {
            binding.scanModeIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_style_white_24));
            scanMode = Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH;
            binding.scanModeTV.setText("Batch Mode");

            Toast.makeText(getApplicationContext(), "Batch Mode", Toast.LENGTH_SHORT).show();


            Prefs.UserSettingsCaptureImage.setScanMode(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH);
        } else {
            binding.scanModeIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_crop_portrait_white_24));

            scanMode = Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_SINGLE;
            Prefs.UserSettingsCaptureImage.setScanMode(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_SINGLE);
            binding.scanModeTV.setText("Single Mode");
            Toast.makeText(getApplicationContext(), "Single Mode", Toast.LENGTH_SHORT).show();


        }
    }

    @Override
    public void flashModeChange(ImageView view) {
        super.flashModeChange(view);

        if (getImageCapture() == null)
            return;


        int state = Prefs.UserSettingsCaptureImage.getFlash(getApplicationContext());
        switch (state) {

            case Prefs.UserSettingsCaptureImageWrapper.FLASH_AUTO:

                Prefs.UserSettingsCaptureImage.setFlash(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.FLASH_OFF);
                ((ImageView) view).setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.baseline_flash_off_white_24));

                getImageCapture().setFlashMode(ImageCapture.FLASH_MODE_OFF);

                break;

            case Prefs.UserSettingsCaptureImageWrapper.FLASH_OFF:

                Prefs.UserSettingsCaptureImage.setFlash(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.FLASH_ON);
                ((ImageView) view).setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.baseline_flash_on_white_24));
                getImageCapture().setFlashMode(ImageCapture.FLASH_MODE_ON);
                break;

            case Prefs.UserSettingsCaptureImageWrapper.FLASH_ON:

                Prefs.UserSettingsCaptureImage.setFlash(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.FLASH_AUTO);
                ((ImageView) view).setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.baseline_flash_auto_white_24));
                getImageCapture().setFlashMode(ImageCapture.FLASH_MODE_AUTO);
                break;

        }
    }


    boolean aaa = false;

    @Override
    public void captureImage(ImageView view) {
        super.captureImage(view);


        if (isCapturing)
            return;

        cameraShutterAnimation();
        binding.processingCapture.setVisibility(View.VISIBLE);
        view.setAlpha(0.4f);
        isCapturing = true;

        if (scanDirPath == null) {
            scanDirPath = FileNav.newScanDir(currentDir).getAbsolutePath();

        }

        if (imageDetails == null)

            imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDirPath));


        if (scanMode == Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH) {


            File imageFile = FileNav.newOriginalImageFile(new File(scanDirPath));


            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();


            getImageCapture().takePicture(outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                    imageSaved(imageFile, imageFile.getName(), null, false);


                    isCapturing = false;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            binding.processingCapture.setVisibility(View.GONE);
                        }
                    });

                }


                @Override
                public void onError(@NonNull ImageCaptureException exception) {

                    isCapturing = false;

                    isCapturing = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            binding.processingCapture.setVisibility(View.GONE);
                        }
                    });

                }
            });


            return;
        }


        File imageFile = FileNav.getTempFile(CameraScanActivity.this, "single_mode_capture.jpg");


        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();


        getImageCapture().takePicture(outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        binding.processingCapture.setVisibility(View.GONE);
                    }
                });


                ImageCropActivity.rotationDegrees = 0;


                Intent intent = new Intent(CameraScanActivity.this, ImageCropActivity.class);

                HashMap<String, File> map = FileNav.newImageFile(scanDirPath);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        binding.processingCapture.setVisibility(View.GONE);
                    }
                });


                try {
                    String originalFileName = map.get(FileNav.ORIGINAL_IMAGE_FILE).getCanonicalPath();
                    String processedImageFileName = map.get(FileNav.PROCESSED_IMAGE_FILE).getCanonicalPath();


                    intent.putExtra(FileNav.ORIGINAL_IMAGE_FILE, originalFileName);
                    intent.putExtra(FileNav.PROCESSED_IMAGE_FILE, processedImageFileName);


                    intent.putExtra(ImageCropActivity.DOCUMENT_TYPE_KEY, getDocumentType());
                    startActivityForResult(intent, CROP_ACTIVITY_CODE);


                } catch (IOException e) {
                    e.printStackTrace();
                }


            }


            @Override
            public void onError(@NonNull ImageCaptureException exception) {


                isCapturing = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        binding.processingCapture.setVisibility(View.GONE);
                    }
                });


            }
        });


        ///////////////////////////////////////////////////////
//        getImageCapture().takePicture(Executors.newSingleThreadExecutor(), new ImageCapture.OnImageCapturedCallback() {
//            @Override
//            public void onCaptureSuccess(@NonNull ImageProxy image) {
//
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        binding.processingCapture.setVisibility(View.GONE);
//                    }
//                });
//
//
//                ImageCropActivity.originalBitmap = BitmapUtils.imageProxyToBitmap(image);
//                ImageCropActivity.rotationDegrees = image.getImageInfo().getRotationDegrees();
//
//
//                Intent intent = new Intent(CameraScanActivity.this, ImageCropActivity.class);
//
//                HashMap<String, File> map = FileNav.newImageFile(scanDirPath);
//
//                isCapturing = false;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        binding.processingCapture.setVisibility(View.GONE);
//                    }
//                });
//
//
//                try {
//                    String originalFileName = map.get(FileNav.ORIGINAL_IMAGE_FILE).getCanonicalPath();
//                    String processedImageFileName = map.get(FileNav.PROCESSED_IMAGE_FILE).getCanonicalPath();
//
//
//                    intent.putExtra(FileNav.ORIGINAL_IMAGE_FILE, originalFileName);
//                    intent.putExtra(FileNav.PROCESSED_IMAGE_FILE, processedImageFileName);
//
//                    intent.putExtra(ImageCropActivity.DOCUMENT_TYPE_KEY, getDocumentType());
//                    super.onCaptureSuccess(image);
//                    startActivityForResult(intent, CROP_ACTIVITY_CODE);
//
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//
//            }
//        });


    }


    private void cameraShutterAnimation() {


        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        try {

                            binding.cameraAlphaAnimationRL.setVisibility(View.VISIBLE);

                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.2f, 0.4f, 0.6f, 0.8f, 1f, 1f, 1f, 0.8f, 0.6f, 0.4f, 0.2f, 0f);

                            valueAnimator.setDuration(150);

                            valueAnimator.setInterpolator(new AccelerateInterpolator());

                            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator valueAnimator) {

                                    float value = (float) valueAnimator.getAnimatedValue();
                                    binding.cameraAlphaAnimationRL.setAlpha(value);


                                    if (value < 0.1f) {

                                        binding.cameraAlphaAnimationRL.setVisibility(View.GONE);
                                    }

                                }
                            });


                            try {

                                if (cameraShutterSound != null) {
                                    if (cameraShutterSound.isPlaying())
                                        cameraShutterSound.stop();

                                    cameraShutterSound.start();
                                }

                            } catch (Exception e) {
                            }


                            valueAnimator.start();
                        } catch (Exception e) {
                        }


                    }
                });
            }
        }, 300);
    }


    private void imageSaved(File filepath, String filename, Effects effects, boolean isNext) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                if (showGuide) {

                    showGuide = false;


                    new GuideView.Builder(CameraScanActivity.this)
                            .setTitle("Next")
                            .setContentSpan((Spannable) Html.fromHtml("<b>Proceed next</b> if you are <b>done</b>."))
                            .setGravity(Gravity.auto) //optional
                            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                            .setTargetView(binding.next)

                            .setGuideListener(new GuideListener() {
                                @Override
                                public void onDismiss(View view) {


                                    new GuideView.Builder(CameraScanActivity.this)
                                            .setTitle("Capture more")
                                            .setContentSpan((Spannable) Html.fromHtml("Or <b>capture more </b> images in one go."))
                                            .setGravity(Gravity.auto) //optional
                                            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                            .setTargetView(binding.cameraCapture)

                                            .setGuideListener(new GuideListener() {
                                                @Override
                                                public void onDismiss(View view) {

                                                }
                                            })
                                            .build()
                                            .show();

                                }
                            })
                            .build()
                            .show();

                }


                binding.next.setAlpha(1);


                binding.cameraCapture.setAlpha(1.0f);


                if (effects != null) {

                    imageDetails.putEffects(filename, effects);
                }

                imageDetails.getDocType().put(filename, getDocumentType());
                imageDetails.getOrdering().add(insertAt, filename);


                if (filename != null && insertAt == 0 && executorService != null && !executorService.isTerminated() && scanDirPath != null) {



                    try {

                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {

                                try {

                                    File originalFile = new File(scanDirPath + File.separator + FileNav.ORIGINAL_IMAGE_DIR + File.separator + filename);


                                    if (!originalFile.exists())
                                        return;

                                    Mat mat = Imgcodecs.imread(originalFile.getAbsolutePath());

                                    int width = mat.width();
                                    int height = mat.height();

                                    float mp = (float) ((mat.width() / 1000.0) * (mat.height() / 1000.0));
                                    if (mp > 2) {
                                        float scale = 2.0f / mp;

                                        Imgproc.resize(mat, mat, new Size(width * scale, height * scale), Imgproc.INTER_AREA);

                                    }


                                    Imgcodecs.imwrite(scanDirPath + File.separator + "thumbnail.jpg", mat);
                                    mat.release();


                                } catch (Exception e) {
                                }


                            }
                        });


                    } catch (Exception e) {
                    }

                }


                if (isRetakeImage && insertAt > 0) {

                    String oldImageName = imageDetails.getAt(insertAt - 1);

                    File file1 = new File(scanDirPath + File.separator + FileNav.ORIGINAL_IMAGE_DIR + File.separator + oldImageName);
                    File file2 = new File(scanDirPath + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + oldImageName);

                    if (file1.exists()) {
                        FileUtils.deleteQuietly(file1);
                    }
                    if (file2.exists()) {
                        FileUtils.deleteQuietly(file2);
                    }


                    imageDetails.getOrdering().remove(insertAt - 1);

                    if (filepath != null) {
                        try {
                            autocropThis(filepath.getAbsolutePath(), imageDetails);
                        } catch (Exception e) {
                        }

                    }

                    // autocropThis(filepath.getAbsolutePath(), imageDetails);
                    imageDetails.sync();
                    goToDocViewer();
                    return;

                } else {


                    if (filepath != null) {


                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    autocropThis(filepath.getAbsolutePath(), imageDetails);
                                } catch (Exception e) {
                                }

                            }
                        });


                    } else {


                        if (importImages || intentResult) {

                            if (isNext) {
                                incrementPageCounters();
                                isCapturing = false;


                                imageDetails.sync();
                                next(binding.next);

                                return;
                            }
                        }


                    }


                    incrementPageCounters();
                    isCapturing = false;
                }


            }
        });


    }


    public void autocropThis(String path, SavedImageDetails imageDetails) {

        Mat originalMat = Imgcodecs.imread(path);

        if (originalMat.channels() == 4)
            Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGRA2BGR);

        MatOfPoint2f cropBoundsMat = new MatOfPoint2f();


        MatFilter.cropV1(originalMat.getNativeObjAddr(), cropBoundsMat.getNativeObjAddr());


        HashMap<Integer, PointF> cropBoundsMap = new HashMap<>();
        Point[] sortedPoints = BitmapUtils.sortMatofPoints2f(cropBoundsMat, new Size(originalMat.width(), originalMat.height()));


        for (int i = 0; i < 4; i++) {


            cropBoundsMap.put(i, new PointF((float) sortedPoints[i].x, (float) sortedPoints[i].y));
        }


        Effects effects1 = new Effects(cropBoundsMap, MatFilter.DEFAULT_COLOR_CODE, false, 0, 0);

        imageDetails.putEffects(new File(path).getName(), effects1);


        originalMat.release();
        cropBoundsMat.release();


    }


    @Override
    public void onResume() {


        Utils.checkOpenCV(this);

        super.onResume();

        isCapturing = false;

        if (executorService == null || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(2);
        }


        if (cameraShutterSound == null) {
            try {

                cameraShutterSound = new MediaPlayer();
                cameraShutterSound.setAudioStreamType(AudioManager.STREAM_RING);
                cameraShutterSound.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.camera_shutter));
                cameraShutterSound.prepare();
            } catch (Exception e) {

            }
        }

    }


    private void incrementPageCounters() {
        numPages++;
        insertAt++;
        capturedImages++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.numPagesTV.setText(numPages + "");
            }
        });

    }


}
