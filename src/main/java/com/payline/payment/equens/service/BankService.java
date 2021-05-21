package com.payline.payment.equens.service;

import com.payline.payment.equens.bean.business.banks.BankAffiliation;
import com.payline.payment.equens.bean.business.banks.BanksAffiliation;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.business.BankBusiness;
import com.payline.payment.equens.business.impl.BankBusinessImpl;
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

    private BankBusiness bankBusiness = new BankBusinessImpl();

    /**
     * Retrieve Aspsp with aspspId given in parameter.
     * @param pluginConfiguration
     *      Plugin configuration.
     * @param aspspId
     *      aspsp identifier.
     * @return
     *      Aspsp object if found.
     */
    public Aspsp getAspsp(final String pluginConfiguration, final String aspspId) {
        final List<Aspsp> aspspList = jsonService.fromJson(pluginConfiguration, GetAspspsResponse.class).getAspsps();
        final List<Aspsp> resultList = new ArrayList<>();

        for (Aspsp aspsp: aspspList) {
            if (aspspId.equals(aspsp.getAspspId())) {
                resultList.add(aspsp);
            } else if (aspsp.getSubsidiariesList() != null) {
                resultList.addAll(aspsp.getSubsidiariesList()
                        .stream().filter(e -> e.getAspspId().equals(aspspId)).collect(Collectors.toList()));
            }
        }
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    /**
     * Method used to build Banks with subsidiariesList.
     * @param aspspsList
     *      List of Aspsps with subsidiairies.
     * @return
     *      Aspsp list wi
     *
     */
    public List<Aspsp> buildBanksWithAffiliation(List<Aspsp> aspspsList) {
        final Map<String, BankAffiliation> primaryBankList = fetchMotherBanks(BANK_AFFILIATION_PATH);
        final List<Aspsp> resultList = new ArrayList<>();
        final Map<String, Aspsp> primaryList = new HashMap<>();

        // On transforme les banques primaires en AspspId
        primaryBankList.forEach((k, v) -> {
            final Aspsp temp = bankBusiness.convertToAspsp(k, v);
            primaryList.put(temp.getBic(), temp);
        });

        // On compare la liste reçu par Equens si on trouve un bic qui commençe par
        //alors on l'ajoute à la liste des subsidiaries sinon on considère que c'est une banque primaire
        for (Aspsp aspsp : aspspsList) {
            if (!PluginUtils.isEmpty(aspsp.getBic()) && !PluginUtils.isEmptyList(aspsp.getName())) {
                final Aspsp motherBank = primaryList.get(bankBusiness.getPrefixBic(aspsp.getBic()));
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

    /**
     * This method parses the PluginConfiguration string to read the list of ASPSPs and convert it to a list of choices
     * for a select list. The key of each option is the AspspId and the value is "BIC - name".
     * PAYLAPMEXT-204: if BIC is null, the selection option's value will just be the name of the bank.
     * PAYLAPMEXT-203: filter the list using the countryCode (if provided) to keep only the banks which country code matches.
     *
     * @param pluginConfiguration The PluginConfiguration string
     * @param listCountryCode     List of 2-letters country code
     * @return The list of banks, as select options.
     */
    public List<Aspsp> fetchBanks(String pluginConfiguration, List<String> listCountryCode, String paymentMode) {
        final List<Aspsp> aspspList = new ArrayList<>();
        if (pluginConfiguration == null) {
            LOGGER.warn("pluginConfiguration is null");
        } else {
            final List<Aspsp> aspsps = jsonService.fromJson(pluginConfiguration, GetAspspsResponse.class).getAspsps();
            final List<Aspsp> validAspsps = fetchValidAspsp(aspsps, listCountryCode, paymentMode);
            aspspList.addAll(validAspsps);
        }
        return aspspList;
    }

    public boolean isIbanRequired(Aspsp aspsp) {
        return bankBusiness.isIbanRequired(aspsp);
    }

    public boolean isCompatibleBank(Aspsp aspsp, final String paymentMode) {
        return bankBusiness.isCompatibleBank(aspsp.getDetails(), paymentMode);
    }

    protected Map<String, BankAffiliation> fetchMotherBanks(String motherBankPath) {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(motherBankPath)) {
            if (input == null) {
                LOGGER.error("Unable to load file {}", motherBankPath);
                throw new PluginException("Plugin error: unable to load bank affiliation file");
            }

            final String bankContent = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));

            BanksAffiliation banksAffiliation = jsonService.fromJson(bankContent, BanksAffiliation.class);
            return banksAffiliation.getBanksOrganizationList();
        } catch (final IOException e) {
            throw new PluginException("Plugin error: unable to read the bank affiliation file", e);
        }
    }

    protected List<Aspsp> fetchValidAspsp(List<Aspsp> aspsps, List<String> listCountryCode, String paymentMode) {
        final List<Aspsp> validAspsp = aspsps.stream()
                .filter(e -> e.getCountryCode() != null)
                .filter(e -> listCountryCode.isEmpty() || listCountryCode.contains(e.getCountryCode()))
                .filter(e -> !PluginUtils.isEmpty(e.getBic()))
                .collect(Collectors.toList());

        final List<Aspsp> resultList = new ArrayList<>();
        for (final Aspsp aspsp : validAspsp) {
            if (PluginUtils.isEmptyList(aspsp.getSubsidiariesList()) && bankBusiness.isCompatibleBank(aspsp.getDetails(), paymentMode)) {
                resultList.add(aspsp);
            }
            if (!PluginUtils.isEmptyList(aspsp.getSubsidiariesList())) {
                final List<Aspsp> validSubsidiaires = fetchValidSubsidiaries(aspsp, paymentMode);
                if (!PluginUtils.isEmptyList(validSubsidiaires)) {
                    aspsp.setSubsidiariesList(validSubsidiaires);
                    resultList.add(aspsp);
                }
            }
        }
        return resultList;
    }

    /**
     * Return subsidiaires list availables for paymentMode and aspsp.
     * @param aspsp
     * @param paymentMode
     * @return
     */
    protected List<Aspsp> fetchValidSubsidiaries(final Aspsp aspsp, final String paymentMode) {
        final List<Aspsp> validSubsidiaires = new ArrayList<>();
        for (final Aspsp sub : aspsp.getSubsidiariesList()) {
            if (bankBusiness.isCompatibleBank(sub.getDetails(), paymentMode)) {
                validSubsidiaires.add(sub);
            }
        }
        return validSubsidiaires;
    }

}