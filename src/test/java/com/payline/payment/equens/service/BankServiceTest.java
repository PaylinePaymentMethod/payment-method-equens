package com.payline.payment.equens.service;

import com.payline.payment.equens.bean.business.banks.BankAffiliation;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.PluginUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {

    BankService bankService = BankService.getInstance();

    @Nested
    class fetchMotherBanks {
        @Test
        void nominalCase() {
            final Map<String, BankAffiliation> bankAffiliationMap = (bankService.fetchMotherBanks(BankService.BANK_AFFILIATION_PATH));
            assertNotNull(bankAffiliationMap);
            assertEquals(6, bankAffiliationMap.size());
            final BankAffiliation bankAffiliation = bankAffiliationMap.get("Caisse d'Epargne");
            assertNotNull(bankAffiliation);
            assertEquals("FR", bankAffiliation.getCountry());
            assertEquals("CEPAFRPP", bankAffiliation.getPrefixBIC());
        }

        @Test
        void noFileFound() {
            assertThrows(PluginException.class, () -> bankService.fetchMotherBanks("invalidPath"));
        }

        @Test
        void withoutBank() {
            final Map<String, BankAffiliation> bankAffiliationMap = bankService.fetchMotherBanks("bank/Empty_BankAffiliation.json");
            assertEquals(0, bankAffiliationMap.size());
        }
    }
    @Nested
    class buildBankWithAffiliation{

        @Test
        void buildBanksWithAffiliation() {
            final List<Aspsp> aspspList = new ArrayList<>();
            aspspList.add(buildAspsp("PSSTFRPPBOR", "La Banque Postale"));
            aspspList.add(buildAspsp(null, "Banque incomplete"));
            aspspList.add(buildAspsp("PSSTFRPPBO1", null));
            aspspList.add(buildAspsp("AGRIFRPPXX2", "Credit Agricole Paris"));
            aspspList.add(buildAspsp("AGRIFRPPXX3", "Credit Agricole Alpes Provence"));
            aspspList.add(buildAspsp("AGRIFRPPXX1", "Credit Agricole Alpes Maritimes"));
            final List<Aspsp> result = bankService.buildBanksWithAffiliation(aspspList);
            assertEquals(2, result.size());

            final Aspsp bank1 = result.get(0);
            final Aspsp bank2 = result.get(1);

            assertEquals("Credit Agricole", bank1.getName().get(0));
            assertEquals("AGRIFRPP", bank1.getBic());

            final List<Aspsp> subsidiariesList = bank1.getSubsidiariesList();
            assertEquals(Collections.singletonList("Credit Agricole Alpes Maritimes"), subsidiariesList.get(0).getName());
            assertEquals(Collections.singletonList("Credit Agricole Alpes Provence"), subsidiariesList.get(1).getName());
            assertEquals(Collections.singletonList("Credit Agricole Paris"),subsidiariesList.get(2).getName());
            assertEquals("AGRIFRPPXX1", subsidiariesList.get(0).getBic());
            assertEquals("AGRIFRPPXX3", subsidiariesList.get(1).getBic());
            assertEquals("AGRIFRPPXX2", subsidiariesList.get(2).getBic());

            assertEquals("La Banque Postale", bank2.getName().get(0));
            assertEquals("PSSTFRPPBOR", bank2.getBic());


        }

        private Aspsp buildAspsp(final String bic, final String name) {
            final Aspsp aspsp = new Aspsp();
            aspsp.setBic(bic);
            aspsp.setCountryCode("FR");
            aspsp.setName(name != null ? Collections.singletonList(name): new ArrayList<>());
            return aspsp;
        }
    }

}