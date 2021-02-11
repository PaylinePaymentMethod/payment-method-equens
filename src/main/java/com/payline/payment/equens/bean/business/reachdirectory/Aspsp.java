package com.payline.payment.equens.bean.business.reachdirectory;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Aspsp {

    @SerializedName("AspspId")
    private String aspspId;
    @SerializedName("BIC")
    private String bic;
    @SerializedName("CountryCode")
    private String countryCode;
    @SerializedName("Name")
    private List<String> name;
    @SerializedName("Details")
    private List<Detail> details;

    public String getAspspId() {
        return aspspId;
    }

    public String getBic() {
        return bic;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public List<String> getName() {
        return name;
    }

    public List<Detail> getDetails() {
        return details;
    }
}
