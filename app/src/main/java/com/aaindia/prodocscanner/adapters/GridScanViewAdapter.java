package com.aaindia.prodocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.GridScanViewActivity;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ScanViewActivity;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.Prefs;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;

public class GridScanViewAdapter extends RecyclerView.Adapter<GridScanViewAdapter.ViewHolder> {


    public ArrayList<String> originalFilepaths;
    private Context context;
    public String scanDirName;

    private int layoutHeight;

    private ItemPressHelper itemPressHelper;

    public HashSet<Integer> selectedItems = new HashSet<>();


    public boolean globalSelect = false;


    public GridScanViewAdapter(Activity context, String scanDirName, ArrayList<String> originalFilepaths, ItemPressHelper itemPressHelper) {

        this.originalFilepaths = originalFilepaths;
        this.context = context;
        this.scanDirName = scanDirName;
        this.itemPressHelper = itemPressHelper;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        layoutHeight = (int) ((width / 2) * (4 / 3.0));
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_scan_view, parent, false);

        view.getLayoutParams().height = layoutHeight;
        return new GridScanViewAdapter.ViewHolder(view);

    }

    public boolean firstTime = false;


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        if (firstTime) {
            firstTime = !Prefs.firstTimeSeenScreen(context, "grid_scan_2");
        }

        if (position == 0 && firstTime) {

            firstTime = false;

            new GuideView.Builder(context)
                    .setTitle("Page options")
                    .setContentSpan((Spannable) Html.fromHtml("<b>Tap</b> the page to <b>edit</b> it.<br><b>Press and Hold</b> the image for <b>more options</b>."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(holder.itemView)
                    .build().show();


        }


        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setClickable(true);


        holder.tint.setAlpha(1f);
        holder.tint.setVisibility(View.GONE);


        holder.pageNumber.setText((position + 1) + "");
        File processedFile = FileNav.getProcessedFileFromName(scanDirName, new File(originalFilepaths.get(position)).getName());

        String filepath = processedFile.getAbsolutePath();

        if (!processedFile.exists()) {

            filepath = originalFilepaths.get(position);
        }

        String finalFilepath = filepath;


        Glide.with(context).clear(holder.imageView);

        Glide.with(context)


                .load(finalFilepath)

                .skipMemoryCache(true)

                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())


                .into(holder.imageView);


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemPressHelper.onTap(holder, position);
            }
        });
        holder.itemView.setClickable(true);

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                itemPressHelper.onLongPress(holder, position);
                return true;
            }
        });

        if (globalSelect) {
            holder.tint.setVisibility(View.VISIBLE);
            selectedItems.add(position);
        }

    }

    @Override
    public int getItemCount() {

        if (originalFilepaths != null) {

            if (originalFilepaths.size() != 0) {
                return originalFilepaths.size();
            } else {
                return 0;
            }
        }


        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView pageNumber;
        public RelativeLayout tint;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            imageView = itemView.findViewById(R.id.image);
            pageNumber = itemView.findViewById(R.id.pageNumberTV);
            tint = itemView.findViewById(R.id.tint);

        }
    }

    public interface ItemPressHelper {

        void onTap(GridScanViewAdapter.ViewHolder holder, int position);

        void onLongPress(GridScanViewAdapter.ViewHolder holder, int position);


    }


}
