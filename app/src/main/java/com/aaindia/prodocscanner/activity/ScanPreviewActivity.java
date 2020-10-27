package com.aaindia.prodocscanner.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.SpannableString;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ShareScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.GridScanViewAdapter;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;

public class ScanPreviewActivity extends ShareScanPreviewActivity implements View.OnClickListener {


    public static final String SCAN_DIR_PATH = "scan_dir_path";
    public static final String SCROLL_TO = "scroll_to";

    private static final int ADD_PAGES_ACTIVITY_RESULT_CODE = 929;


    public static final int EXPORT_TO_DEVICE_CODE = 826;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Utils.checkOpenCV(this);


        if (getIntent() != null && getIntent().getExtras() != null) {


            int scrollTo = getIntent().getExtras().getInt(SCROLL_TO, 0);


            getBinding().recyclerView.post(new Runnable() {
                @Override
                public void run() {

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getBinding().recyclerView.scrollToPosition(scrollTo);
                                    //    getBinding().recyclerView.getLayoutManager().scrollToPosition(scrollTo);
                                }
                            });
                        }
                    }, 200);

                }
            });
            //  getBinding().recyclerView.smoothScrollToPosition(scrollTo);
        }


    }

    @Override
    public void onResume() {

        Utils.checkOpenCV(this);
        super.onResume();

        if (executorService2==null || executorService2.isTerminated()){
            executorService2 = Executors.newFixedThreadPool(3);
            Toast.makeText(getApplicationContext(),"ewgeg",0).show();
        }


    }

    @Override
    public void addPages(ScanPreviewAdapter.ViewHolder holder, int position, boolean retake) {
        super.addPages(holder, position, retake);

        MainActivity.listingModified = true;

        Intent intent = new Intent(ScanPreviewActivity.this, CameraScanActivity.class);


        try {
            String currentDir = new File(getScanDirPath() + "/../").getCanonicalPath();

            intent.putExtra(CameraScanActivity.CURRENT_DIR_KEY, currentDir);
            intent.putExtra(CameraScanActivity.NUM_PAGES, getImageDetails().getOrdering().size());
            intent.putExtra(CameraScanActivity.SCAN_PATH_KEY, getScanDirPath());
            intent.putExtra(CameraScanActivity.INSERT_AT, position + 1);
            intent.putExtra(CameraScanActivity.ADD_PAGES, true);
            intent.putExtra(CameraScanActivity.RETAKE_IMAGE, retake);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, ADD_PAGES_ACTIVITY_RESULT_CODE);


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void deleteFile(ScanPreviewAdapter.ViewHolder holder, int position) {

        super.deleteFile(holder, position);


        SpannableString cancel = new SpannableString("Cancel");
        setSpanActionColor(cancel, 1, 1);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete this page?");


        builder.setPositiveButton("Delete", (dialog, which) -> {


            MainActivity.listingModified = true;

            deleteFileFinal(holder, position);


        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();

        getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);

    }

    private void deleteFileFinal(ScanPreviewAdapter.ViewHolder holder, int position) {


        String originalFilePath = getOriginalFilepaths().get(position);
        String processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position)).getAbsolutePath();

        // start deleting


        getOriginalFilepaths().remove(position);

        // ((ScanPreviewAdapter) getRecyclerView().getAdapter()).originalFilepaths = getOriginalFilepaths();


        getImageDetails().removePage(position);
        getImageDetails().sync();
        FileNav.deleteDirectoryQuietely(new File(originalFilePath));
        FileNav.deleteDirectoryQuietely(new File(processedImageFilepath));


        if (getOriginalFilepaths().size() == 0) {


            FileNav.deleteDirectoryQuietely((new File(getScanDirPath())));

            Intent intent = new Intent(ScanPreviewActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            try {
                String currentDir = new File(getScanDirPath() + "/../").getCanonicalPath();

                intent.putExtra(MainActivity.CURRENT_DIR_PATH, currentDir);
                startActivity(intent);

            } catch (Exception e) {
            }

        } else {


            getRecyclerView().getAdapter().notifyDataSetChanged();

            if (position <= getRecyclerView().getAdapter().getItemCount()) {

                getRecyclerView().scrollToPosition(Math.max(position - 1, 0));
            }

            generateThumbnail();
        }
    }


    @Override
    public void rename() {
        super.rename();

        MainActivity.listingModified = true;

        SpannableString spannableString = new SpannableString("Rename");

        setSpanActionColor(spannableString, 1, 1);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Rename");

        final EditText input = new EditText(this);

        String fromName = FileNav.getPDFName(getScanDirPath()).replace(".pdf", "");

        input.setText(fromName);
        input.setSelectAllOnFocus(true);

        fromName += FileNav.SCAN_IDENTIFIER;

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        input.requestFocus();

        InputMethodManager imm1 = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm1.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);


        String finalFromName = fromName;
        builder.setPositiveButton(spannableString, (dialog, which) -> {
            String newName = input.getText().toString() + FileNav.SCAN_IDENTIFIER;


            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);


            if (!FileNav.validFileName(newName)) {
                Toast.makeText(ScanPreviewActivity.this, "Not a valid name", Toast.LENGTH_SHORT).show();
                rename();
            } else {


                File outputDir = FileNav.getOutputDir(getScanDirPath());
                String pdfName = FileNav.getPDFName(getScanDirPath());


                File outputFile = new File(outputDir, pdfName);


                // TODO RENAME FILE INSTEAD OF DELETING
                FileUtils.deleteQuietly(outputFile);


                boolean renamed = FileNav.rename(getScanDirPath(), finalFromName, newName);


                if (renamed) {

                    setScanDirPath(getScanDirPath().replace(finalFromName, newName));


                    loadInitialData();
                    getRecyclerView().getAdapter().notifyDataSetChanged();


                } else {
                    Toast.makeText(ScanPreviewActivity.this, "Scan already exits with same name", Toast.LENGTH_SHORT).show();

                    rename();
                }


            }


        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {

            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

            dialog.cancel();


        });


        builder.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == ADD_PAGES_ACTIVITY_RESULT_CODE && resultCode == Activity.RESULT_OK) {

            MainActivity.listingModified = true;
//            loadInitialData();
            getRecyclerView().getAdapter().notifyDataSetChanged();
//
        } else if (requestCode == EXPORT_TO_DEVICE_CODE && resultCode == Activity.RESULT_OK) {


            Uri uri = null;
            if (data != null) {
                uri = data.getData();

            } else {
                if (data.getClipData() != null) {

                    uri = data.getClipData().getItemAt(0).getUri();
                }

            }

            if (uri != null) {
                processExport(uri);
            }

        }


    }

    private void processExport(Uri uri) {


        if (getPDFOutputPathExport() != null) {

            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Saving PDF");
            pd.setMessage("It may take a while");

            pd.setCancelable(false);
            pd.show();


            executorService2.execute(new Runnable() {
                @Override
                public void run() {


                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {


                        FileUtils.copyFile(new File(getPDFOutputPathExport()), outputStream);


                    } catch (FileNotFoundException e) {

                        e.printStackTrace();
                    } catch (IOException e) {

                        e.printStackTrace();
                    } finally {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    pd.dismiss();
                                } catch (Exception e) {

                                }

                                Toast.makeText(getApplicationContext(), "Saved to device", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }


                    setPDFOutputPathExport(null);

                }
            });


        }


    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getBinding().colorControlRL.getVisibility() == View.VISIBLE) {

                Rect outRect = new Rect();
                getBinding().colorControlRL.getGlobalVisibleRect(outRect);

                Rect outRect2 = new Rect();
                getBinding().colorRL.getGlobalVisibleRect(outRect2);

                if ((!outRect.contains((int) event.getRawX(), (int) event.getRawY())) && (!outRect2.contains((int) event.getRawX(), (int) event.getRawY())))
                    showHideColorRL(false);
            }
        }


        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {


        if (getBinding().protector.getVisibility() == View.VISIBLE) {
            return;


        }

        if (getBottomMenu1().getState() == BottomSheetBehavior.STATE_EXPANDED || getBottomMenu1().getState() == BottomSheetBehavior.STATE_COLLAPSED || getBottomMenu1().getState() == BottomSheetBehavior.STATE_EXPANDED ||
                getBottomMenu1().getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        super.onBackPressed();

    }

    @Override
    public void onProcessed(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.onProcessed(holder, position);


    }


    @Override
    protected void onDestroy() {

        Log.d("aaaaaaaaaaaaaa", "destro before");
        super.onDestroy();

        try {
            if (executorService2 != null && !executorService2.isTerminated()) {
                executorService2.shutdownNow();

                while (!executorService2.isTerminated()) {

                    Log.d("aaaaaaaaaaaaaa", "destro1111");
                }
            }
        } catch (Exception e) {
        }


        Log.d("aaaaaaaaaaaaaa", "destroy after");

    }
}