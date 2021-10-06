package com.payline.payment.equens.bean.business.payment;

import com.google.gson.annotations.SerializedName;

/**
 * Classe contenant les liens pour un Aspsp donnée (redirection...).
 */
public class AspspLinks {

    @SerializedName("AspspRedirectUrl")
    private Link aspspRedirectUrl;

    public Link getAspspRedirectUrl() {
        return aspspRedirectUrl;
    }
}
