package com.payline.payment.equens.bean.business.payment;

import com.google.gson.annotations.SerializedName;

public enum PaymentStatus {

    @SerializedName(value = "Open", alternate = {"OPEN"})
    OPEN,
    @SerializedName(value = "Authorised", alternate = {"AUTHORISED"})
    AUTHORISED,
    @SerializedName(value = "Settlement_in_process", alternate = {"SETTLEMENT_IN_PROCESS"})
    SETTLEMENT_IN_PROCESS,
    @SerializedName(value = "Settlement_completed", alternate = {"SETTLEMENT_COMPLETED"})
    SETTLEMENT_COMPLETED,
    @SerializedName(value = "Cancelled", alternate = {"CANCELLED"})
    CANCELLED,
    @SerializedName(value = "Error", alternate = {"ERROR"})
    ERROR,
    @SerializedName(value = "Expired", alternate = {"EXPIRED"})
    EXPIRED,
    @SerializedName(value = "Pending", alternate = {"PENDING"})
    PENDING


}
