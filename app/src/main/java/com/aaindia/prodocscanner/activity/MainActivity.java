package com.aaindia.prodocscanner.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.aaindia.prodocscanner.BuildConfig;
import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.GridScanViewActivity;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ShareScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.ListFilesAdapter;
import com.aaindia.prodocscanner.ocr.OcrActivity;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.databinding.ActivityMainBinding;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.utils.pdf.DocMaker;
import com.aaindia.prodocscanner.utils.pdf.PDFRendererWhiteBG;
import com.aaindia.prodocscanner.utils.share.ShareDialog;
import com.aaindia.prodocscanner.utils.share.Sharer;
import com.aaindia.prodocscanner.wrappers.Clipboard;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.ListFIlesInfo;
import com.aaindia.prodocscanner.wrappers.MyGridLayoytManager;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.tom_roush.pdfbox.contentstream.operator.state.Save;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage;
import com.tom_roush.pdfbox.rendering.PDFRenderer;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.junit.internal.runners.statements.RunAfters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;

import angtrim.com.fivestarslibrary.FiveStarsDialog;
import angtrim.com.fivestarslibrary.NegativeReviewListener;
import angtrim.com.fivestarslibrary.ReviewListener;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

public class MainActivity extends AppCompatActivity implements ListFilesAdapter.OnScanClickListener, View.OnClickListener {


    public static final String CURRENT_DIR_PATH = "current_dir_path";

    public static final String CLASS_NAME = "MainActivity";

    public static final int REQUEST_CODE_IMPORT_LOCAL_PDF = 31;

    public static final String TAG = "MainActivityDebug";
    public static final String GROUPED = "Grouped";
    public static final String ALL_DOCS = "All Docs";
    public static final String SORT_BY = "Sort";
    public static final String NEWEST_FIRST = "Newest First";
    public static final String OLDEST_FIRST = "Oldest First";

    public static final String MORE_OPTIONS_CREATE_NEW_FOLDER = "New Folder";
    public static final String MORE_OPTIONS_PASTE_ = "Paste Here";
    public static final String MORE_OPTIONS_IMPORT_PDF = "Import PDF";
    public static final String MORE_OPTIONS_SYNC_SETTINGS = "Sync Settings";
    public static final String MORE_OPTIONS_RATE_APP = "Rate App";
    public static final String MORE_OPTIONS_SHARE_APP = "Share App";


    public static final String ITEM_OPTIONS_OPEN_WITH = "Open";
    public static final String ITEM_OPTIONS_SELECT_MULTIPLE_SELECT = "Select Multiple";
    public static final String ITEM_OPTIONS_SELECT_MULTIPLE_DESELECT = "Deselect";

    public static final String ITEM_OPTIONS_SELECT_MULTIPLE_DESELECT_ALL = "Deselect All";

    public static final String ITEM_OPTIONS_SHARE = "Share";
    public static final String ITEM_OPTIONS_CUT = "Cut";
    public static final String ITEM_OPTIONS_COPY = "Copy";
    public static final String ITEM_OPTIONS_DELETE = "Delete";
    public static final String ITEM_OPTIONS_RENAME = "Rename";


    public static final String _ALL = " Selected";
    public static final String BLANK_SPACE_5 = "     ";

    public static final int MENU_ITEM_ID_MORE_OPTIONS_CREATE_NEW_FOLDER = 1;
    public static final int MENU_ITEM_ID_MORE_OPTIONS_PASTE = 2;
    public static final int MENU_ITEM_ID_MORE_OPTIONS_IMPORT_PDF = 3;
    public static final int MENU_ITEM_ID_MORE_OPTIONS_SYNC_SETTINGS = 4;
    public static final int MENU_ITEM_ID_MORE_OPTIONS_RATE_APP = 5;
    public static final int MENU_ITEM_ID_MORE_OPTIONS_SHARE_APP = 6;


    public static final int MENU_ITEM_ID_OPEN_WITH = 1;
    public static final int MENU_ITEM_ID_SELECT_MULTIPLE = 2;

    public static final int MENU_ITEM_ID_SHARE = 3;
    public static final int MENU_ITEM_ID_CUT = 4;
    public static final int MENU_ITEM_ID_COPY = 5;
    public static final int MENU_ITEM_ID_DELETE = 6;
    public static final int MENU_ITEM_ID_DESELECT_ALL = 7;
    public static final int MENU_ITEM_ID_MORE_OPTIONS_RENAME = 8;

    public static final int MENU_ITEM_ID_MODIFY = 10;

    public static final int GROUPPED_ITEM_ID = 1;
    public static final int ALL_DOCS_ITEM_ID = 2;
    public static final int SORT_NEWEST_ITEM_ID = 3;
    public static final int SORT_OLDEST_ITEM_ID = 4;
    private static final int IMPORT_ACTIVITY_CODE = 182;
    private static final CharSequence ITEM_OPTIONS_EXPORT = "Export";
    private static final int EXPORT_REQUEST_CODE = 291;
    private static final String MORE_OPTIONS_IMPORT_BACKUP = "Import Backup";
    private static final String MORE_OPTIONS_EXPORT_BACKUP = "Create Backup";
    private static final int MENU_ITEM_ID_MORE_OPTIONS_IMPORT_BACKUP = 10;
    private static final int MENU_ITEM_ID_MORE_OPTIONS_EXPORT_BACKUP = 11;
    private static final int IMPORT_BACKUP_REQUEST_CODE = 220;
    private static final int EXPORT_BACKUP_REQUEST_CODE = 221;
    private static final int MENU_ITEM_ID_SHARE_HIGH_RES = 12;

    private static final int MENU_ITEM_ID_SHARE_MED_RES = 13;

    private static final int MENU_ITEM_ID_SHARE_LOW_RES = 14;
    private static final int MENU_ITEM_ID_OCR = 15;
    private static final int REQ_CODE_VERSION_UPDATE = 1839;
    private static final int MENU_ITEM_MORE_OPTIONS_ABOUT = 16;

    public ActivityMainBinding binding;
    public ListFilesAdapter adapter;

    public ArrayList<ListFIlesInfo> fIlesInfos = null;

    public String currentPath = null;
    public String baseDirPath = null;

    public Clipboard clipboard = null;

    public int selectedFilesNos = 0;

    public static boolean listingModified = false;
    private FirebaseAnalytics firebaseInstance;

    ExecutorService executor;

    private int IN_APP_UPDATE_REQUEST_CODE = 9261;

    AppUpdateManager appUpdateManager = null;

    InstallStateUpdatedListener listener = null;

    boolean updateDownloaded = false;

    boolean isPaused = false;


    private void installUpdate() {


        if (isPaused) {

            updateDownloaded = true;
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                Utils.vibrate(MainActivity.this, 40);

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                builder.setTitle("Install update");
                builder.setMessage("An update was downloaded.\nInstalling the update will only take a few seconds.");
                builder.setCancelable(false);
                builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {


                    }
                });
                builder.setPositiveButton("Install Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {


                        ProgressDialog pd = new ProgressDialog(MainActivity.this);
                        pd.setTitle("Preparing Update");
                        pd.setMessage("Just a moment");
                        pd.setCancelable(false);
                        pd.show();


                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        updateDownloaded = false;
                                        appUpdateManager.completeUpdate();
                                    }
                                });
                            }
                        }, 2800);

                    }
                });

                builder.show();

            }
        });


    }

    @Override
    public void onResume() {


        super.onResume();

        isPaused = false;
        Utils.checkOpenCV(this);


        if (executor == null || executor.isTerminated() || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(2);
        }

        if (updateDownloaded) {
            installUpdate();
        }


//        if (executor != null) {
//
//
//            try {
//                executor.execute(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        ArrayList<ListFIlesInfo> x = FileNav.getDirInfo(currentPath, null);
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//
//                                if (listingModified || x.size() != adapter.data.size()) {
//
//
//
//                                    listingModified = false;
//                                }
//                            }
//                        });
//                    }
//                });
//            } catch (Exception e) {
//            }
//        }


        nagivateTo(currentPath);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        try {
            executor.shutdown();

            while (!(executor.isTerminated() || executor.isShutdown())) {
            }
        } catch (Exception e) {
        }


    }


    boolean showDownloadingUpdateInBackground = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);


        Utils.checkOpenCV(this);

        executor = Executors.newFixedThreadPool(2);

        firebaseInstance = FirebaseAnalytics.getInstance(MainActivity.this);

        appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);

        listener = new InstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(InstallState state) {
                if (state.installStatus() == InstallStatus.DOWNLOADED) {

                    installUpdate();
                }

                if (state.installStatus() == InstallStatus.DOWNLOADING) {

                    if (showDownloadingUpdateInBackground)
                        Toast.makeText(getApplicationContext(), "Downloading update in background", Toast.LENGTH_LONG).show();

                    showDownloadingUpdateInBackground = false;
                }
            }
        };


        FileNav.createBaseDir(this);
        fIlesInfos = new ArrayList<>();


        clipboard = new Clipboard(null, false);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);


        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name_short));


        ((SearchView.SearchAutoComplete) binding.searchView.findViewById(R.id.search_src_text)).setTextColor(Color.WHITE);
        ((SearchView.SearchAutoComplete) binding.searchView.findViewById(R.id.search_src_text)).setHintTextColor(Color.argb(80, 255, 255, 255));


        baseDirPath = FileNav.getBaseDir(this).getAbsolutePath();
        currentPath = baseDirPath;


        adapter = new ListFilesAdapter(this, new ArrayList<ListFIlesInfo>(), this);
        binding.scanList.setHasFixedSize(true);
        binding.scanList.setLayoutManager(new MyGridLayoytManager(this, 3));
        binding.scanList.setAdapter(adapter);


//
//        if (Prefs.displayPrefs.getDisplayStyle(this) == Prefs.displayPrefs.ALL_DOCS)
//            allDocsDispay();

        if (getIntent().getExtras() != null && getIntent().getExtras().getString(CURRENT_DIR_PATH) != null) {

            currentPath = getIntent().getExtras().getString(CURRENT_DIR_PATH);

            nagivateTo(currentPath);
        }


        binding.backArrow.setOnClickListener(this);
        binding.middleOptionsRl.setOnClickListener(this);
        binding.moreOptionsRl.setOnClickListener(this);
        binding.cameraCapture.setOnClickListener(this);

        binding.addPhotos.setOnClickListener(this);

        binding.emptyStartScanning.setOnClickListener(this::onClick);
        searchHandle(binding.searchView);

        nagivateTo(currentPath);


        if (getIntent() != null && getIntent().getType() != null) {


            importPdf(getIntent());
        }


        showcase();

        netRequestDetails();


    }

    private void netRequestDetails() {


        if (Prefs.firstTimeSeenScreen(MainActivity.this, "first_time_app_open_ion_main_activity_news")) {

            Ion.with(this)
                    //  .load("http://prodocstatic.awessamapps.com/news/news.json")
                    .load("https://raw.githubusercontent.com/awessamapps/awessamapps.github.io/master/fewi.json")
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {


                            if (e == null) {


                                try {

                                    Long id = result.get("newsId").getAsLong();

                                    String news = result.get("msg").getAsString();
                                    String header = result.get("header").getAsString();

                                    Boolean isUpdate = result.get("isUpdate").getAsBoolean();


                                    if (id != null && news != null) {


                                        long curId = Prefs.NewsPrefs.getLatestInt(MainActivity.this);

                                        if (id > curId) {


                                            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                                            builder.setTitle(Html.fromHtml(header));

                                            TextView textView = new TextView(MainActivity.this);

                                            textView.setMovementMethod(LinkMovementMethod.getInstance());

                                            textView.setText(Html.fromHtml(news));

                                            textView.setPadding(50, 50, 50, 50);

                                            builder.setView(textView);
                                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {


                                                    Prefs.NewsPrefs.setLatestInt(MainActivity.this, id);

                                                }
                                            });
                                            builder.setCancelable(false);

                                            builder.show();

                                        } else {
                                            int acceptRating = result.get("acceptRating").getAsInt();

                                            if (isUpdate != null && isUpdate) {

                                                inAppUpdateManager(acceptRating);


                                            } else {


                                                inAppReview(acceptRating);

                                            }


                                        }
                                    }
                                } catch (Exception e1) {
                                } catch (Error e2) {
                                }


                            }

                        }
                    });

        }


    }


    private void inAppReview(int acceptRating) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                FiveStarsDialog fiveStarsDialog = new FiveStarsDialog(MainActivity.this, "");
                fiveStarsDialog.setRateText("How was your experience with this app?")
                        .setTitle("Rate app")
                        .setForceMode(false)
                        .setUpperBound(8)
                        .setNegativeReviewListener(new NegativeReviewListener() {
                            @Override
                            public void onNegativeReview(int i) {

                            }
                        })
                        .setReviewListener(new ReviewListener() {
                            @Override
                            public void onReview(int i) {

                                Bundle params = new Bundle();
                                params.putString("star_rating", String.valueOf(i));


                                if (i == 1) {
                                    firebaseInstance.logEvent("one_star_rating", params);
                                } else if (i == 2) {
                                    firebaseInstance.logEvent("two_star_rating", params);
                                } else if (i == 3) {
                                    firebaseInstance.logEvent("three_star_rating", params);
                                } else if (i == 4) {
                                    firebaseInstance.logEvent("four_star_rating", params);
                                } else if (i == 5) {
                                    firebaseInstance.logEvent("five_star_rating", params);
                                }


                                if (i >= acceptRating) {

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {


                                            Utils.vibrate(MainActivity.this, 40);
                                            Toast.makeText(getApplicationContext(), "Thank you for your feedback", Toast.LENGTH_SHORT).show();
                                            Toast.makeText(getApplicationContext(), "Please also submit your rating to Google Play", Toast.LENGTH_LONG).show();


                                            ReviewManager manager = ReviewManagerFactory.create(MainActivity.this);
                                            Task<ReviewInfo> request = manager.requestReviewFlow();
                                            request.addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    // We can get the ReviewInfo object
                                                    ReviewInfo reviewInfo = task.getResult();


                                                    Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
                                                    flow.addOnCompleteListener(task2 -> {

                                                    });


                                                } else {

                                                }
                                            });


                                        }
                                    });


                                } else {

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Thank you for your feedback", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }


                            }
                        })
                        .showAfter(5);


            }
        });

    }

    private void inAppUpdateManager(int acceptRating) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

                appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {


                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        installUpdate();

                    } else if ((appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            || appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                    appUpdateInfo,
                                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                    AppUpdateType.FLEXIBLE,
                                    // The current activity making the update request.
                                    MainActivity.this,
                                    // Include a request code to later monitor this update request.
                                    IN_APP_UPDATE_REQUEST_CODE);

                            appUpdateManager.registerListener(listener);

                        } catch (Exception e) {

                        }
                    } else {
                        inAppReview(acceptRating);
                    }

                });

            }
        });


    }


    private void showcase() {


        if (!Prefs.firstTimeSeenScreen(MainActivity.this, "main_activity")) {
            new GuideView.Builder(this)
                    .setTitle("Camera")
                    .setContentSpan((Spannable) Html.fromHtml("<b>Capture</b> images using camera."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(binding.cameraCapture)

                    .setGuideListener(new GuideListener() {
                        @Override
                        public void onDismiss(View view) {
                            new GuideView.Builder(MainActivity.this)
                                    .setTitle("Import photos")

                                    .setContentSpan((Spannable) Html.fromHtml("<b>Import</b> images from the device."))
                                    .setGravity(Gravity.auto) //optional
                                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                    .setTargetView(binding.addPhotos)
                                    .setGuideListener(new GuideListener() {
                                        @Override
                                        public void onDismiss(View view) {
                                            new GuideView.Builder(MainActivity.this)
                                                    .setTitle("More options")

                                                    .setContentSpan((Spannable) Html.fromHtml("<b>More options</b> includes Create a <b>New folder</b>, <b>Import PDF</b> and Create or Import <b>Backup</b> of your data."))
                                                    .setGravity(Gravity.auto) //optional
                                                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                                    .setTargetView(binding.moreOptionsRl)


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

    private void importPdf(Intent intent) {


        if (intent.getClipData() != null) {


            ArrayList<Uri> uris = new ArrayList<>();

            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {

                uris.add(intent.getClipData().getItemAt(i).getUri());
            }

            processImportLocalPdf(uris);


        } else if (intent.getData() != null) {

            ArrayList<Uri> uris = new ArrayList<>();

            uris.add(intent.getData());
            processImportLocalPdf(uris);
        }


    }


    public void searchHandle(SearchView searchView) {


        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {


                nagivateTo(currentPath);
                return false;
            }
        });


        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fIlesInfos = adapter.data;

            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                doSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                doSearch(newText);

                return true;
            }

            public void doSearch(String newText) {

                newText = newText.toLowerCase();

                adapter.data = new ArrayList<>(fIlesInfos);

                for (int i = 0; i < adapter.data.size(); i++) {

                    String filename = adapter.data.get(i).filename.toLowerCase();
                    int index = filename.indexOf(newText);

                    if (index == -1) {
                        adapter.data.remove(i);

                        i--;
                    }
                }

                binding.scanList.getAdapter().notifyDataSetChanged();
            }
        });


    }

    public void setSpanActionColor(SpannableString s, int compare1, int compare2) {

        if (compare1 == compare2)
            s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSecondary)), 0, s.length(), 0);
    }


    public void setSpannableDrawable(int drawableId, SpannableString spannableString) {

        Drawable d = ContextCompat.getDrawable(this, drawableId);
        d.setTint(getResources().getColor(R.color.colorPrimary));
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
        spannableString.setSpan(span, 0, 3, Spannable.SPAN_INTERMEDIATE);
    }


    public void showMoreOptions(View view) {


        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);

        Menu menu = popupMenu.getMenu();

        SpannableString newFolder = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_CREATE_NEW_FOLDER);
        SpannableString paste = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_PASTE_);
        SpannableString importPdf = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_IMPORT_PDF);
        SpannableString syncSettings = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_SYNC_SETTINGS);
        SpannableString rateApp = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_RATE_APP);
        SpannableString shareApp = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_SHARE_APP);

        SpannableString importBackup = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_IMPORT_BACKUP);

        SpannableString exportBackup = new SpannableString(BLANK_SPACE_5 + MORE_OPTIONS_EXPORT_BACKUP);

        SpannableString more = new SpannableString(BLANK_SPACE_5 + "More");
        SpannableString about = new SpannableString(BLANK_SPACE_5 + "About");
        SpannableString localBackup = new SpannableString(BLANK_SPACE_5 + "Local Backup");


        setSpannableDrawable(R.drawable.baseline_create_new_folder_black_24, newFolder);
        setSpannableDrawable(R.drawable.baseline_content_paste_black_24, paste);
        setSpannableDrawable(R.drawable.baseline_picture_as_pdf_white_24, importPdf);
        setSpannableDrawable(R.drawable.baseline_cloud_black_24, syncSettings);
        setSpannableDrawable(R.drawable.baseline_rate_review_black_24, rateApp);
        setSpannableDrawable(R.drawable.baseline_share_black_24, shareApp);

        setSpannableDrawable(R.drawable.baseline_archive_white_24, importBackup);
        setSpannableDrawable(R.drawable.baseline_unarchive_white_24, exportBackup);
        setSpannableDrawable(R.drawable.baseline_more_white_24, more);
        setSpannableDrawable(R.drawable.baseline_info_white_24, about);
        setSpannableDrawable(R.drawable.baseline_sd_storage_white_24, localBackup);


        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_CREATE_NEW_FOLDER, 0, newFolder);

        if (clipboard != null && clipboard.filepaths != null)
            menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_PASTE, 0, paste);


        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_IMPORT_PDF, 0, importPdf);

        //TODO
        // menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_SYNC_SETTINGS, 0, syncSettings);


        SubMenu sMenu = menu.addSubMenu(localBackup);

        sMenu.setHeaderTitle(Html.fromHtml("<h4>Local Backup</h4>"));
        sMenu.add(0, MENU_ITEM_ID_MORE_OPTIONS_IMPORT_BACKUP, 0, importBackup);
        sMenu.add(0, MENU_ITEM_ID_MORE_OPTIONS_EXPORT_BACKUP, 0, exportBackup);


        SubMenu sMenu1 = menu.addSubMenu(more);
        sMenu1.setHeaderTitle(Html.fromHtml("<h4>More</h4>"));
        sMenu1.add(0, MENU_ITEM_ID_MORE_OPTIONS_RATE_APP, 0, rateApp);
        sMenu1.add(0, MENU_ITEM_ID_MORE_OPTIONS_SHARE_APP, 0, shareApp);
        sMenu1.add(0, MENU_ITEM_MORE_OPTIONS_ABOUT, 0, about);

//        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_RATE_APP, 0, rateApp);
//        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_SHARE_APP, 0, shareApp);


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {


                int itemId = item.getItemId();


                switch (itemId) {


                    case MENU_ITEM_ID_MORE_OPTIONS_CREATE_NEW_FOLDER:

                        newDir();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_PASTE:
                        pasteHere();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_IMPORT_PDF:
                        importLocalPdf();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_SYNC_SETTINGS:
                        openSyncSettings();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_RATE_APP:
                        rateThisApp();
                        break;
                    case MENU_ITEM_ID_MORE_OPTIONS_SHARE_APP:
                        shareThisApp();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_IMPORT_BACKUP:

                        importLocalBackup();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_EXPORT_BACKUP:

                        exportLocalBackup();
                        break;

                    case MENU_ITEM_MORE_OPTIONS_ABOUT:

                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                        builder.setTitle(Html.fromHtml("<h4>Prodoc Scanner <i>v" + BuildConfig.VERSION_NAME + "</i></h4>"));

                        String s = "Developed by <b><i>Awessam Apps India</i></b>" + "<br><br>" + "<a href='mailto:correspondence.awessamapps@gmail.com'>correspondence.awessamapps@gmail.com</a>";
                        TextView textView = new TextView(MainActivity.this);

                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                        textView.setTextIsSelectable(true);

                        textView.setText(Html.fromHtml(s));

                        textView.setPadding(50, 50, 50, 50);
                        builder.setView(textView);
                        builder.show();

                        break;


                }


                return false;
            }
        });

        popupMenu.show();


    }

    private void importLocalBackup() {


        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");


        startActivityForResult(intent, IMPORT_BACKUP_REQUEST_CODE);


    }


    private void processImportLocalBackup(Uri uri) {

        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setTitle("Importing Backup");
        pd.setMessage("It will take a while");
        pd.setCancelable(false);
        pd.show();


        executor.execute(new Runnable() {
            @Override
            public void run() {


                try (InputStream fin = getContentResolver().openInputStream(uri);) {

                    long totalSize = fin.available();
                    final long[] doneSize = {0};

                    FileNav.unZipAll(fin, FileNav.getBaseDir(getApplicationContext()), new FileNav.OnZipProgress() {
                        @Override
                        public void onProgress(long progress) {

                            doneSize[0] += progress;


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.setMessage("" + String.format("%.2f", Math.min(100, (float) doneSize[0] * 100.0f / totalSize * 1.0f)) + "% complete");
                                }
                            });
                        }
                    });


                } catch (FileNotFoundException e1) {


                    e1.printStackTrace();
                } catch (IOException e2) {

                    e2.printStackTrace();

                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                        } catch (Exception e) {

                        }

                        nagivateTo(currentPath);
                    }
                });

            }
        });


    }


    private void processExportLocalBackup(Uri uri) {


        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setTitle("Creating Backup");
        pd.setMessage("It may take a while");
        pd.setCancelable(false);
        pd.show();

        executor.execute(new Runnable() {
            @Override
            public void run() {

                File mainDir = FileNav.getBaseDir(getApplicationContext());


                OutputStream outputStream = null;
                ZipOutputStream outputStream1 = null;

                try {
                    outputStream = getContentResolver().openOutputStream(uri);

                    outputStream1 = new ZipOutputStream(outputStream);


                    try {
                        File baseD = FileNav.getBaseDir(getApplicationContext());
                        FileNav.zipDir(baseD.getAbsolutePath(), baseD, outputStream1);
                    } catch (Exception e) {
                    }


                    if (outputStream1 != null)
                        outputStream1.close();


                    if (outputStream != null)
                        outputStream.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                        } catch (Exception e) {

                        }

                        Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });


    }

    private void exportLocalBackup() {


        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.putExtra(Intent.EXTRA_TITLE, "Prodoc Backup " + new SimpleDateFormat("dd-MM-yy HH.mm.ss").format(System.currentTimeMillis()) + ".zip");
        intent.setType("*/*");


        startActivityForResult(intent, EXPORT_BACKUP_REQUEST_CODE);

    }

    private void shareThisApp() {


        String SHARE_MESSAGE = "";
        String SHARE_MESSAGE_EXTRA = "";

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        String shareBody = SHARE_MESSAGE + "https://play.google.com/store/apps/details?id=" + getPackageName();

        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, SHARE_MESSAGE_EXTRA);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));

    }

    private void rateThisApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }

    }

    private void openSyncSettings() {

    }

    private void importLocalPdf() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);


        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        intent.setType("application/pdf");

        startActivityForResult(intent, REQUEST_CODE_IMPORT_LOCAL_PDF);

    }


    private void pasteHere() {


        if (clipboard.filepaths != null && clipboard.filepaths.size() != 0) {


            ProgressDialog pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("");
            pd.setCancelable(false);
            pd.show();
            executor.execute(() -> {


                for (int i = 0; i < clipboard.filepaths.size(); i++) {


                    int finalI = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.setMessage("Copying... " + (finalI + 1) + " of " + clipboard.filepaths.size());
                        }
                    });

                    File src = new File(clipboard.filepaths.get(i));

                    String fName = src.getName();

                    File dst = new File(currentPath + File.separator + fName);


                    boolean sameExists = false;
                    for (int j = 0; j < 50; j++) {


                        if (dst.exists()) {
                            fName = "Copy " + fName;

                            sameExists = true;
                            dst = new File(currentPath + File.separator + fName);
                        } else
                            break;
                    }

                    try {
                        FileUtils.copyDirectory(src, dst);

                        if (clipboard.deleteAfter) {

                            if (!FileNav.isChild(src, new File(currentPath)))
                                FileNav.deleteDirectoryQuietely(src);
                            clipboard.filepaths.remove(i);
                            i--;


                        }


                    } catch (Exception e) {

                    }


                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        try {
                            pd.dismiss();
                        } catch (Exception e) {

                        }
                        nagivateTo(currentPath);

                    }
                });


            });


        } else {
            simpleToast("Nothing to paste");
        }
    }

    private void newDir() {


        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Folder name");

// Set up the input
        TextInputEditText input = new TextInputEditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        input.requestFocus();

        InputMethodManager imm1 = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm1.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

// Set up the buttons

        builder.setPositiveButton("Create", (dialog, which) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);


            String folderName = input.getText().toString();


            int created = FileNav.createDir(currentPath, folderName);


            switch (created) {

                case FileNav.DIR_CREATED:
                    nagivateTo(currentPath);
                    break;

                case FileNav.DIR_ALREADY_EXISTS:
                    Toast.makeText(getApplicationContext(), "Folder with this name already exists", Toast.LENGTH_SHORT).show();
                    newDir();
                    break;

                case FileNav.DIR_NAME_INVALID:
                    Toast.makeText(getApplicationContext(), "Invalid folder name", Toast.LENGTH_SHORT).show();
                    newDir();
                    break;

            }

        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {

            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

            dialog.cancel();
        });

        builder.show();

    }


    public void showMiddleOptions(View view) {


        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);

        Menu menu = popupMenu.getMenu();

        SpannableString groupped = new SpannableString("     " + GROUPED);
        SpannableString allDocs = new SpannableString("     " + ALL_DOCS);

        SpannableString sortBy = new SpannableString("     " + SORT_BY);

        SpannableString newestFirst = new SpannableString(NEWEST_FIRST);
        SpannableString oldestFirst = new SpannableString(OLDEST_FIRST);


        setSpannableDrawable(R.drawable.baseline_receipt_black_24, allDocs);
        setSpannableDrawable(R.drawable.baseline_folder_black_24, groupped);
        setSpannableDrawable(R.drawable.baseline_sort_black_24, sortBy);


        int displayPref = Prefs.displayPrefs.getDisplayStyle(this);
        int sortPref = Prefs.displayPrefs.getSort(this);


        setSpanActionColor(groupped, displayPref, Prefs.displayPrefs.GROUPPED);
        setSpanActionColor(allDocs, displayPref, Prefs.displayPrefs.ALL_DOCS);

        setSpanActionColor(newestFirst, sortPref, Prefs.displayPrefs.NEWEST_FIRST);
        setSpanActionColor(oldestFirst, sortPref, Prefs.displayPrefs.OLDEST_FIRST);


        menu.add(0, GROUPPED_ITEM_ID, 0, groupped);
        menu.add(0, ALL_DOCS_ITEM_ID, 0, allDocs);


        SubMenu sMenu = menu.addSubMenu(sortBy);
        sMenu.add(0, SORT_NEWEST_ITEM_ID, 0, newestFirst);
        sMenu.add(0, SORT_OLDEST_ITEM_ID, 0, oldestFirst);


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {


                int itemId = item.getItemId();

                if (itemId == GROUPPED_ITEM_ID) {

                    Prefs.displayPrefs.setDisplayStyle(getApplicationContext(), Prefs.displayPrefs.GROUPPED);
                    binding.middleOptionsText.setText(GROUPED);
                    nagivateTo(currentPath);

                } else if (itemId == ALL_DOCS_ITEM_ID) {


                    allDocsDispay();

                } else if (itemId == SORT_NEWEST_ITEM_ID) {

                    Prefs.displayPrefs.setSort(getApplicationContext(), Prefs.displayPrefs.NEWEST_FIRST);

                    sortScans();
                    binding.scanList.getAdapter().notifyDataSetChanged();


                } else if (itemId == SORT_OLDEST_ITEM_ID) {

                    Prefs.displayPrefs.setSort(getApplicationContext(), Prefs.displayPrefs.OLDEST_FIRST);
                    sortScans();
                    binding.scanList.getAdapter().notifyDataSetChanged();


                }


                return false;
            }
        });

        popupMenu.show();


    }

    private void sortScans() {

        int sortInt = Prefs.displayPrefs.getSort(getApplicationContext());

        Collections.sort(adapter.data, new Comparator<ListFIlesInfo>() {
            @Override
            public int compare(ListFIlesInfo lhs, ListFIlesInfo rhs) {


                if (lhs.dateModified > rhs.dateModified)
                    return 1 * sortInt;
                else if (lhs.dateModified == rhs.dateModified)
                    return 0;
                else
                    return -1 * sortInt;
            }
        });
    }


    private void allDocsDispay() {


        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Searching for docs...");
        pd.setCancelable(false);
        pd.show();


        Prefs.displayPrefs.setDisplayStyle(this, Prefs.displayPrefs.ALL_DOCS);

        binding.middleOptionsText.setText(ALL_DOCS);

        executor.execute(new Runnable() {
            @Override
            public void run() {

                fIlesInfos = FileNav.listDirRec(new File(currentPath));
                adapter.data = fIlesInfos;
                sortScans();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        try {
                            pd.dismiss();
                        } catch (Exception e) {

                        }
                    }
                });

            }
        });


    }


    public void onClickBack() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                nagivateBack();
            }
        }, 100);
    }


    public void nagivateBack() {

        String newPath = null;
        try {
            newPath = new File(currentPath + "/../").getCanonicalPath();

            if (!currentPath.equals(baseDirPath))
                nagivateTo(newPath);
        } catch (IOException e) {

        }

    }

    public void nagivateTo(String path) {

        Prefs.displayPrefs.setDisplayStyle(this, Prefs.displayPrefs.GROUPPED);

        binding.middleOptionsText.setText(GROUPED);

        currentPath = path;

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {

                    fIlesInfos = FileNav.getDirInfo(path, null);
                    adapter.data = fIlesInfos;
                    sortScans();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            if (adapter.data.size() > 0) {
                                binding.emptyDocumentImage.setVisibility(View.GONE);
                            } else {

                                binding.emptyDocumentImage.setVisibility(View.VISIBLE);

                                if (!currentPath.equals(baseDirPath)) {
                                    binding.emptyDocumentTV.setText("FOLDER IS EMPTY");
                                    binding.emptyDocumentTV1.setText("SCAN NOW");

                                } else {
                                    binding.emptyDocumentTV.setText("IT'S EMPTY HERE");
                                    binding.emptyDocumentTV1.setText("START SCANNING");
                                }
                            }


                            toogleBackArrow(!path.equals(baseDirPath));

                            binding.scanList.getAdapter().notifyDataSetChanged();

                        }
                    });

                }
            });
        } catch (Exception e) {
        }


    }

    public void onDirClick(String dirPath) {

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                nagivateTo(dirPath);

            }
        }, 100);

    }

    public void toogleBackArrow(boolean show) {

        if (show) {
            binding.backArrow.setVisibility(View.VISIBLE);
            binding.toolbarTitle.setVisibility(View.GONE);
        } else {
            binding.backArrow.setVisibility(View.GONE);
            binding.toolbarTitle.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onScanClick(int position, View view) {


        if (view.getId() == R.id.moreOptions) {


            showItemMoreOptions(view, position);


        } else if (view.getId() == R.id.mainRl) {

            if (selectedFilesNos > 0) {

                selectDeselect(position);
                return;
            }

            if (!adapter.getItem(position).is_scan)
                onDirClick(adapter.getItem(position).filepath);

            else {


                if (!adapter.data.get(position).is_scan)
                    return;


                openScan(position);
            }

        }


    }

    private void openScan(int position) {
        Intent intent = new Intent(MainActivity.this, ScanPreviewActivity.class);
        intent.putExtra(ScanPreviewActivity.SCAN_DIR_PATH, adapter.data.get(position).filepath);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


        startActivity(intent);
    }

    @Override
    public void onLongPress(int position, View view) {

        showItemMoreOptions(view, position);


        //selectDeselect(position);
    }

    private void showItemMoreOptions(View view, int position) {

        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);

        Menu menu = popupMenu.getMenu();

        boolean itemSelected = selectedFilesNos > 0;

        SpannableString select = null;


        if (itemSelected && adapter.getItem(position).isSelected)
            select = new SpannableString("Deselect");

        else if (itemSelected && adapter.getItem(position).isSelected == false)
            select = new SpannableString("Select");
        else
            select = new SpannableString("Select Multiple");


        SpannableString share = new SpannableString(itemSelected ? ITEM_OPTIONS_SHARE + _ALL : ITEM_OPTIONS_SHARE);
        SpannableString cut = new SpannableString(itemSelected ? ITEM_OPTIONS_CUT + _ALL : ITEM_OPTIONS_CUT);
        SpannableString copy = new SpannableString(itemSelected ? ITEM_OPTIONS_COPY + _ALL : ITEM_OPTIONS_COPY);
        SpannableString delete = new SpannableString(itemSelected ? ITEM_OPTIONS_DELETE + _ALL : ITEM_OPTIONS_DELETE);
        SpannableString rename = new SpannableString(itemSelected ? ITEM_OPTIONS_RENAME + _ALL : ITEM_OPTIONS_RENAME);


        menu.add(0, MENU_ITEM_ID_SELECT_MULTIPLE, 0, select);


        if (itemSelected) {
            SpannableString deselectAll = new SpannableString(ITEM_OPTIONS_SELECT_MULTIPLE_DESELECT_ALL);
            menu.add(0, MENU_ITEM_ID_DESELECT_ALL, 0, deselectAll);

        } else {
            menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_RENAME, 0, rename);


            if (adapter.data.get(position).is_scan) {
//                SpannableString export = new SpannableString(ITEM_OPTIONS_EXPORT);
//                menu.add(0, MENU_ITEM_ID_MODIFY, 0, export);

                SpannableString export = new SpannableString("Modify Scan");
                menu.add(0, MENU_ITEM_ID_MODIFY, 0, export);


                SpannableString ocr = new SpannableString("OCR");
                menu.add(0, MENU_ITEM_ID_OCR, 0, ocr);


            }

        }

        if (adapter.data.get(position).is_scan) {

            boolean showShare = true;
            for (ListFIlesInfo fIlesInfo : adapter.data) {

                if (fIlesInfo.isSelected && !fIlesInfo.is_scan) {
                    showShare = false;
                    break;
                }
            }

            if (showShare) {


                menu.add(0, MENU_ITEM_ID_SHARE, 0, share);

                //todo  share here

            }


        }


        menu.add(0, MENU_ITEM_ID_CUT, 0, cut);
        menu.add(0, MENU_ITEM_ID_COPY, 0, copy);
        menu.add(0, MENU_ITEM_ID_DELETE, 0, delete);


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {


                int itemId = item.getItemId();


                if (itemId != MENU_ITEM_ID_SELECT_MULTIPLE && selectedFilesNos == 1) {
                    deselectAll();

                }

                switch (itemId) {


                    case MENU_ITEM_ID_OCR:

                        ocr(position);


                        break;
                    case MENU_ITEM_ID_SHARE:

                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }
                        shareDocs();

                        break;

                    case MENU_ITEM_ID_SELECT_MULTIPLE:

                        selectDeselect(position);

                        break;

                    case MENU_ITEM_ID_DESELECT_ALL:

                        deselectAll();

                        break;


                    case MENU_ITEM_ID_CUT:

                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }

                        copyToClipBoard(true);


                        break;

                    case MENU_ITEM_ID_COPY:

                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }
                        copyToClipBoard(false);


                        break;

                    case MENU_ITEM_ID_DELETE:


                        if (selectedFilesNos == 0) {
                            deleteFileAtPath(adapter.getItem(position).filepath, position);

                        } else {

                            if (!adapter.getItem(position).isSelected)
                                selectDeselect(position);

                            deleteAll();

                        }

                        break;


                    case MENU_ITEM_ID_MORE_OPTIONS_RENAME:

                        renameFile(adapter.data.get(position).filepath, position);

                        break;

                    case MENU_ITEM_ID_MODIFY:

                        openScan(position);

                        break;
                }


                return false;
            }
        });

        popupMenu.show();


    }

    private void ocr(int position) {

        SavedImageDetails imageDetails = new SavedImageDetails(FileNav.getEffectsFile(adapter.data.get(position).filepath));

        ArrayList<String> paths = new ArrayList<>();

        ListIterator<String> iterator = imageDetails.getOrdering().listIterator();

        while (iterator.hasNext()) {

            String name = iterator.next();

            String bmpPath = adapter.data.get(position).filepath + File.separator + FileNav.ORIGINAL_IMAGE_DIR + File.separator + name;

            String processedPath = adapter.data.get(position).filepath + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + name;

            if (new File(processedPath).exists()) {
                bmpPath = processedPath;
            }
            paths.add(bmpPath);


        }


        Bundle args = new Bundle();

        Intent intent = new Intent(MainActivity.this, OcrActivity.class);
        args.putSerializable(OcrActivity.IMAGE_PATHS, (Serializable) paths);
        intent.putExtra(OcrActivity.IMAGE_PATHS, args);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);


    }

    private void shareDocs() {


        ProgressDialog pd1 = new ProgressDialog(MainActivity.this);
        pd1.setTitle("Please wait");
        pd1.setMessage("Processing uncropped images");
        pd1.setCancelable(false);


        executor.execute(new Runnable() {
            @Override
            public void run() {


                File outputDir = null;


                ArrayList<String> selectedDocs = new ArrayList<>();

                ArrayList<SavedImageDetails> imageDetailsArrayList = new ArrayList<>();


                double size = 0;

                for (ListFIlesInfo fIlesInfo : adapter.data) {

                    if (fIlesInfo.isSelected && fIlesInfo.is_scan) {

                        outputDir = FileNav.getOutputDir(fIlesInfo.filepath);

                        outputDir.mkdirs();

                        SavedImageDetails imageDetails = new SavedImageDetails(FileNav.getEffectsFile(fIlesInfo.filepath));

                        Utils.copyNotProcessedOriginals(null, imageDetails, fIlesInfo.filepath, new Utils.OnUpdateCopy() {
                            @Override
                            public void showDialog() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            if (!pd1.isShowing())
                                                pd1.show();
                                        } catch (Exception e) {
                                        } catch (Error e2) {
                                        }

                                    }
                                });
                            }
                        });

                        imageDetailsArrayList.add(imageDetails);

                        selectedDocs.add(fIlesInfo.filepath);

                        File[] fs = new File(fIlesInfo.filepath + File.separator + FileNav.PROCESSED_IMAGE_DIR).listFiles();

                        for (File f : fs) {

                            size += f.length() / 1024.0;
                        }

                    }


                }


                double finalSize = size;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            pd1.dismiss();
                        } catch (Exception e) {
                        }


                        ProgressDialog pd = new ProgressDialog(MainActivity.this);
                        pd.setTitle("Making PDF 1");
                        pd.setMessage("Page 1");
                        pd.setCancelable(false);


                        new ShareDialog(MainActivity.this, finalSize, new ShareDialog.OnShareDialogListener() {
                            @Override
                            public void share(boolean isPDF, double quality, String password) {

                                quality = (quality) / 200.0;


                                pd.show();
                                double finalQuality = quality;
                                executor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        makeAllPDFs(selectedDocs, imageDetailsArrayList, finalQuality, pd, isPDF, password);
                                    }
                                });


                            }
                        }).build(false).show();

                        deselectAll();


                    }
                });


            }
        });


    }


    private static boolean isComplete = false;

    private void makeAllPDFs(ArrayList<String> selectedDocs, ArrayList<SavedImageDetails> imageDetailsArrayList, double quality, ProgressDialog pd, boolean isPDF, String password) {


        ArrayList<File> arrayList = new ArrayList<>();

        for (int i = 0; i < selectedDocs.size(); i++) {

            int finalI = i;


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (isPDF) {
                        pd.setTitle("Making PDF " + (finalI + 1) + "/" + selectedDocs.size());
                    } else {
                        pd.setTitle("Preparing Doc " + (finalI + 1) + "/" + selectedDocs.size());
                    }
                }
            });


            String scanDirPath = selectedDocs.get(i);

            File outputDir = FileNav.getOutputDir(scanDirPath);
            String pdfName = FileNav.getPDFName(scanDirPath);

            File outputFile = new File(outputDir, pdfName);


            ArrayList<File> files = new ArrayList<>();
            ListIterator listIterator = imageDetailsArrayList.get(i).getOrdering().listIterator();

            while (listIterator.hasNext()) {

                String s = scanDirPath + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + listIterator.next();

                File f = new File(s);

                files.add(f);

            }


            if (isPDF) {

                try {
                    new DocMaker(MainActivity.this, files, quality, password).make(outputFile.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
                        @Override
                        public void onUpdate(int currentPage, int totalPage) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.setMessage("Page " + currentPage + "/" + totalPage);
                                }
                            });
                        }

                        @Override
                        public void onComplete(String output) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    arrayList.add(new File(output));


                                }
                            });

                        }

                        @Override
                        public void onComplete(ArrayList<File> output) {

                        }
                    });
                } catch (IOException e) {


                }

            } else {


                isComplete = false;
                try {
                    new DocMaker(MainActivity.this, files, quality, password).makeImages(outputDir.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
                        @Override
                        public void onUpdate(int currentPage, int totalPage) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.setMessage("Page " + currentPage + "/" + totalPage);
                                }
                            });
                        }

                        @Override
                        public void onComplete(String output) {

                        }

                        @Override
                        public void onComplete(ArrayList<File> output) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    arrayList.addAll(output);

                                    isComplete = true;

                                }
                            });

                        }
                    });


                    while (!isComplete) {

                        Thread.sleep(400);
                    }

                } catch (Exception e) {


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pd.dismiss();
                            } catch (Exception ex) {

                            }


                        }
                    });
                }


            }


        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    pd.dismiss();
                } catch (Exception e) {

                }

                new Sharer(MainActivity.this, arrayList).share();
            }
        });


    }

    private void export(int position) {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, adapter.data.get(position).filename + ".pdf");
        intent.setType("application/pdf");


        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }


    private void copyToClipBoard(boolean deleteAfter) {


        simpleToast("Copied to clipboard");
        Utils.vibrate(MainActivity.this, 25);


        if (!Prefs.firstTimeSeenScreen(MainActivity.this, "copy_doc_folder")) {

            new GuideView.Builder(MainActivity.this)
                    .setTitle("How to paste?")

                    .setContentSpan((Spannable) Html.fromHtml("At first, <b>navigate</b> to the desired <b>folder</b> and then <b>click</b> here to <b>paste</b>."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(binding.moreOptionsRl)


                    .build()
                    .show();
        }

        clipboard.deleteAfter = deleteAfter;


        clipboard.filepaths = new ArrayList<>();


        for (ListFIlesInfo fIlesInfo : adapter.data) {

            if (fIlesInfo.isSelected) {
                clipboard.filepaths.add(fIlesInfo.filepath);
            }
        }

        deselectAll();

    }

    private void renameFile(String filepath, int position) {


        SpannableString spannableString = new SpannableString("Rename");

        setSpanActionColor(spannableString, 1, 1);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Rename");

        String fromNameAtomic = adapter.getItem(position).filename;
        final TextInputEditText input = new TextInputEditText(this);

        input.setText(fromNameAtomic);
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        input.requestFocus();

        InputMethodManager imm1 = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm1.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);


        builder.setPositiveButton(spannableString, (dialog, which) -> {

            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);


            String newName = input.getText().toString();

            String toName = newName;

            toName += adapter.getItem(position).is_scan ? FileNav.SCAN_IDENTIFIER : FileNav.DIR_IDENTIFIER;

            if (!FileNav.validFileName(newName)) {
                simpleToast("Not a valid file name");
                renameFile(filepath, position);
            } else {


                String fromName = fromNameAtomic + (adapter.getItem(position).is_scan ? FileNav.SCAN_IDENTIFIER : FileNav.DIR_IDENTIFIER);


                //TODO RENAME FILE INSTEAD OF DELETING

                FileUtils.deleteQuietly(FileNav.getOutputDir(adapter.getItem(position).filepath));
                boolean renamed = FileNav.rename(adapter.getItem(position).filepath, fromName, toName);


                if (renamed) {

                    adapter.getItem(position).filepath = currentPath + File.separator + toName;


                    File processedDir = new File(adapter.getItem(position).filepath + File.separator + FileNav.PROCESSED_IMAGE_DIR);

                    if (processedDir.exists()) {

                        File[] imgs = processedDir.listFiles();

                        if (imgs.length != 0) {
                            adapter.getItem(position).thumbnailPath = imgs[0].getAbsolutePath();
                        }

                    } else {
                        File originalDir = new File(adapter.getItem(position).filepath + File.separator + FileNav.ORIGINAL_IMAGE_DIR);


                        if (originalDir.exists()) {

                            File[] imgs = originalDir.listFiles();

                            if (imgs.length != 0) {

                                adapter.getItem(position).thumbnailPath = imgs[0].getAbsolutePath();
                            }

                        }


                    }

                    adapter.getItem(position).filename = newName;
                    adapter.notifyItemChanged(position);
                } else {
                    simpleToast("File already exits with same name");
                    renameFile(filepath, position);
                }


            }


        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {

            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {

            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);


            dialog.cancel();

        });

        builder.show();

    }

    private void deleteAll() {

        SpannableString cancel = new SpannableString("Cancel");
        setSpanActionColor(cancel, 1, 1);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete " + selectedFilesNos + " items ?");


        builder.setPositiveButton("Delete", (dialog, which) -> {

            selectedFilesNos = 0;

            for (int i = 0; i < adapter.data.size(); i++) {


                if (adapter.data.get(i).isSelected) {


                    try {
                        // FileUtils.deleteDirectory(new File(adapter.data.get(i).filepath));

                        FileNav.deleteDirectoryQuietely(new File(adapter.data.get(i).filepath));

                    } catch (Exception e) {

                    }
                    adapter.data.remove(i);

                    i--;
                }

            }
            adapter.notifyDataSetChanged();
            deselectAll();
            simpleToast("Deleted");
        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void selectDeselect(int position) {

        adapter.data.get(position).isSelected = !adapter.data.get(position).isSelected;
        adapter.notifyItemChanged(position);

        selectedFilesNos += adapter.data.get(position).isSelected ? 1 : -1;
    }

    private void deselectAll() {
        selectedFilesNos = 0;


        for (int i = 0; i < adapter.data.size(); i++) {


            boolean selected = adapter.data.get(i).isSelected;
            adapter.data.get(i).isSelected = false;

            if (selected)
                adapter.notifyItemChanged(i);
        }


    }


    private void deleteFileAtPath(String filepath, int position) {

        SpannableString cancel = new SpannableString("Cancel");
        setSpanActionColor(cancel, 1, 1);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete?");


        builder.setPositiveButton("Delete", (dialog, which) -> {

            try {
                //   FileUtils.deleteDirectory(new File(filepath));

                FileNav.deleteDirectoryQuietely(new File(filepath));
                nagivateTo(currentPath);
                simpleToast("Deleted");
                deselectAll();
            } catch (Exception e) {

            }


        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();


    }


    private void simpleToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }


    private void pdfToBitmap(InputStream in, PDDocument pdDocument, ProgressDialog dialog, String name) throws IOException {

        name = name == null ? "PDF" : name;

        String finalName = name;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setTitle("Importing");
                dialog.setMessage("It will take a while");

                if (!dialog.isShowing()) {
                    try {
                        dialog.show();
                    } catch (Exception e) {
                    }
                }
            }
        });


        pdDocument.setAllSecurityToBeRemoved(true);

        File tempFile = FileNav.getTempFile(MainActivity.this, "temp.pdf");

        if (tempFile.exists()) {
            FileUtils.deleteQuietly(tempFile);
            tempFile = FileNav.getTempFile(MainActivity.this, "temp.pdf");
        }

        pdDocument.save(tempFile);
        pdDocument.close();


        File scanDir = FileNav.newScanDir(currentPath);

        File originalImageDir = FileNav.originalScanDirFromScanDir(scanDir);


        SavedImageDetails imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDir.getAbsoluteFile()));


        PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY));
        int pages = renderer.getPageCount();


        Bitmap bitmap;


        for (int i = 0; i < pages; i++) {


            PdfRenderer.Page page = renderer.openPage(i);


            int width = 4 * page.getWidth();
            int height = 4 * page.getHeight();


            if (width < 2500) {
                double scale = 2500.0 / width;

                width *= scale;
                height *= scale;
            }


            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, 0, 0, null);


            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);


            String filename = i + ".jpg";
            String filepath = originalImageDir.getAbsolutePath() + File.separator + filename;

            File processedFileDir = new File(scanDir + File.separator + FileNav.PROCESSED_IMAGE_DIR);

            processedFileDir.mkdirs();

            File processedFile = new File(processedFileDir, filename);


            try (FileOutputStream out = new FileOutputStream(filepath)) {

                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

                imageDetails.getOrdering().add(filename);

                HashMap<Integer, PointF> pointMap = new HashMap<>();

                pointMap.put(0, new PointF(0, 0));
                pointMap.put(1, new PointF(width, 0));
                pointMap.put(2, new PointF(0, height));
                pointMap.put(3, new PointF(width, height));

                Effects effects = new Effects(pointMap, MatFilter.COLOR_ORIGINAL, false, 0, 0);
                imageDetails.putEffects(filename, effects);

                out.close();

                int finalI = i;
                String finalName1 = name;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage(Html.fromHtml("<b>" + finalName1 + "</b><br><br>" + "Page " + (finalI + 1) + "/" + pages));

                        if (!dialog.isShowing()) {
                            try {
                                dialog.show();
                            } catch (Exception e) {
                            }
                        }

                    }
                });


            } catch (IOException e) {

            }

            FileUtils.copyFile(new File(filepath), processedFile);

            imageDetails.sync();


            if (bitmap != null && !bitmap.isRecycled())
                bitmap.recycle();
            page.close();

            in.close();

        }


        renderer.close();

        FileUtils.deleteQuietly(tempFile);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    if (dialog.isShowing())
                        dialog.dismiss();

                } catch (Exception e) {
                }

                nagivateTo(currentPath);

            }
        });


    }


    private void processImportLocalPdf(ArrayList<Uri> uris) {


        ExecutorService service = Executors.newFixedThreadPool(1);


        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setTitle("Importing PDF");
        pd.setMessage("It will take a while");
        pd.setButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {


                            service.shutdown();

                            while (!(service.isTerminated() || service.isShutdown())) {
                            }


                        } catch (Exception e) {
                        }

                        try {
                            if (pd.isShowing())
                                pd.dismiss();
                        } catch (Exception e) {
                        }

                        nagivateTo(currentPath);
                    }
                });

            }
        });
        pd.setCancelable(false);
        pd.show();


        executor.execute(new Runnable() {
            @Override
            public void run() {


                for (int i = 0; i < uris.size(); i++) {


                    int finalI = i;
                    boolean encrypted = false;


                    try {


                        InputStream in = getContentResolver().openInputStream(uris.get(i));


                        PDDocument pdDocument = null;
                        try {

                            pdDocument = PDDocument.load(in);

//
//                            if (pdDocument.isEncrypted()) {
//                                encrypted = true;
//
//                            }
                        } catch (InvalidPasswordException e2) {
                            encrypted = true;
                        }


                        if (encrypted) {
                            in.close();

                            try {
                                pdDocument.close();
                            } catch (Exception e2e2) {
                            }
                        }

                        if (!encrypted) {

                            PDDocument finalPdDocument = pdDocument;

                            try {
                                service.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            pdfToBitmap(in, finalPdDocument, pd, Utils.getFileNameFromUri(uris.get(finalI), MainActivity.this));
                                        } catch (IOException e) {

                                        }
                                    }
                                });
                            } catch (Exception e) {
                            }

                        } else {


                            int finalI1 = i;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    String name = Utils.getFileNameFromUri(uris.get(finalI), MainActivity.this);

                                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                                    builder.setTitle(name);
                                    builder.setMessage("This document is password protected.");
                                    TextInputEditText input = new TextInputEditText(MainActivity.this);
                                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                                    input.setHint("Password");
                                    builder.setView(input);

                                    input.requestFocus();

                                    InputMethodManager imm1 = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                    imm1.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);


                                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int few) {


                                            if (pd.isShowing()) {

                                                try {

                                                    pd.dismiss();
                                                } catch (Exception e) {
                                                }
                                            }

                                        }
                                    });

                                    builder.setPositiveButton("Import", (dialog, which) -> {
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);


                                        try {


                                            boolean enc = false;


                                            Log.d("aaaaa", "check " + enc);

                                            InputStream in2 = getContentResolver().openInputStream(uris.get(finalI1));
                                            PDDocument pdDocument2 = null;

                                            try {

                                                pdDocument2 = PDDocument.load(in2, input.getText().toString());

//                                                if (pdDocument2.isEncrypted()) {
//                                                    enc = true;
//                                                    try {
//                                                        pdDocument2.close();
//                                                    } catch (Exception rwer) {
//                                                    }
//
//                                                }
                                            } catch (InvalidPasswordException e2) {
                                                enc = true;
                                            }


                                            if (enc) {


                                                if (input.getParent() != null) {
                                                    ((ViewGroup) input.getParent()).removeView(input);
                                                }

                                                builder.show();


                                                Utils.vibrate(MainActivity.this, 30);
                                                simpleToast("Wrong password.");


                                            } else {

                                                PDDocument finalPdDocument = pdDocument2;

                                                try {
                                                    service.execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                pdfToBitmap(in2, finalPdDocument, pd, Utils.getFileNameFromUri(uris.get(finalI), MainActivity.this));
                                                            } catch (IOException e) {

                                                            }
                                                        }
                                                    });
                                                } catch (Exception e) {
                                                }


                                            }
                                        } catch (Exception e) {

                                            Log.d("aaaaaaaaaaa", e.getMessage());
                                        }


                                    });


                                    builder.show();

                                }
                            });
                        }


                    } catch (IOException e) {

                    }
                    //
                }


            }
        });


    }

    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case R.id.backArrow:
                onClickBack();
                break;
            case R.id.middle_options_rl:

                showMiddleOptions(view);


                break;

            case R.id.more_options_rl:
                showMoreOptions(view);
                break;

            case R.id.cameraCapture:


                startScanning();


                break;

            case R.id.addPhotos:


                Intent intent2 = new Intent(MainActivity.this, CameraScanActivity.class);


                intent2.putExtra(CameraScanActivity.CURRENT_DIR_KEY, currentPath);
                intent2.putExtra(CameraScanActivity.INSERT_AT, 0);
                intent2.putExtra(CameraScanActivity.IMPORT_IMAGES, true);
                intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                MainActivity.this.startActivity(intent2);


                break;


            case R.id.emptyStartScanning:


                startScanning();


                break;

        }
    }

    private void startScanning() {

        binding.imageOptionsRL.setTransitionName("reveal");


        Intent intent = new Intent(MainActivity.this, CameraScanActivity.class);


        intent.putExtra(CameraScanActivity.CURRENT_DIR_KEY, currentPath);
        intent.putExtra(CameraScanActivity.INSERT_AT, 0);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        MainActivity.this.startActivity(intent);
    }


    @Override
    public void onBackPressed() {

        if (selectedFilesNos > 0) {
            deselectAll();
            return;
        }

        if (!binding.searchView.isIconified()) {
            binding.searchView.setIconified(true);
            return;
        }

        if (!currentPath.equals(baseDirPath)) {
            nagivateBack();

            return;
        }


        super.onBackPressed();

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == REQUEST_CODE_IMPORT_LOCAL_PDF
                && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {

                importPdf(resultData);

            }
        } else if (requestCode == EXPORT_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                processExport(uri);

            }
        } else if (requestCode == EXPORT_BACKUP_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                processExportLocalBackup(uri);

            }
        } else if (requestCode == IMPORT_BACKUP_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                processImportLocalBackup(uri);

            }
        } else if (requestCode == IN_APP_UPDATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
        }

    }


    @Override
    protected void onPause() {
        super.onPause();

        isPaused = true;
    }

    private void processExport(Uri uri) {


    }


    public interface OnDialogConfirm {

        void onConfirm();
    }
}