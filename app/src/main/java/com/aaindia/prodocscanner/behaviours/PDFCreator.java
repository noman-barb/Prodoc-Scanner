package com.aaindia.prodocscanner.behaviours;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.collection.SimpleArrayMap;

import com.aaindia.prodocscanner.utils.BitmapUtils;
import com.aaindia.prodocscanner.utils.FileNav;
import com.aaindia.prodocscanner.wrappers.SavedImageDetails;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class PDFCreator {


    public static int ERROR_CODE_NO_IMAGE = 0;
    public static int ERROR_CODE_NO_ERROR = -1;
    public static int ERROR_CODE_INTURPPTED = 1;

    public static int QUALITY_FULL = 100;

    public static int QUALITY_MEDIUM = 30;


    public static int QUALITY_LOW = 8;


    Activity activity;
    String scanDirPath;
    String outputPath;
    int quality = 100;
    boolean documentChanged = true;

    OnCompleteListener onCompleteListener = null;

    Thread thread = null;

    boolean stop = false;


    public void setDocumentChanged(boolean documentChanged) {
        this.documentChanged = documentChanged;
    }

    public PDFCreator(Activity activity, String scanDirPath, String outputPath) {
        this.activity = activity;
        this.scanDirPath = scanDirPath;
        this.outputPath = outputPath;


    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }


    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void create(boolean newThread) {


        if (onCompleteListener == null) {
            onCompleteListener = new OnCompleteListener() {
                @Override
                public void onComplete(int error) {

                }

                @Override
                public void onProgress(int page, int total) {

                }
            };
        }

//        if (!documentChanged) {
//            onCompleteListener.onComplete(ERROR_CODE_NO_ERROR);
//            return;
//        }

        if (newThread) {

            thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        makeDocument();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        } else {
            try {
                makeDocument();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    private void makeDocument() throws IOException {


        PDDocument document = new PDDocument();


        File processed = new File(scanDirPath + File.separator + FileNav.PROCESSED_IMAGE_DIR);


        if (processed.listFiles().length == 0) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onCompleteListener.onComplete(ERROR_CODE_NO_IMAGE);
                }
            });
            return;
        }


        SavedImageDetails imageDetails = new SavedImageDetails(FileNav.getEffectsFile(scanDirPath));

        LinkedList<String> imagesPaths = imageDetails.getOrdering();


        for (int i = 0; i < imagesPaths.size(); i++) {


            if (stop) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCompleteListener.onComplete(ERROR_CODE_INTURPPTED);
                    }
                });
                return;
            }

            int finalI = i;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onCompleteListener.onProgress(finalI + 1, imagesPaths.size());
                }
            });


            Bitmap bitmap = BitmapFactory.decodeFile(
                    FileNav.getProcessedFileFromName(scanDirPath, imagesPaths.get(i)).getAbsolutePath());


            int pdfWidth = (int) PDRectangle.A4.getWidth();

            int pdfHeight = pdfWidth * bitmap.getHeight() / bitmap.getWidth();

            PDPage page = new PDPage(new PDRectangle(pdfWidth, pdfHeight));


            document.addPage(page);


            PDPageContentStream contentStream = new PDPageContentStream(document, page);


            PDImageXObject ximage = JPEGFactory.createFromImage(document, bitmap, quality / 250.0f, 300);


            contentStream.drawXObject(ximage, 0, 0, pdfWidth, pdfHeight);

            contentStream.close();

            bitmap.recycle();


        }

        document.save(outputPath);
        document.close();


        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCompleteListener.onComplete(ERROR_CODE_NO_ERROR);
            }
        });

    }


    public interface OnCompleteListener {


        void onComplete(int error);

        void onProgress(int page, int total);
    }

}
