package com.aaindia.prodocscanner.wrappers;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.util.Observable;

public class UnifiedNativeAdObsevable extends Observable {

    private UnifiedNativeAd ad = null;


    public synchronized UnifiedNativeAd getAd() {
        return ad;
    }

    public synchronized void setAd(UnifiedNativeAd ad) {
        this.ad = ad;

        setChanged();
        notifyObservers();
    }
}
