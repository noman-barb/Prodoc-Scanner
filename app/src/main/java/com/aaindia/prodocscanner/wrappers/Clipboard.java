package com.aaindia.prodocscanner.wrappers;

import java.util.ArrayList;

public class Clipboard{


    public ArrayList<String> filepaths;
    public boolean deleteAfter = false;
    public Clipboard(ArrayList<String> filepaths, boolean deleteAfter ){

        this.filepaths = filepaths;
        this.deleteAfter = deleteAfter;
    }
}