package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.animation.ValueAnimator;
import android.content.Intent;
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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.GridScanViewAdapter;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.ocr.OcrActivity;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.Prefs;
import com.aaindia.prodocscanner.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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


        adapter = new GridScanViewAdapter(this, getScanDirPath(), getOriginalFilepaths(), this);

        binding.gridRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
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
        binding.deselectAllRL.setOnClickListener(this::onClick);
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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.gridRecyclerView);


        binding.selectMultipleRL.setOnClickListener(this);


        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().getBoolean(ScanPreviewActivity.NEW_SCAN, false)) {


            singlePageViewModeToggle(true, null, 0);
        } else {

            if (!adapter.firstTime) {

                adapter.firstTime = true;

                if (adapter.originalFilepaths.size() > 0)
                    adapter.notifyItemChanged(0);

            }
        }


    }

    private boolean swapped = false;

    private void swap(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        swapped = true;


        int fromPostion = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        if (adapter.selectedItems.contains(fromPostion)) {
            adapter.selectedItems.remove(fromPostion);
            adapter.selectedItems.add(toPosition);
        }


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


                                            int pos = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();

                                            if (pos < 0)
                                                return;


                                            ScanPreviewAdapter.ViewHolder hold = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(pos);

                                            if (hold == null)
                                                return;


                                            if (hold.nextAction.getVisibility() == View.GONE)
                                                return;


                                            new GuideView.Builder(GridScanViewActivity.this)
                                                    .setTitle("Crop Image")

                                                    .setGravity(Gravity.auto) //optional
                                                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                                                    .setTargetView(hold.processImage)
                                                    .build().show();


                                        }
                                    })
                                    .build().show();


                        }


                    }
                }
            });


            animator.start();
        } else {

            if (!adapter.firstTime) {

                adapter.firstTime = true;

                if (adapter.originalFilepaths.size() > 0)
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

        Utils.vibrate(this, 12);
        for (int i = 0; i < adapter.originalFilepaths.size(); i++) {


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

    }

    private void syncDataAcrossViews() {
        setOriginalFilepaths(getOriginalFilepaths());
        ((ScanPreviewAdapter) getRecyclerView().getAdapter()).originalFilepaths = getOriginalFilepaths();
        adapter.originalFilepaths = getOriginalFilepaths();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        int id = view.getId();

        if (id == R.id.selectAll) {
            adapter.globalSelect = true;
            adapter.notifyDataSetChanged();
        } else if (id == R.id.deselectAllRL) {


            deselectAll();
        } else if (id == R.id.deleteSelectedRL) {

            deleteSelected();
        } else if (id == R.id.selectMultipleRL) {
            singlePageViewModeToggle(false, null, 1);
            getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);
        } else if (id == R.id.cameraCapture) {
            addPages(null, getOriginalFilepaths().size() - 1);
        } else if (id == R.id.shareSelectedRL) {
            shareSelected(false);
        } else if (id == R.id.exportSelectedRL) {
            shareSelected(true);
        } else if (id == R.id.ocrSelectedRL) {
            ocrSelected();
        }

    }

    private void ocrSelected() {

        ArrayList<String> files = new ArrayList<>();
        double size = 0;

        for (int i = 0; i < adapter.originalFilepaths.size(); i++) {

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

    private void shareSelected(boolean isExport) {

        Utils.copyNotProcessedOriginals(getImageDetails(), getScanDirPath());

        ArrayList<File> files = new ArrayList<>();
        double size = 0;

        for (int i = 0; i < adapter.originalFilepaths.size(); i++) {

            if (adapter.selectedItems.contains(i)) {

                File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), new File(adapter.originalFilepaths.get(i)).getName());

                files.add(processedFile);

                size += processedFile.length() / 1024.0;
            }

        }

        deselectAll();

        shareFromArray(files, size, isExport);
    }

    private void deleteSelected() {


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

            for (int i = 0; i < adapter.originalFilepaths.size(); i++) {


                if (set.contains(i)) {
                    pathDelete.add(adapter.originalFilepaths.get(i));
                    fileDelete.add(getImageDetails().getOrdering().get(i));
                    adapter.notifyItemRemoved(i);
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


        });
        builder.setNegativeButton(cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }


}
