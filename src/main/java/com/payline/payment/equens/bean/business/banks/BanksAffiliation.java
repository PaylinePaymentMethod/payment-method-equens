package com.payline.payment.equens.bean.business.banks;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class BanksAffiliation {

    @SerializedName("BankOrganizationsList")
    private Map<String, BankAffiliation> banksOrganizationList;

}
