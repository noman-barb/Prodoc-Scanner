package com.aaindia.prodocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activityExtenders.ScanPreview.ReorderScanViewActivity;
import com.aaindia.prodocscanner.constants.Constants;
import com.aaindia.prodocscanner.utils.FileNav;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.ArrayList;

public class ReorderScanViewAdapter extends RecyclerView.Adapter<ReorderScanViewAdapter.ViewHolder> {


    public ArrayList<String> originalFilepaths;
    private Context context;
    private String scanDirName;

    private int layoutHeight;

    public ReorderScanViewAdapter(Activity context, String scanDirName, ArrayList<String> originalFilepaths) {

        this.originalFilepaths = originalFilepaths;
        this.context = context;
        this.scanDirName = scanDirName;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        layoutHeight = (int) ((width / 3) * (4 / 3.0));
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reorder_scan_view, parent, false);

        view.getLayoutParams().height = layoutHeight;
        return new ReorderScanViewAdapter.ViewHolder(view);

    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


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

                .transition(DrawableTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.NONE)

                .into(holder.imageView);


    }

    @Override
    public int getItemCount() {

        if (originalFilepaths != null)
            return originalFilepaths.size();

        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView pageNumber;
        public String filepath = null;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            imageView = itemView.findViewById(R.id.image);
            pageNumber = itemView.findViewById(R.id.pageNumberTV);
        }
    }
}
