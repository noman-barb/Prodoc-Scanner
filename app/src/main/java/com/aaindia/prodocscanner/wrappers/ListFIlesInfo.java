package com.aaindia.prodocscanner.wrappers;

public class ListFIlesInfo {



    public String filepath, filename, thumbnailPath;
    public int numPages;
    public  boolean is_scan;
    public long dateModified;

    public boolean isSelected = false;

    public ListFIlesInfo(String filepath, String filename, long dateModified, int numPages, boolean is_scan, String thumbnailPath){

        this.filename  = filename;
        this.filepath = filepath;
        this.dateModified = dateModified;
        this.numPages = numPages;
        this.is_scan = is_scan;
        this.thumbnailPath = thumbnailPath;
    }
}
