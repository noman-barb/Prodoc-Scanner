package com.aaindia.prodocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;

public class ReorderScanViewAdapter extends RecyclerView.Adapter<ReorderScanViewAdapter.ViewHolder> {


    public ArrayList<String> originalFilepaths;
    private Context context;
    private String scanDirName;


    public ReorderScanViewAdapter(Context context, String scanDirName, ArrayList<String> originalFilepaths) {

        this.originalFilepaths = originalFilepaths;
        this.context = context;
        this.scanDirName = scanDirName;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ReorderScanViewAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reorder_scan_view, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        holder.pageNumber.setText((position + 1) + "");
        File processedFile = FileNav.getProcessedFileFromName(scanDirName, new File(originalFilepaths.get(position)).getName());

        String filepath = processedFile.getAbsolutePath();

        if (!processedFile.exists()) {

            filepath = originalFilepaths.get(position);
        }

        Glide.with(context)

                .load(filepath)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.imageView);

        holder.filepath = filepath;

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
