package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.ViewUtils;
import com.aaindia.prodocscanner.wrappers.Effects;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.HashMap;


public class EditScanViewActivity extends ProcessScanViewActivity {


    int colorTune = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void chooseColor(ScanPreviewAdapter.ViewHolder holder, int position) {


        if (getImageDetails().getEffects(getImageDetails().getAt(position)) == null) {

            File processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position));

            // but processed image exists

            if (processedImageFilepath.exists()) {


                crop(holder, position);
                return;
            }


        }

        binding.protector.setVisibility(View.VISIBLE);


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                prepareMat(holder, position);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.protector.setVisibility(View.GONE);
                    }
                });
            }
        });

        t.start();


        Effects effects = getImageDetails().getEffects(getImageDetails().getAt(position));


        int colorCode = MatFilter.DEFAULT_COLOR_CODE;
        boolean colorGray = false;
        int colorTune = 60;

        if (effects != null) {
            colorCode = effects.color;
            colorGray = effects.isGray;
            colorTune = effects.colorTune;

        }


        setColorTuneListen(false);
        showHideColorRL(true);
        getBinding().colorGrayCheck.setChecked(colorGray);
        getBinding().colorTuneSK.setProgress(colorTune);


        PopupMenu popupMenu = new PopupMenu(this, binding.colorRL);

        Menu menu = popupMenu.getMenu();

        SpannableString original = new SpannableString("Orignal");
        SpannableString contrast = new SpannableString("Contrast");
        SpannableString paperStyle = new SpannableString("Paper ");
        SpannableString whiteBoardStyle = new SpannableString("Clean Text");

        setSpanActionColor(original, colorCode, MatFilter.COLOR_ORIGINAL);
        setSpanActionColor(contrast, colorCode, MatFilter.COLOR_CONTRAST);
        setSpanActionColor(paperStyle, colorCode, MatFilter.COLOR_PAPER);
        setSpanActionColor(whiteBoardStyle, colorCode, MatFilter.COLOR_WHITEBOARD);

        menu.add(0, MatFilter.COLOR_ORIGINAL, 0, original);
        menu.add(0, MatFilter.COLOR_CONTRAST, 0, contrast);
        menu.add(0, MatFilter.COLOR_PAPER, 0, paperStyle);
        menu.add(0, MatFilter.COLOR_WHITEBOARD, 0, whiteBoardStyle);

        boolean finalColorGray = colorGray;
        setColorTuneListen(true);


        boolean finalColorGray1 = colorGray;
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {


                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int id = item.getItemId();

                int colorCode = id;

                int colorTune = MatFilter.getDefaultTune(id);

                binding.colorTuneSK.setProgress(colorTune);
                binding.colorGrayCheck.setChecked(finalColorGray);

                Effects effects = getImageDetails().getEffects(getImageDetails().getAt(position));

                effects.color = colorCode;


                if (effects.color == MatFilter.COLOR_WHITEBOARD) {
                    effects.isGray = true;

                    setColorTuneListen(false);

                    getBinding().colorGrayCheck.setChecked(effects.isGray);
                    setColorTuneListen(true);
                }

                effects.colorTune = MatFilter.getDefaultTune(colorCode);

                processImage(holder, position, true);

                return true;
            }
        });

        popupMenu.show();


    }


    @Override
    public void crop(ScanPreviewAdapter.ViewHolder holder, int position) {


        zoomageEnableDisable(holder, false);

        holder.processing.setVisibility(View.VISIBLE);
        recyclerViewActivateDeact(false);
        getBinding().protector.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {


                prepareMat(holder, position);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        holder.imageView.getLayoutParams().width = (int) holder.displayBitmap.getWidth();
                        holder.imageView.getLayoutParams().height = (int) holder.displayBitmap.getHeight();
                        holder.imageView.setImageBitmap(holder.displayBitmap);
                        holder.imageView.requestLayout();
                        initCrop(position, holder);

                    }
                });


            }
        }).start();

    }


    private void initCrop(int position, ScanPreviewAdapter.ViewHolder holder) {

        holder.polygonView.setVisibility(View.VISIBLE);

        getBinding().protector.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();

        RelativeLayout.LayoutParams params1 = ViewUtils.copy(params);

        holder.polygonView.setLayoutParams(params1);
        holder.polygonView.getLayoutParams().height += holder.polygonView.ballSize;
        holder.polygonView.getLayoutParams().width += holder.polygonView.ballSize;
        holder.polygonView.requestLayout();


        Effects effects = getImageDetails().getEffects(new File(getOriginalFilepaths().get(position)).getName());


        double scaleX = holder.displayBitmap.getWidth() * 1.0 / holder.originalMat.width();
        double scaleY = holder.displayBitmap.getHeight() * 1.0 / holder.originalMat.height();

        HashMap<Integer, PointF> corners = effects.corners;

        HashMap<Integer, PointF> cropbounds = new HashMap<>();

        for (int i = 0; i < 4; i++) {

            PointF pointF = corners.get(i);


            int x = (int) (pointF.x * scaleX);
            int y = (int) (pointF.y * scaleY);
            PointF pointF1 = new PointF(x, y);


            cropbounds.put(i, pointF1);
        }


        try {

            if (getImageDetails().getDocType() != null) {


                if (getImageDetails().getEffects(getImageDetails().getAt(position)) == null) {


                    String documentType = getImageDetails().getDocType().get(getImageDetails().getAt(position));

                    if (documentType != null) {

                        effects.color = MatFilter.colorCodeFromDocumentType(documentType);

                    }


                }


            }
        } catch (Exception e) {
        }


        effects.colorTune = MatFilter.getDefaultTune(effects.color);
        if (effects.color == MatFilter.COLOR_WHITEBOARD) {
            effects.isGray = true;
            setColorTuneListen(false);
            getBinding().colorGrayCheck.setChecked(effects.isGray);
            setColorTuneListen(true);
        }


        holder.polygonView.setPoints(cropbounds);
        holder.polygonView.requestLayout();

        holder.nextAction.setVisibility(View.VISIBLE);
        rotateImageView(0, effects.rotation, holder, false);

        getBinding().protector.setVisibility(View.GONE);
        holder.processing.setVisibility(View.GONE);

    }


    private void rotateImageView(int globalRotation, int rotateBy, ScanPreviewAdapter.ViewHolder holder, boolean animateTrue) {


        globalRotation += rotateBy;


        if (Math.abs(globalRotation) > 360)
            globalRotation = 90;


        if (globalRotation >= 90)
            holder.imageViewParent.setRotation(globalRotation - 90);




        double scale = 1.0;


        if (Math.abs(globalRotation) == 90 || Math.abs(globalRotation) == 270) {


            int maxWidth = holder.imageViewParent.getMeasuredWidth();
            int maxHeight = holder.imageViewParent.getMeasuredHeight();

            int currentWidth = holder.displayBitmap.getHeight();
            int currentHeight = holder.displayBitmap.getWidth();


            // try scaling width

            int newWidth = currentWidth * maxHeight / maxWidth;
            int newHeight = newWidth * currentHeight / currentWidth;


            if (newWidth > maxWidth) {
                newHeight = currentHeight * maxWidth / maxHeight;
                newWidth = newHeight * currentWidth / currentHeight;
            }

            scale = newHeight * 1.0 / newWidth;


        }




        holder.imageViewParent.setRotation(globalRotation);
        holder.imageViewParent.setScaleX((float) scale);
        holder.imageViewParent.setScaleY((float) scale);


    }

    @Override
    public void colorGrayChanged(ScanPreviewAdapter.ViewHolder holder, int position, boolean b) {

        getImageDetails().getEffects(getImageDetails().getAt(position)).isGray = b;
        prepareMat(holder, position);
        processImage(holder, position, true);
    }


    @Override
    public void colorTuneChanged(ScanPreviewAdapter.ViewHolder holder, int position, int progress) {

        getImageDetails().getEffects(getImageDetails().getAt(position)).colorTune = progress;
        prepareMat(holder, position);
        processImage(holder, position, true);
    }


    @Override
    public void rotate(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.rotate(holder, position);


        prepareMat(holder, position);


        if (getImageDetails().getEffects(getImageDetails().getAt(position)) == null) {

            File processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position));

            // but processed image exists

            if (processedImageFilepath.exists()) {


                crop(holder, position);
                return;
            }


        }

        rotateImageView((int) holder.imageViewParent.getRotation(), 90, holder, true);

        getImageDetails().getEffects(getImageDetails().getAt(position)).rotation += 90;

        if (getImageDetails().getEffects(getImageDetails().getAt(position)).rotation == 360)
            getImageDetails().getEffects(getImageDetails().getAt(position)).rotation = 0;

        processImage(holder, position, true);

    }
}