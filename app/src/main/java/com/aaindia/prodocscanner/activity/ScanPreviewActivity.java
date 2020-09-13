package com.aaindia.prodocscanner.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.aaindia.prodocscanner.activityExtenders.ScanPreview.EditScanViewActivity;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ReorderScanViewActivity;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ScanViewActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.behaviours.PDFCreator;
import com.aaindia.prodocscanner.constants.Constants;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;

public class ScanPreviewActivity extends ReorderScanViewActivity implements View.OnClickListener {


    public static final String SCAN_DIR_PATH = "scan_dir_path";
    public static final String SCROLL_TO = "scroll_to";
    private static final int ADD_PAGES_ACTIVITY_RESULT_CODE = 929;

    private static final int EXPORT_TO_DEVICE_CODE = 826;
    private String outputPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    @Override
    public void addPages(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.addPages(holder, position);

        Intent intent = new Intent(ScanPreviewActivity.this, CameraScanActivity.class);


        try {
            String currentDir = new File(getScanDirPath() + "/../").getCanonicalPath();

            intent.putExtra(CameraScanActivity.CURRENT_DIR_KEY, currentDir);
            intent.putExtra(CameraScanActivity.NUM_PAGES, getImageDetails().getOrdering().size());
            intent.putExtra(CameraScanActivity.SCAN_PATH_KEY, getScanDirPath());
            intent.putExtra(CameraScanActivity.INSERT_AT, position + 1);
            intent.putExtra(CameraScanActivity.ADD_PAGES, true);

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete this page?");


        builder.setPositiveButton("Delete", (dialog, which) -> {


            deleteFileFinal(holder, position);


        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();


    }

    private void deleteFileFinal(ScanPreviewAdapter.ViewHolder holder, int position) {


        String originalFilePath = getOriginalFilepaths().get(position);
        String processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position)).getAbsolutePath();

        // start deleting


        getOriginalFilepaths().remove(position);

        getImageDetails().removePage(position);
        getImageDetails().sync();
        //FileUtils.deleteQuietly(new File(originalFilePath));
        //FileUtils.deleteQuietly(new File(processedImageFilepath));

        FileNav.deleteDirectoryQuietely(new File(originalFilePath));

        FileNav.deleteDirectoryQuietely(new File(processedImageFilepath));

        if (getOriginalFilepaths().size() == 0) {


            // FileUtils.deleteQuietly(new File(getScanDirPath()));

            FileNav.deleteDirectoryQuietely(new File(getScanDirPath()));

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
        }
    }

    @Override
    public void shareSinglePage(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.shareSinglePage(holder, position);

        Intent share = new Intent(Intent.ACTION_SEND);


        String processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position)).getAbsolutePath();


        String shareFilePath = processedImageFilepath;

        if (!new File(shareFilePath).exists())
            shareFilePath = getOriginalFilepaths().get(position);

        share.setType("*/*");
        share.putExtra(Intent.EXTRA_SUBJECT, Constants.singlePageShareMessage(position + 1));
        share.putExtra(Intent.EXTRA_TEXT, Constants.singlePageShareMessage(position + 1));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {


            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", new File(shareFilePath)));

        } else {
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(shareFilePath));

        }

        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


        startActivity(Intent.createChooser(share, Constants.singlePageShareMessage(position + 1)));
    }

    @Override
    public void save() {

        createPdf(false, PDFCreator.QUALITY_FULL, "");

    }

    private void createPdf(boolean shareIt, int quality, String intentAction) {


        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Making PDF");

        pd.setCancelable(false);
        pd.show();

        Utils.copyNotProcessedOriginals(getImageDetails(), getScanDirPath());


        PDFBoxResourceLoader.init(getApplicationContext());


        File outputDir = FileNav.getOutputDir(getScanDirPath(), quality);


        String pdfName = FileNav.getPDFName(getScanDirPath());
        outputPath = outputDir.getAbsolutePath() + File.separator + pdfName;

        PDFCreator pdfCreator = new PDFCreator(this, getScanDirPath(), outputPath);

        pdfCreator.setQuality(quality);
        pdfCreator.setDocumentChanged(documentChanged());

        pdfCreator.setOnCompleteListener(new PDFCreator.OnCompleteListener() {
            @Override
            public void onComplete(int error) {

                pd.dismiss();

                setDocumentChanged(false);

                if (!shareIt) {


                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    intent.putExtra(Intent.EXTRA_TITLE, FileNav.getPDFName(getScanDirPath()));
                    intent.setType("application/pdf");


                    try {

                        startActivityForResult(intent, EXPORT_TO_DEVICE_CODE);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "No app is installed to open PDF", Toast.LENGTH_LONG).show();
                    }


                } else {
                    sharePDF(outputPath, pdfName, intentAction);
                }


            }

            @Override
            public void onProgress(int page, int total) {
                pd.setMessage("Page " + page + "/" + total);
            }
        });

        pdfCreator.create(true);
    }

    private void sharePDF(String pdfPath, String pdfName, String intentAction) {


        if (intentAction.equals(Intent.ACTION_VIEW)) {

            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                File file = new File(pdfPath);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

                intent.setDataAndType(uri, "*/*");

                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            } else {

                intent.setDataAndType(Uri.parse(pdfName), "*/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pdfPath));
            }

            intent.putExtra(Intent.EXTRA_SUBJECT, "Shared using Prodoc Scanner");
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);


            return;
        }


        Intent intent = new Intent(intentAction);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Shared using Prodoc Scanner");
        intent.putExtra(Intent.EXTRA_TEXT, "Scanned using ProDoc Scanner");


        intent.setType("*/*");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            File file = new File(pdfPath);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        } else {

            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pdfPath));
        }


        startActivity(Intent.createChooser(intent, "Share"));


    }


    @Override
    public void share() {


        Utils.copyNotProcessedOriginals(getImageDetails(), getScanDirPath());


//        float size = FileNav.getFolderSizeMb(FileNav.processedScanDirFromScanDir(new File(getScanDirPath())));
//
//
//        float mediumSize = size * PDFCreator.QUALITY_MEDIUM / 100.0f;
//
//
//        float lowSize = size * PDFCreator.QUALITY_LOW / 100.0f;

        PopupMenu popupMenu = new PopupMenu(this, getBinding().shareRL);

        Menu menu = popupMenu.getMenu();

//        SpannableString originalResolution = new SpannableString("Orignal Resolution (" + String.format("%.2f", size) + ") Mb");
//        SpannableString mediumResolution = new SpannableString("Medium Resolution (" + String.format("%.2f", mediumSize) + ") Mb");
//        SpannableString lowResolution = new SpannableString("Low Resolution (" + String.format("%.2f", lowSize) + ") Mb");
//
//


        SpannableString originalResolution = new SpannableString("Orignal Resolution");
        SpannableString mediumResolution = new SpannableString("Medium Resolution");
        SpannableString lowResolution = new SpannableString("Low Resolution");


        menu.add(0, PDFCreator.QUALITY_FULL, 0, originalResolution);
        menu.add(0, PDFCreator.QUALITY_MEDIUM, 0, mediumResolution);
        menu.add(0, PDFCreator.QUALITY_LOW, 0, lowResolution);


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {


                int id = item.getItemId();

                createPdf(true, id, Intent.ACTION_SEND);

                return true;
            }
        });


        popupMenu.show();


    }


    @Override
    public void rename() {
        super.rename();

        SpannableString spannableString = new SpannableString("Rename");

        setSpanActionColor(spannableString, 1, 1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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


        if (requestCode == ADD_PAGES_ACTIVITY_RESULT_CODE) {

            loadInitialData();
            getRecyclerView().getAdapter().notifyDataSetChanged();

        } else if (requestCode == EXPORT_TO_DEVICE_CODE && resultCode == Activity.RESULT_OK) {


            Uri uri = null;
            if (data != null) {
                uri = data.getData();

                processExport(uri);

            }

        }


    }

    private void processExport(Uri uri) {


        if (outputPath != null) {

            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Saving PDF");
            pd.setMessage("It may take a while");

            pd.setCancelable(false);
            pd.show();


            new Thread(new Runnable() {
                @Override
                public void run() {


                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {


                        FileUtils.copyFile(new File(outputPath), outputStream);


                    } catch (FileNotFoundException e) {

                        e.printStackTrace();
                    } catch (IOException e) {

                        e.printStackTrace();
                    } finally {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                            }
                        });
                    }


                }
            }).start();


        }


    }

    @Override
    public void viewPDF() {

        createPdf(true, PDFCreator.QUALITY_FULL, Intent.ACTION_VIEW);

    }


    @Override
    public void onBackPressed() {


        if (isReordeing()) {
            reorderViewEnableDisable(false);

            return;
        }

        super.onBackPressed();
    }
}