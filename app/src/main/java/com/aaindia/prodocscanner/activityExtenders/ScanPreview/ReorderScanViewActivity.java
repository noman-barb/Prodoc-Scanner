package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.adapters.ReorderScanViewAdapter;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.bumptech.glide.Glide;
import com.google.android.datatransport.runtime.synchronization.SynchronizationException;

import java.util.ArrayList;
import java.util.LinkedList;

public class ReorderScanViewActivity extends EditScanViewActivity {


    ArrayList<String> originalPaths = new ArrayList<>();

    LinkedList<String> ordering = new LinkedList<>();
    private ReorderScanViewAdapter adapter;


    private boolean isReordeing = false;


    public boolean isReordeing() {
        return isReordeing;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding.reOrderRecyclerView.setItemViewCacheSize(50);


        adapter = new ReorderScanViewAdapter(this, getScanDirPath(), originalPaths);

        binding.reOrderRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        getBinding().reOrderRecyclerView.setAdapter(adapter);


        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {


                int fromPostion = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();


                if (toPosition < fromPostion) {
                    originalPaths.add(toPosition, originalPaths.remove(fromPostion));
                    ordering.add(toPosition, ordering.remove(fromPostion));
                } else {

                    String s1 = originalPaths.get(fromPostion);
                    String s2 = ordering.get(fromPostion);

                    originalPaths.add(toPosition + 1, s1);
                    ordering.add(toPosition + 1, s2);

                    originalPaths.remove(fromPostion);
                    ordering.remove(fromPostion);

                }


                getBinding().reOrderRecyclerView.getAdapter().notifyItemMoved(fromPostion, toPosition);


                for (int i = 0; i < originalPaths.size(); i++) {

                    ReorderScanViewAdapter.ViewHolder holder = (ReorderScanViewAdapter.ViewHolder) binding.reOrderRecyclerView.findViewHolderForAdapterPosition(i);


                    if (holder != null) {

                        holder.pageNumber.setText((i + 1) + "");
                    }

                }


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


                releaseGlide();

                adapter.originalFilepaths.clear();

                adapter.notifyDataSetChanged();

                isReordeing = false;


                System.gc();

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

                releaseGlide();

                adapter.originalFilepaths.clear();

                adapter.notifyDataSetChanged();


                System.gc();
                isReordeing = false;


            }
        });


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);

        itemTouchHelper.attachToRecyclerView(binding.reOrderRecyclerView);


    }

    private void releaseGlide() {

        for (int i=0 ; i <adapter.originalFilepaths.size() ; i++){

            try {

                ReorderScanViewAdapter.ViewHolder holder = (ReorderScanViewAdapter.ViewHolder) binding.reOrderRecyclerView.findViewHolderForLayoutPosition(i);

                if (holder!=null)

                    Glide.with(ReorderScanViewActivity.this).clear(holder.imageView);
            }

            catch (Exception e){}

        }
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


        isReordeing = true;


    }

    public void reorderViewEnableDisable(boolean reorder) {

        isReordeing = reorder;


        if (reorder) {
            getBinding().recyclerView.setVisibility(View.GONE);
            getBinding().reOrderRecyclerView.setVisibility(View.VISIBLE);
            getBinding().footerActionRL.setVisibility(View.VISIBLE);
        } else {
            getBinding().recyclerView.setVisibility(View.VISIBLE);
            getBinding().reOrderRecyclerView.setVisibility(View.GONE);
            getBinding().footerActionRL.setVisibility(View.GONE);
            getBinding().reOrderRecyclerView.setVisibility(View.GONE);
        }


    }
}
