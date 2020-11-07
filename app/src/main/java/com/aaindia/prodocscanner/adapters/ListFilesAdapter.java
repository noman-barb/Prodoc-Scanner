package com.aaindia.prodocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.wrappers.ListFIlesInfo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ListFilesAdapter extends RecyclerView.Adapter<ListFilesAdapter.ViewHolder> {


    private Context context;
    public   ArrayList<ListFIlesInfo> data;
    SimpleDateFormat simpleDateFormat ;
    OnScanClickListener scanClickListener;




    public ListFilesAdapter(Context context, ArrayList<ListFIlesInfo> data, OnScanClickListener scanClickListener){
        this.data = data;
        this.context = context;
        this.scanClickListener = scanClickListener;

        simpleDateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    }


    public ListFIlesInfo getItem(int n){

        return  data.get(n);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_entry_main_activity, parent, false), scanClickListener);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {



        Glide.with(context).clear(holder.thumbnail);



        if (  getItem(position).isSelected ){
            holder.foreground.setVisibility(View.VISIBLE);

        }
        else {
            holder.foreground.setVisibility(View.GONE);
        }

        String filename = getItem(position).filename;

        holder.dateCreated.setText(   simpleDateFormat.format( getItem(position).dateModified )   );
        holder.filename.setText(filename);

        holder.numPages.setText( getItem(position).numPages+"" );


        if (getItem(position).is_scan){

            if (getItem(position).thumbnailPath!=null){

              //  holder.thumbnail.setImageBitmap(BitmapUtils.loadThumb(getItem(position).thumbnailPath ));

                Glide.with(context)

                        .load(getItem(position).thumbnailPath )
                        .skipMemoryCache(true)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)

                        .into(holder.thumbnail);
            }
            else {

//                holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.scan_folder_icon));

                Glide.with(context)

                        .load("" )
                        .placeholder(R.drawable.scan_folder_icon)
                        .skipMemoryCache(true)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)

                        .into(holder.thumbnail);

            }

        }
        else {


            Glide.with(context)

                    .load("" )
                    .placeholder(R.drawable.folder_icon)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)

                    .into(holder.thumbnail);

        }



    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{

        TextView filename, dateCreated, numPages;
        ImageView moreOptions, thumbnail;
        RelativeLayout mainRl;
        LinearLayout foreground;
        OnScanClickListener scanClickListener;

        public ViewHolder(@NonNull View itemView, OnScanClickListener scanClickListener) {
            super(itemView);

            this.filename = itemView.findViewById(R.id.fileName);
            this.dateCreated = itemView.findViewById(R.id.dateCreated);
            this.numPages = itemView.findViewById(R.id.numPages);
            this.moreOptions = itemView.findViewById(R.id.moreOptions);
            this.thumbnail = itemView.findViewById(R.id.thumbnail);
            this.mainRl = itemView.findViewById(R.id.mainRl);
            this.scanClickListener = scanClickListener;

            this.foreground = itemView.findViewById(R.id.foreground_mask);

            this.moreOptions.setOnClickListener(this);
            this.mainRl.setOnClickListener(this);
            this.mainRl.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View view) {


            scanClickListener.onScanClick(getAdapterPosition(), view);
        }

        @Override
        public boolean onLongClick(View view) {

            scanClickListener.onLongPress(getAdapterPosition(), view);
            return true;
        }
    }




    public interface OnScanClickListener{
        void onScanClick(int position, View view);

        void onLongPress(int position, View view);
    }
}
