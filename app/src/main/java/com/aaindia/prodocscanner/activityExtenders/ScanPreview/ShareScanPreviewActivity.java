package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aaindia.prodocscanner.activity.MainActivity;
import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.pdf.DocMaker;
import com.aaindia.prodocscanner.utils.share.ShareDialog;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.utils.share.Sharer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.spongycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;

public class ShareScanPreviewActivity extends GridScanViewActivity {





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    private void requestStoragePermission(int perm) {


        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {


            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Permission Needed");
            builder.setMessage("Storage permission is required to export");
            builder.setCancelable(false);


            builder.setPositiveButton("OK", (dialog, which) -> {


                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, perm);

                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    //
                }
            });


            builder.create();
            builder.show();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, perm);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    if (requestCode == PERMISION_REQUEST_CODE_SINGLE_PAGE) {


                        int position = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findFirstVisibleItemPosition();


                        ScanPreviewAdapter.ViewHolder holder = (ScanPreviewAdapter.ViewHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);

                        if (position > -1 && holder != null) {

                            shareSinglePage(holder, position, true);

                        }


                    } else if (requestCode == PERMISION_REQUEST_CODE_ALL) {


                        share(true);
                    }


                }
            });

        } else {

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Permission not granted");
            builder.setMessage("Cannot export as permission to write to external storage was denied");

            builder.setCancelable(false);

            builder.setPositiveButton("OK", (dialog, which) -> {


                try {
                    dialog.dismiss();
                } catch (Exception e) {

                }
            });


            builder.create();
            builder.show();
        }

    }


    @Override
    public void shareSinglePage(ScanPreviewAdapter.ViewHolder holder, int position, boolean isExport) {
        super.shareSinglePage(holder, position, isExport);




        Bundle bundle = new Bundle();

        getFirebaseInstance().logEvent("share_single_is_export_"+isExport, bundle);



        if (isExport) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {


                } else {
                    requestStoragePermission(PERMISION_REQUEST_CODE_SINGLE_PAGE);
                    return;
                }

            }


        }


        getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);


        ProgressDialog pd1 = new ProgressDialog(ShareScanPreviewActivity.this);
        pd1.setTitle("Please wait");
        pd1.setMessage("Processing uncropped images");
        pd1.setCancelable(false);


        HashSet<String> set = new HashSet<>(1);

        set.add(getImageDetails().getAt(position));


        final boolean[] notProcessedAvailable = {false};
        executorService2.execute(new Runnable() {
            @Override
            public void run() {

                Utils.copyNotProcessedOriginals(set, getImageDetails(), getScanDirPath(), new Utils.OnUpdateCopy() {
                    @Override
                    public void showDialog() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notProcessedAvailable[0] = true;
                                try {

                                    if (!pd1.isShowing())
                                        pd1.show();
                                } catch (Exception e) {
                                } catch (Error e1) {
                                }
                            }
                        });
                    }
                });


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (notProcessedAvailable[0]){
                            binding.recyclerView.getAdapter().notifyDataSetChanged();
                        }

                        pd1.dismiss();

                        File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), new File(getOriginalFilepaths().get(position)).getName());

                        ArrayList<File> files = new ArrayList<>();

                        files.add(processedFile);

                        shareFromArray(files, processedFile.length() / 1024.0, isExport);


                    }
                });

            }
        });


    }

    @Override
    public void save() {


        share(true);
    }


    @Override
    public void share(boolean isExport) {



        Bundle bundle = new Bundle();

        bundle.putString(FirebaseAnalytics.Param.QUANTITY, getImageDetails().getOrdering().size()+"");
        getFirebaseInstance().logEvent("share_doc_is_export_"+isExport, bundle);


        if (isExport) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {




                } else {


                    requestStoragePermission(PERMISION_REQUEST_CODE_ALL);
                    return;
                }

            }


        }


        ProgressDialog pd1 = new ProgressDialog(ShareScanPreviewActivity.this);
        pd1.setTitle("Please wait");
        pd1.setMessage("Processing uncropped images");
        pd1.setCancelable(false);


        HashSet<String> set = new HashSet<>(getImageDetails().getOrdering().size());

        set.addAll(getImageDetails().getOrdering());


        executorService2.execute(new Runnable() {
            @Override
            public void run() {


                Utils.copyNotProcessedOriginals(set, getImageDetails(), getScanDirPath(), new Utils.OnUpdateCopy() {
                    @Override
                    public void showDialog() {


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    if (!pd1.isShowing())
                                        pd1.show();
                                } catch (Exception e) {
                                } catch (Error e2) {
                                }

                            }
                        });

                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        pd1.dismiss();

                        ArrayList<File> files = new ArrayList<>();
                        double size = 0;
                        ListIterator listIterator = getImageDetails().getOrdering().listIterator();

                        while (listIterator.hasNext()) {

                            String s = getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + listIterator.next();

                            File f = new File(s);
                            size += f.length() / 1024.0;

                            files.add(f);

                        }

                        shareFromArray(files, size, isExport);
                    }
                });

            }
        });


    }


    @Override
    public void shareFromArray(ArrayList<File> files, double size, boolean isExport) {

        ProgressDialog pd = new ProgressDialog(ShareScanPreviewActivity.this);
        pd.setTitle("Making PDF");
        pd.setMessage("Page 1/" + files.size());
        pd.setCancelable(false);


        new ShareDialog(this, (int) size,new ShareDialog.OnShareDialogListener() {
            @Override
            public void share(boolean isPDF, double quality, String password) {


                executorService2.execute(new Runnable() {
                    @Override
                    public void run() {

                        prepareDoc(files, isPDF, quality, pd, isExport, password);

                    }
                });

            }
        }).build(isExport).show();
    }


    private void prepareDoc(ArrayList<File> files, boolean isPDF, double quality, ProgressDialog pd, boolean export, String passowrd) {


        quality = (quality) / 200.0;

        if (isPDF) {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.setTitle("Making PDF");
                    pd.show();


                }
            });


            File outputDir = FileNav.getOutputDir(getScanDirPath());
            String pdfName = FileNav.getPDFName(getScanDirPath());

            File outputFile = new File(outputDir, pdfName);

            try {
                new DocMaker(ShareScanPreviewActivity.this, files, quality, passowrd).make(outputFile.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
                    @Override
                    public void onUpdate(int currentPage, int totalPage) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.setMessage("Page " + currentPage + "/" + totalPage);
                            }
                        });
                    }

                    @Override
                    public void onComplete(String output) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    pd.dismiss();
                                } catch (Exception e) {

                                }


                                setPDFOutputPathExport(output);

                                ArrayList<File> arrayList = new ArrayList<>();

                                arrayList.add(new File(output));

                                if (!export)
                                    new Sharer(ShareScanPreviewActivity.this, arrayList).share();
                                else
                                    new Sharer(ShareScanPreviewActivity.this, arrayList).exportPDF(pdfName);
                            }
                        });

                    }

                    @Override
                    public void onComplete(ArrayList<File> output) {

                    }
                });
            } catch (IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        try {
                            pd.dismiss();
                        } catch (Exception ex) {

                        }


                    }
                });
            }
        } else {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.setTitle("Saving Images");
                    pd.show();
                }
            });


            File outputDir = FileNav.getOutputDir(getScanDirPath());
            String pdfName = FileNav.getPDFName(getScanDirPath());

            File outputFile = new File(outputDir, pdfName);

            try {
                new DocMaker(ShareScanPreviewActivity.this, files, quality, passowrd).makeImages(outputDir.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
                    @Override
                    public void onUpdate(int currentPage, int totalPage) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.setMessage("Page " + currentPage + "/" + totalPage);
                            }
                        });
                    }

                    @Override
                    public void onComplete(String output) {

                    }

                    @Override
                    public void onComplete(ArrayList<File> output) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    pd.dismiss();
                                } catch (Exception e) {

                                }


                                if (!export)
                                    new Sharer(ShareScanPreviewActivity.this, output).share();

                                else
                                    new Sharer(ShareScanPreviewActivity.this, output).saveImagesToGallery(pdfName.substring(0, pdfName.length() - 3));
                            }
                        });

                    }
                });
            } catch (Exception e) {


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                        } catch (Exception ex) {

                        }


                    }
                });
            }


        }
    }

    @Override
    public void viewPDF() {


        ProgressDialog pd1 = new ProgressDialog(ShareScanPreviewActivity.this);
        pd1.setTitle("Please wait");
        pd1.setMessage("Processing uncropped images");
        pd1.setCancelable(false);


        ArrayList<File> files = new ArrayList<>(getImageDetails().getOrdering().size());
        HashSet<String> fileSet = new HashSet<>(getImageDetails().getOrdering().size());
        double size = 0;
        ListIterator listIterator = getImageDetails().getOrdering().listIterator();

        while (listIterator.hasNext()) {

            String name = (String) listIterator.next();

            fileSet.add(name);


            String s = getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + name;

            File f = new File(s);
            size += f.length() / 1024.0;

            files.add(f);

        }


        double finalSize = size;
        executorService2.execute(new Runnable() {
            @Override
            public void run() {


                final boolean[] notProcessedAvailable = {false};

                Utils.copyNotProcessedOriginals(fileSet, getImageDetails(), getScanDirPath(), new Utils.OnUpdateCopy() {
                    @Override
                    public void showDialog() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notProcessedAvailable[0] = true;
                                try {
                                    if (!pd1.isShowing())
                                        pd1.show();
                                } catch (Exception e) {
                                } catch (Error e2) {
                                }
                            }
                        });

                    }
                });


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        if (notProcessedAvailable[0]){
                            binding.recyclerView.getAdapter().notifyDataSetChanged();
                        }

                        pd1.dismiss();


                        ProgressDialog pd = new ProgressDialog(ShareScanPreviewActivity.this);
                        pd.setTitle("Making PDF");
                        pd.setMessage("Page 1/" + files.size());
                        pd.setCancelable(false);


                        new ShareDialog(ShareScanPreviewActivity.this, (int) finalSize ,new ShareDialog.OnShareDialogListener() {
                            @Override
                            public void share(boolean isPDF, double quality, String password) {


                                executorService2.execute(new Runnable() {
                                    @Override
                                    public void run() {


                                        double q = (quality) / 200.0;

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pd.setTitle("Making PDF");
                                                pd.show();
                                            }
                                        });


                                        File outputDir = FileNav.getOutputDir(getScanDirPath());
                                        String pdfName = FileNav.getPDFName(getScanDirPath());

                                        File outputFile = new File(outputDir, pdfName);


                                        try {
                                            new DocMaker(ShareScanPreviewActivity.this, files, q, password).make(outputFile.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
                                                @Override
                                                public void onUpdate(int currentPage, int totalPage) {

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            pd.setMessage("Page " + currentPage + "/" + totalPage);
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onComplete(String output) {

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                pd.dismiss();
                                                            } catch (Exception e) {

                                                            }


                                                            Intent intent = new Intent(Intent.ACTION_VIEW);

                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                                                                File file = new File(output);
                                                                Uri uri = FileProvider.getUriForFile(ShareScanPreviewActivity.this, getPackageName() + ".provider", file);

                                                                intent.setDataAndType(uri, "application/pdf");

                                                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                                            } else {

                                                                intent.setDataAndType(Uri.parse(pdfName), "application/pdf");
                                                                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(output));
                                                            }

                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                                                            try {
                                                                startActivity(intent);
                                                            } catch (Exception e) {

                                                                Toast.makeText(getApplicationContext(), "No PDF viewer app found", Toast.LENGTH_SHORT).show();
                                                            }

                                                        }
                                                    });

                                                }

                                                @Override
                                                public void onComplete(ArrayList<File> output) {

                                                }
                                            });
                                        } catch (IOException e) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        pd.dismiss();
                                                    } catch (Exception ex) {

                                                    }
                                                }
                                            });
                                        }


                                    }
                                });

                            }
                        }).typeView().build(false).show();


                    }
                });

            }
        });


    }


}
