package com.aaindia.prodocscanner.wrappers;

public class UserSettingsCaptureImage{



    public static final int FLASH_AUTO = 0;
    public static final int FLASH_TORCH = 1;
    public static final int FLASH_ON = 2;
    public static final int FLASH_OFF = 3;

    public static final int SCAN_MODE_BATCH = 0;
    public static final int SCAN_MODE_SINGLE = 1;

    public static final int GRID_ON = 0;
    public static final int GRID_OFF = 1;

    public int flash, scanMode, grid;


    public UserSettingsCaptureImage(int flash, int scanMode, int grid){

        this.flash = flash;
        this.scanMode = scanMode;
        this.grid = grid;


    }


}