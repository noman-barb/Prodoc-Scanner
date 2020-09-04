package com.aaindia.prodocscanner.constants;

public class Constants {

    public static final String SHARE_SINGLE_PAGE_MSG = "Shared from ProDoc Scanner";

    public static String singlePageShareMessage(int position){
        return SHARE_SINGLE_PAGE_MSG + ". Page "+ position;
    }
}
