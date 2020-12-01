package com.aaindia.prodocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.views.PolygonView;
import com.aaindia.prodocscanner.views.TouchableReyclerView;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.Interfaces;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.jsibbold.zoomage.ZoomageView;


import org.opencv.core.Mat;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class ScanPreviewAdapter extends RecyclerView.Adapter<ScanPreviewAdapter.ViewHolder> {


    public static final String SCROLL_TO = "scroll_to";
    public String scanDirName;
    public LinkedList<String> orders;

//    public ArrayList<String> originalFilepaths;
//    public SavedImageDetails savedImageDetails;
    private Context context;
    private int imageViewMargin = 0;

    private AdapterInterface adapterInterface;

    private TouchableReyclerView reyclerView;

    private float x1, x2;
    int deltaX = 0, deltaSum = 0;
    private int THRESH_DISTANCE = 100;
    private boolean touchLock = false;

    public DataProvider dataProvider;


    public ScanPreviewAdapter(Activity context, String scanDirName, TouchableReyclerView reyclerView, AdapterInterface adapterInterface, DataProvider dataProvider) {
        this.context = context;
        this.scanDirName = scanDirName;
        this.reyclerView = reyclerView;
        this.adapterInterface = adapterInterface;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        THRESH_DISTANCE = (int) (width * 0.10);
        this.dataProvider = dataProvider;


    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScanPreviewAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_page_large, parent, false));


    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Glide.with(context).clear(holder.imageView);

        holder.isBusy = false;

        holder.isPreparingMat = false;
       // holder.imageView.recycle();
        holder.polygonView.setVisibility(View.GONE);

        if (position >= (dataProvider.filepathProvider().size())) {
            return;
        }

        holder.imageViewParent.clearAnimation();
        ;
        holder.imageViewParent.setScaleX(1.0f);
        holder.imageViewParent.setScaleY(1.0f);

        holder.imageViewParent.setRotation(0);


        holder.imageView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.imageView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.imageView.requestLayout();

        if (imageViewMargin == 0) {
            int margin = (int) (holder.polygonView.ballSize * 1);
            ((RelativeLayout.LayoutParams) holder.imageView.getLayoutParams()).setMargins(margin, margin, margin, margin);
            holder.imageView.requestLayout();

        }

        String imageFilename = new File(dataProvider.filepathProvider().get(position)).getName();

        File processedFile = FileNav.getProcessedFileFromName(scanDirName, imageFilename);


        final String[] imageShowPath = {dataProvider.filepathProvider().get(position)};


        holder.processing.setVisibility(View.VISIBLE);

        holder.nextAction.setVisibility(View.GONE);

        holder.imageView.clearAnimation();




        boolean processedFileExists = processedFile.exists();




        holder.imageView.post(new Runnable() {
            @Override
            public void run() {


                if (processedFileExists && dataProvider.imageDetailsProvider().getEffects(imageFilename) != null) {


                    holder.nextAction.setVisibility(View.GONE);

                    imageShowPath[0] = processedFile.getAbsolutePath();


                    holder.processing.setVisibility(View.GONE);


                    holder.imageViewParent.setScaleX(1.0f);
                    holder.imageViewParent.setScaleY(1.0f);
                    holder.imageViewParent.setRotation(0);





                    Glide.with(context)

                            .load(imageShowPath[0])
                            .skipMemoryCache(true)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {


                                    ((Activity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {


                                            holder.processing.setVisibility(View.GONE);


                                            holder.imageViewParent.setScaleX(1.0f);
                                            holder.imageViewParent.setScaleY(1.0f);
                                            holder.imageViewParent.setRotation(0);


                                            holder.imageView.setImageDrawable(resource);


                                        }
                                    });


                                    return true;
                                }
                            })
                            .into(holder.imageView);

                } else {







                    Glide.with(context)

                            .load(imageShowPath[0])
                            .skipMemoryCache(true)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

                                    ((Activity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.processing.setVisibility(View.GONE);


                                            ((Activity) context).runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {


                                                    holder.processing.setVisibility(View.GONE);


                                                    holder.imageViewParent.setScaleX(1.0f);
                                                    holder.imageViewParent.setScaleY(1.0f);
                                                    holder.imageViewParent.setRotation(0);

                                                    holder.imageView.setImageDrawable(resource);


                                                    adapterInterface.notProcessed(position, holder);

                                                }
                                            });
                                        }
                                    });

                                    return false;
                                }
                            })
                            .into(holder.imageView);




                }


                holder.pageNumber.setText("Page " + (position + 1) + "/" + dataProvider.filepathProvider().size());

                holder.polygonView.setVisibility(View.GONE);

                holder.processImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        adapterInterface.process(position, holder);
                    }
                });


                holder.clearAction.setOnClickListener(new View.OnClickListener() {


                    @Override
                    public void onClick(View view) {


                        notifyItemChanged(position);

                        holder.nextAction.setVisibility(View.GONE);

                        adapterInterface.processExit(position, holder);
                    }
                });
            }
        });


        holder.imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {


                switch (event.getAction()) {


                    case MotionEvent.ACTION_DOWN:

                        touchLock = false;
                        deltaX = 0;
                        deltaSum = 0;
                        x1 = event.getX();


                        break;
                    case MotionEvent.ACTION_MOVE:


                        //

                        if (!touchLock && holder.imageView.getCurrentScaleFactor() < 1.1) {

//                        if (!touchLock && holder.imageView.getScale() < 1.1) {
                            touchLock = event.getPointerCount() > 1;
                            x2 = event.getX();

                            deltaX = (int) (x2 - x1);
                            deltaSum += deltaX;

                            // reyclerView.smoothScrollBy(-deltaX,0);
                            reyclerView.scrollBy(-deltaX, -0);
                        }

                        break;

                    case MotionEvent.ACTION_UP:


                        if (!touchLock && holder.imageView.getCurrentScaleFactor() < 1.1) {

                      //  if (!touchLock && holder.imageView.getScale() < 1.1) {

                            if (Math.abs(deltaSum) > THRESH_DISTANCE) {


                                if (deltaSum < 0) {
                                    reyclerView.smoothScrollToPosition(Math.min(position + 1, getItemCount()));
                                } else {
                                    reyclerView.smoothScrollToPosition(Math.max(position - 1, 0));
                                }

                            } else {
                                reyclerView.smoothScrollToPosition(position);
                            }
                        }

                        touchLock = false;


                        break;
                }


                return false;
            }
        });


    }


    @Override
    public int getItemCount() {


        if (dataProvider.filepathProvider()!= null)
            return dataProvider.filepathProvider().size();

        return 0;
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView pageNumber;
        public PolygonView polygonView;
        public ZoomageView imageView;
        public RelativeLayout imageViewParent;
        public ProgressBar processing;
        public RelativeLayout nextAction;
        public ImageView processImage;
        public ImageView clearAction;

        public boolean isBusy = false;


     //   public SubsamplingScaleImageView imageView;


        public Bitmap displayBitmap = null;

        public Mat originalMat = null; // store original image
        //    public Mat processedMat = new Mat(); // store after complete processing
        //public Mat displayMat = new Mat();  // mat used for display only


        public int matPosition = -1;

        public boolean isPreparingMat = false;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            pageNumber = itemView.findViewById(R.id.pageNumberTV);
            polygonView = itemView.findViewById(R.id.polygonView);
            imageView = itemView.findViewById(R.id.theImage);
            imageViewParent = itemView.findViewById(R.id.theImageParent);
            processing = itemView.findViewById(R.id.processing);
            nextAction = itemView.findViewById(R.id.footerActionRL);
            processImage = itemView.findViewById(R.id.nextActionIV);
            clearAction = itemView.findViewById(R.id.nextClearActionIV);
        }
    }


    public interface AdapterInterface {

        void process(int postion, ViewHolder holder);

        void notProcessed(int postion, ViewHolder holder);

        void processExit(int position, ViewHolder holder);
    }

    public interface DataProvider{

        ArrayList<String> filepathProvider();
        SavedImageDetails imageDetailsProvider();

    }
}
