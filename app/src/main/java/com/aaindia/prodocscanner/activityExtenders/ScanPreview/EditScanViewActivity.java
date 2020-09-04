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


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                prepareMats(holder, position);
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
                effects.colorTune = MatFilter.getDefaultTune(colorCode);

                processImage(holder, position, true);

                return true;
            }
        });

        popupMenu.show();


    }


    @Override
    public void crop(ScanPreviewAdapter.ViewHolder holder, int position) {


        recyclerViewActivateDeact(false);


        new Thread(new Runnable() {
            @Override
            public void run() {

                prepareMats(holder, position);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        holder.imageView.getLayoutParams().width = (int) getDisplayBitmap().getWidth();
                        holder.imageView.getLayoutParams().height = (int) getDisplayBitmap().getHeight();
                        holder.imageView.setImageBitmap(getDisplayBitmap());
                        holder.imageView.requestLayout();
                        initAutoCrop(position, holder);

                    }
                });


            }
        }).start();

    }


    private void initAutoCrop(int position, ScanPreviewAdapter.ViewHolder holder) {

        holder.polygonView.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();

        RelativeLayout.LayoutParams params1 = ViewUtils.copy(params);

        holder.polygonView.setLayoutParams(params1);
        holder.polygonView.getLayoutParams().height += holder.polygonView.ballSize;
        holder.polygonView.getLayoutParams().width += holder.polygonView.ballSize;
        holder.polygonView.requestLayout();


        Effects effects = getImageDetails().getEffects(new File(getOriginalFilepaths().get(position)).getName());

        if (effects == null) {

            MatOfPoint2f cropBoundsMat = new MatOfPoint2f();

            holder.processing.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {

                    MatFilter.cropV1(getDisplayMat().getNativeObjAddr(), cropBoundsMat.getNativeObjAddr());


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            holder.processing.setVisibility(View.GONE);


                            HashMap<Integer, PointF> cropBoundsMap = new HashMap<>();
                            Point[] sortedPoints = BitmapUtils.sortMatofPoints2f(cropBoundsMat);


                            for (int i = 0; i < 4; i++) {


                                if (sortedPoints[i] == null) {

                                    sortedPoints[i] = new Point();

                                    switch (i) {
                                        case 0:

                                            sortedPoints[0].x = 0;
                                            sortedPoints[0].y = 0;
                                            break;

                                        case 1:

                                            sortedPoints[1].x = 400;
                                            sortedPoints[1].y = 0;
                                            break;
                                        case 2:
                                            sortedPoints[2].x = 0;
                                            sortedPoints[2].y = 400;

                                            break;
                                        case 3:

                                            sortedPoints[3].x = 400;
                                            sortedPoints[3].y = 400;

                                            break;

                                    }

                                }

                                cropBoundsMap.put(i, new PointF((float) sortedPoints[i].x, (float) sortedPoints[i].y));
                            }

                            holder.polygonView.setPoints(cropBoundsMap);
                            holder.polygonView.requestLayout();


                            float widthScaleFactor = getOriginalMat().width() * 1.0f / getDisplayMat().width();
                            float heightScaleFactor = getOriginalMat().height() * 1.0f / getDisplayMat().height();


                            HashMap<Integer, PointF> cropBoundsOriginalMap = new HashMap<>();

                            for (int i = 0; i < 4; i++) {

                                PointF pointF = cropBoundsMap.get(i);


                                pointF.x *= widthScaleFactor;
                                pointF.y *= heightScaleFactor;

                                cropBoundsOriginalMap.put(i, new PointF(pointF.x, pointF.y));

                            }

                            Effects effects1 = new Effects(cropBoundsOriginalMap, MatFilter.DEFAULT_COLOR_CODE, false, 0, 0);

                            getImageDetails().putEffects(new File(getOriginalFilepaths().get(position)).getName(), effects1);
                            getImageDetails().sync();
                            holder.nextAction.setVisibility(View.VISIBLE);


                        }
                    });


                }
            }).start();


        } else {


            double scaleX = getDisplayMat().width() * 1.0 / getOriginalMat().width();
            double scaleY = getDisplayMat().height() * 1.0 / getOriginalMat().height();

            HashMap<Integer, PointF> corners = effects.corners;

            HashMap<Integer, PointF> cropbounds = new HashMap<>();

            for (int i = 0; i < 4; i++) {

                PointF pointF = corners.get(i);


                int x = (int) (pointF.x * scaleX);
                int y = (int) (pointF.y * scaleY);
                PointF pointF1 = new PointF(x, y);


                cropbounds.put(i, pointF1);
            }

            holder.polygonView.setPoints(cropbounds);
            holder.polygonView.requestLayout();

            holder.nextAction.setVisibility(View.VISIBLE);
            rotateImageView(0, effects.rotation, holder, false);
        }


    }

    private void rotateImageView(int globalRotation, int rotateBy, ScanPreviewAdapter.ViewHolder holder, boolean animateTrue) {

        animateTrue = false;

        globalRotation += rotateBy;

        //globalRotation = 0;

        if (Math.abs(globalRotation) > 360)
            globalRotation = 90;


        if (globalRotation >= 90)
            holder.imageViewParent.setRotation(globalRotation - 90);

        int width = holder.imageViewParent.getWidth();
        int height = holder.imageViewParent.getHeight();


        double scale = 1.0;


        if (Math.abs(globalRotation) == 90 || Math.abs(globalRotation) == 270) {


            int maxWidth = holder.imageViewParent.getMeasuredWidth();
            int maxHeight = holder.imageViewParent.getMeasuredHeight();

            int currentWidth = getDisplayMat().height();
            int currentHeight = getDisplayMat().width();


            // try scaling width

            int newWidth = currentWidth * maxHeight / maxWidth;
            int newHeight = newWidth * currentHeight / currentWidth;


            if (newWidth > maxWidth) {
                newHeight = currentHeight * maxWidth / maxHeight;
                newWidth = newHeight * currentWidth / currentHeight;
            }

            scale = newHeight * 1.0 / newWidth;


        }


        if (animateTrue) {
            ValueAnimator anim = ValueAnimator.ofFloat(holder.imageViewParent.getRotation(), globalRotation);


            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float val = (float) valueAnimator.getAnimatedValue();

                    holder.imageViewParent.setRotation(val);

                    if (val >= 350)
                        holder.imageViewParent.setRotation(0);


                }
            });

            anim.setInterpolator(new AccelerateInterpolator());
            anim.setDuration(300);
            anim.start();

        } else {
            holder.imageViewParent.setRotation(globalRotation);

        }
        holder.imageViewParent.setScaleX((float) scale);
        holder.imageViewParent.setScaleY((float) scale);


    }

    @Override
    public void colorGrayChanged(ScanPreviewAdapter.ViewHolder holder, int position, boolean b) {

        getImageDetails().getEffects(getImageDetails().getAt(position)).isGray = b;
        // prepareMats(holder, position);
        processImage(holder, position, true);
    }


    @Override
    public void colorTuneChanged(ScanPreviewAdapter.ViewHolder holder, int position, int progress) {

        getImageDetails().getEffects(getImageDetails().getAt(position)).colorTune = progress;
        // prepareMats(holder, position);
        processImage(holder, position, true);
    }


    @Override
    public void rotate(ScanPreviewAdapter.ViewHolder holder, int position) {
        super.rotate(holder, position);


        prepareMats(holder, position);


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
