package com.aaindia.prodocscanner.wrappers;

import android.graphics.PointF;
import android.util.Log;

import com.google.gson.Gson;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class SavedImageDetails {


    public static final int DOCUMENT_TYPE_KEY = 1;

    private LinkedList<String> order = null;
    private HashMap<String, Effects> imageEffects = null;

    private HashMap<String, String> docType = null;

    private File storageLocation = null;


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


        } catch (FileNotFoundException e) {

            order = new LinkedList<>();
            imageEffects = new HashMap<>();
            docType = new HashMap<>();


        } catch (IOException e) {
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


    public boolean sync() {

        boolean done = false;
        try {
            FileWriter fileWriter = new FileWriter(storageLocation);
            Gson gson = new Gson();

            gson.toJson(this, fileWriter);
            fileWriter.close();
            done = true;


        } catch (IOException e) {


            e.printStackTrace();
        }


        return done;
    }


}
