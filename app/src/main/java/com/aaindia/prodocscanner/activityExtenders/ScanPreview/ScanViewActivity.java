package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aaindia.prodocscanner.App;
import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.databinding.ActivityScanViewBinding;
import com.aaindia.prodocscanner.ocr.OcrActivity;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.views.TouchableReyclerView;
import com.aaindia.prodocscanner.wrappers.CompleteEffectHolder;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.Interfaces;
import com.aaindia.prodocscanner.wrappers.MyLinearLayoutManager;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.jsibbold.zoomage.ZoomageView;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;

public class ScanViewActivity extends AppCompatActivity implements View.OnClickListener, ScanPreviewAdapter.AdapterInterface {

    public static final String NEW_SCAN = "new_scan";

    public static final String SCAN_DIR_PATH = "scan_dir_path";


    ActivityScanViewBinding binding;

    private String scanDirPath = null;
    private String activityCalledFromClassname = null;
    private ArrayList<String> originalFilepaths = null;
    private ScanPreviewAdapter adapter;


    private SavedImageDetails imageDetails;
    public boolean colorOnly = false;
    private File originalDirFile;
    private File processedDirFile;


    private HashSet<String> autoCropped = new HashSet<>();

    private boolean isChange = true;

    private boolean colorTuneListen = true;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;

    public void setColorTuneListen(boolean b) {
        colorTuneListen = b;
    }


    public void setDocumentChanged(boolean isChange) {
        this.isChange = isChange;
    }


    public boolean isProcessing = false;
    public int wd = -1;
    public int ht = -1;

    private int colorTuneLastProgress = -1;

    private String outputPathPDFExport = null;

    public String getPDFOutputPathExport() {

        return outputPathPDFExport;
    }

    public void setPDFOutputPathExport(String path) {

        this.outputPathPDFExport = path;
    }


    public static final int PERMISION_REQUEST_CODE_SINGLE_PAGE = 2910;
    public static final int PERMISION_REQUEST_CODE_SELECTED = 2911;
    public static final int PERMISION_REQUEST_CODE_ALL = 2912;


    public ExecutorService executorService2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executorService2 = Executors.newFixedThreadPool(3);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_view);

        scanDirPath = (String) getIntent().getExtras().get(ScanPreviewActivity.SCAN_DIR_PATH);
        activityCalledFromClassname = (String) getIntent().getExtras().get(GlobalConstants.CLASS_NAME);


        adapter = new ScanPreviewAdapter(this, scanDirPath, getRecyclerView(), this, new ScanPreviewAdapter.DataProvider() {
            @Override
            public ArrayList<String> filepathProvider() {
                return getOriginalFilepaths();
            }

            @Override
            public SavedImageDetails imageDetailsProvider() {
                return getImageDetails();
            }
        });


        loadInitialData();

        binding.recyclerView.setLayoutManager(new MyLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));


        SnapHelper helper = new PagerSnapHelper();
        helper.attachToRecyclerView(binding.recyclerView);

        binding.recyclerView.setItemViewCacheSize(1);

        binding.recyclerView.setAdapter(adapter);


        binding.cropRL.setOnClickListener(this::onClick);
        binding.colorRL.setOnClickListener(this::onClick);
        binding.rotateRL.setOnClickListener(this::onClick);


        binding.shareSinglePageRL.setOnClickListener(this::onClick);

        binding.deleteRL.setOnClickListener(this);
        binding.addPagesRL.setOnClickListener(this);
        binding.saveRL.setOnClickListener(this::onClick);
        binding.shareRL.setOnClickListener(this::onClick);


        binding.renameRL.setOnClickListener(this);

        binding.viewRL.setOnClickListener(this::onClick);
        binding.moreRL.setOnClickListener(this);
        binding.noCropRL.setOnClickListener(this::onClick);
        binding.exportSinglePageRL.setOnClickListener(this::onClick);
        binding.OcrRL.setOnClickListener(this::onClick);
        binding.retakeRL.setOnClickListener(this::onClick);
        binding.copySingleRL.setOnClickListener(this::onClick);
        binding.pasteRL.setOnClickListener(this::onClick);


        binding.colorTuneSK.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {


                int progress = (int) slider.getValue();

                if (colorTuneLastProgress == progress) {

                    return;
                }

                int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);


                colorTuneChanged(holder, position, progress);


            }
        });


        binding.colorGrayCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


                if (!colorTuneListen)
                    return;

                if (!compoundButton.isPressed())
                    return;


                int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);

                colorGrayChanged(holder, position, b);


            }
        });


        bottomsheetBehaviour();

    }

    public BottomSheetBehavior getBottomMenu1() {
        binding.pasteSingleRLSeperator.setVisibility(App.CLIPBOARD.isEmpty() ? View.GONE : View.VISIBLE);
        binding.pasteRL.setVisibility(App.CLIPBOARD.isEmpty() ? View.GONE : View.VISIBLE);
        return sheetBehavior;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED || sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || sheetBehavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {

                Rect outRect = new Rect();
                binding.bottomSheet.getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY()))
                    getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }


        return super.dispatchTouchEvent(event);
    }


    private void bottomsheetBehaviour() {


        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);


        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        loadInitialData();
        binding.recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();


        getBinding().recyclerView.getAdapter().notifyDataSetChanged();
    }


    public HashSet<String> getAutoCroppedSet() {
        return autoCropped;
    }

    public void loadInitialData() {

        originalDirFile = new File(scanDirPath, FileNav.ORIGINAL_IMAGE_DIR);
        processedDirFile = new File(scanDirPath, FileNav.PROCESSED_IMAGE_DIR);

        onScanDirPathChange(originalDirFile.getAbsolutePath());

        originalDirFile.mkdirs();
        processedDirFile.mkdirs();


        originalFilepaths = new ArrayList<>();
        imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDirPath));


        // check auto crop

        ProgressDialog pd = new ProgressDialog(ScanViewActivity.this);
        pd.setTitle("Just a moment");
        pd.setMessage("Detecting document edges");
        pd.setCancelable(false);


        executorService2.execute(new Runnable() {
            @Override
            public void run() {

                LinkedList<String> linkedList = imageDetails.getOrdering();


                File[] originalFiles = originalDirFile.listFiles();


                ListIterator<String> listIterator = linkedList.listIterator();

                HashSet<String> set = new HashSet<>();

                while (listIterator.hasNext()) {

                    set.add(listIterator.next());
                }

                for (File f : originalFiles) {

                    if (!set.contains(f.getName())) {

                        if (f.length()>0)
                        linkedList.add(f.getName());
                        else
                            FileUtils.deleteQuietly(f);
                    }

                }

                imageDetails.sync();


                listIterator = linkedList.listIterator();

                int count = 0;

                File currentFile;

                while ((listIterator.hasNext())) {


                    int finalI = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.setMessage("Detecting document edges " + (finalI + 1) + "/" + linkedList.size());
                        }
                    });


                    String filename = listIterator.next();

                    String path = originalDirFile.getAbsolutePath() + File.separator + filename;

                    currentFile = new File(path);

                    if ( currentFile.length() <= 0) {
                        linkedList.remove(path);
                        FileUtils.deleteQuietly(currentFile);
                        continue;
                    }

                    originalFilepaths.add(path);


                    if (imageDetails.getEffects(filename) == null) {


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                if (!pd.isShowing())
                                    pd.show();
                            }
                        });

                        autocropThis(path, imageDetails);
                    }

                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        imageDetails.sync();

                        if (pd.isShowing()) {
                            try {
                                pd.dismiss();
                            } catch (Exception e) {

                            }

                        }
                        adapter.scanDirName = getScanDirPath();
//                        adapter.originalFilepaths = getOriginalFilepaths();
//                        adapter.savedImageDetails = imageDetails;

                        binding.fileNameTV.setText(FileNav.getPDFName(getScanDirPath()).replace(".pdf", ""));

                        getBinding().recyclerView.getAdapter().notifyDataSetChanged();


                    }
                });


            }
        });


    }

    public synchronized void autocropThis(String path, SavedImageDetails imageDetails) {
    }


    public boolean getColorOnly() {
        return colorOnly;
    }

    public void setColorOnly(boolean colorOnly) {
        this.colorOnly = colorOnly;
    }

    public TouchableReyclerView getRecyclerView() {
        return binding.recyclerView;
    }

    public ActivityScanViewBinding getBinding() {
        return binding;
    }

    public ArrayList<String> getOriginalFilepaths() {
        return originalFilepaths;
    }

    public void setOriginalFilepaths(ArrayList<String> filepaths) {
        this.originalFilepaths = filepaths;
    }

    public SavedImageDetails getImageDetails() {
        return imageDetails;
    }


    public String getScanDirPath() {
        return scanDirPath;
    }

    public void setScanDirPath(String path) {
        scanDirPath = path;
    }

    public void recyclerViewActivateDeact(boolean activate) {

        binding.recyclerView.setItemTouchable(activate);


    }


    public void processImage(ScanPreviewAdapter.ViewHolder holder, int position, boolean colorOnly) {
    }

    public void chooseColor(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void crop(ScanPreviewAdapter.ViewHolder holder, int position, boolean noCrop) {
    }

    public void rotate(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void shareSinglePage(ScanPreviewAdapter.ViewHolder holder, int position, boolean isExport) {


    }


    public void addPages(ScanPreviewAdapter.ViewHolder holder, int position, boolean retake) {
    }

    public void deleteFile(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void save() {
    }

    public void share(boolean isExport) {


    }


    public void rename() {

    }

    public void viewPDF() {


    }

    public void colorTuneChanged(ScanPreviewAdapter.ViewHolder holder, int position, int progress) {

    }

    public void colorGrayChanged(ScanPreviewAdapter.ViewHolder holder, int position, boolean b) {

    }


    public void setSpanActionColor(SpannableString s, int compare1, int compare2) {

        if (compare1 == compare2)
            s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSecondary)), 0, s.length(), 0);
    }

    public void shareFromArray(ArrayList<File> files, double size, boolean isExport) {
    }


    @Override
    public void process(int postion, ScanPreviewAdapter.ViewHolder holder) {

        if (isProcessing)
            return;


        isProcessing = true;
        processImage(holder, postion, false);


    }

    @Override
    public void notProcessed(int postion, ScanPreviewAdapter.ViewHolder holder) {


        getBinding().protector.setVisibility(View.VISIBLE);


        if (!autoCropped.contains(getImageDetails().getAt(postion))) {


            holder.isBusy = true;
            if (wd == -1) {


                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                crop(holder, postion, false);
                            }
                        });

                    }
                }, 350);

            } else {


                crop(holder, postion, false);
            }


        } else {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    holder.imageView.setAlpha(1);
                }
            });
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            getBinding().protector.setVisibility(View.GONE);

                            if (holder != null && holder.itemView != null && holder.imageViewParent != null && holder.nextAction != null) {


                                holder.nextAction.setVisibility(View.GONE);
                                getRecyclerView().getAdapter().notifyDataSetChanged();
                            }


                        }
                    });
                }
            }, 1000);
        }


    }

    @Override
    public void processExit(int position, ScanPreviewAdapter.ViewHolder holder) {

        zoomageEnableDisable(holder, true);
    }


    @Override
    public void onClick(View view) {

        if (isProcessing)
            return;


        int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findFirstVisibleItemPosition();


        ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);


        int id = view.getId();


        if (id != R.id.colorRL) {
            showHideColorRL(false);
        }

        if (id == R.id.cropRL) {

            if (position < 0)
                return;
            if (holder == null || holder.processing == null)
                return;

            crop(holder, position, false);
        } else if (id == R.id.colorRL) {
            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            chooseColor(holder, position);
        } else if (id == R.id.rotateRL) {
            if (position < 0)
                return;
            if (holder == null || holder.processing == null)
                return;

            rotate(holder, position);
        } else if (id == R.id.shareSinglePageRL) {
            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            shareSinglePage(holder, position, false);
        } else if (id == R.id.exportSinglePageRL) {

            shareSinglePage(holder, position, true);


        } else if (id == R.id.addPagesRL) {
            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            addPages(holder, position, false);
        } else if (id == R.id.deleteRL) {
            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            deleteFile(holder, position);
        } else if (id == R.id.shareRL) {
            share(false);
        } else if (id == R.id.saveRL) {
            save();
        } else if (id == R.id.exitRL) {

            onBackPressed();
        } else if (id == R.id.renameRL) {
            rename();
        } else if (id == R.id.viewRL) {

            viewPDF();
        } else if (id == R.id.noCropRL) {
            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            noCrop(holder, position);
        } else if (id == R.id.moreRL) {

            if (sheetBehavior != null && sheetBehavior.getState() == sheetBehavior.STATE_HIDDEN)
                getBottomMenu1().setState(BottomSheetBehavior.STATE_EXPANDED);
        } else if (id == R.id.OcrRL) {

            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);
            ocrPage(holder, position);

        } else if (id == R.id.retakeRL) {


            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            addPages(holder, position, true);

        } else if (id == R.id.copySingleRL) {

            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            copySinglePage(holder, position);

        } else if (id == R.id.pasteRL) {

            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            getBottomMenu1().setState(getBottomMenu1().STATE_HIDDEN);

            paste(position);


        }

    }

    private void paste(int pos) {


        ProgressDialog pd1 = new ProgressDialog(ScanViewActivity.this);
        pd1.setTitle("Pasting");
        pd1.setMessage("Page ");
        pd1.setCancelable(false);
        pd1.show();


        executorService2.execute(new Runnable() {
            @Override
            public void run() {


                int position = pos;
                position++;

                ListIterator iterator = App.CLIPBOARD.listIterator();


                String filename, originalFilePath, processedFilePath;


                while (iterator.hasNext()) {

                    int finalPosition = position;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pd1.isShowing()) {
                                pd1.setMessage("Page " + finalPosition + "/" + App.CLIPBOARD.size());
                            }
                        }
                    });

                    CompleteEffectHolder effectHolder = (CompleteEffectHolder) iterator.next();

                    filename = System.currentTimeMillis() + ".jpg";

                    originalFilePath = getScanDirPath() + File.separator + FileNav.ORIGINAL_IMAGE_DIR + File.separator + filename;

                    processedFilePath = getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + filename;


                    try {
                        FileUtils.copyFile(new File(effectHolder.originalPath), new File(originalFilePath));


                        try {
                            FileUtils.copyFile(new File(effectHolder.processedPath), new File(processedFilePath));
                        } catch (Exception e) {
                        }


                        Effects effects = new Effects(effectHolder.corners, effectHolder.color, effectHolder.isGray, effectHolder.rotation, effectHolder.colorTune);

                        getImageDetails().getOrdering().add(position, filename);
                        getImageDetails().putEffects(filename, effects);
                        position++;

                    } catch (Exception e) {

                    }

                }

                getImageDetails().sync();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        try {
                            pd1.dismiss();
                        } catch (Exception e) {
                        }
                        loadInitialData();

                        Toast.makeText(getApplicationContext(), "Pasted", Toast.LENGTH_SHORT).show();
                        Utils.vibrate(ScanViewActivity.this, 25);

                        Utils.vibrate(ScanViewActivity.this, 25);
                    }
                });

            }
        });


    }

    private void copySinglePage(ScanPreviewAdapter.ViewHolder holder, int position) {

        getBottomMenu1().setState(getBottomMenu1().STATE_HIDDEN);

        App.CLIPBOARD.clear();

        String processedPath = getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + getImageDetails().getAt(position);
        Effects effects = getImageDetails().getEffects(getImageDetails().getAt(position));
        CompleteEffectHolder effectHolder = new CompleteEffectHolder(effects.corners, effects.color, effects.isGray, effects.rotation, effects.colorTune,
                getOriginalFilepaths().get(position), processedPath, getScanDirPath(), getImageDetails().getAt(position));

        App.CLIPBOARD.add(effectHolder);

        Toast.makeText(ScanViewActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        Utils.vibrate(ScanViewActivity.this, 25);


        if (!Prefs.firstTimeSeenScreen(ScanViewActivity.this, "copy_single_page_doc")) {

            new GuideView.Builder(ScanViewActivity.this)
                    .setTitle("How to paste?")

                    .setContentSpan((Spannable) Html.fromHtml("At first, <b>navigate</b> to the desired <b>document</b> and then <b>tap</b> here to find <b>paste</b> option."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere)
                    .setTargetView(binding.moreRL)


                    .build()
                    .show();
        }
    }

    private void ocrPage(ScanPreviewAdapter.ViewHolder holder, int position) {


        String imageFilename = new File(getOriginalFilepaths().get(position)).getName();

        String bitmapPath = getOriginalFilepaths().get(position);


        File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), imageFilename);

        if (processedFile.exists()) {
            bitmapPath = processedFile.getAbsolutePath();
        }

        Intent intent = new Intent(ScanViewActivity.this, OcrActivity.class);

        ArrayList<String> bitmapPaths = new ArrayList<>();
        bitmapPaths.add(bitmapPath);

        Bundle args = new Bundle();
        args.putSerializable(OcrActivity.IMAGE_PATHS, (Serializable) bitmapPaths);
        intent.putExtra(OcrActivity.IMAGE_PATHS, args);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void noCrop(ScanPreviewAdapter.ViewHolder holder, int position) {

        crop(holder, position, true);
    }


    private boolean showHideColorRLIsAnimating = false;

    public synchronized void showHideColorRL(boolean show) {


        if (show) {

            binding.colorControlRL.setVisibility(View.VISIBLE);
        } else {
            binding.colorControlRL.setVisibility(View.GONE);
        }

//        if (showHideColorRLIsAnimating) {
//
//
//            binding.colorControlRL.setVisibility(show ? View.VISIBLE : View.GONE);
//
//
//            return;
//        }
//
//        binding.colorControlRL.clearAnimation();
//
//
//        showHideColorRLIsAnimating = true;
//
//        if (show && binding.colorControlRL.getVisibility() == View.GONE) {
//
//
//            binding.colorControlRL.setVisibility(View.VISIBLE);
//            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
//            animator.setInterpolator(new AccelerateDecelerateInterpolator());
//            animator.setDuration(200);
//            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                @Override
//                public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                    binding.colorControlRL.setAlpha((Float) valueAnimator.getAnimatedValue());
//
//                    if ((float) valueAnimator.getAnimatedValue() == 1)
//                        showHideColorRLIsAnimating = false;
//                }
//            });
//
//            animator.start();
//
//
//        } else if (!show && binding.colorControlRL.getVisibility() == View.VISIBLE) {
//
//            ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
//            animator.setInterpolator(new AccelerateDecelerateInterpolator());
//            animator.setDuration(200);
//            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                @Override
//                public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                    binding.colorControlRL.setAlpha((Float) valueAnimator.getAnimatedValue());
//
//                    if ((Float) valueAnimator.getAnimatedValue() == 0) {
//                        binding.colorControlRL.setVisibility(View.GONE);
//                        showHideColorRLIsAnimating = false;
//                    }
//                }
//            });
//
//            animator.start();
//        }


    }


    public void generateThumbnail() {
        try {

            executorService2.execute(new Runnable() {
                @Override
                public void run() {

                    try {

                        File originalFile = new File(getOriginalFilepaths().get(0));

                        File processedFile = new File(getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + getImageDetails().getAt(0));


                        File toCopy = processedFile.exists() ? processedFile : originalFile;

                        if (!toCopy.exists())
                            return;

                        Mat mat = Imgcodecs.imread(toCopy.getAbsolutePath());

                        int width = mat.width();
                        int height = mat.height();

                        float mp = (float) ((mat.width() / 1000.0) * (mat.height() / 1000.0));
                        if (mp > 2) {
                            float scale = 2.0f / mp;

                            Imgproc.resize(mat, mat, new Size(width * scale, height * scale), Imgproc.INTER_AREA);

                        }

                        Imgcodecs.imwrite(getScanDirPath() + File.separator + "thumbnail.jpg", mat);
                        mat.release();


                    } catch (Exception e) {
                    }


                }
            });


        } catch (Exception e) {
        }
    }

    public void zoomageEnableDisable(ScanPreviewAdapter.ViewHolder holder, boolean enable) {

        holder.imageView.reset(true);
        ((ZoomageView) holder.imageView).setZoomable(enable);
        holder.imageView.setDoubleTapToZoom(enable);
        holder.imageView.setTranslatable(enable);


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }


    public void onScanDirPathChange(String path) {

    }
}
