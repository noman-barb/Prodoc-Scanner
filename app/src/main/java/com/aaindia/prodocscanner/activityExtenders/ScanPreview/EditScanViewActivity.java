package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.PopupMenu;

import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.utils.ViewUtils;
import com.aaindia.prodocscanner.views.PolygonView;
import com.aaindia.prodocscanner.wrappers.Effects;


import java.io.File;
import java.util.HashMap;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;


public class EditScanViewActivity extends ProcessScanViewActivity {


    int colorTune = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void chooseColor(ScanPreviewAdapter.ViewHolder holder, int position) {


        if (holder.isBusy) {


            new GuideView.Builder(EditScanViewActivity.this)
                    .setTitle("Crop")
                    .setContentSpan((Spannable) Html.fromHtml("<b>Crop</b> the image at first."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(holder.processImage)
                    .build().show();

            return;


        }

        if (getImageDetails().getEffects(getImageDetails().getAt(position)) == null) {

            File processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position));

            // but processed image exists

            if (processedImageFilepath.exists()) {


                crop(holder, position, false);
                return;
            }


        }


        executorService2.execute(new Runnable() {
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
        getBinding().colorTuneSK.setValue(colorTune);


        PopupMenu popupMenu = new PopupMenu(this, binding.colorRL);

        Menu menu = popupMenu.getMenu();

        SpannableString original = new SpannableString("Orignal");
        SpannableString contrast = new SpannableString("Photo");
        SpannableString paperStyle = new SpannableString("Note");
        SpannableString whiteBoardStyle = new SpannableString("Document");
        SpannableString textStyle = new SpannableString("Text");

        setSpanActionColor(original, colorCode, MatFilter.COLOR_ORIGINAL);
        setSpanActionColor(contrast, colorCode, MatFilter.COLOR_CONTRAST);
        setSpanActionColor(paperStyle, colorCode, MatFilter.COLOR_PAPER);
        setSpanActionColor(whiteBoardStyle, colorCode, MatFilter.COLOR_WHITEBOARD);
        setSpanActionColor(textStyle, colorCode, MatFilter.COLOR_TEXT);

        menu.add(0, MatFilter.COLOR_ORIGINAL, 0, original);
        menu.add(0, MatFilter.COLOR_CONTRAST, 0, contrast);
        menu.add(0, MatFilter.COLOR_PAPER, 0, paperStyle);
        menu.add(0, MatFilter.COLOR_WHITEBOARD, 0, whiteBoardStyle);
        //  menu.add(0, MatFilter.COLOR_TEXT, 0, textStyle );

        boolean finalColorGray = colorGray;
        setColorTuneListen(true);


        boolean finalColorGray1 = colorGray;
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {







                int id = item.getItemId();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.protector.setVisibility(View.VISIBLE);
                    }
                });
                isProcessing = true;

                int colorCode = id;

                int colorTune = MatFilter.getDefaultTune(id);

                binding.colorTuneSK.setValue(colorTune);
                binding.colorGrayCheck.setChecked(finalColorGray);

                Effects effects = getImageDetails().getEffects(getImageDetails().getAt(position));

                effects.color = colorCode;


                effects.colorTune = MatFilter.getDefaultTune(colorCode);

                processImage(holder, position, true);

                return true;
            }
        });

        popupMenu.show();


    }

    private Bitmap bitmapTemp = null;

    @Override
    public void crop(ScanPreviewAdapter.ViewHolder holder, int position, boolean noCrop) {


        holder.isBusy = true;


        zoomageEnableDisable(holder, false);

        holder.processing.setVisibility(View.VISIBLE);
        recyclerViewActivateDeact(false);
        getBinding().protector.setVisibility(View.VISIBLE);

        executorService2.execute(new Runnable() {
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


                        holder.polygonView.pointMove = new PolygonView.OnPointMove() {
                            @Override
                            public void onMove(double x, double y) {


                                x = x - 40;
                                y = y - 40;

                                int tempX, tempY;
                                binding.roi1IV.setVisibility(View.GONE);
                                if (bitmapTemp != null) {
                                    bitmapTemp.recycle();
                                }

                                if (x >= 0 && x < (holder.displayBitmap.getWidth() - 80) && y >= 0 && y < (holder.displayBitmap.getHeight() - 80)) {


                                    bitmapTemp = Bitmap.createBitmap(holder.displayBitmap, (int) x, (int) y, 80, 80);

                                } else {

                                    bitmapTemp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);

                                    for (int i = (int) x; i < (int) x + 80; i++) {


                                        if (i >= 0 && i < holder.displayBitmap.getWidth()) {
                                            for (int j = (int) y; j < (int) y + 80; j++) {


                                                if (j >= 0 && j < holder.displayBitmap.getHeight()) {

                                                    tempX = (int) (i - x);
                                                    tempY = (int) (j - y);

                                                    if (tempX <= 79 && tempX <= 79)
                                                        bitmapTemp.setPixel(tempX, tempY, holder.displayBitmap.getPixel(i, j));

                                                }

                                            }
                                        }
                                    }
                                }

                                binding.roi1IV.setImageBitmap(bitmapTemp);

                                binding.roi1IV.setVisibility(View.VISIBLE);
                                binding.roi1IV.setRotation(holder.imageViewParent.getRotation());

                            }

                            @Override
                            public void onStop() {

                                ValueAnimator animator = ValueAnimator.ofFloat(1, 0);

                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        float val = (float) valueAnimator.getAnimatedValue();
                                        binding.roitRoot.setAlpha(val);
                                        if (val == 0) {
                                            binding.roitRoot.setVisibility(View.GONE);
                                        }
                                    }
                                });

                                animator.setDuration(200);
                                animator.start();

                            }

                            @Override
                            public void onStart() {

                                binding.roitRoot.setVisibility(View.VISIBLE);

                                ValueAnimator animator = ValueAnimator.ofFloat(0, 1);

                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        float val = (float) valueAnimator.getAnimatedValue();
                                        binding.roitRoot.setAlpha(val);
                                    }
                                });

                                animator.setDuration(200);
                                animator.start();

                            }
                        };

                        initCrop(position, holder, noCrop);

                    }
                });


            }
        });

    }


    boolean nullEffectTempFixEnable = true;

    private void initCrop(int position, ScanPreviewAdapter.ViewHolder holder, boolean noCrop) {

        holder.polygonView.setVisibility(View.VISIBLE);

        getBinding().protector.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();

        RelativeLayout.LayoutParams params1 = ViewUtils.copy(params);

        holder.polygonView.setLayoutParams(params1);
        holder.polygonView.getLayoutParams().height += holder.polygonView.ballSize;
        holder.polygonView.getLayoutParams().width += holder.polygonView.ballSize;
        holder.polygonView.requestLayout();


        Effects effects = getImageDetails().getEffects(new File(getOriginalFilepaths().get(position)).getName());

        if (nullEffectTempFixEnable) {
            if (effects == null || effects.corners == null) {

                nullEffectTempFixEnable = false;
                loadInitialData();
                return;
            }
        }


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

                String imageFilename = new File(getOriginalFilepaths().get(position)).getName();

                File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), imageFilename);

                if (!processedFile.exists()) {


                    String documentType = getImageDetails().getDocType().get(getImageDetails().getAt(position));

                    if (documentType != null) {

                        effects.color = MatFilter.colorCodeFromDocumentType(documentType);

                    }


                }


            }
        } catch (Exception e) {
        }


        effects.colorTune = MatFilter.getDefaultTune(effects.color);


        if (noCrop) {

            cropbounds.put(0, new PointF(0, 0));
            cropbounds.put(1, new PointF(holder.displayBitmap.getWidth(), 0));
            cropbounds.put(2, new PointF(0, holder.displayBitmap.getHeight()));
            cropbounds.put(3, new PointF(holder.displayBitmap.getWidth(), holder.displayBitmap.getHeight()));
        }

        holder.polygonView.setPoints(cropbounds);
        holder.polygonView.requestLayout();


        rotateImageView(0, effects.rotation, holder, false);


        holder.nextAction.setVisibility(View.VISIBLE);
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


            int maxWidth = (int) (holder.imageViewParent.getMeasuredWidth() * 0.93);
            int maxHeight = (int) (holder.imageViewParent.getMeasuredHeight() * 0.93);

            int currentWidth = holder.displayBitmap.getHeight();
            int currentHeight = holder.displayBitmap.getWidth();

            int newWidth = maxWidth;

            int newHeight = (int) (currentHeight * (newWidth) * 1.0 / currentWidth);

            if (newHeight > maxHeight) {
                //scale height

                newHeight = maxHeight;
                newWidth = (int) (currentWidth * 1.0 * (newHeight / currentHeight));

            }


            scale = newHeight * 1.0f / currentHeight * 1.0f;


        }


        holder.imageViewParent.setRotation(globalRotation);

        holder.imageViewParent.setScaleX((float) scale);
        holder.imageViewParent.setScaleY((float) scale);


    }

    @Override
    public void colorGrayChanged(ScanPreviewAdapter.ViewHolder holder, int position, boolean b) {

        if (holder.isBusy) {


            new GuideView.Builder(EditScanViewActivity.this)
                    .setTitle("Crop")
                    .setContentSpan((Spannable) Html.fromHtml("<b>Crop</b> the image at first."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(holder.processImage)
                    .build().show();

            return;


        }

        getImageDetails().getEffects(getImageDetails().getAt(position)).isGray = b;
        prepareMat(holder, position);
        processImage(holder, position, true);
    }


    @Override
    public void colorTuneChanged(ScanPreviewAdapter.ViewHolder holder, int position, int progress) {


        if (holder.isBusy) {


            new GuideView.Builder(EditScanViewActivity.this)
                    .setTitle("Crop")
                    .setContentSpan((Spannable) Html.fromHtml("<b>Crop</b> the image at first."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(holder.processImage)
                    .build().show();

            return;


        }


        getImageDetails().getEffects(getImageDetails().getAt(position)).colorTune = progress;
        prepareMat(holder, position);
        processImage(holder, position, true);
    }


    @Override
    public void rotate(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.rotate(holder, position);


        if (holder.isBusy) {


            new GuideView.Builder(EditScanViewActivity.this)
                    .setTitle("Crop")
                    .setContentSpan((Spannable) Html.fromHtml("<b>Crop</b> the image at first."))
                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(holder.processImage)
                    .build().show();

            return;


        }

        isProcessing = true;


        prepareMat(holder, position);


        if (getImageDetails().getEffects(getImageDetails().getAt(position)) == null) {

            File processedImageFilepath = FileNav.getProcessedFileFromName(getScanDirPath(), getImageDetails().getOrdering().get(position));

            // but processed image exists

            if (processedImageFilepath.exists()) {


                crop(holder, position, false);
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