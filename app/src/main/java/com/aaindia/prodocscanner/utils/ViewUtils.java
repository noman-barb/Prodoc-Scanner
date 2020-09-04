package com.aaindia.prodocscanner.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

public class ViewUtils {

    public static RelativeLayout.LayoutParams copy(ViewGroup.LayoutParams viewLayoutParamsToCopy) {
        RelativeLayout.LayoutParams copiedParams = new RelativeLayout.LayoutParams(viewLayoutParamsToCopy);
        if (viewLayoutParamsToCopy instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams relativeLayoutParamsToCopy = (RelativeLayout.LayoutParams) viewLayoutParamsToCopy;
            int[] rulesToCopy = relativeLayoutParamsToCopy.getRules();
            for (int verb = 0; verb < rulesToCopy.length; verb++) {
                int subject = rulesToCopy[verb];
                copiedParams.addRule(verb, subject);
            }
        }
        return copiedParams;
    }

    public static void viewOnReady(ViewGroup viewGroup, Utils.ViewOnReady viewOnReady) {


        if (viewGroup.getMeasuredHeight() > 4) {

            viewOnReady.onReady(viewGroup);
        } else {

            viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    viewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    viewOnReady.onReady(viewGroup);

                }
            });

        }

    }

    public static void viewOnReady(View viewGroup, Utils.ViewOnReady viewOnReady){



        if (viewGroup.getMeasuredHeight()>4){

            viewOnReady.onReady(viewGroup);
        }
        else {

            viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    viewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    viewOnReady.onReady(viewGroup);

                }
            });

        }

    }


}
