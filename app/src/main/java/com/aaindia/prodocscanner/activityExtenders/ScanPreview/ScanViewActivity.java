package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.databinding.ActivityScanViewBinding;
import com.aaindia.prodocscanner.ocr.OcrActivity;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.views.TouchableReyclerView;
import com.aaindia.prodocscanner.wrappers.Interfaces;
import com.aaindia.prodocscanner.wrappers.MyLinearLayoutManager;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.slider.Slider;
import com.google.gson.internal.$Gson$Preconditions;
import com.jsibbold.zoomage.ZoomageView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

public class ScanViewActivity extends AppCompatActivity implements View.OnClickListener, ScanPreviewAdapter.AdapterInterface{

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


    public boolean documentChanged() {
        return isChange;
    }

    public void setDocumentChanged(boolean isChange) {
        this.isChange = isChange;
    }

    public File getOriginalDirFile() {
        return originalDirFile;
    }


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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_view);

        scanDirPath = (String) getIntent().getExtras().get(ScanPreviewActivity.SCAN_DIR_PATH);
        activityCalledFromClassname = (String) getIntent().getExtras().get(GlobalConstants.CLASS_NAME);


        adapter = new ScanPreviewAdapter(this, scanDirPath, getRecyclerView(), this);


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
        return sheetBehavior;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED || sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || sheetBehavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {

                Rect outRect = new Rect();
                binding.bottomSheet.getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY()))
                    sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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


        new Thread(new Runnable() {
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

                        linkedList.add(f.getName());
                    }

                }

                imageDetails.sync();


                listIterator = linkedList.listIterator();


                for (int i = 0; i < linkedList.size(); i++) {


                    int finalI = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.setMessage("Detecting document edges " + (finalI + 1) + "/" + linkedList.size());
                        }
                    });


                    String filename = linkedList.get(i);

                    String path = originalDirFile.getAbsolutePath() + File.separator + filename;

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
                        adapter.originalFilepaths = getOriginalFilepaths();
                        adapter.savedImageDetails = imageDetails;

                        binding.fileNameTV.setText(FileNav.getPDFName(getScanDirPath()).replace(".pdf", ""));

                        getBinding().recyclerView.getAdapter().notifyDataSetChanged();


                    }
                });


            }
        }).start();


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

    public void addPages(ScanPreviewAdapter.ViewHolder holder, int position) {
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

        processImage(holder, postion, false);


    }

    @Override
    public void notProcessed(int postion, ScanPreviewAdapter.ViewHolder holder) {


        getBinding().protector.setVisibility(View.VISIBLE);

        if (!autoCropped.contains(getImageDetails().getAt(postion))) {


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

            addPages(holder, position);
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
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else if (id == R.id.OcrRL) {

            if (position < 0)
                return;

            if (holder == null || holder.processing == null)
                return;

            getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);
            ocrPage(holder, position);

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


        if (showHideColorRLIsAnimating) {


            binding.colorControlRL.setVisibility(show ? View.VISIBLE : View.GONE);


            return;
        }

        binding.colorControlRL.clearAnimation();


        showHideColorRLIsAnimating = true;

        if (show && binding.colorControlRL.getVisibility() == View.GONE) {


            binding.colorControlRL.setVisibility(View.VISIBLE);
            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(200);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    binding.colorControlRL.setAlpha((Float) valueAnimator.getAnimatedValue());

                    if ((float) valueAnimator.getAnimatedValue() == 1)
                        showHideColorRLIsAnimating = false;
                }
            });

            animator.start();


        } else if (!show && binding.colorControlRL.getVisibility() == View.VISIBLE) {

            ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(200);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    binding.colorControlRL.setAlpha((Float) valueAnimator.getAnimatedValue());

                    if ((Float) valueAnimator.getAnimatedValue() == 0) {
                        binding.colorControlRL.setVisibility(View.GONE);
                        showHideColorRLIsAnimating = false;
                    }
                }
            });

            animator.start();
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
