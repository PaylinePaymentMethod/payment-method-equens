package com.payline.payment.equens.bean.business.reachdirectory;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Aspsp {

    @SerializedName("AspspId")
    private String aspspId;
    @SerializedName("BIC")
    private String bic;
    @SerializedName("CountryCode")
    private String countryCode;
    // Do not map Details because it would require another bean that we would not use anyway...
    @SerializedName("Name")
    private List<String> name;
    @SerializedName("Details")
    private List<Detail> details;

    private List<Aspsp> subsidiariesList;

}
