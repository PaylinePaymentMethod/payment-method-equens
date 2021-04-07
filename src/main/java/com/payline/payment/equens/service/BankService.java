package com.payline.payment.equens.service;

import com.payline.payment.equens.bean.business.banks.BankAffiliation;
import com.payline.payment.equens.bean.business.banks.BanksAffiliation;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.pmapi.logger.LogManager;
import org.apache.logging.log4j.Logger;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;

import java.util.stream.Collectors;

public class BankService {

    protected static final String BANK_AFFILIATION_PATH = "bank/Bank_Affiliation_file.json";

    private static class Holder {
        private static final BankService instance = new BankService();
    }

    public static BankService getInstance() {
        return BankService.Holder.instance;
    }

    private static final Logger LOGGER = LogManager.getLogger(BankService.class);

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

    public List<Aspsp> buildBanksWithAffiliation(List<Aspsp> aspspsList) {
        final Map<String, BankAffiliation> primaryBankList = fetchMotherBanks(BANK_AFFILIATION_PATH);
        final List<Aspsp> resultList = new ArrayList<>();
        final Map<String, Aspsp> primaryList = new HashMap<>();

        // On transforme les banques primaires en AspspId
        primaryBankList.forEach((k, v) -> {
            final Aspsp temp = convertToAspsp(k, v);
            primaryList.put(temp.getBic(), temp);
        });

        // On compare la liste reçu par Equens si on trouve un bic qui commençe par
        //alors on l'ajoute à la liste des subsidiaries sinon on considère que c'est une banque primaire
        for (Aspsp aspsp : aspspsList) {
            if (!PluginUtils.isEmpty(aspsp.getBic()) && !PluginUtils.isEmptyList(aspsp.getName())) {
                final Aspsp motherBank = primaryList.get(getPrefixBic(aspsp.getBic()));
                if (motherBank == null) {
                    resultList.add(aspsp);
                } else {
                    List<Aspsp> subsidiariesList = motherBank.getSubsidiariesList();
                    if (subsidiariesList == null) {
                        subsidiariesList = new ArrayList<>();
                    }
                    subsidiariesList.add(aspsp);
                    subsidiariesList.sort(Comparator.comparing(e -> e.getName().get(0)));
                    motherBank.setSubsidiariesList(subsidiariesList);
                }
            }
        }
        //On ajoute les maisons mères qui ont au moins une filiale et on les tri
        //par nom.
        resultList.addAll(primaryList.values()
                .stream()
                .filter(e -> !PluginUtils.isEmptyList(e.getSubsidiariesList()))
                .collect(Collectors.toList()));

        //On tri les résultats par nom.
        resultList.sort(Comparator.comparing(e -> e.getName().get(0)));
        return resultList;
    }

    protected Map<String, BankAffiliation> fetchMotherBanks(String motherBankPath) {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(motherBankPath)) {
            if (input == null) {
                LOGGER.error("Unable to load file {}", motherBankPath);
                throw new PluginException("Plugin error: unable to load bank affiliation file");
            }

            String bankContent = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));

            BanksAffiliation banksAffiliation = jsonService.fromJson(bankContent, BanksAffiliation.class);
            return banksAffiliation.getBanksOrganizationList();
        } catch (final IOException e) {
            throw new PluginException("Plugin error: unable to read the logo", e);
        }
    }

    private String getPrefixBic(String bic) {
        return bic.length() >= 8 ? bic.substring(0,8) : bic;
    }

    private Aspsp convertToAspsp(final String label, final BankAffiliation bankAffiliation) {
        final Aspsp aspsp = new Aspsp();
        aspsp.setBic(bankAffiliation.getPrefixBIC());
        aspsp.setCountryCode(bankAffiliation.getCountry());
        aspsp.setName(Collections.singletonList(label));
        return aspsp;
    }


}