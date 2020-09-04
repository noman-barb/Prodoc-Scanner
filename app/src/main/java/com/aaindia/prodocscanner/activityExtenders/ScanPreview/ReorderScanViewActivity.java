package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.adapters.ReorderScanViewAdapter;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class ReorderScanViewActivity extends EditScanViewActivity {


    ArrayList<String> originalPaths = new ArrayList<>();

    LinkedList<String> ordering = new LinkedList<>();
    private ReorderScanViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ReorderScanViewAdapter(this, getScanDirPath(), originalPaths);

        binding.reOrderRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        getBinding().reOrderRecyclerView.setAdapter(adapter);


        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {


                int fromPostion = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();


                Collections.swap(originalPaths, fromPostion, toPosition);
                Collections.swap(ordering, fromPostion, toPosition);


                getBinding().reOrderRecyclerView.getAdapter().notifyItemMoved(fromPostion, toPosition);


                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };


        getBinding().fooActionClearIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                reorderViewEnableDisable(false);

            }
        });


        getBinding().fooActionOKIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setOriginalFilepaths(new ArrayList<>(originalPaths));
                ((ScanPreviewAdapter) getRecyclerView().getAdapter()).originalFilepaths = getOriginalFilepaths();
                getImageDetails().setOrdering(new LinkedList<>(ordering));
                getImageDetails().sync();
                reorderViewEnableDisable(false);

                getRecyclerView().getAdapter().notifyDataSetChanged();

            }
        });


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);

        itemTouchHelper.attachToRecyclerView(binding.reOrderRecyclerView);


    }


    @Override
    public void reorder() {
        super.reorder();


        reorderViewEnableDisable(true);

        originalPaths = new ArrayList<>();
        ordering = new LinkedList<>();

        for (int i = 0; i < getOriginalFilepaths().size(); i++) {
            originalPaths.add(getOriginalFilepaths().get(i));

        }

        for (int i = 0; i < getImageDetails().getOrdering().size(); i++) {
            ordering.add(getImageDetails().getOrdering().get(i));
        }

        if (adapter != null) {

            adapter.originalFilepaths = originalPaths;

        }
        binding.reOrderRecyclerView.getAdapter().notifyDataSetChanged();


    }

    private void reorderViewEnableDisable(boolean reorder) {


        if (reorder) {
            getBinding().recyclerView.setVisibility(View.GONE);
            getBinding().reOrderRecyclerView.setVisibility(View.VISIBLE);
            getBinding().footerActionRL.setVisibility(View.VISIBLE);
        } else {
            getBinding().recyclerView.setVisibility(View.VISIBLE);
            getBinding().reOrderRecyclerView.setVisibility(View.GONE);
            getBinding().footerActionRL.setVisibility(View.GONE);
        }


    }
}
