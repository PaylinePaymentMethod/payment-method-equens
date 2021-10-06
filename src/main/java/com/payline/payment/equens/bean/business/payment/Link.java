package com.payline.payment.equens.bean.business.payment;

import com.google.gson.annotations.SerializedName;

import java.net.URL;

/**
 * Standard representation of link.
 */
public class Link {

    @SerializedName("Href")
    private URL href;

    public URL getHref() {
        return href;
    }
}
