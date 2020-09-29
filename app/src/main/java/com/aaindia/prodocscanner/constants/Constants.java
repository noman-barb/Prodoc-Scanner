package com.aaindia.prodocscanner.constants;

import java.util.ArrayList;

public class Constants {


    public static final String OCR_MODELS = "http://static.awessamapps.com/prodoc/ocr-train-data.json";
    public static final String SHARE_SINGLE_PAGE_MSG = "Shared from ProDoc Scanner";

    public static String singlePageShareMessage(int position) {
        return SHARE_SINGLE_PAGE_MSG + ". Page " + position;
    }


    public static final String DOCUMENT_TYPE_NOTE = "NOTE";

    public static final String DOCUMENT_TYPE_DOCUMENT = "DOCUMENT";

    public static final String DOCUMENT_TYPE_PHOTO = "PHOTO";
    public static final String DEFAULT_DOCUMENT_TYPE = DOCUMENT_TYPE_DOCUMENT;


    private static ArrayList<String> documentImageTypesData = null;

    public static ArrayList<String> documentImageTypes() {


        if (documentImageTypesData != null)
            return documentImageTypesData;


        documentImageTypesData = new ArrayList<>();

        documentImageTypesData.add(DOCUMENT_TYPE_NOTE);
        documentImageTypesData.add(DOCUMENT_TYPE_DOCUMENT);
        documentImageTypesData.add(DOCUMENT_TYPE_PHOTO);

        return documentImageTypesData;

    }
}
