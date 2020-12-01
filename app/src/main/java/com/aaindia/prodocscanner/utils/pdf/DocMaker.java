package com.aaindia.prodocscanner.utils.pdf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocMaker {


    private Activity context;
    private ArrayList<File> images;
    private double quality;

    private String password = null;

    public DocMaker(Activity context, ArrayList<File> images, double quality, String password) {

        this.context = context;
        this.images = images;
        this.quality = quality;
        this.password = password;
    }


    public void makeImages(String outputDir, OnPDFMakerUpdate update) {

        HashMap<Integer, File> map = new HashMap<>();

        double q = quality * 100;


        if (q > 39) {


            update.onComplete(images);

            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        for (File f : images) {

            executorService.execute(new Runnable() {
                @Override
                public void run() {


                   Mat m = Imgcodecs.imread(f.getAbsolutePath());


                   MatOfInt matOfInt = new MatOfInt(new int[]{Imgcodecs.IMWRITE_JPEG_QUALITY, (int) q});

                    String fname = outputDir + File.separator + images.indexOf(f) + ".jpg";
                    Imgcodecs.imwrite(fname, m, matOfInt);


                    map.put(images.indexOf(f), new File(fname));

                    update.onUpdate(map.size(), images.size());

                    m.release();
                    matOfInt.release();


                    if (map.size() == images.size()) {

                        ArrayList<File> output = new ArrayList<>();

                        for (int i = 0; i < map.size(); i++) {

                            output.add(map.get(i));
                        }


                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                executorService.shutdown();

                                while (! (executorService.isTerminated() || executorService.isShutdown() ) ) {
                                }

                                update.onComplete(output);
                            }
                        });

                    }
                }
            });

        }


    }

    public void make(String outputPath, OnPDFMakerUpdate update) throws IOException {


        PDDocument document = new PDDocument();


        for (int i = 0; i < images.size(); i++) {

            update.onUpdate(i + 1, images.size());

            Bitmap bitmap = BitmapFactory.decodeFile(images.get(i).getAbsolutePath());


            int pdfWidth = (int) PDRectangle.A4.getWidth();

            int pdfHeight = pdfWidth * bitmap.getHeight() / bitmap.getWidth();


            pdfHeight=(int) PDRectangle.A4.getHeight();

            pdfWidth = pdfHeight*bitmap.getWidth()/bitmap.getHeight();


            PDPage page = new PDPage(new PDRectangle(pdfWidth, pdfHeight));


            document.addPage(page);


            PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true, true);


            PDImageXObject ximage = JPEGFactory.createFromImage(document, bitmap, (float) quality, 72);

            contentStream.drawXObject(ximage, 0, 0, pdfWidth, pdfHeight);

            //


            contentStream.close();

            // document.importPage(page);

            bitmap.recycle();


        }


        if (password != null && password.length() > 0) {

            AccessPermission ap = new AccessPermission();


            StandardProtectionPolicy spp = new StandardProtectionPolicy
                    (password, password, ap);

            spp.setEncryptionKeyLength(128);

            spp.setPermissions(ap);
            document.protect(spp);

        }


        document.save(outputPath);
        document.close();

        update.onComplete(outputPath);

    }

    public interface OnPDFMakerUpdate {

        void onUpdate(int currentPage, int totalPage);

        void onComplete(String output);

        void onComplete(ArrayList<File> output);
    }
}