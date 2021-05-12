package com.payline.payment.equens.business;

import com.payline.payment.equens.bean.business.banks.BankAffiliation;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.Detail;

import java.util.List;

public interface BankBusiness {

    boolean isCompatibleBank(List<Detail> details, String paymentMode);

    String getPrefixBic(String bic);

    Aspsp convertToAspsp(String label, BankAffiliation bankAffiliation);

    boolean isIbanRequired(Aspsp aspsp);
}
