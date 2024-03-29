package com.payline.payment.equens.bean.business.payment;

import com.google.gson.annotations.SerializedName;

/**
 * Response obtained from the payment initiation request.
 */
public class PaymentInitiationResponse {

    /** Id generated by the TPP solution. This should be used to refer to this payment in subsequent api calls. */
    @SerializedName("PaymentId")
    private String paymentId;
    /** Payment status */
    @SerializedName("PaymentStatus")
    private PaymentStatus paymentStatus;

    /**
     * URL to be used by the Initiating Party to redirect the PSU towards the ASPSP.
     */
    @SerializedName("Links")
    private AspspLinks links;

   PaymentInitiationResponse(){
    }

    public String getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public AspspLinks getLinks() {
        return links;
    }
}
