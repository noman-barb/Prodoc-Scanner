package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.App;
import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.GridScanViewAdapter;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.ocr.OcrActivity;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.CompleteEffectHolder;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.MyGridLayoytManager;
import com.aaindia.prodocscanner.wrappers.MyLinearLayoutManager;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

public class GridScanViewActivity extends EditScanViewActivity implements GridScanViewAdapter.ItemPressHelper {


    private GridScanViewAdapter adapter;

    private boolean selectActive = false;

    int width, height;
    float dpi;
    private boolean singlePageMode = false;
    private BottomSheetBehavior<RelativeLayout> bottomSheetBehaviour;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        binding.recyclerView.setVisibility(View.GONE);
        binding.gridRecyclerView.setVisibility(View.VISIBLE);


        binding.gridRecyclerView.setItemViewCacheSize(11);
        binding.gridRecyclerView.setHasFixedSize(true);


        adapter = new GridScanViewAdapter(this, getScanDirPath(), new GridScanViewAdapter.DataModel() {
            @Override
            public ArrayList<String> dataProvider() {

                return getOriginalFilepaths();
            }
        }, this);


        binding.gridRecyclerView.setLayoutManager(new MyGridLayoytManager(this, 2));
        getBinding().gridRecyclerView.setAdapter(adapter);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        dpi = displayMetrics.densityDpi / 160.0f;

        bottomSheetBehaviour = BottomSheetBehavior.from(binding.footerGrid);
        bottomSheetBehaviour.setState(BottomSheetBehavior.STATE_HIDDEN);

        binding.exitRL.setOnClickListener(this);
        binding.selectAll.setOnClickListener(this::onClick);
        binding.copySelectedRL.setOnClickListener(this::onClick);
        binding.deleteSelectedRL.setOnClickListener(this::onClick);
        binding.shareSelectedRL.setOnClickListener(this::onClick);
        binding.exportSelectedRL.setOnClickListener(this);
        binding.cameraCapture.setOnClickListener(this);
        binding.exportSelectedRL.setOnClickListener(this::onClick);
        binding.ocrSelectedRL.setOnClickListener(this::onClick);



        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {


                swap(recyclerView, viewHolder, target);



                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {


            }
        };


        binding.gridRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

               adapter.setScrolling(newState != RecyclerView.SCROLL_STATE_IDLE);


            }
        });



        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);


        itemTouchHelper.attachToRecyclerView(binding.gridRecyclerView);


        binding.selectMultipleRL.setOnClickListener(this);



        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().getBoolean(ScanPreviewActivity.NEW_SCAN, false)) {



            getFirebaseInstance().logEvent("screen_scan_view_new_scan", new Bundle());

            singlePageViewModeToggle(true, null, 0);


        } else {


            getFirebaseInstance().logEvent("screen_scan_view_new_scan", new Bundle());

            loadAndShowAd1();

            if (!adapter.firstTime) {

                adapter.firstTime = true;

                if (adapter.model.dataProvider().size() > 0)
                    adapter.notifyItemChanged(0);

            }
        }





    }

    private boolean swapped = false;

    private void swap(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        onLongPress((GridScanViewAdapter.ViewHolder) viewHolder, viewHolder.getAdapterPosition());

        swapped = true;


        int fromPostion = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

//        if (adapter.selectedItems.contains(fromPostion)) {
//            adapter.selectedItems.remove(fromPostion);
//            adapter.selectedItems.add(toPosition);
//        }


        if (toPosition < fromPostion) {
            getOriginalFilepaths().add(toPosition, getOriginalFilepaths().remove(fromPostion));
            getImageDetails().getOrdering().add(toPosition, getImageDetails().getOrdering().remove(fromPostion));
        } else {

            String s1 = getOriginalFilepaths().get(fromPostion);
            String s2 = getImageDetails().getOrdering().get(fromPostion);

            getOriginalFilepaths().add(toPosition + 1, s1);
            getImageDetails().getOrdering().add(toPosition + 1, s2);

            getOriginalFilepaths().remove(fromPostion);
            getImageDetails().getOrdering().remove(fromPostion);


        }


        recyclerView.getAdapter().notifyItemMoved(fromPostion, toPosition);


        for (int i = 0; i < getOriginalFilepaths().size(); i++) {

            GridScanViewAdapter.ViewHolder holder = (GridScanViewAdapter.ViewHolder) binding.gridRecyclerView.findViewHolderForAdapterPosition(i);


            if (holder != null) {

                holder.pageNumber.setText((i + 1) + "");
            }

        }

    }

    @Override
    public void onTap(GridScanViewAdapter.ViewHolder holder, int position) {


        if (selectActive) {


            if (adapter.selectedItems.contains(position)) {
                adapter.selectedItems.remove(position);


                holder.tint.setVisibility(View.GONE);


            } else {
                adapter.selectedItems.add(position);
                holder.tint.clearAnimation();


                holder.tint.setVisibility(View.VISIBLE);

            }


            binding.topbarRightSelectedTxtView.setText(Math.max(adapter.selectedItems.size(), 1) + " Selected");

            if (adapter.selectedItems.size() == 0) {
                selectActive = false;
                deselectAll();

            }
        } else {

            singlePageViewModeToggle(true, holder, position);

        }


    }


    @Override
    public void onProcessed(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.onProcessed(holder, position);

        adapter.notifyItemChanged(position);
    }


    private boolean singleModeFirstTime = true;

    public void singlePageViewModeToggle(boolean showSIngleMode, GridScanViewAdapter.ViewHolder holde, int position) {


        singlePageMode = showSIngleMode;


        int yTrans = 70;
        if (showSIngleMode) {

            getRecyclerView().getAdapter().notifyDataSetChanged();

            if (singleModeFirstTime) {
                singleModeFirstTime = !Prefs.firstTimeSeenScreen(GridScanViewActivity.this, "grid_scan_3");
            }


            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(300);
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.foote.setVisibility(View.VISIBLE);
            binding.recyclerView.scrollToPosition(position);
            binding.foote.setTranslationY(yTrans * dpi);

            binding.cameraCapture.setVisibility(View.VISIBLE);


            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {

                    float val = (float) valueAnimator.getAnimatedValue();


                    binding.gridRecyclerView.setAlpha(1 - val);
                    binding.recyclerView.setAlpha((float) Math.min(val + 0.2, 1));
                    binding.recyclerView.setScaleX(Math.min(0.4f + val, 1));
                    binding.recyclerView.setScaleY(Math.min(0.4f + val, 1));
                    // binding.foote.setAlpha(val);
                    binding.foote.setTranslationY(yTrans * dpi - yTrans * dpi * val);

                    binding.imageOptionsRL.setScaleX(1 - val);
                    binding.imageOptionsRL.setScaleY(1 - val);

                    if (val == 1) {

                        binding.foote.setTranslationY(0);
                        binding.recyclerView.setScaleX(1);
                        binding.recyclerView.setScaleY(1);
                        binding.recyclerView.setAlpha(val);
                        binding.imageOptionsRL.setScaleX(1);
                        binding.imageOptionsRL.setScaleY(1);
                        binding.gridRecyclerView.setVisibility(View.GONE);
                        binding.imageOptionsRL.setVisibility(View.GONE);


                        binding.gridRecyclerView.setAlpha(1);


                        if (singleModeFirstTime) {

                            singleModeFirstTime = false;

                            new GuideView.Builder(GridScanViewActivity.this)
                                    .setTitle("Edit options")
                                    .setContentSpan((Spannable) Html.fromHtml("<b>Add images</b>, <b>Crop</b>, <b>Apply Filter</b> and more.<br><b>Tap</b> on <b>More</b> for more options like <b>Delete</b> and <b>OCR</b>."))
                                    .setGravity(Gravity.auto) //optional
                                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                    .setTargetView(binding.foote)
                                    .setGuideListener(new GuideListener() {
                                        @Override
                                        public void onDismiss(View view) {


                                            int pos = ((MyLinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();

                                            if (pos < 0)
                                                return;


                                            ScanPreviewAdapter.ViewHolder hold = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(pos);

                                            if (hold == null)
                                                return;


                                            if (hold.nextAction.getVisibility() == View.GONE)
                                                return;


                                            if (binding.recyclerView.getVisibility() == View.VISIBLE) {

                                                new GuideView.Builder(GridScanViewActivity.this)
                                                        .setTitle("Crop Image")

                                                        .setGravity(Gravity.auto) //optional
                                                        .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                                        .setTargetView(hold.processImage)
                                                        .build().show();


                                            }
                                        }
                                    })
                                    .build().show();


                        }


                    }
                }
            });


            animator.start();
        } else {



            adapter.notifyDataSetChanged();

            if (!adapter.firstTime) {

                adapter.firstTime = true;

                if (adapter.model.dataProvider().size() > 0)
                    adapter.notifyItemChanged(0);

            }



            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(200);


            binding.imageOptionsRL.setVisibility(View.VISIBLE);

            binding.imageOptionsRL.setScaleX(1);
            binding.imageOptionsRL.setScaleY(1);

            binding.gridRecyclerView.setVisibility(View.VISIBLE);
            binding.foote.setTranslationY(0);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {

                    float val = (float) valueAnimator.getAnimatedValue();

                    binding.gridRecyclerView.setAlpha(val);
                    binding.recyclerView.setAlpha(1 - val);
                    binding.recyclerView.setScaleX(1 - val);
                    binding.recyclerView.setScaleY(1 - val);
                    binding.foote.setTranslationY(yTrans * dpi * val);
                    binding.imageOptionsRL.setScaleX(val);
                    binding.imageOptionsRL.setScaleY(val);
                    if (val == 1) {
                        binding.foote.setTranslationY(0);
                        binding.recyclerView.setScaleX(1);
                        binding.recyclerView.setScaleY(1);
                        binding.recyclerView.setAlpha(1);
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.foote.setVisibility(View.GONE);
                    }
                }
            });
            animator.start();
        }

    }


    private boolean firstTime = true;

    @Override
    public void onLongPress(GridScanViewAdapter.ViewHolder holder, int position) {







        if (firstTime) {

            firstTime = !Prefs.firstTimeSeenScreen(GridScanViewActivity.this, "grid_scan_view");
        }


        if (!selectActive) {


            if (firstTime) {

                firstTime = false;

                new GuideView.Builder(GridScanViewActivity.this)
                        .setTitle("Tip")
                        .setContentSpan((Spannable) Html.fromHtml("You can <b>change the order</b> of pages by <b>holding and moving</b>."))
                        .setGravity(Gravity.auto) //optional
                        .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                        .setTargetView(holder.itemView)
                        .build().show();


            }


            Utils.vibrate(this, 20);

            holder.tint.setVisibility(View.VISIBLE);
            selectActive = true;
            bottomSheetBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED);

            binding.topbarRightSelectedTxt.setVisibility(View.VISIBLE);
            binding.topbarRight.setVisibility(View.INVISIBLE);


            binding.imageOptionsRL.clearAnimation();
            binding.imageOptionsRL.setVisibility(View.VISIBLE);
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0);
            valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            valueAnimator.setDuration(200);

            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float val = (float) valueAnimator.getAnimatedValue();

                    binding.imageOptionsRL.setScaleX(val);
                    binding.imageOptionsRL.setScaleY(val);

                    if (val == 0) {

                        binding.imageOptionsRL.setScaleX(1);

                        binding.imageOptionsRL.setScaleY(1);


                        binding.imageOptionsRL.setVisibility(View.GONE);
                    }
                }
            });

            valueAnimator.start();

            onTap(holder, position);

        }


    }

    @Override
    public void onBackPressed() {

        if (selectActive) {

            deselectAll();

            return;
        }


        if (singlePageMode) {
            singlePageViewModeToggle(false, null, 0);
            return;
        }

        super.onBackPressed();

    }

    private void deselectAll() {


        adapter.globalSelect = false;
        binding.topbarRightSelectedTxt.setVisibility(View.GONE);
        binding.topbarRight.setVisibility(View.VISIBLE);

        bottomSheetBehaviour.setState(BottomSheetBehavior.STATE_HIDDEN);
        selectActive = false;
        adapter.selectedItems.clear();

        Utils.vibrate(GridScanViewActivity.this, 20);
        for (int i = 0; i < adapter.model.dataProvider().size(); i++) {


            try {

                GridScanViewAdapter.ViewHolder holder = (GridScanViewAdapter.ViewHolder) binding.gridRecyclerView.findViewHolderForAdapterPosition(i);

                if (holder != null) {

                    holder.tint.setVisibility(View.GONE);
                }

            } catch (Exception e) {

            }
        }


        if (swapped) {
            swapped = false;


            syncDataAcrossViews();
            getImageDetails().sync();
            binding.gridRecyclerView.getAdapter().notifyDataSetChanged();
            binding.recyclerView.getAdapter().notifyDataSetChanged();

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


        binding.imageOptionsRL.clearAnimation();
        binding.imageOptionsRL.setVisibility(View.VISIBLE);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.setDuration(200);

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();

                binding.imageOptionsRL.setScaleX(val);
                binding.imageOptionsRL.setScaleY(val);

            }
        });

        valueAnimator.start();

        adapter.notifyDataSetChanged();

    }

    private void syncDataAcrossViews() {
        // ((ScanPreviewAdapter) getRecyclerView().getAdapter()).originalFilepaths = getOriginalFilepaths();

    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        int id = view.getId();

        if (id == R.id.selectAll) {
            adapter.globalSelect = true;
            adapter.notifyDataSetChanged();
        } else if (id == R.id.copySelectedRL) {

            copySelected();
        } else if (id == R.id.deleteSelectedRL) {

            deleteSelected();
        } else if (id == R.id.selectMultipleRL) {
            singlePageViewModeToggle(false, null, 1);
            getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);
        } else if (id == R.id.cameraCapture) {
            addPages(null, getOriginalFilepaths().size() - 1, false);
        } else if (id == R.id.shareSelectedRL) {
            shareSelected(false);
        } else if (id == R.id.exportSelectedRL) {
            shareSelected(true);
        } else if (id == R.id.ocrSelectedRL) {
            ocrSelected();
        }

    }

    private void copySelected() {

        Bundle bundle = new Bundle();

        getFirebaseInstance().logEvent("copy_selected", bundle);


        App.CLIPBOARD.clear();


        for (int i = 0; i < adapter.model.dataProvider().size(); i++) {

            if (adapter.selectedItems.contains(i)) {

                String processedPath = getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + getImageDetails().getAt(i);
                Effects effects = getImageDetails().getEffects(getImageDetails().getAt(i));
                CompleteEffectHolder effectHolder = new CompleteEffectHolder(effects.corners, effects.color, effects.isGray, effects.rotation, effects.colorTune,
                        getOriginalFilepaths().get(i), processedPath, getScanDirPath(), getImageDetails().getAt(i));

                App.CLIPBOARD.add(effectHolder);


            }
        }


        int position = ((LinearLayoutManager) binding.gridRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();


        GridScanViewAdapter.ViewHolder holder = (GridScanViewAdapter.ViewHolder) binding.gridRecyclerView.findViewHolderForAdapterPosition(position);


        if (holder != null) {
            if (!Prefs.firstTimeSeenScreen(GridScanViewActivity.this, "copy_selected_pages_doc")) {

                new GuideView.Builder(GridScanViewActivity.this)
                        .setTitle("How to paste?")

                        .setContentSpan((Spannable) Html.fromHtml("1. At first, <b>navigate</b> to the desired <b>document</b>.<br>2. Then <b>tap</b> on the <b>page</b> after which you want to paste the contents.<br>3. <b>Click</b> on <b>more</b> to find <b>paste</b> option."))
                        .setGravity(Gravity.auto) //optional
                        .setDismissType(DismissType.anywhere)
                        .setTargetView(holder.imageView)
                        .build()
                        .show();
            }

        }

        Toast.makeText(GridScanViewActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        Utils.vibrate(GridScanViewActivity.this, 25);


        deselectAll();
    }

    private void ocrSelected() {

        ArrayList<String> files = new ArrayList<>();
        double size = 0;

        for (int i = 0; i < adapter.model.dataProvider().size(); i++) {

            if (adapter.selectedItems.contains(i)) {

                String imageFilename = new File(getOriginalFilepaths().get(i)).getName();

                String bitmapPath = getOriginalFilepaths().get(i);


                File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), imageFilename);

                if (processedFile.exists()) {
                    bitmapPath = processedFile.getAbsolutePath();
                }


                files.add(bitmapPath);
            }

        }

        Bundle args = new Bundle();

        Intent intent = new Intent(GridScanViewActivity.this, OcrActivity.class);
        args.putSerializable(OcrActivity.IMAGE_PATHS, (Serializable) files);
        intent.putExtra(OcrActivity.IMAGE_PATHS, args);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);

        deselectAll();


    }


    private void requestStoragePermission(int perm) {


        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {


            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Permission Needed");
            builder.setMessage("Storage permission is required to export");
            builder.setCancelable(false);


            builder.setPositiveButton("OK", (dialog, which) -> {


                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, perm);

                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    //
                }
            });


            builder.create();
            builder.show();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, perm);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    if (requestCode == PERMISION_REQUEST_CODE_SELECTED) {

                        shareSelected(true);


                    }


                }
            });

        } else {

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Permission not granted");
            builder.setMessage("Cannot export as permission to write to external storage was denied");

            builder.setCancelable(false);

            builder.setPositiveButton("OK", (dialog, which) -> {


                try {
                    dialog.dismiss();
                } catch (Exception e) {

                }
            });


            builder.create();
            builder.show();
        }

    }


    private void shareSelected(boolean isExport) {


        if (isExport) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {


                } else {
                    requestStoragePermission(PERMISION_REQUEST_CODE_SELECTED);
                    return;
                }

            }


        }


        ProgressDialog pd1 = new ProgressDialog(GridScanViewActivity.this);
        pd1.setTitle("Please wait");
        pd1.setMessage("Processing uncropped images");
        pd1.setCancelable(false);


        ArrayList<File> files = new ArrayList<>(adapter.selectedItems.size());
        HashSet<String> set = new HashSet<>(adapter.selectedItems.size());
        double size = 0;


        for (int i = 0; i < adapter.model.dataProvider().size(); i++) {

            if (adapter.selectedItems.contains(i)) {

                set.add(getImageDetails().getAt(i));

                File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), new File(adapter.model.dataProvider().get(i)).getName());

                files.add(processedFile);

                size += processedFile.length() / 1024.0;
            }

        }


        double finalSize = size;
        executorService2.execute(new Runnable() {
            @Override
            public void run() {


                Utils.copyNotProcessedOriginals(set, getImageDetails(), getScanDirPath(), new Utils.OnUpdateCopy() {
                    @Override
                    public void showDialog() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (!pd1.isShowing())
                                        pd1.show();

                                } catch (Exception e) {
                                } catch (Error e1) {
                                }
                            }
                        });
                    }
                });


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        pd1.dismiss();
                        deselectAll();

                        shareFromArray(files, finalSize, isExport);


                    }
                });


            }
        });


    }

    private void deleteSelected() {


        Bundle bundle = new Bundle();

        getFirebaseInstance().logEvent("delete_selected", bundle);



        SpannableString cancel = new SpannableString("Cancel");
        setSpanActionColor(cancel, 1, 1);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete " + adapter.selectedItems.size() + " selected image(s)?");


        builder.setPositiveButton("Delete", (dialog, which) -> {


            HashSet<Integer> set = new HashSet<>();

            for (Integer i : adapter.selectedItems)
                set.add(i);


            deselectAll();


            MainActivity.listingModified = true;

            ArrayList<String> pathDelete = new ArrayList<>();
            LinkedList<String> fileDelete = new LinkedList<>();

            for (int i = 0; i < adapter.model.dataProvider().size(); i++) {


                if (set.contains(i)) {
                    pathDelete.add(adapter.model.dataProvider().get(i));
                    fileDelete.add(getImageDetails().getOrdering().get(i));
                    //adapter.notifyItemRemoved(i);
                }
            }


            getOriginalFilepaths().removeAll(pathDelete);
            getImageDetails().getOrdering().removeAll(fileDelete);

            for (String fPath : pathDelete) {
                FileUtils.deleteQuietly(new File(fPath));
            }

            for (String fPath : fileDelete) {


                String processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), fPath).getAbsolutePath();

                FileUtils.deleteQuietly(new File(processedImageFilepath));
            }

            getImageDetails().sync();

            syncDataAcrossViews();
            binding.recyclerView.getAdapter().notifyDataSetChanged();
            adapter.notifyDataSetChanged();

            generateThumbnail();


        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }


    @Override
    public void onScanDirPathChange(String path) {

        if (adapter != null) {
            adapter.scanDirName = getScanDirPath();
            adapter.notifyDataSetChanged();
        }

    }

}
