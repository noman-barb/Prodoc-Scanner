package com.aaindia.prodocscanner.utils.share;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.adapters.ListFilesAdapter;
import com.aaindia.prodocscanner.constants.AdIds;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.wrappers.UnifiedNativeAdObsevable;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import org.opencv.core.Mat;

import java.util.Observable;
import java.util.Observer;

public class ShareDialog {


    private Observer adObserver;
    private MaterialAlertDialogBuilder builder;
    Activity context;
    LinearLayout linearLayout;


    MaterialRadioButton pdfRadio, imgRadio;

    Slider qualitySlider;
    MaterialTextView expectedSize;
    MaterialTextView qualityTxt;

    LinearLayout qualityControl;

    double initialSizeKB = 0;

    OnShareDialogListener listener;

    String shareTxtBtnTxt = "Share";

    MaterialTextView textViewPDF, txtViewImg;


    TextInputEditText password;
    SwitchMaterial passwordProtect;
    int qualityControlHeight = 0;

    UnifiedNativeAdObsevable adObsevable;

    public ShareDialog(Activity context, double initialSizeKB, UnifiedNativeAdObsevable adObsevable, OnShareDialogListener listener) {

        this.context = context;

        this.initialSizeKB = initialSizeKB;
        this.listener = listener;
        this.adObsevable = adObsevable;

        linearLayout = (LinearLayout) context.getLayoutInflater().inflate(R.layout.share_chooser, null);

        qualityControl = linearLayout.findViewById(R.id.qualityControl);

        pdfRadio = linearLayout.findViewById(R.id.pdfRadio);
        imgRadio = linearLayout.findViewById(R.id.imageRadio);
        qualitySlider = linearLayout.findViewById(R.id.qualitySlider);
        expectedSize = linearLayout.findViewById(R.id.expectedSize);
        qualityTxt = linearLayout.findViewById(R.id.qualityTxt);


        txtViewImg = linearLayout.findViewById(R.id.shareImageTxt);
        textViewPDF = linearLayout.findViewById(R.id.sharePDFTxt);

        password = linearLayout.findViewById(R.id.password);
        passwordProtect = linearLayout.findViewById(R.id.passwordProtectCB);

        password.setVisibility(View.GONE);

        passwordProtect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (compoundButton.isPressed()) {

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (b) {
                                password.setVisibility(View.VISIBLE);
                            } else {
                                password.setVisibility(View.GONE);
                                password.setText("");
                            }
                        }
                    });

                }
            }
        });

        imgRadio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


                if (compoundButton.isPressed()) {
                    pdfRadio.setChecked(!b);


                    linearLayout.findViewById(R.id.passwordProtectRootLL).setVisibility(View.GONE);


                }


            }
        });

        pdfRadio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (compoundButton.isPressed()) {

                    imgRadio.setChecked(!b);


                    linearLayout.findViewById(R.id.passwordProtectRootLL).setVisibility(View.VISIBLE);


                }
            }
        });


        txtViewImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                imgRadio.setChecked(true);
                pdfRadio.setChecked(false);

                linearLayout.findViewById(R.id.passwordProtectRootLL).setVisibility(View.GONE);


            }
        });

        textViewPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfRadio.setChecked(true);
                imgRadio.setChecked(false);

                linearLayout.findViewById(R.id.passwordProtectRootLL).setVisibility(View.VISIBLE);

            }
        });


        qualitySlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {


                if (value > 90) {

                    qualityTxt.setText("Very High");

                } else {
                    if (value > 75) {
                        qualityTxt.setText("High");
                    } else {


                        if (value > 50) {
                            qualityTxt.setText("Good");
                        } else {

                            if (value > 25) {
                                qualityTxt.setText("Medium");
                            } else {

                                if (value > 8) {
                                    qualityTxt.setText("Low");
                                } else {
                                    qualityTxt.setText("Very Low");
                                }
                            }
                        }

                    }
                }


                value /= 100.0;

                setSizeTxt(value);


            }


        });

        setSizeTxt(qualitySlider.getValue() / 100.0);


        adObserver = new Observer() {
            @Override
            public void update(Observable observable, Object o) {

                showAd(((UnifiedNativeAdObsevable) observable).getAd());

                adObsevable.deleteObserver(this);

            }
        };


        UnifiedNativeAd unifiedNativeAd = adObsevable.getAd();

        if (unifiedNativeAd != null) {
            showAd(unifiedNativeAd);

        } else {

            adObsevable.addObserver(adObserver);
        }


    }

    private void showAd(UnifiedNativeAd unifiedNativeAd) {

        if (unifiedNativeAd == null)
            return;

        NativeTemplateStyle styles = new
                NativeTemplateStyle.Builder().build();
        linearLayout.findViewById(R.id.ad_template_1).setVisibility(View.VISIBLE);
        TemplateView template = linearLayout.findViewById(R.id.ad_template_1);
        template.setStyles(styles);
        template.setNativeAd(unifiedNativeAd);
    }


    public ShareDialog typeExport() {


        ((TextView) linearLayout.findViewById(R.id.shareTxt)).setText("Export as:");
        //  ((TextView) linearLayout.findViewById(R.id.sharePDFTxt)).setText("PDF to device:");
        // ((TextView) linearLayout.findViewById(R.id.shareImageTxt)).setText("Images to gallery");
        shareTxtBtnTxt = "Export";

        return this;
    }


    public ShareDialog typeView() {


        ((TextView) linearLayout.findViewById(R.id.shareTxt)).setText("View as:");

        linearLayout.findViewById(R.id.imgRadioParent).setVisibility(View.GONE);
        shareTxtBtnTxt = "View";

        return this;
    }

    private void setSizeTxt(double value) {


        double size = getSize(value, initialSizeKB);

        String unit = "Kb";

        if (size > 1024) {
            unit = "Mb";
            size /= 1024.0;
        }

        String sizeStr = String.format("%.2f", size) + " " + unit;

        expectedSize.setText(sizeStr);
    }

    public AlertDialog.Builder build(boolean isExport) {

        if (isExport)
            typeExport();

        SpannableString shareTxt = new SpannableString(shareTxtBtnTxt);

        Utils.setSpanActionColor(context, shareTxt, 1, 1);
        builder = new MaterialAlertDialogBuilder(context)
                .setTitle(null)
                .setMessage(null)
                .setView(linearLayout)
                .setPositiveButton(shareTxt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {


                        String passwordTxt = null;

                        if (passwordProtect.isChecked()) {

                            String pass = password.getText().toString();

                            if (pass.length() > 0) {
                                passwordTxt = pass;
                            }
                        }

                        listener.share(pdfRadio.isChecked(), qualitySlider.getValue(), passwordTxt);
                    }
                })
                .setNegativeButton("Cancel", /* listener = */ null);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {

                try {
                    if (adObserver != null)
                        adObsevable.deleteObserver(adObserver);
                } catch (Exception e) {
                }
            }
        });


        return builder;
    }


    private double getSize(double quality, double originalSize) {


        return getQualityMultiplier(quality) * originalSize;
    }

    private double getQualityMultiplier(double quality) {

        double x = quality;
        return 43.52 * Math.pow(x, 6) - 122.67 * Math.pow(x, 5) + 132.23 * Math.pow(x, 4) - 66.872 * Math.pow(x, 3) + 15.466 * Math.pow(x, 2) - 0.819 * x + 0.138;

    }


    public interface OnShareDialogListener {

        void share(boolean isPDF, double quality, String password);
    }
}
