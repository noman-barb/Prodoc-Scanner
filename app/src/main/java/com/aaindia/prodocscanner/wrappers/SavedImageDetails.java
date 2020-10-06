package com.aaindia.prodocscanner.wrappers;

import android.graphics.PointF;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class SavedImageDetails {

    public static final String BACKUP_FILE = "effects_backup.json";

    public static final int DOCUMENT_TYPE_KEY = 1;

    private LinkedList<String> order = null;
    private HashMap<String, Effects> imageEffects = null;

    private HashMap<String, String> docType = null;

    public File storageLocation = null;


    public HashMap<String, String> getDocType() {
        return docType;
    }


    public SavedImageDetails(File imageDetailsFile) {

        //Type type = new TypeToken<Map<String, String>>(){}.getType();

        this.storageLocation = imageDetailsFile;


        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(imageDetailsFile);

            SavedImageDetails imageDetails = gson.fromJson(reader, this.getClass());
            reader.close();


            if (imageDetails.imageEffects == null) {
                imageDetails.imageEffects = new HashMap<>();

            } else {
                this.imageEffects = imageDetails.imageEffects;
            }


            if (imageDetails.docType == null) {
                this.docType = new HashMap<>();
            } else {
                this.docType = imageDetails.docType;
            }

            if (imageDetails.order == null) {
                order = new LinkedList<>();
            } else {
                this.order = imageDetails.order;
            }


            if (this.imageEffects == null) {
                this.imageEffects = new HashMap<>();
            }

            if (this.docType == null) {
                this.docType = new HashMap<>();
            }

            if (this.order == null) {
                this.order = new LinkedList<>();
            }


        } catch (Exception e1) {


            // try again

            this.storageLocation = imageDetailsFile;


            try {


                FileUtils.copyFile(new File(this.storageLocation.getParent() + File.separator + BACKUP_FILE), this.storageLocation);


                Gson gson = new Gson();
                FileReader reader = new FileReader(imageDetailsFile);

                SavedImageDetails imageDetails = gson.fromJson(reader, this.getClass());
                reader.close();


                if (imageDetails.imageEffects == null) {
                    imageDetails.imageEffects = new HashMap<>();

                } else {
                    this.imageEffects = imageDetails.imageEffects;
                }


                if (imageDetails.docType == null) {
                    this.docType = new HashMap<>();
                } else {
                    this.docType = imageDetails.docType;
                }

                if (imageDetails.order == null) {
                    this.order = new LinkedList<>();
                } else {
                    this.order = imageDetails.order;
                }


                if (this.imageEffects == null) {
                    this.imageEffects = new HashMap<>();
                }

                if (this.docType == null) {
                    this.docType = new HashMap<>();
                }

                if (this.order == null) {
                    this.order = new LinkedList<>();
                }


            } catch (Exception e2) {


                this.order = new LinkedList<>();
                this.imageEffects = new HashMap<>();
                this.docType = new HashMap<>();

            }


        }

    }


    public LinkedList<String> getOrdering() {
        return order;
    }

    public void removePage(int position) {


        Effects effects = imageEffects.get(order.get(position));

        if (effects != null) {

            imageEffects.remove(order.get(position));
        }


        if (order.get(position) != null)
            order.remove(position);

    }

    public void setOrdering(LinkedList<String> ordering) {
        this.order = ordering;
    }


    public void putEffects(String filepath, Effects effects) {

        imageEffects.put(filepath, effects);

    }


    public Effects getEffects(String filename) {

        return imageEffects.get(filename);
    }


    public int getOrder(String filename) {

        return order.indexOf(filename);
    }


    public int getColor(String filename) {

        if (imageEffects.containsKey(filename))

            return imageEffects.get(filename).color;

        return -1;
    }

    public int getRotation(String filename) {

        if (imageEffects.containsKey(filename))
            return imageEffects.get(filename).rotation;

        return -1;
    }


    public boolean getIsGray(String filename) {

        if (imageEffects.containsKey(filename))
            return imageEffects.get(filename).isGray;

        return false;
    }


    public HashMap<Integer, PointF> getCorners(String filename) {

        if (imageEffects.containsKey(filename))
            return imageEffects.get(filename).corners;

        return null;
    }

    public String getAt(int position) {


        return order.get(position);
    }


    public void setOrder(String filename, int position) {

        order.add(position, filename);
    }


    public synchronized boolean sync() {

        boolean done = false;


        try {


            try {

                File backupLoc = new File(storageLocation.getParent() + File.separator + BACKUP_FILE);
                FileWriter fileWriter = new FileWriter(backupLoc);
                Gson gson = new Gson();

                gson.toJson(this, fileWriter);
                fileWriter.close();


                // make sure the file isnt corrupted
                Gson gson2 = new Gson();
                FileReader reader2 = new FileReader(backupLoc);

                SavedImageDetails imageDetails = gson.fromJson(reader2, this.getClass());
                reader2.close();


                if (imageDetails.imageEffects == null || imageDetails.order == null || imageDetails.docType == null) {
                }


                // write to original


                FileUtils.copyFile(backupLoc, storageLocation); // the only window where original can get corrupted
                // the code has reached this line which means backup is ready


                done = true;


            } catch (IOException e) {


                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


        return done;
    }


}
