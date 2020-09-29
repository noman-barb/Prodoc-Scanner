package com.aaindia.prodocscanner.ocr;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.constants.Constants;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import org.opencv.core.Mat;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class OcrActivity extends AppCompatActivity implements View.OnClickListener, TessBaseAPI.ProgressNotifier {


    private static final int EXPORT_TO_DEVICE_CODE = 3425;
    public static String IMAGE_PATHS;

    private ArrayList<String> bitmapPaths = null;
    private ArrayList<Uri> bitmapUris = null;

    private TessBaseAPI mTess;


    ArrayList<LanguageDataMap.KeyValuePair> list;
    private SpannableStringBuilder extractedText = null;

    ProgressDialog progressDialog = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        Utils.checkOpenCV(this);

        findViewById(R.id.shareRL).setOnClickListener(this::onClick);
        findViewById(R.id.saveRL).setOnClickListener(this::onClick);

        findViewById(R.id.langRL).setOnClickListener(this::onClick);


        if (getIntent() != null && getIntent().getBundleExtra(IMAGE_PATHS) != null) {

            Bundle args = getIntent().getBundleExtra(IMAGE_PATHS);
            bitmapPaths = (ArrayList<String>) args.getSerializable(IMAGE_PATHS);

        }

        if (bitmapPaths == null) {


            bitmapUris = new ArrayList<>();
            Intent data = getIntent();
            if (data.getClipData() != null) {


                for (int i = 0; i < data.getClipData().getItemCount(); i++) {

                    bitmapUris.add(data.getClipData().getItemAt(i).getUri());
                }


            } else if (data.getData() != null) {

                bitmapUris.add(data.getData());
            }
        }


        list = new LanguageDataMap().getLangOptions();




        languageSelect(Prefs.OCRPreference.getLang(this));

    }

    private void languageSelect(String defaultLanguage) {


        if (defaultLanguage != null) {


            for (int i = 0; i < list.size(); i++) {

                if (list.get(i).key.equals(defaultLanguage)) {
                    getDataOnline(i);
                    break;
                }

            }

            return;

        }

        CharSequence[] strLists = new String[list.size()];

        for (int i = 0; i < list.size(); i++) {
            strLists[i] = list.get(i).value;
        }


        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Language")
                .setItems(strLists, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                extractedText = null;

                                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(OcrActivity.this);
                                builder.setTitle("Remember language");
                                builder.setMessage("Make " + strLists[position] + " your default language for text extraction.\nHowever, you can at any later point change the default language for text extraction.");

                                builder.setPositiveButton("Remember", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int rr) {


                                        Prefs.OCRPreference.setLang(OcrActivity.this, list.get(position).key);
                                        getDataOnline(position);

                                    }
                                })
                                        .setNegativeButton("Don't remember", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int gr) {

                                                getDataOnline(position);
                                            }
                                        })
                                        .setCancelable(false)

                                        .show();


                            }
                        });
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                        if (extractedText == null)
                            OcrActivity.this.finish();
                    }
                })
                .show();

    }

    Mat m;

    private void getDataOnline(int i) {

        String language = list.get(i).value;
        String langCode = list.get(i).key;

        File tessDataDir = new File(FileNav.getRootDir(this) + File.separator + "/tesseract/tessdata");

        File testDataRoot = new File(FileNav.getRootDir(this) + File.separator + "/tesseract/");

        tessDataDir.mkdirs();

        File tessDataFile = new File(tessDataDir, langCode + ".traineddata");

        if (!tessDataFile.exists()) {


            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(OcrActivity.this);

            builder.setTitle("Download language pack")
                    .setMessage("The selected language pack has to be downloaded once to perform OCR. Once downloaded, it need not to be downloaded again.")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int idd) {

                            OcrActivity.this.finish();
                        }
                    })
                    .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int ifewf) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {


                                    ProgressDialog pd = new ProgressDialog(OcrActivity.this);
                                    pd.setTitle("Downloading language pack");
                                    pd.setMessage("It may take a while");
                                    pd.setCancelable(false);
                                    pd.show();


                                    Ion.with(OcrActivity.this)
                                            .load(Constants.OCR_MODELS)
                                            .asJsonObject()
                                            .setCallback(new FutureCallback<JsonObject>() {
                                                @Override
                                                public void onCompleted(Exception e, JsonObject result) {

                                                    if (e != null) {
                                                        handleError();

                                                        return;
                                                    }

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            pd.dismiss();
                                                        }
                                                    });

                                                    String link = result.get(langCode).getAsString();


                                                    if (link == null) {
                                                        handleError();
                                                    } else {

                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                downloadPack(langCode, tessDataFile, i, link);
                                                            }
                                                        });


                                                    }

                                                }
                                            });


                                }
                            });

                        }
                    })

                    .show();


        } else {

            try {
                mTess = new TessBaseAPI(OcrActivity.this);
            } catch (Exception e) {


                try {
                    System.loadLibrary("jpeg");

                } catch (Exception e2) {
                }

                try {
                    System.loadLibrary("png");

                } catch (Exception e2) {
                }


                try {
                    System.loadLibrary("leptonica");

                } catch (Exception e2) {
                }

                try {

                    System.loadLibrary("tesseract");


                } catch (Exception e2) {
                }


                mTess = new TessBaseAPI(OcrActivity.this);


            }


            progressDialog = new ProgressDialog(OcrActivity.this);
            progressDialog.setTitle("Extracting texts");
            progressDialog.setMessage("It will take a while");
            progressDialog.setCancelable(false);
            progressDialog.show();

            mTess.init(testDataRoot.getAbsolutePath(), langCode);


            new Thread(new Runnable() {
                @Override
                public void run() {

                    if (extractedText == null) {
                        extractedText = new SpannableStringBuilder("");
                    }


                    int size = 0;

                    if (bitmapPaths != null) {
                        size = bitmapPaths.size();


                    } else {
                        size = bitmapUris.size();
                    }

                    for (int i = 0; i < size; i++) {


                        Spannable spannable = new SpannableString("<br><br><b>Page " + (i + 1) + "<br>__________<br><br></b>");


                        extractedText.append(spannable);

                        int finalI = i;
                        int finalSize = size;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.setTitle("Extracting texts " + (finalI + 1) + "/" + finalSize);
                            }
                        });


                        try {

                            Glide.with(OcrActivity.this)

                                    .asBitmap()

                                    .skipMemoryCache(true)

                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .load(bitmapUris == null ? bitmapPaths.get(i) : bitmapUris.get(i))


                                    .addListener(new RequestListener<Bitmap>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                            return true;
                                        }

                                        @Override
                                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {

                                            if (resource == null) {
                                                return true;
                                            }


                                            m = new Mat();

                                            org.opencv.android.Utils.bitmapToMat(resource, m);

                                            resource.recycle();

                                            MatFilter.colorize(m, MatFilter.COLOR_WHITEBOARD, MatFilter.getDefaultTune(MatFilter.COLOR_WHITEBOARD), true);

                                            resource = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

                                            org.opencv.android.Utils.matToBitmap(m, resource);

                                            m.release();

                                            mTess.setImage(resource);

                                            extractedText.append(mTess.getHOCRText(0).replace("/n", "<br>"));

                                            resource.recycle();

                                            return true;
                                        }
                                    }).submit().get();


                        } catch (Exception e) {
                        }


                    }

                    if (mTess != null) mTess.end();


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();


                            ((TextView) (OcrActivity.this.findViewById(R.id.ocrTxt))).setText(Html.fromHtml(extractedText.toString()));


                        }
                    });


                }
            }).start();


        }

    }

    private void downloadPack(String langCode, File tessDataFile, int i, String link) {


        ProgressDialog pd = new ProgressDialog(OcrActivity.this);
        pd.setTitle("Downloading language pack");
        pd.setMessage("It may take a while");
        pd.setCancelable(false);
        pd.show();

        Ion.with(OcrActivity.this)
                .load(link)
                .progress(new ProgressCallback() {
                    @Override
                    public void onProgress(long downloaded, long total) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.setMessage(String.format("%.2f", downloaded / (1024.0 * 1024)) + "/" + String.format("%.2f", total / (1024.0 * 1024)) + " mb");
                            }
                        });


                    }
                })
                .write(tessDataFile)
                .setCallback(new FutureCallback<File>() {
                    @Override
                    public void onCompleted(Exception e, File file) {

                        if (e != null) {

                            handleError();
                        } else {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    pd.dismiss();
                                    getDataOnline(i);
                                }
                            });

                        }
                    }
                });
    }

    private void handleError() {

        Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        OcrActivity.this.finish();
    }


    @Override
    public void onDestroy() {
        if (mTess != null) mTess.end();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {


        finish();
        super.onBackPressed();


    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.shareRL) {
            share();
        } else if (id == R.id.saveRL) {
            save();
        } else if (id == R.id.langRL) {
            languageSelect(null);
        }
    }

    private void save() {


        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.putExtra(Intent.EXTRA_TITLE, "Prodoc OCR " + new SimpleDateFormat("dd-MM-yy HH.mm.ss").format(System.currentTimeMillis()) + ".txt");
        intent.setType("text/plain");


        startActivityForResult(intent, OcrActivity.EXPORT_TO_DEVICE_CODE);
    }

    private void share() {

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        String shareBody =
                ((TextView) (OcrActivity.this.findViewById(R.id.ocrTxt))).getText().toString();

        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "OCR extracted using Prodoc Scanner");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == EXPORT_TO_DEVICE_CODE && resultCode == Activity.RESULT_OK) {

            Uri uri = null;
            if (data != null) {
                uri = data.getData();

            } else {
                if (data.getClipData() != null) {

                    uri = data.getClipData().getItemAt(0).getUri();
                }

            }

            if (uri != null) {

                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {

                    PrintWriter printWriter = new PrintWriter(outputStream);

                    printWriter.print(
                            ((TextView) (OcrActivity.this.findViewById(R.id.ocrTxt))).getText());
                    printWriter.flush();
                    printWriter.close();


                } catch (Exception e) {
                }

            }


        }
    }


    @Override
    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {

        if (progressDialog != null) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("It will take a while (" + progressValues.getPercent() + "%)");
                }
            });
        }

    }

}