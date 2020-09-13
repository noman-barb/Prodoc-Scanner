package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.aaindia.prodocscanner.R;
import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.activity.ScanPreviewActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.databinding.ActivityScanViewBinding;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.GlobalConstants;
import com.aaindia.prodocscanner.utils.MatFilter;
import com.aaindia.prodocscanner.views.TouchableReyclerView;
import com.aaindia.prodocscanner.wrappers.Effects;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.xw.repo.BubbleSeekBar;

import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

public class ScanViewActivity extends AppCompatActivity implements View.OnClickListener, ScanPreviewAdapter.AdapterInterface {


    public static final String SCAN_DIR_PATH = "scan_dir_path";

    ActivityScanViewBinding binding;

    private String scanDirPath = null;
    private String activityCalledFromClassname = null;
    private ArrayList<String> originalFilepaths = null;
    private ScanPreviewAdapter adapter;


    private SavedImageDetails imageDetails;
    public boolean colorOnly = false;
    private File originalDirFile;
    private File processedDirFile;


    private HashSet<String> autoCropped = new HashSet<>();

    private boolean isChange = true;

    private boolean colorTuneListen = true;

    public void setColorTuneListen(boolean b) {
        colorTuneListen = b;
    }


    public boolean documentChanged() {
        return isChange;
    }

    public void setDocumentChanged(boolean isChange) {
        this.isChange = isChange;
    }

    public File getOriginalDirFile() {
        return originalDirFile;
    }


    public int wd = -1;
    public int ht = -1;

    private int colorTuneLastProgress = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_view);

        scanDirPath = (String) getIntent().getExtras().get(ScanPreviewActivity.SCAN_DIR_PATH);
        activityCalledFromClassname = (String) getIntent().getExtras().get(GlobalConstants.CLASS_NAME);


        adapter = new ScanPreviewAdapter(this, scanDirPath, this);


        loadInitialData();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));


        SnapHelper helper = new PagerSnapHelper();
        helper.attachToRecyclerView(binding.recyclerView);

        binding.recyclerView.setItemViewCacheSize(1);

        binding.recyclerView.setAdapter(adapter);


        binding.cropRL.setOnClickListener(this::onClick);
        binding.colorRL.setOnClickListener(this::onClick);
        binding.rotateRL.setOnClickListener(this::onClick);


        binding.shareSinglePageRL.setOnClickListener(this::onClick);
        binding.reorderRL.setOnClickListener(this);
        binding.deleteRL.setOnClickListener(this);
        binding.addPagesRL.setOnClickListener(this);
        binding.deleteRL.setOnClickListener(this::onClick);
        binding.saveRL.setOnClickListener(this::onClick);
        binding.shareRL.setOnClickListener(this::onClick);

        binding.exitRL.setOnClickListener(this);

        binding.renameRL.setOnClickListener(this);

        binding.view.setOnClickListener(this::onClick);

        binding.reorderRL.setOnClickListener(this::onClick);

        binding.colorTuneSK.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {


            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {


                if (colorTuneLastProgress == progress) {

                    return;
                }

                colorTuneLastProgress = progress;

                if (!colorTuneListen)
                    return;
                int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);


                colorTuneChanged(holder, position, progress);

                binding.colorTuneSK.setProgress(progress);


            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {


            }
        });


        binding.colorGrayCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


                if (!colorTuneListen)
                    return;


                int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);

                colorGrayChanged(holder, position, b);


            }
        });

        binding.recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                showHideColorRL(false);
                return false;
            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        loadInitialData();
        binding.recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();


        getBinding().recyclerView.getAdapter().notifyDataSetChanged();
    }


    public HashSet<String> getAutoCroppedSet() {
        return autoCropped;
    }

    public void loadInitialData() {

        originalDirFile = new File(scanDirPath, FileNav.ORIGINAL_IMAGE_DIR);
        processedDirFile = new File(scanDirPath, FileNav.PROCESSED_IMAGE_DIR);

        originalDirFile.mkdirs();
        processedDirFile.mkdirs();


        originalFilepaths = new ArrayList<>();
        imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDirPath));


        // check auto crop

        ProgressDialog pd = new ProgressDialog(ScanViewActivity.this);
        pd.setTitle("Just a moment");
        pd.setMessage("Detecting document edges");
        pd.setCancelable(false);


        new Thread(new Runnable() {
            @Override
            public void run() {

                LinkedList<String> linkedList = imageDetails.getOrdering();


                File[] originalFiles = originalDirFile.listFiles();


                ListIterator<String> listIterator = linkedList.listIterator();

                HashSet<String> set = new HashSet<>();

                while (listIterator.hasNext()) {

                    set.add(listIterator.next());
                }

                for (File f : originalFiles) {

                    if (!set.contains(f.getName())) {

                        linkedList.add(f.getName());
                    }

                }

                imageDetails.sync();


                listIterator = linkedList.listIterator();


                for (int i = 0; i < linkedList.size(); i++) {


                    int finalI = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.setMessage("Detecting document edges " + (finalI + 1) + "/" + linkedList.size());
                        }
                    });


                    String filename = linkedList.get(i);

                    String path = originalDirFile.getAbsolutePath() + File.separator + filename;

                    originalFilepaths.add(path);


                    if (imageDetails.getEffects(filename) == null) {


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                if (!pd.isShowing())
                                    pd.show();
                            }
                        });

                        autocropThis(path, imageDetails);
                    }

                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        imageDetails.sync();

                        if (pd.isShowing())
                            pd.dismiss();
                        adapter.scanDirName = getScanDirPath();
                        adapter.originalFilepaths = getOriginalFilepaths();
                        adapter.savedImageDetails = imageDetails;

                        binding.fileNameTV.setText(FileNav.getPDFName(getScanDirPath()).replace(".pdf", ""));

                        getBinding().recyclerView.getAdapter().notifyDataSetChanged();


                    }
                });


            }
        }).start();


    }

    public synchronized void autocropThis(String path, SavedImageDetails imageDetails) {
    }


    public boolean getColorOnly() {
        return colorOnly;
    }

    public void setColorOnly(boolean colorOnly) {
        this.colorOnly = colorOnly;
    }

    public TouchableReyclerView getRecyclerView() {
        return binding.recyclerView;
    }

    public ActivityScanViewBinding getBinding() {
        return binding;
    }

    public ArrayList<String> getOriginalFilepaths() {
        return originalFilepaths;
    }

    public void setOriginalFilepaths(ArrayList<String> filepaths) {
        this.originalFilepaths = filepaths;
    }

    public SavedImageDetails getImageDetails() {
        return imageDetails;
    }


    public String getScanDirPath() {
        return scanDirPath;
    }

    public void setScanDirPath(String path) {
        scanDirPath = path;
    }

    public void recyclerViewActivateDeact(boolean activate) {

        binding.recyclerView.setItemTouchable(activate);


    }


    public void processImage(ScanPreviewAdapter.ViewHolder holder, int position, boolean colorOnly) {
    }

    public void chooseColor(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void crop(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void rotate(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void shareSinglePage(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void addPages(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void deleteFile(ScanPreviewAdapter.ViewHolder holder, int position) {
    }

    public void save() {
    }

    public void share() {
    }


    public void rename() {

    }

    public void viewPDF() {

    }

    public void colorTuneChanged(ScanPreviewAdapter.ViewHolder holder, int position, int progress) {

    }

    public void colorGrayChanged(ScanPreviewAdapter.ViewHolder holder, int position, boolean b) {

    }

    public void reorder() {

    }

    public void setSpanActionColor(SpannableString s, int compare1, int compare2) {

        if (compare1 == compare2)
            s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSecondary)), 0, s.length(), 0);
    }


    @Override
    public void process(int postion, ScanPreviewAdapter.ViewHolder holder) {

        processImage(holder, postion, false);


    }

    @Override
    public void notProcessed(int postion, ScanPreviewAdapter.ViewHolder holder) {


        getBinding().protector.setVisibility(View.VISIBLE);

        if (!autoCropped.contains(getImageDetails().getAt(postion))) {


            if (wd == -1) {

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                crop(holder, postion);
                            }
                        });

                    }
                }, 350);

            } else {
                crop(holder, postion);
            }


        } else {

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            getBinding().protector.setVisibility(View.GONE);

                            if (holder != null && holder.itemView != null && holder.imageViewParent != null && holder.nextAction != null) {


                                holder.nextAction.setVisibility(View.GONE);
                                getRecyclerView().getAdapter().notifyDataSetChanged();
                            }


                        }
                    });
                }
            }, 1000);
        }


    }


    @Override
    public void onClick(View view) {


        int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
        ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);


        int id = view.getId();


        if (id != R.id.colorRL) {
            showHideColorRL(false);
        }

        if (id == R.id.cropRL) {
            crop(holder, position);
        } else if (id == R.id.colorRL) {
            chooseColor(holder, position);
        } else if (id == R.id.rotateRL) {
            rotate(holder, position);
        } else if (id == R.id.shareSinglePageRL) {
            shareSinglePage(holder, position);
        } else if (id == R.id.addPagesRL) {
            addPages(holder, position);
        } else if (id == R.id.deleteRL) {
            deleteFile(holder, position);
        } else if (id == R.id.shareRL) {
            share();
        } else if (id == R.id.saveRL) {
            save();
        } else if (id == R.id.exitRL) {
            finish();
        } else if (id == R.id.renameRL) {
            rename();
        } else if (id == R.id.view) {
            viewPDF();
        } else if (id == R.id.reorderRL) {
            reorder();
        }


    }

    public void showHideColorRL(boolean show) {

        if (show)
            binding.colorControlRL.setVisibility(View.VISIBLE);

        else {

            binding.colorControlRL.setVisibility(View.GONE);
        }


    }

    public void zoomageEnableDisable(ScanPreviewAdapter.ViewHolder holder, boolean enable) {


    }


}
