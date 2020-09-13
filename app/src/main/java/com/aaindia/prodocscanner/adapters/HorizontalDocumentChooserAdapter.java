package com.aaindia.prodocscanner.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.constants.Constants;

import java.util.ArrayList;

public class HorizontalDocumentChooserAdapter extends RecyclerView.Adapter<HorizontalDocumentChooserAdapter.Viewholder> {


    ArrayList<String> data = Constants.documentImageTypes();

    Context context;

    OnItemTouchListener listener;

    public HorizontalDocumentChooserAdapter(Context context, OnItemTouchListener listener) {


        this.context = context;

        this.listener = listener;


    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Viewholder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizintal_document_type_chooser, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {


        holder.documentItem.setTextColor(Color.GRAY);
        ((TextView) holder.documentItem).setText(data.get(position));

        if (data.get(position).equals(Constants.DEFAULT_DOCUMENT_TYPE)) {

            holder.documentItem.setTextColor(Color.WHITE);
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class Viewholder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView documentItem;


        public Viewholder(@NonNull View itemView) {
            super(itemView);

            documentItem = itemView.findViewById(R.id.itemText);

            itemView.setOnClickListener(this);


        }

        @Override
        public void onClick(View view) {

            listener.onTouch(this, getAdapterPosition());
        }
    }

    public interface OnItemTouchListener {


        void onTouch(Viewholder viewholder, int position);
    }
}

