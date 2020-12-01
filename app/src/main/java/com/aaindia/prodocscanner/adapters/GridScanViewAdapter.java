package com.aaindia.prodocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import org.opencv.core.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;

public class GridScanViewAdapter extends RecyclerView.Adapter<GridScanViewAdapter.ViewHolder> {


    private Context context;
    public String scanDirName;

    private int layoutHeight;

    private ItemPressHelper itemPressHelper;

    public HashSet<Integer> selectedItems = new HashSet<>();


    public boolean globalSelect = false;

    public DataModel model;



    private boolean isScrolling = false;

    public GridScanViewAdapter(Activity context, String scanDirName, DataModel model, ItemPressHelper itemPressHelper) {


        this.context = context;
        this.scanDirName = scanDirName;
        this.itemPressHelper = itemPressHelper;

        this.model = model;

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
        File processedFile = FileNav.getProcessedFileFromName(scanDirName, new File(model.dataProvider().get(position)).getName());

        String filepath = processedFile.getAbsolutePath();

        if (!processedFile.exists()) {

            filepath = model.dataProvider().get(position);
        }

        String finalFilepath = filepath;


        Glide.with(context).clear(holder.imageView);

        Glide.with(context)


                .load(finalFilepath)

                .skipMemoryCache(true)

                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())


                .into(holder.imageView);


//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                itemPressHelper.onTap(holder, position);
//            }
//        });
        // holder.itemView.setClickable(true);


//        holder.imageView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                itemPressHelper.onLongPress(holder, position);
//                return true;
//            }
//        });



        holder.imageView.setOnTouchListener(new View.OnTouchListener() {


            @Override
            public boolean onTouch(View v, MotionEvent event) {



                if (isScrolling){
                    holder.isLongPress = false;
                    Log.d("aaaaaaa","long false");
                }

                if (holder.handler == null)
                    holder.handler = new Handler(Looper.getMainLooper());


                if (holder.runnable == null) {
                    holder.runnable = new Runnable() {
                        @Override
                        public void run() {

                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (holder.isLongPress && !isScrolling) {
                                        itemPressHelper.onLongPress(holder, position);
                                        Log.d("aaaaaaa", "LONG PRESS");

                                    }else {
                                        Log.d("aaaaaaa", "CANCEL PRESS");
                                    }
                                }
                            });
                        }
                    };
                }


                if (holder.pointF==null){
                    holder.pointF = new PointF(0,0);
                }


                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    holder.pointF.x = event.getX();
                    holder.pointF.y = event.getY();

                    holder.isLongPress = true;
                    holder.startTime = System.currentTimeMillis();

                    holder.handler.removeCallbacks(holder.runnable);
                    holder.handler.postDelayed(holder.runnable, 400);






                } else {

//                    if (System.currentTimeMillis() - holder.startTime > 200) {
//                        itemPressHelper.onLongPress(holder, position);
//                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {

                        holder.handler.removeCallbacks(holder.runnable);
                        holder.isLongPress = false;
                        holder.handler = null;
                        holder.runnable = null;
                        holder.pointF = null;


                        if (System.currentTimeMillis() - holder.startTime<200){
                            itemPressHelper.onTap(holder, position);
                            return true;
                        }


                    }
                    else if (event.getAction() == MotionEvent.ACTION_MOVE){


                        float errorX = Math.abs(holder.pointF.x - event.getX());
                        float errorY = Math.abs(holder.pointF.y - event.getY());


                        if ((errorX+errorY)>0  ){

                            holder.handler.removeCallbacks(holder.runnable);
                            holder.isLongPress = false;
                            holder.handler = null;
                            holder.runnable = null;
                            holder.pointF = null;
                        }

                    }

                }


                return false;
            }
        });


        if (globalSelect) {
            holder.tint.setVisibility(View.VISIBLE);
            selectedItems.add(position);
        }

    }

    @Override
    public int getItemCount() {

        if (model != null) {

            if (model.dataProvider().size() != 0) {
                return model.dataProvider().size();
            } else {
                return 0;
            }
        }


        return 0;
    }

    public void setScrolling(boolean scrolling) {
        isScrolling = scrolling;
    }

    public boolean isScrolling() {
        return isScrolling;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView pageNumber;
        public RelativeLayout tint;

        public boolean isLongPress = false;

        public long startTime = 0, endTime = 0;


        public Handler handler;
        public Runnable runnable;

        public PointF pointF;

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

    public interface DataModel {


        public ArrayList<String> dataProvider();
    }


}
