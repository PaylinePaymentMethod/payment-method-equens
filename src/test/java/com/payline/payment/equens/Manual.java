package com.payline.payment.equens;

import com.payline.payment.equens.bean.business.payment.PaymentInitiationRequest;
import com.payline.payment.equens.bean.business.payment.PaymentInitiationResponse;
import com.payline.payment.equens.bean.business.payment.PaymentStatusResponse;
import com.payline.payment.equens.bean.business.psu.Psu;
import com.payline.payment.equens.bean.business.psu.PsuCreateRequest;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;
import com.payline.payment.equens.utils.constant.ContractConfigurationKeys;
import com.payline.payment.equens.utils.constant.PartnerConfigurationKeys;
import com.payline.payment.equens.utils.http.PisHttpClient;
import com.payline.payment.equens.utils.http.PsuHttpClient;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.configuration.request.RetrievePluginConfigurationRequest;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.ContractProperty;
import com.payline.pmapi.logger.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * To run this manual test class, you need to send several system properties to the JVM :
 * project.certificateChainPath: the path of the local file containing the full certificate chain in PEM format
 * project.pkPath: the path of the local file containing the private key, exported following PKCS#8 standard, not encrypted, in PEM format
 * project.merchantIban: The test merchant IBAN
 * project.onboardingId: The onboarding ID of the test account
 *
 * This information being sensitive, it must not appear in the source code !
 */
public class Manual {

    private static final Logger LOGGER = LogManager.getLogger(Manual.class);
    private static PisHttpClient pisHttpClient = PisHttpClient.getInstance();
    private static PsuHttpClient psuHttpClient = PsuHttpClient.getInstance();

    public static void main( String[] args ){
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        try {
            // Manual test of ConfigurationServiceImpl#retrievePluginConfiguration
            RetrievePluginConfigurationRequest request = MockUtils.aRetrievePluginConfigurationRequestBuilder()
                    .withContractConfiguration( initContractConfiguration() )
                    .withPartnerConfiguration( initPartnerConfiguration() )
                    .build();
            String result = new ConfigurationServiceImpl().retrievePluginConfiguration( request );

            // Manual tests of the HTTP clients
            RequestConfiguration requestConfiguration = new RequestConfiguration(
                    initContractConfiguration(), MockUtils.anEnvironment(), initPartnerConfiguration());

            pisHttpClient.init( requestConfiguration.getPartnerConfiguration() );
            psuHttpClient.init( requestConfiguration.getPartnerConfiguration() );

            // GET aspsps
            GetAspspsResponse banks = pisHttpClient.getAspsps( requestConfiguration );

            // POST psu
            Psu psuCreated = psuHttpClient.createPsu( new PsuCreateRequest.PsuCreateRequestBuilder().build(), requestConfiguration );

            // POST payment

            PaymentInitiationRequest.PaymentInitiationRequestBuilder init = MockUtils.aPaymentInitiationRequestBuilder(MockUtils.getIbanFR());
            //init.withPsuId( psuCreated.getPsuId() );
            init.withPsuId( null );
            PaymentInitiationResponse paymentInitiationResponse = pisHttpClient.initPayment( init.build(), requestConfiguration );

            // GET payment
            PaymentStatusResponse paymentStatusResponse = pisHttpClient.paymentStatus( "666666", requestConfiguration, true );

            LOGGER.info("END");
        }
        catch( PluginException e ){
            LOGGER.error("PluginException: errorCode=\"{}\", failureCause={}", e.getErrorCode(), e.getFailureCause().toString());
            e.printStackTrace();
        }
        catch( Exception e ){
            e.printStackTrace();
        }
    }

    private static ContractConfiguration initContractConfiguration(){
        ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry());
        Map<String, ContractProperty> contractProperties = contractConfiguration.getContractProperties();

        contractProperties.put(ContractConfigurationKeys.MERCHANT_IBAN, new ContractProperty( System.getProperty("project.merchantIban") ));
        contractProperties.put(ContractConfigurationKeys.ONBOARDING_ID, new ContractProperty( System.getProperty("project.onboardingId") ));

        return contractConfiguration;
    }

    private static PartnerConfiguration initPartnerConfiguration() throws IOException {
        Map<String, String> partnerConfigurationMap = new HashMap<>();
        partnerConfigurationMap.put(PartnerConfigurationKeys.API_URL_TOKEN, "https://xs2a.awltest.de/xs2a/routingservice/services/authorize/token");
        partnerConfigurationMap.put(PartnerConfigurationKeys.API_URL_PIS_ASPSPS, "https://xs2a.awltest.de/xs2a/routingservice/services/directory/v1/aspsps?allDetails=true");
        partnerConfigurationMap.put(PartnerConfigurationKeys.API_URL_PIS_PAYMENTS, "https://xs2a.awltest.de/xs2a/routingservice/services/pis/v1/payments");
        partnerConfigurationMap.put(PartnerConfigurationKeys.API_URL_PIS_PAYMENTS_STATUS, "https://xs2a.awltest.de/xs2a/routingservice/services/pis/v1/payments/{paymentId}/status");
        partnerConfigurationMap.put(PartnerConfigurationKeys.API_URL_PSU_PSUS, "https://xs2a.awltest.de/xs2a/routingservice/services/psumgmt/v1/psus");
        partnerConfigurationMap.put( PartnerConfigurationKeys.PAYLINE_CLIENT_NAME, "MarketPay" );
        //partnerConfigurationMap.put( PartnerConfigurationKeys.PAYLINE_ONBOARDING_ID, System.getProperty("project.onboardingId") );
        partnerConfigurationMap.put( PartnerConfigurationKeys.PAYMENT_PRODUCT, "Instant" );

        Map<String, String> sensitiveConfigurationMap = new HashMap<>();
        sensitiveConfigurationMap.put( PartnerConfigurationKeys.CLIENT_CERTIFICATE, new String(Files.readAllBytes(Paths.get(System.getProperty("project.certificateChainPath")))) );
        sensitiveConfigurationMap.put( PartnerConfigurationKeys.CLIENT_PRIVATE_KEY, new String(Files.readAllBytes(Paths.get(System.getProperty("project.pkPath")))) );

        return new PartnerConfiguration( partnerConfigurationMap, sensitiveConfigurationMap );
    }

}
