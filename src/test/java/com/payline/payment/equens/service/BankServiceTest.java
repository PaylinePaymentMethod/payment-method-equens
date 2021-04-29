package com.payline.payment.equens.service;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.banks.BankAffiliation;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.Detail;
import com.payline.payment.equens.business.BankBusiness;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;
import com.payline.payment.equens.service.impl.PaymentFormConfigurationServiceImpl;
import com.payline.pmapi.bean.paymentform.bean.field.SelectOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class BankServiceTest {

    private BankService underTest = BankService.getInstance();

    private final String aspspsJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
            // FR - Normal|Instant
            "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"MOOBARBAZXX\"}," +
            // FR - Normal|Instant
            "{\"AspspId\":\"4321\",\"Name\":[\"another Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"FOOBARBA\"}," +
            // FR - Normal
            "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Normal\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"PSSTFRPP\"}," +
            // ES - Instant
            "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"BBVAESMM\"}," +
            // ES - Instant
            "{\"AspspId\":\"1602\",\"Name\":[\"Santander\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"ES140049\"}," +
            // IT - Instant
            "{\"AspspId\":\"1603\",\"Name\":[\"Santander\"],\"CountryCode\":\"IT\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"IT14004\"}," +
            // DE - Instant
            "{\"AspspId\":\"224\",\"CountryCode\":\"DE\",\"Name\":[\"08/15direkt\"],\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}]}" +
            "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

    @Nested
    class fetchMotherBanks {
        @Test
        void nominalCase() {
            final Map<String, BankAffiliation> bankAffiliationMap = (underTest.fetchMotherBanks(BankService.BANK_AFFILIATION_PATH));
            assertNotNull(bankAffiliationMap);
            assertEquals(6, bankAffiliationMap.size());
            final BankAffiliation bankAffiliation = bankAffiliationMap.get("Caisse d'Epargne");
            assertNotNull(bankAffiliation);
            assertEquals("FR", bankAffiliation.getCountry());
            assertEquals("CEPAFRPP", bankAffiliation.getPrefixBIC());
        }

        @Test
        void noFileFound() {
            assertThrows(PluginException.class, () -> underTest.fetchMotherBanks("invalidPath"));
        }

        @Test
        void withoutBank() {
            final Map<String, BankAffiliation> bankAffiliationMap = underTest.fetchMotherBanks("bank/Empty_BankAffiliation.json");
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
            final List<Aspsp> result = underTest.buildBanksWithAffiliation(aspspList);
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

    @Test
    void getBanks_aspspWithoutBic() {
        // @see https://payline.atlassian.net/browse/PAYLAPMEXT-204
        // @see https://payline.atlassian.net/browse/PAYLAPMEXT-219

        // when: calling getBanks method
        final List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.GERMANY.getCountry());
        final List<Aspsp> result = underTest.fetchBanks(aspspsJson, listCountry, ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode());

        // then: the aspsp is ignered because there is no BIC
        assertTrue(result.isEmpty());
    }

    @Test
    void getBanks_filterAspspByCountryCode() {
        // @see: https://payline.atlassian.net/browse/PAYLAPMEXT-203

        // when: calling getBanks method
        final List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        final List<Aspsp> result = underTest.fetchBanks(aspspsJson, listCountry, ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode());

        // then: there is only 1 bank choice at the end
        assertEquals(2, result.size());
    }

    @Test
    void getBanks_filterAspspByMultipleCountryCode() {
        // @see: https://payline.atlassian.net/browse/PAYLAPMEXT-203

        // when: calling getBanks method
        final List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        listCountry.add("ES");
        final List<Aspsp> result = underTest.fetchBanks(aspspsJson, listCountry,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode());

        // then: there is 2 banks choice at the end
        assertEquals(4, result.size());
    }

    @Nested
    class fetchValidSubsidiaries {
        @Test
        void withValidSub() {
            final Detail detail = new Detail("POST /payments", "PaymentProduct", "SUPPORTED", "Normal", "");
            final Aspsp aspsp = MockUtils.anAspsp();
            final Aspsp sub = MockUtils.anAspsp();
            sub.setDetails(Collections.singletonList(detail));
            aspsp.setSubsidiariesList(Collections.singletonList(sub));
            final List<Aspsp> result = underTest.fetchValidSubsidiaries(aspsp, "Normal");
            assertFalse(underTest.fetchValidSubsidiaries(aspsp, "Normal").isEmpty());
            assertEquals(sub, result.get(0));
        }

        @Test
        void withInvalidSub() {
            final Detail detail = new Detail("POST /payments", "PaymentProduct", "SUPPORTED", "Instant", "");
            final Aspsp aspsp = MockUtils.anAspsp();
            final Aspsp sub = MockUtils.anAspsp();
            sub.setDetails(Collections.singletonList(detail));
            aspsp.setSubsidiariesList(Collections.singletonList(sub));
            assertTrue(underTest.fetchValidSubsidiaries(aspsp, "Normal").isEmpty());
        }

    }

    @Nested
    class getAspsps {

        private String pluginConfiguration;

        @BeforeEach
        public void load() {
            try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("test_plugin_configuration.json")) {
                if (input == null) {
                    fail("Impossible de charger le fichier de test des apspspId");
                }
                pluginConfiguration = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                        .collect(Collectors.joining("\n"));
            } catch (IOException exception) {
                fail("Impossible de charger les plugins configuration pour les tests");
            }
        }

        @Test
        void withPrimaryBank() {
            final Aspsp aspsp = underTest.getAspsp(pluginConfiguration, "1410");
            assertEquals("AXA Banque", aspsp.getName().get(0));
            assertEquals("AXABFRPP", aspsp.getBic());
            assertEquals("1410", aspsp.getAspspId());
            assertEquals("FR", aspsp.getCountryCode());
        }

        @Test
        void withSubsidiaryBank() {
            final Aspsp aspsp = underTest.getAspsp(pluginConfiguration, "6514");
            assertEquals("Crédit Agricole Alpes-Provence", aspsp.getName().get(0));
            assertEquals("AGRIFRPP813", aspsp.getBic());
            assertEquals("6514", aspsp.getAspspId());
            assertEquals("FR", aspsp.getCountryCode());
        }

        @Test
        void noBankFound() {
            assertNull(underTest.getAspsp(pluginConfiguration, "0000"));
        }
    }



}