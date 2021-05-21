package com.payline.payment.equens.bean.business.banks;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Bank affiliation parameters.
 */
@Getter
@Setter
@AllArgsConstructor
public class BankAffiliation {

    private String prefixBIC;

    private String country;

}

