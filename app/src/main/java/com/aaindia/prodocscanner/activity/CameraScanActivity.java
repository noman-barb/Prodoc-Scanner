package com.aaindia.prodocscanner.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.databinding.ActivityCameraScanBinding;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.views.TouchableReyclerView;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CameraScanActivity extends CameraPreviewActivity {


    public static final String IMPORT_IMAGES = "import_images";
    public static String SCAN_PATH_KEY = "scan_path";
    public static String CURRENT_DIR_KEY = "current_dir";
    public static String NUM_PAGES = "num_pages";
    public static String INSERT_AT = "insert_at";
    public static String ADD_PAGES = "add_pages";


    private ActivityCameraScanBinding binding;

    private int scanMode = -1;
    private String scanDirPath = null;
    private String currentDir = null;
    private int numPages = 0;
    private int insertAt = 0;


    private boolean isCapturing = false;

    private SavedImageDetails imageDetails;


    int getScanMode = 0;
    public int CROP_ACTIVITY_CODE = 821;
    public int IMPORT_ACTIVITY_CODE = 134;


    private boolean backpressReturnToDir = true;
    private boolean isAddPages = false;
    private boolean importImages = false;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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

                imageSaved(new File(originalImageFilename).getName(), effects);

                // newly added

                if (importImages) {
                    goToDocViewer();
                    finish();
                }


            } else if (resultCode == Activity.RESULT_CANCELED) {

                isCapturing = false;
                binding.cameraCapture.setAlpha(1.0f);

            }
        }


        if (requestCode == IMPORT_ACTIVITY_CODE
                && resultCode == Activity.RESULT_OK) {


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

                            imageSaved(originalFile.getName(), null);

                        } catch (Exception e) {


                        }


                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();

                            // newly added

                            if (importImages) {
                                goToDocViewer();
                                finish();
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


            File temp = FileNav.getTempFile(getApplicationContext(), "temp1.jpg");
            OutputStream out = new FileOutputStream(temp);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();


            ImageCropActivity.originalBitmap = BitmapFactory.decodeFile(temp.getAbsolutePath());
            ImageCropActivity.rotationDegrees = BitmapUtils.exifRotationDegrees(temp.getAbsolutePath());


            HashMap<String, File> map = FileNav.newImageFile(scanDirPath);


            String originalFileName = map.get(FileNav.ORIGINAL_IMAGE_FILE).getCanonicalPath();
            String processedImageFileName = map.get(FileNav.PROCESSED_IMAGE_FILE).getCanonicalPath();


            Intent intent = new Intent(getApplicationContext(), ImageCropActivity.class);

            intent.putExtra(FileNav.ORIGINAL_IMAGE_FILE, originalFileName);
            intent.putExtra(FileNav.PROCESSED_IMAGE_FILE, processedImageFileName);


            startActivityForResult(intent, CROP_ACTIVITY_CODE);


        } catch (Exception e) {

        }


    }


    private void initializeDir() {

        if (scanDirPath == null)
            scanDirPath = FileNav.newScanDir(currentDir).getAbsolutePath();


        if (imageDetails == null)

            imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDirPath));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        importImages = getIntent().getExtras().getBoolean(IMPORT_IMAGES, false);

        setRequestPermission(!importImages);

        super.onCreate(savedInstanceState);


        binding = getBinding();
        scanMode = Prefs.UserSettingsCaptureImage.getScanMode(this);

        fetchFromIntent(savedInstanceState);

        if (numPages == 0) {
            binding.next.setAlpha(0.4f);
        }


        if (importImages) {
            importImages();
        }


    }

    private void fetchFromIntent(Bundle savedInstanceState) {


        scanMode = Prefs.UserSettingsCaptureImage.getScanMode(getApplicationContext());


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentDir = extras.getString(CURRENT_DIR_KEY);
            scanDirPath = extras.getString(SCAN_PATH_KEY);
            isAddPages = extras.getBoolean(ADD_PAGES, false);

            if (scanDirPath != null) {
                insertAt = extras.getInt(INSERT_AT);
                numPages = extras.getInt(NUM_PAGES);

                backpressReturnToDir = false;
            }


        }


    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (backpressReturnToDir) {


            if (numPages == 0 && scanDirPath != null) {

                FileUtils.deleteQuietly(new File(scanDirPath));
            }
            finish();

        } else {


            try {

                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
            } catch (Exception e) {
            }

            finish();
        }


    }

    @Override
    public void next(Button view) {
        super.next(view);

        if (isAddPages) {

            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            finish();

            return;
        }


        if (numPages == 0)
            return;

        goToDocViewer();

    }

    private void goToDocViewer() {
        Intent intent = new Intent(CameraScanActivity.this, ScanPreviewActivity.class);
        intent.putExtra(ScanPreviewActivity.SCAN_DIR_PATH, scanDirPath);
        intent.putExtra(GlobalConstants.CLASS_NAME, MainActivity.CLASS_NAME);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);


    }


    @Override
    public void importImages() {
        super.importImages();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        intent.setType("image/*");

        startActivityForResult(intent, IMPORT_ACTIVITY_CODE);

    }

    @Override
    public void scanModeChange(ImageView view) {
        super.scanModeChange(view);

        int state = Prefs.UserSettingsCaptureImage.getScanMode(getApplicationContext());


        if (state != Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH) {
            binding.scanModeIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_style_white_24));
            scanMode = Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH;
            binding.scanModeTV.setText("Batch Mode");


            Prefs.UserSettingsCaptureImage.setScanMode(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_BATCH);
        } else {
            binding.scanModeIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_crop_portrait_white_24));

            scanMode = Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_SINGLE;
            Prefs.UserSettingsCaptureImage.setScanMode(getApplicationContext(), Prefs.UserSettingsCaptureImageWrapper.SCAN_MODE_SINGLE);
            binding.scanModeTV.setText("Single Mode");


        }
    }

    @Override
    public void flashModeChange(ImageView view) {
        super.flashModeChange(view);

        if (getImageCapture()==null)
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

    @Override
    public void captureImage(ImageView view) {
        super.captureImage(view);


        if (isCapturing)
            return;
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

                    isCapturing = false;

                    imageSaved(imageFile.getName(), null);

                }


                @Override
                public void onError(@NonNull ImageCaptureException exception) {

                    isCapturing = false;

                }
            });


            return;
        }


        getImageCapture().takePicture(Executors.newSingleThreadExecutor(), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {


                isCapturing = false;

                ImageCropActivity.originalBitmap = BitmapUtils.imageProxyToBitmap(image);
                ImageCropActivity.rotationDegrees = image.getImageInfo().getRotationDegrees();


                Intent intent = new Intent(CameraScanActivity.this, ImageCropActivity.class);

                HashMap<String, File> map = FileNav.newImageFile(scanDirPath);

                try {
                    String originalFileName = map.get(FileNav.ORIGINAL_IMAGE_FILE).getCanonicalPath();
                    String processedImageFileName = map.get(FileNav.PROCESSED_IMAGE_FILE).getCanonicalPath();


                    intent.putExtra(FileNav.ORIGINAL_IMAGE_FILE, originalFileName);
                    intent.putExtra(FileNav.PROCESSED_IMAGE_FILE, processedImageFileName);

                    super.onCaptureSuccess(image);
                    startActivityForResult(intent, CROP_ACTIVITY_CODE);


                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });


    }

    private void imageSaved(String filename, Effects effects) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                binding.next.setAlpha(1);

                isCapturing = false;
                binding.cameraCapture.setAlpha(1.0f);


                if (effects != null) {

                    imageDetails.putEffects(filename, effects);
                }

                imageDetails.getOrdering().add(insertAt, filename);

                imageDetails.sync();


                incrementPageCounters();
            }
        });


    }


    private void incrementPageCounters() {
        numPages++;
        insertAt++;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.numPagesTV.setText(numPages + "");
            }
        });

    }


}
