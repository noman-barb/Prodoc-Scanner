package com.aaindia.prodocscanner.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class TouchableReyclerView extends RecyclerView {


    private boolean itemTouchable = true;


    public boolean getItemTouchable(){
        return itemTouchable;
    }
    public void setItemTouchable(boolean itemTouchable){
        this.itemTouchable = itemTouchable;
    }

    public TouchableReyclerView(@NonNull Context context) {
        super(context);
    }

    public TouchableReyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableReyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (itemTouchable) {
            return super.onInterceptTouchEvent(event);
        }

        this.stopScroll();

        return false;
    }


}
