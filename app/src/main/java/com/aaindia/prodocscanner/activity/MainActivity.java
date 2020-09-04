package com.aaindia.prodocscanner.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.adapters.ListFilesAdapter;
import com.aaindia.prodocscanner.behaviours.PDFCreator;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.databinding.ActivityMainBinding;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.Clipboard;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.ListFIlesInfo;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

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


    public static final String _ALL = " All";
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

    public static final int MENU_ITEM_ID_EXPORT = 10;

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

    public ActivityMainBinding binding;
    public ListFilesAdapter adapter;

    public ArrayList<ListFIlesInfo> fIlesInfos = null;

    public String currentPath = null;
    public String baseDirPath = null;

    public Clipboard clipboard = null;

    public int selectedFilesNos = 0;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {

                System.loadLibrary("native-lib");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });


            } else {
                super.onManagerConnected(status);
            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {

            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }


        File[] f = new File(currentPath).listFiles();


        if (f.length != adapter.data.size()) {

            nagivateTo(currentPath);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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
        binding.scanList.setLayoutManager(new GridLayoutManager(this, 3));
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

        searchHandle(binding.searchView);


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


        setSpannableDrawable(R.drawable.baseline_create_new_folder_black_24, newFolder);
        setSpannableDrawable(R.drawable.baseline_content_paste_black_24, paste);
        setSpannableDrawable(R.drawable.baseline_picture_as_pdf_white_24, importPdf);
        setSpannableDrawable(R.drawable.baseline_cloud_black_24, syncSettings);
        setSpannableDrawable(R.drawable.baseline_rate_review_black_24, rateApp);
        setSpannableDrawable(R.drawable.baseline_share_black_24, shareApp);

        setSpannableDrawable(R.drawable.baseline_archive_white_24, importBackup);
        setSpannableDrawable(R.drawable.baseline_backup_white_24, exportBackup);


        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_CREATE_NEW_FOLDER, 0, newFolder);

        if (clipboard != null && clipboard.filepaths != null)
            menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_PASTE, 0, paste);


        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_IMPORT_PDF, 0, importPdf);

        //TODO
        // menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_SYNC_SETTINGS, 0, syncSettings);


        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_IMPORT_BACKUP, 0, importBackup);
        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_EXPORT_BACKUP, 0, exportBackup);


        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_RATE_APP, 0, rateApp);
        menu.add(0, MENU_ITEM_ID_MORE_OPTIONS_SHARE_APP, 0, shareApp);


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

                    case MENU_ITEM_ID_MORE_OPTIONS_SHARE_APP:
                        shareThisApp();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_IMPORT_BACKUP:

                        importLocalBackup();
                        break;

                    case MENU_ITEM_ID_MORE_OPTIONS_EXPORT_BACKUP:

                        exportLocalBackup();
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
        pd.setMessage("It may take a while");
        pd.setCancelable(false);
        pd.show();


        new Thread(new Runnable() {
            @Override
            public void run() {


                try (InputStream fin = getContentResolver().openInputStream(uri);) {


                    FileNav.unZipAll(fin, FileNav.getBaseDir(getApplicationContext()));


                } catch (FileNotFoundException e) {

                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();

                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();

                        nagivateTo(currentPath);
                    }
                });

            }
        }).start();


    }


    private void processExportLocalBackup(Uri uri) {


        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setTitle("Creating Backup");
        pd.setMessage("It may take a while");
        pd.setCancelable(false);
        pd.show();

        new Thread(new Runnable() {
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
                        pd.dismiss();
                    }
                });

            }
        }).start();


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

        intent.setType("application/pdf");

        startActivityForResult(intent, REQUEST_CODE_IMPORT_LOCAL_PDF);

    }


    private void pasteHere() {


        if (clipboard.filepaths != null && clipboard.filepaths.size() != 0) {


            ProgressDialog pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("");
            pd.setCancelable(false);
            pd.show();
            new Thread(() -> {


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
                            FileUtils.deleteDirectory(src);
                            clipboard.filepaths.remove(i);
                            i--;


                        }


                    } catch (IOException e) {

                    }


                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        pd.dismiss();
                        nagivateTo(currentPath);

                    }
                });


            }).start();


        } else {
            simpleToast("Nothing to paste");
        }
    }

    private void newDir() {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Folder name");

// Set up the input
        final EditText input = new EditText(this);
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

        new Thread(new Runnable() {
            @Override
            public void run() {

                fIlesInfos = FileNav.listDirRec(new File(currentPath));
                adapter.data = fIlesInfos;
                sortScans();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        pd.dismiss();
                    }
                });

            }
        }).start();


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

        new Thread(new Runnable() {
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

                            if (!currentPath.equals(baseDirPath))
                                binding.emptyDocumentTV.setText("Folder is empty.");
                            else {
                                binding.emptyDocumentTV.setText("It's empty here. Start scanning.");
                            }
                        }


                        toogleBackArrow(!path.equals(baseDirPath));

                        binding.scanList.getAdapter().notifyDataSetChanged();

                    }
                });

            }
        }).start();


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


                Intent intent = new Intent(MainActivity.this, ScanPreviewActivity.class);
                intent.putExtra(ScanPreviewActivity.SCAN_DIR_PATH, adapter.data.get(position).filepath);
                intent.putExtra(GlobalConstants.CLASS_NAME, MainActivity.CLASS_NAME);

                startActivity(intent);
            }

        }


    }

    @Override
    public void onLongPress(int position, View view) {

        showItemMoreOptions(view, position);

        selectDeselect(position);
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
                SpannableString export = new SpannableString(ITEM_OPTIONS_EXPORT);
                menu.add(0, MENU_ITEM_ID_EXPORT, 0, export);

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


                //  menu.add(0, MENU_ITEM_ID_SHARE, 0, share);

                SubMenu sMenu = menu.addSubMenu(share);
                sMenu.add(0, MENU_ITEM_ID_SHARE_HIGH_RES, 0, "Original");
                sMenu.add(0, MENU_ITEM_ID_SHARE_MED_RES, 0, "Medium Resolution");
                sMenu.add(0, MENU_ITEM_ID_SHARE_LOW_RES, 0, "Low Resolution");

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

                    case MENU_ITEM_ID_SHARE_HIGH_RES:

                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }
                        sharePDFChooser(PDFCreator.QUALITY_FULL);
                        break;


                    case MENU_ITEM_ID_SHARE_MED_RES:

                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }
                        sharePDFChooser(PDFCreator.QUALITY_MEDIUM);
                        break;


                    case MENU_ITEM_ID_SHARE_LOW_RES:

                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }
                        sharePDFChooser(PDFCreator.QUALITY_LOW);
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

                    case MENU_ITEM_ID_EXPORT:
                        if (!adapter.getItem(position).isSelected) {

                            selectDeselect(position);
                        }

                        export(position);

                        break;
                }


                return false;
            }
        });

        popupMenu.show();


    }

    private void export(int position) {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, adapter.data.get(position).filename + ".pdf");
        intent.setType("application/pdf");

        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }

    private void sharePDFChooser(int quality) {

        sharePDFs(quality, null);
    }


    private void sharePDFs(int quality, Uri saveURI) {


        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Preparing");

        pd.setCancelable(false);
        pd.show();


        new Thread(new Runnable() {
            @Override
            public void run() {


                File outputDir = null;

                ArrayList<Uri> uris = new ArrayList<>();

                for (ListFIlesInfo fIlesInfo : adapter.data) {

                    if (fIlesInfo.isSelected) {

                        outputDir = FileNav.getOutputDir(fIlesInfo.filepath, quality);

                        outputDir.mkdirs();

                        if (outputDir.listFiles().length > 0) {


                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                                uris.add(FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", outputDir.listFiles()[0]));


                            } else {
                                uris.add(Uri.parse(outputDir.listFiles()[0].getAbsolutePath()));


                            }

                        } else {


                            String pdfName = FileNav.getPDFName(fIlesInfo.filepath);
                            String outputPath = outputDir.getAbsolutePath() + File.separator + pdfName;

                            Utils.copyNotProcessedOriginals(new SavedImageDetails(FileNav.getEffectsFile(fIlesInfo.filepath)), fIlesInfo.filepath);


                            PDFCreator pdfCreator = new PDFCreator(MainActivity.this, fIlesInfo.filepath, outputPath);
                            File finalOutputDir1 = outputDir;
                            pdfCreator.setOnCompleteListener(new PDFCreator.OnCompleteListener() {
                                @Override
                                public void onComplete(int error) {

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                                        uris.add(FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", finalOutputDir1.listFiles()[0]));


                                    } else {
                                        uris.add(Uri.parse(finalOutputDir1.listFiles()[0].getAbsolutePath()));


                                    }
                                }

                                @Override
                                public void onProgress(int page, int total) {

                                }
                            });


                            pdfCreator.setQuality(quality);
                            pdfCreator.create(false);

                        }
                    }
                }


                File finalOutputDir = outputDir;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deselectAll();


                        if (saveURI != null && finalOutputDir != null) {


                            try {
                                OutputStream outputStream = getContentResolver().openOutputStream(uris.get(0));

                                InputStream inputStream = new FileInputStream(finalOutputDir.listFiles()[0]);


                                try {
                                    IOUtils.copy(inputStream, outputStream);
                                } catch (Exception e) {
                                }


                                if (inputStream != null)
                                    inputStream.close();
                                if (outputStream != null)
                                    outputStream.close();


                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            pd.dismiss();
                            return;
                        }


                        if (uris.size() == 1) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            //      intent.setData(uris.get(0));
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_TEXT, "Scanned using ProDoc Scanner");

                            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));


                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            }


                            startActivity(Intent.createChooser(intent, "Share"));


                        } else if (uris.size() > 1) {


                            Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);

                            share.setType("application/pdf");
                            share.putExtra(Intent.EXTRA_TEXT, "Scanned using ProDoc Scanner");
                            share.putExtra(Intent.EXTRA_SUBJECT, "Scans from Prodoc Scanner");

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            }

                            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                            startActivity(Intent.createChooser(share, "Share"));


                        }


                        pd.dismiss();
                    }
                });

            }
        }).start();


    }

    private void copyToClipBoard(boolean deleteAfter) {

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename");

        String fromNameAtomic = adapter.getItem(position).filename;
        final EditText input = new EditText(this);

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Are you sure you want to delete " + selectedFilesNos + " items ?");


        builder.setPositiveButton("Delete", (dialog, which) -> {

            selectedFilesNos = 0;

            for (int i = 0; i < adapter.data.size(); i++) {


                if (adapter.data.get(i).isSelected) {


                    try {
                        FileUtils.deleteDirectory(new File(adapter.data.get(i).filepath));
                    } catch (IOException e) {

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete?");


        builder.setPositiveButton("Delete", (dialog, which) -> {

            try {
                FileUtils.deleteDirectory(new File(filepath));
                nagivateTo(currentPath);
                simpleToast("Deleted");
                deselectAll();
            } catch (IOException e) {

            }


        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();


    }


    private void simpleToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }


    private void pdfToBitmapSave(File pdfFile, ProgressDialog pd) {


        try {

            File scanDir = FileNav.newScanDir(currentPath);

            File originalImageDir = FileNav.originalScanDirFromScanDir(scanDir);


            SavedImageDetails imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDir.getAbsoluteFile()));


            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

            Bitmap bitmap;

            final int pageCount = renderer.getPageCount();

            for (int i = 0; i < pageCount; i++) {


                int finalI = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.setMessage("Importing " + (finalI + 1) + "/" + pageCount);
                    }
                });

                PdfRenderer.Page page = renderer.openPage(i);

                int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                String filename = i + ".jpg";
                String filepath = originalImageDir.getAbsolutePath() + File.separator + filename;

                try (FileOutputStream out = new FileOutputStream(filepath)) {

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);

                    imageDetails.getOrdering().add(filename);

                    out.close();

                } catch (IOException e) {

                }

                imageDetails.sync();

                page.close();


            }

            renderer.close();
        } catch (Exception ex) {

        }

    }

    private void processImportLocalPdf(Uri uri) {


        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Importing PDF...");
        pd.setCancelable(false);
        pd.show();


        new Thread(new Runnable() {
            @Override
            public void run() {


                try {

                    File temp = FileNav.getTempFile(getApplicationContext(), "temp1.pdf");


                    InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream out = new FileOutputStream(temp);


                    try {
                        IOUtils.copy(in, out);
                    } catch (Exception e) {
                    }


                    try {
                        out.close();
                    } catch (Exception e) {

                    }
                    try {
                        in.close();
                    } catch (Exception e) {

                    }


                    pdfToBitmapSave(temp, pd);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            simpleToast("Imported");
                            nagivateTo(currentPath);
                        }
                    });


                } catch (Exception e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            simpleToast("Could not import");
                        }
                    });

                }


            }
        }).start();


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


                Intent intent = new Intent(MainActivity.this, CameraScanActivity.class);


                intent.putExtra(CameraScanActivity.CURRENT_DIR_KEY, currentPath);
                intent.putExtra(CameraScanActivity.INSERT_AT, 0);

                MainActivity.this.startActivity(intent);


                break;

            case R.id.addPhotos:


                Intent intent2 = new Intent(MainActivity.this, CameraScanActivity.class);


                intent2.putExtra(CameraScanActivity.CURRENT_DIR_KEY, currentPath);
                intent2.putExtra(CameraScanActivity.INSERT_AT, 0);
                intent2.putExtra(CameraScanActivity.IMPORT_IMAGES, true);

                MainActivity.this.startActivity(intent2);


                break;


        }
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
                uri = resultData.getData();
                processImportLocalPdf(uri);

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
        }

    }

    private void processExport(Uri uri) {

        sharePDFs(PDFCreator.QUALITY_FULL, uri);
    }


    public interface OnDialogConfirm {

        void onConfirm();
    }
}