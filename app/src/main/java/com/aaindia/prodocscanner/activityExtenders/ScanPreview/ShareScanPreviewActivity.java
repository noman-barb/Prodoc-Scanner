package com.aaindia.prodocscanner.activityExtenders.ScanPreview;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.aaindia.prodocscanner.adapters.ScanPreviewAdapter;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.utils.pdf.DocMaker;
import com.aaindia.prodocscanner.utils.share.ShareDialog;
import com.aaindia.prodocscanner.utils.Utils;
import com.aaindia.prodocscanner.utils.share.Sharer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

public class ShareScanPreviewActivity extends GridScanViewActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    @Override
    public void shareSinglePage(ScanPreviewAdapter.ViewHolder holder, int position, boolean isExport) {
        super.shareSinglePage(holder, position, isExport);

        getBottomMenu1().setState(BottomSheetBehavior.STATE_HIDDEN);

        Utils.copyNotProcessedOriginals(getImageDetails(), getScanDirPath());

        File processedFile = FileNav.getProcessedFileFromName(getScanDirPath(), new File(getOriginalFilepaths().get(position)).getName());

        ArrayList<File> files = new ArrayList<>();

        files.add(processedFile);

        shareFromArray(files, processedFile.length() / 1024.0, isExport);

    }

    @Override
    public void save() {


        share(true);
    }


    @Override
    public void share(boolean isExport) {


        Utils.copyNotProcessedOriginals(getImageDetails(), getScanDirPath());


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


    @Override
    public void shareFromArray(ArrayList<File> files, double size, boolean isExport) {

        ProgressDialog pd = new ProgressDialog(ShareScanPreviewActivity.this);
        pd.setTitle("Making PDF");
        pd.setMessage("Page 1/" + files.size());
        pd.setCancelable(false);


        new ShareDialog(this, (int) size, new ShareDialog.OnShareDialogListener() {
            @Override
            public void share(boolean isPDF, double quality) {


                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        prepareDoc(files, isPDF, quality, pd, isExport);

                    }
                }).start();

            }
        }).build(isExport).show();
    }


    private void prepareDoc(ArrayList<File> files, boolean isPDF, double quality, ProgressDialog pd, boolean export) {


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
                new DocMaker(ShareScanPreviewActivity.this, files, quality).make(outputFile.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
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
                                pd.dismiss();


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
                        pd.dismiss();
                    }
                });
            }
        } else {




            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.setTitle("Compressing Images");
                    pd.show();
                }
            });


            File outputDir = FileNav.getOutputDir(getScanDirPath());
            String pdfName = FileNav.getPDFName(getScanDirPath());

            File outputFile = new File(outputDir, pdfName);

            try {
                new DocMaker(ShareScanPreviewActivity.this, files, quality).makeImages(outputDir.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
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
                                pd.dismiss();


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
                        pd.dismiss();
                    }
                });
            }


        }
    }

    @Override
    public void viewPDF() {


        Utils.copyNotProcessedOriginals(getImageDetails(), getScanDirPath());


        ArrayList<File> files = new ArrayList<>();
        double size = 0;
        ListIterator listIterator = getImageDetails().getOrdering().listIterator();

        while (listIterator.hasNext()) {

            String s = getScanDirPath() + File.separator + FileNav.PROCESSED_IMAGE_DIR + File.separator + listIterator.next();

            File f = new File(s);
            size += f.length() / 1024.0;

            files.add(f);

        }

        ProgressDialog pd = new ProgressDialog(ShareScanPreviewActivity.this);
        pd.setTitle("Making PDF");
        pd.setMessage("Page 1/" + files.size());
        pd.setCancelable(false);


        new ShareDialog(this, (int) size, new ShareDialog.OnShareDialogListener() {
            @Override
            public void share(boolean isPDF, double quality) {


                new Thread(new Runnable() {
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
                            new DocMaker(ShareScanPreviewActivity.this, files, q).make(outputFile.getAbsolutePath(), new DocMaker.OnPDFMakerUpdate() {
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
                                            pd.dismiss();


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
                                    pd.dismiss();
                                }
                            });
                        }


                    }
                }).start();

            }
        }).typeView().build(false).show();


    }


}
