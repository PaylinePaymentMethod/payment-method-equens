package com.payline.payment.equens.service;

import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;

import java.util.List;
import java.util.stream.Collectors;

public class BankService {

    private static class Holder {
        private static final BankService instance = new BankService();
    }

    public static BankService getInstance() {
        return BankService.Holder.instance;
    }

    private JsonService jsonService = JsonService.getInstance();

    /**
     * Méthode permettant de récupérer un AspspId selon les plugins configurations passés en paramètre.
     * @param pluginConfiguration
     *      La liste des plugin configurations.
     * @param aspspId
     *      L'identifiant Aspsp recherché.
     * @return
     *      Aspsp l'ASPSP si trouvé null sinon.
     */
    public Aspsp fetchAspsp(final String pluginConfiguration, final String aspspId) {
        final List<Aspsp> aspspList = jsonService.fromJson(pluginConfiguration, GetAspspsResponse.class).getAspsps();
        final List<Aspsp> resultList = aspspList.stream().filter(e -> e.getAspspId().equals(aspspId)).collect(Collectors.toList());
        return resultList.isEmpty() ? null : resultList.get(0);
     }
}