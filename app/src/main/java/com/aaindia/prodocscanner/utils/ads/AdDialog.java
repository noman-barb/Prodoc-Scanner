package com.aaindia.prodocscanner.utils.ads;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.aaindia.prodocscanner.R;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AdDialog {


    private AlertDialog.Builder builder;

    private TemplateView template;

    public AdDialog(Activity context) {


        LayoutInflater li = LayoutInflater.from(context);

        LinearLayout layout = (LinearLayout) li.inflate(R.layout.ad_dialog, null);
        template = layout.findViewById(R.id.ad_template_dialog);

        builder = new MaterialAlertDialogBuilder(context);
        builder.setCancelable(false);


        builder.setView(layout);
        AlertDialog dialog = builder.create();

        dialog.show();

        layout.findViewById(R.id.ad_skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.cancel();
            }
        });

    }


   public AdDialog setAd(UnifiedNativeAd nativeAd){

        template.setNativeAd(nativeAd);
        return this;
   }
}
