package com.payline.payment.equens.business.impl;

import com.payline.payment.equens.bean.business.banks.BankAffiliation;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.Detail;
import com.payline.payment.equens.business.BankBusiness;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankBusinessImpl implements BankBusiness {

    private static final String PAYMENT_PRODUCT_FIELD_NAME = "PaymentProduct";
    private static final String SUPPORTED_TYPE = "SUPPORTED";
    private static final String POST_PAYMENTS_API = "POST /payments";

    /**
     * Check if a bank is compatible with payment mode given in parameter .
     * see PAYLAPMEXT-294
     *
     * @param details
     *      Aspsp details.
     * @return true if aspsp is compatible else false.
     */
    @Override
    public boolean isCompatibleBank(final List<Detail> details, final String paymentMode) {
        final Map<String, Boolean> compatibilityMap = new HashMap<>();
        for (ConfigurationServiceImpl.PaymentProduct product : ConfigurationServiceImpl.PaymentProduct.values()) {
            compatibilityMap.put(product.getPaymentProductCode(), product.getSupportedByDefault());
        }

        if (details != null) {
            for (Detail detail : details) {
                if (PAYMENT_PRODUCT_FIELD_NAME.equals(detail.getFieldName())
                        && SUPPORTED_TYPE.equals(detail.getType())
                        && POST_PAYMENTS_API.equals(detail.getApi())) {
                    for (ConfigurationServiceImpl.PaymentProduct product : ConfigurationServiceImpl.PaymentProduct.values()) {
                        compatibilityMap.put(product.getPaymentProductCode(), detail.getValue().contains(product.getPaymentProductCode()));
                    }
                }
            }
        }
        return compatibilityMap.get(paymentMode);
    }

    @Override
    public String getPrefixBic(String bic) {
        return bic.length() >= 8 ? bic.substring(0,8) : bic;
    }
    @Override
    public Aspsp convertToAspsp(final String label, final BankAffiliation bankAffiliation) {
        final Aspsp aspsp = new Aspsp();
        aspsp.setBic(bankAffiliation.getPrefixBIC());
        aspsp.setCountryCode(bankAffiliation.getCountry());
        aspsp.setName(Collections.singletonList(label));
        return aspsp;
    }

}
