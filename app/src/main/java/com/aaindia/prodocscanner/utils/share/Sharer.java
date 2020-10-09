package com.aaindia.prodocscanner.utils.share;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.aaindia.prodocscanner.activity.ScanPreviewActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.UUID;

public class Sharer {


    ArrayList<Uri> uris;
    Activity context;
    ArrayList<File> files;

    public Sharer(Activity context, ArrayList<File> files) {

        this.context = context;
        this.files = files;

        uris = new ArrayList<>();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {


            Uri u;
            for (File f : files) {
                u = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName() + ".provider", f);

                uris.add(u);
            }

        } else {


            for (File f : files) {
                uris.add(Uri.parse(f.getAbsolutePath()));
            }
        }

    }


    public void share() {

        if (uris.size() == 1) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            //      intent.setData(uris.get(0));
            intent.setType("*/*");
            // intent.putExtra(Intent.EXTRA_TEXT, "Scanned using ProDoc Scanner");

            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            }


            context.startActivity(Intent.createChooser(intent, "Share"));


        } else if (uris.size() > 1) {


            Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);

            share.setType("*/*");
            //  share.putExtra(Intent.EXTRA_TEXT, "Scanned using ProDoc Scanner");
            //  share.putExtra(Intent.EXTRA_SUBJECT, "Scans from Prodoc Scanner");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            }

            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            context.startActivity(Intent.createChooser(share, "Share"));


        }

    }


    public void saveImagesToGallery(String filename) {

        ;
        for (File f : files) {

            try {


                MediaStore.Images.Media.insertImage(context.getContentResolver(), f.getAbsolutePath(), filename + " " + System.currentTimeMillis() + ".jpg", "Prodoc Scanner Images");
            } catch (Exception e) {


            }
        }

        Toast.makeText(context, "Saved to pictures", Toast.LENGTH_SHORT).show();
    }

    public void exportPDF(String filename) {


        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.putExtra(Intent.EXTRA_TITLE, filename);
        intent.setType("application/pdf");


        context.startActivityForResult(intent, ScanPreviewActivity.EXPORT_TO_DEVICE_CODE);


    }
}
