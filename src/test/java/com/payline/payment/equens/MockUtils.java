package com.payline.payment.equens;

import com.payline.payment.equens.bean.business.fraud.PsuSessionInformation;
import com.payline.payment.equens.bean.business.payment.*;
import com.payline.payment.equens.bean.business.psu.Psu;
import com.payline.payment.equens.bean.business.psu.PsuCreateRequest;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.TestUtils;
import com.payline.payment.equens.utils.http.Authorization;
import com.payline.payment.equens.utils.security.RSAHolder;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.bean.configuration.request.RetrievePluginConfigurationRequest;
import com.payline.pmapi.bean.payment.*;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.request.RedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.request.TransactionStatusRequest;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.request.PaymentFormLogoRequest;
import org.tomitribe.auth.signatures.Algorithm;
import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Signer;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class that generates mocks of frequently used objects.
 */
public class MockUtils {

    /**
     * Generate a valid accountInfo, an attribute of a {@link ContractParametersCheckRequest} instance.
     */
    public static Map<String, String> anAccountInfo(){
        return anAccountInfo( aContractConfiguration() );
    }

    /**
     * Generate a valid accountInfo, an attribute of a {@link ContractParametersCheckRequest} instance,
     * from the given {@link ContractConfiguration}.
     *
     * @param contractConfiguration The model object from which the properties will be copied
     */
    public static Map<String, String> anAccountInfo( ContractConfiguration contractConfiguration ){
        Map<String, String> accountInfo = new HashMap<>();
        for( Map.Entry<String, ContractProperty> entry : contractConfiguration.getContractProperties().entrySet() ){
            accountInfo.put(entry.getKey(), entry.getValue().getValue());
        }
        return accountInfo;
    }

    /**
     * Generate a valid {@link Authorization}.
     */
    public static Authorization anAuthorization(){
        return anAuthorizationBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link Authorization}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static Authorization.AuthorizationBuilder anAuthorizationBuilder(){
        return new Authorization.AuthorizationBuilder()
                .withAccessToken("ABCD1234567890")
                .withTokenType("Bearer")
                .withExpiresAt( TestUtils.addTime(new Date(), Calendar.HOUR, 1) );
    }

    /**
     * Generate a valid {@link Browser}.
     */
    public static Browser aBrowser(){
        return Browser.BrowserBuilder.aBrowser()
                .withLocale( Locale.getDefault() )
                .withIp( "192.168.0.1" )
                .withUserAgent( aUserAgent() )
                .build();
    }

    /**
     * Generate a valid {@link Buyer}.
     */
    public static Buyer aBuyer(){
        return Buyer.BuyerBuilder.aBuyer()
                .withFullName( new Buyer.FullName( "Marie", "Durand", "1" ) )
                .build();
    }

    /**
     * @return A fake but valid client certiicate.
     */
    public static Certificate aClientCertificate(){
        try {
            return anRsaHolder().getClientCertificate();
        } catch (Exception e) {
            // this would happen in testing context: ignore the exception
            return null;
        }
    }

    /**
     * @return A fake client certificate in PEM format.
     */
    public static String aClientCertificatePem(){
        return "-----BEGIN CERTIFICATE-----\n" +
                "MIIDsTCCApmgAwIBAgIEK96RSTANBgkqhkiG9w0BAQsFADCBiDELMAkGA1UEBhMC\n" +
                "RlIxDzANBgNVBAgTBkZyYW5jZTEYMBYGA1UEBxMPQWl4LWVuLVByb3ZlbmNlMRgw\n" +
                "FgYDVQQKEw9UaGFsZXMgU2VydmljZXMxGDAWBgNVBAsTD01vbmV4dCBBUE0gVGVh\n" +
                "bTEaMBgGA1UEAxMRU2ViYXN0aWVuIFBsYW5hcmQwHhcNMTkwODA2MDk0NjU2WhcN\n" +
                "MjAwODA1MDk0NjU2WjCBiDELMAkGA1UEBhMCRlIxDzANBgNVBAgTBkZyYW5jZTEY\n" +
                "MBYGA1UEBxMPQWl4LWVuLVByb3ZlbmNlMRgwFgYDVQQKEw9UaGFsZXMgU2Vydmlj\n" +
                "ZXMxGDAWBgNVBAsTD01vbmV4dCBBUE0gVGVhbTEaMBgGA1UEAxMRU2ViYXN0aWVu\n" +
                "IFBsYW5hcmQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC5V6x4Ljhr\n" +
                "riUEj171bPjAd38F/WC/Qdw9FvpiqpoJ1p85qncqFmDd5nYaWW1rnGjoLu0apzD0\n" +
                "PLvAK8cbAMDn+PKA0vjkabndQrUp0vDNyYvTuCg4DLFdO/XfZP2IsTSACgctNp//\n" +
                "G/IKH5nWE9w04g9d4oOT0klB4FC8XQd7ceWQOaaDbGqetzWv1neuVqv++tnsNtS0\n" +
                "vYdIIgkh+acLxVTyliSOQNeOrCI4ZGt9RClJgcmah5JZ1VbaQjisAIv8a//PhgbO\n" +
                "ULKT7B8Ol6R1DQHh8MGT+1Aju6KVTQXra1cVELIu25sBGnIeoAZ1YF0T0eZbiXLc\n" +
                "Qvs1lUbb1FlfAgMBAAGjITAfMB0GA1UdDgQWBBSQ/k9OCF9bw8UiVmjkZSqTiVaG\n" +
                "9zANBgkqhkiG9w0BAQsFAAOCAQEAFdrUHZZksNehc4N2pFrnnnq6KjbVC1BeQaPj\n" +
                "uSOS2r8AyOmBp121s5XUgDw+SN3JqHd9XMJceAvTsrstyL+JFUtibShP1eXNKoEB\n" +
                "bXqMUmP5d1qSa8vmLgb/sYPNKRwT0cxlrMYOpQGtO1FRjIJrthTPJ4B2mExZxZWe\n" +
                "f21DIzhFzqqaR3aullpcQt8i5xFYlhJUtlcAPQPjPCUqQ8GOOGyWnYWwMp62CsZD\n" +
                "tF5HZMno+ctxHXcGjLjFSgr5+/pN5X5aAaI+lVxajwFGGlMUN+9l9wQN/KL6kGq8\n" +
                "EoLe9DHIFvmhXi80iUBauD7NgdoyyjKeT+jogEm4LeJgM3islA==\n" +
                "-----END CERTIFICATE-----";
    }

    /**
     * Generate a valid {@link ContractConfiguration}.
     */
    public static ContractConfiguration aContractConfiguration(){
        Map<String, ContractProperty> contractProperties = new HashMap<>();
        contractProperties.put(Constants.ContractConfigurationKeys.CHANNEL_TYPE,
                new ContractProperty( ConfigurationServiceImpl.ChannelType.ECOMMERCE ));
        contractProperties.put(Constants.ContractConfigurationKeys.CHARGE_BEARER,
                new ContractProperty( ConfigurationServiceImpl.ChargeBearer.SLEV ));
        contractProperties.put(Constants.ContractConfigurationKeys.CLIENT_NAME, new ContractProperty( "MarketPay" ));
        contractProperties.put(Constants.ContractConfigurationKeys.MERCHANT_IBAN, new ContractProperty( "FR33BBBBBGGGGGCCCCCCCCCCC00" ));
        contractProperties.put(Constants.ContractConfigurationKeys.MERCHANT_NAME, new ContractProperty( "John Snow" ));
        contractProperties.put(Constants.ContractConfigurationKeys.ONBOARDING_ID, new ContractProperty( "XXXXXX" ));
        contractProperties.put(Constants.ContractConfigurationKeys.SCA_METHOD,
                new ContractProperty( ConfigurationServiceImpl.ScaMethod.REDIRECT ));
        contractProperties.put(Constants.ContractConfigurationKeys.PURPOSE_CODE,
                new ContractProperty( ConfigurationServiceImpl.PurposeCode.COMMERCE ));

        return new ContractConfiguration("INST EquensWorldline", contractProperties);
    }

    /**
     * Generate a valid {@link ContractParametersCheckRequest}.
     */
    public static ContractParametersCheckRequest aContractParametersCheckRequest(){
        return aContractParametersCheckRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link ContractParametersCheckRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static ContractParametersCheckRequest.CheckRequestBuilder aContractParametersCheckRequestBuilder(){
        return ContractParametersCheckRequest.CheckRequestBuilder.aCheckRequest()
                .withAccountInfo( anAccountInfo() )
                .withContractConfiguration( aContractConfiguration() )
                .withEnvironment( anEnvironment() )
                .withLocale( Locale.getDefault() )
                .withPartnerConfiguration( aPartnerConfiguration() );
    }

    public static String aMessageCreateDateTime(){
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
    }

    /**
     * Generate a valid {@link Environment}.
     */
    public static Environment anEnvironment(){
        return new Environment("http://notificationURL.com",
                "http://redirectionURL.com",
                "http://redirectionCancelURL.com",
                true);
    }

    /**
     * Generate a valid, but not complete, {@link Order}
     */
    public static Order anOrder(){
        return Order.OrderBuilder.anOrder()
                .withDate( new Date() )
                .withAmount( aPaylineAmount() )
                .withReference( "ORDER-REF-123456" )
                .build();
    }

    /**
     * Generate a valid {@link PartnerConfiguration}.
     */
    public static PartnerConfiguration aPartnerConfiguration(){
        Map<String, String> partnerConfigurationMap = new HashMap<>();
        partnerConfigurationMap.put( Constants.PartnerConfigurationKeys.API_BASE_URL, "https://xs2a.awltest.de/xs2a/routingservice/services" );
        partnerConfigurationMap.put( Constants.PartnerConfigurationKeys.PAYLINE_CLIENT_NAME, "MarketPay" );
        partnerConfigurationMap.put( Constants.PartnerConfigurationKeys.PAYLINE_ONBOARDING_ID, "XXXXXX" );
        partnerConfigurationMap.put( Constants.PartnerConfigurationKeys.PAYMENT_PRODUCT, "Instant" );

        Map<String, String> sensitiveConfigurationMap = new HashMap<>();
        sensitiveConfigurationMap.put( Constants.PartnerConfigurationKeys.CLIENT_CERTIFICATE, aClientCertificatePem() );
        sensitiveConfigurationMap.put( Constants.PartnerConfigurationKeys.CLIENT_PRIVATE_KEY, aPrivateKeyPem() );

        return new PartnerConfiguration( partnerConfigurationMap, sensitiveConfigurationMap );
    }

    /**
     * Generate a valid Payline <code>Address</code>.
     */
    public static Buyer.Address aPaylineAddress(){
        return Buyer.Address.AddressBuilder.anAddress()
                .withCity("Aix-en-Provence")
                .withCountry("FR")
                .withEmail("john.doe@mythalesgroup.io")
                .withFullName( new Buyer.FullName("Thales", "Services", "M.") )
                .withStreet1("150 rue dont le nom est le plus long que j'ai jamais vu. Y'a pas idée d'habiter un endroit pareil !")
                .withStreet2("Le grand bâtiment orange, avec les fenêtres un peu hautes mais un peu larges aussi, et un toit bleu")
                .withZipCode("13100")
                .build();
    }

    /**
     * Generate a valid Payline Amount.
     */
    public static com.payline.pmapi.bean.common.Amount aPaylineAmount(){
        return new com.payline.pmapi.bean.common.Amount(BigInteger.valueOf(1000), Currency.getInstance("EUR"));
    }

    /**
     * Generate a valid {@link PaymentRequest}.
     */
    public static PaymentRequest aPaylinePaymentRequest(){
        return aPaylinePaymentRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link PaymentRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static PaymentRequest.Builder aPaylinePaymentRequestBuilder(){
        return PaymentRequest.builder()
                .withAmount( aPaylineAmount() )
                .withBrowser( aBrowser() )
                .withBuyer( aBuyer() )
                .withCaptureNow( true )
                .withContractConfiguration( aContractConfiguration() )
                .withDifferedActionDate( TestUtils.addTime( new Date(), Calendar.DATE, 5) )
                .withEnvironment( anEnvironment() )
                .withLocale( Locale.getDefault() )
                .withOrder( anOrder() )
                .withPartnerConfiguration( aPartnerConfiguration() )
                .withPaymentFormContext( aPaymentFormContext() )
                .withPluginConfiguration( aPluginConfiguration() )
                .withSoftDescriptor( "softDescriptor" )
                .withTransactionId( "123456789012345678901" );
    }

    /**
     * Generate a valid {@link PaymentFormContext}.
     */
    public static PaymentFormContext aPaymentFormContext(){
        Map<String, String> paymentFormParameter = new HashMap<>();
        paymentFormParameter.put( BankTransferForm.BANK_KEY, "PSSTFRPP" );

        return PaymentFormContext.PaymentFormContextBuilder.aPaymentFormContext()
                .withPaymentFormParameter( paymentFormParameter )
                .withSensitivePaymentFormParameter( new HashMap<>() )
                .build();
    }

    /**
     * Generate a valid {@link PaymentFormConfigurationRequest}.
     */
    public static PaymentFormConfigurationRequest aPaymentFormConfigurationRequest(){
        return aPaymentFormConfigurationRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link PaymentFormConfigurationRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static PaymentFormConfigurationRequest.PaymentFormConfigurationRequestBuilder aPaymentFormConfigurationRequestBuilder(){
        return PaymentFormConfigurationRequest.PaymentFormConfigurationRequestBuilder.aPaymentFormConfigurationRequest()
                .withAmount( aPaylineAmount() )
                .withBuyer( aBuyer() )
                .withContractConfiguration( aContractConfiguration() )
                .withEnvironment( anEnvironment() )
                .withLocale( Locale.getDefault() )
                .withOrder( anOrder() )
                .withPartnerConfiguration( aPartnerConfiguration() )
                .withPluginConfiguration( aPluginConfiguration() );
    }

    /**
     * Generate a valid {@link PaymentFormLogoRequest}.
     */
    public static PaymentFormLogoRequest aPaymentFormLogoRequest(){
        return PaymentFormLogoRequest.PaymentFormLogoRequestBuilder.aPaymentFormLogoRequest()
                .withContractConfiguration( aContractConfiguration() )
                .withEnvironment( anEnvironment() )
                .withPartnerConfiguration( aPartnerConfiguration() )
                .withLocale( Locale.getDefault() )
                .build();
    }

    /**
     * Generate a valid payment ID, similar to the ones the partner API would return.
     */
    public static String aPaymentId(){
        return "123456";
    }



    /**
     * Generate a valid {@link PaymentInitiationRequest}.
     */
    public static PaymentInitiationRequest aPaymentInitiationRequest(){
        return aPaymentInitiationRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link PaymentInitiationRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static PaymentInitiationRequest.PaymentInitiationRequestBuilder aPaymentInitiationRequestBuilder(){
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return new PaymentInitiationRequest.PaymentInitiationRequestBuilder()
                .withAspspId("1402")
                .withEndToEndId( "PAYLINE" + timestamp )
                .withInitiatingPartyReferenceId( "REF" + timestamp )
                .withInitiatingPartyReturnUrl( "http://google.fr?result=success" )
                .withRemittanceInformation( "softDescriptor" )
                .withRemittanceInformationStructured(
                        new RemittanceInformationStructured.RemittanceInformationStructuredBuilder()
                                .withReference( "REF" + timestamp )
                                .build()
                )
                /*
                .withDebtorAccount(
                        new Account.AccountBuilder()
                                .withIdentification( "AT880000000000000001" )
                                .build()
                )
                */
                .withCreditorAccount(
                        new Account.AccountBuilder()
                                .withIdentification("ES1400490001510000000002")
                                .build()
                )
                .withCreditorName("John Smith")
                .withPaymentAmount("10.00")
                .withPaymentCurrency("EUR")
                .withPurposeCode( ConfigurationServiceImpl.PurposeCode.COMMERCE )
                .withPsuSessionInformation(
                        new PsuSessionInformation.PsuSessionInformationBuilder()
                                .withIpAddress( "192.168.0.1" )
                                .withHeaderUserAgent( MockUtils.aUserAgent() )
                                .build()
                )
                .withRiskInformation(
                        new RiskInformation.RiskInformationBuilder()
                                .withDeliveryAddress(
                                        new Address.AddressBuilder()
                                                .withPostCode( "13100" )
                                                .withTownName( "Aix-en-Provence" )
                                                .withCountry( "FR" )
                                                .build()
                                )
                                .withChannelType( ConfigurationServiceImpl.ChannelType.ECOMMERCE )
                                .build()
                )
                .addPreferredScaMethod( ConfigurationServiceImpl.ScaMethod.REDIRECT )
                .withChargeBearer( ConfigurationServiceImpl.ChargeBearer.SLEV )
                .withPsuId( "1" )
                .withPaymentProduct( MockUtils.aPartnerConfiguration().getProperty( Constants.PartnerConfigurationKeys.PAYMENT_PRODUCT ) );
    }

    /**
     * Generate a valid {@link PaymentInitiationResponse}.
     */
    public static PaymentInitiationResponse aPaymentInitiationResponse(){
        return PaymentInitiationResponse.fromJson("{\n" +
                "    \"MessageCreateDateTime\": \"" + aMessageCreateDateTime() + "\",\n" +
                "    \"MessageId\": \"e8683740-38be-4026-b48e-72089b023e\",\n" +
                "    \"PaymentId\": \"" + MockUtils.aPaymentId() + "\",\n" +
                "    \"InitiatingPartyReferenceId\": \"REF1574181352\",\n" +
                "    \"PaymentStatus\": \"OPEN\",\n" +
                "    \"AspspRedirectUrl\": \"https://xs2a.banking.co.at/xs2a-sandbox/m044/v1/pis/confirmation/btWMz6mTz7I3SOe4lMqXiwciqe6igXBCeebfVWlmZ8N8zVw_qRKMMuhlLLXtPrVcBeH6HIP2qhdTTZ1HINXSkg==_=_psGLvQpt9Q/authorisations/fa8e44a7-3bf7-4543-82d1-5a1163aaaaad\"\n" +
                "}");
    }

    /**
     * Generate a valid {@link PaymentStatusResponse} with the given status.
     * @param status The payment status.
     */
    public static PaymentStatusResponse aPaymentStatusResponse( PaymentStatus status ){
        return PaymentStatusResponse.fromJson("{\n" +
                "    \"MessageCreateDateTime\": \"" + aMessageCreateDateTime() + "\",\n" +
                "    \"MessageId\": \"ca58925c-57cc-44b0-a827-cd439fb87f\",\n" +
                "    \"PaymentId\": \"" + MockUtils.aPaymentId() + "\",\n" +
                "    \"PaymentStatus\": \"" + status.name() + "\",\n" +
                "    \"AspspPaymentId\": \"im7QC5rZ-jyNr237sJb6VqEnBd8uNDnU6b9-rnAYVxTNub1NwmkrY3CBGDMRXsx5BeH6HIP2qhdTTZ1HINXSkg==_=_psGLvQpt9Q\",\n" +
                "    \"InitiatingPartyReferenceId\": \"REF1574257016\",\n" +
                "    \"DebtorAgent\": \"BNPADEFF\",\n" +
                "    \"DebtorAccount\": \"AT880000000000000001\"\n" +
                "}");
    }

    /**
     * Generate a valid plugin configuration, as a <code>String</code>.
     */
    public static String aPluginConfiguration(){
        return "{\"Application\":\"PIS\",\"ASPSP\":[" +
                    "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"BIC\":\"PSSTFRPP\"}," +
                    "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"BIC\":\"BBVAESMM\"}" +
                "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}" +
                "&&&thisIsAKey";
    }

    /**
     * @return A fake private key, for test purpose.
     */
    public static PrivateKey aPrivateKey(){
        try {
            return (PrivateKey) anRsaHolder().getPrivateKey();
        } catch (Exception e) {
            // this is testing context: ignore the exception
            return null;
        }
    }

    /**
     * @return A fake private key in PEM format.
     */
    public static String aPrivateKeyPem(){
        return "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5V6x4LjhrriUE\n" +
                "j171bPjAd38F/WC/Qdw9FvpiqpoJ1p85qncqFmDd5nYaWW1rnGjoLu0apzD0PLvA\n" +
                "K8cbAMDn+PKA0vjkabndQrUp0vDNyYvTuCg4DLFdO/XfZP2IsTSACgctNp//G/IK\n" +
                "H5nWE9w04g9d4oOT0klB4FC8XQd7ceWQOaaDbGqetzWv1neuVqv++tnsNtS0vYdI\n" +
                "Igkh+acLxVTyliSOQNeOrCI4ZGt9RClJgcmah5JZ1VbaQjisAIv8a//PhgbOULKT\n" +
                "7B8Ol6R1DQHh8MGT+1Aju6KVTQXra1cVELIu25sBGnIeoAZ1YF0T0eZbiXLcQvs1\n" +
                "lUbb1FlfAgMBAAECggEAQe+p4Nt4oP5EFxo2SxOobzhTzTq193SjuGv7SaytvkBH\n" +
                "RwmY+TUa4vaBH6Ce58CuJaPEIE5IgSs2FAi+/aFH/362LxRfDUt7nvaDYsyzdFJ2\n" +
                "VyzVyhLh6mxRbVcNR1rbdY2bvf8H7obvlaBmZx2LopilpN3Xt3gBLdlyf4CND+H6\n" +
                "YqCMWgIjhLkUk98/p479gSh5qaxkj7lsjlGk+4CLB/MlHuZ7WgWKH+q058l3JeAp\n" +
                "KQBHi/N/HOEdBrg2dilFwoPy0xRwxAjK5TCdn2277DIwr4lA28O4VqlICDXGNgUx\n" +
                "dKWwBnhOpfvlvDQr4Ktt2pyuL7H5KfrkC8lvxOCJGQKBgQDxItb9M2hfrEY5uq8U\n" +
                "4T/aEKV6lrQfeGRoEx2smotzt7hx9J4XUb3RAMnuK108kfWrjhqyOmDgIHwyL1ag\n" +
                "9NsKgVWvNw+ZGLfZHblTvTNPVqPu9xQk6Rrq8kQzFqYelAKhYKrEVJiZ5MK3NCts\n" +
                "a24Qn46mN2mBN/6mD0m9fr3B8wKBgQDExGcvdfUddf9vp640Sq7TxiWOZJXo1Gk8\n" +
                "z+kVa6rIWB87zFq82IojmjofYYmYF+d50kTyw/s5trob8LqWL2GLrDF0K64nbon7\n" +
                "tW1MSggHwQu5BtbxIwkyL8pv6BWnpf0k2Lf29txzBo1hvBZ1M9fPGvpd/eP2Hz4x\n" +
                "yU+G92555QKBgQC4vfp8boBOnEwJOo+crZ4f0ZUWQJOrcK9sVQjtDlI8y8rR85mT\n" +
                "QBrvH22VvT9ngmP3lZ26YqOJ0xmT0VTLaAzRFZmx7btTje58txsfntrKtBRQppeW\n" +
                "V8k5q3a4tWd8EeWaAdeTJ0Tq0qqjdaK3I+9laPj/O4DncSD11MyoE4wKJQKBgFWv\n" +
                "XWvKhyoEJ2786xx0ZTttbw9Z9/oC/azwsQSV9TH3Reqpa94OweENGUBvHhbwWemv\n" +
                "yjyZYX5ZdyQRqX8bNPQ40PRQzS74sPe+otD08BhIVY2GT/WEF04Wh6ZBv6RY4Sq5\n" +
                "gSr3hzpD4S9tU65IHDNhASQLGskkA9Z0XsBcYWyNAoGAO7UpGmNCywW8x/MrSqF+\n" +
                "Nl8EIyL+oPat0awur9FwxL3AyKTL75fykdiOf6Qy96Je4X7WojGmyL7a3Hbh29NT\n" +
                "VNAzHrCYpRtxCNVoatW2lA8AvySWsiEwMTmdNMubjWcSPx8gHVmzGoOnKK44Ytaf\n" +
                "TZVu0T1HwCkWzUMS7ULfwtw=\n" +
                "-----END PRIVATE KEY-----";
    }

    /**
     * Generate a valid {@link Psu}.
     */
    public static Psu aPsu(){
        return new Psu.PsuBuilder()
                .withPsuId("1")
                .withFirstName("John")
                .withLastName("Doe")
                .withEmail("john.doe@mythalesgroup.io")
                .withStatus("ACTIVE")
                .build();
    }

    /**
     * Generate a valid {@link PsuCreateRequest}.
     */
    public static PsuCreateRequest aPsuCreateRequest(){
        return new PsuCreateRequest.PsuCreateRequestBuilder().build();
    }

    /**
     * Generate an {@link RSAHolder} instance, containing fake elements, for test purpose.
     */
    public static RSAHolder anRsaHolder(){
        try {
            return new RSAHolder.RSAHolderBuilder()
                    .parseChain( aClientCertificatePem() )
                    .parsePrivateKey( aPrivateKeyPem() )
                    .build();
        } catch (Exception e) {
            // this is testing context: ignore the exception. The test case using this will probably fail anyway.
            return null;
        }
    }

    /**
     * Generate a valid {@link RedirectionPaymentRequest}.
     */
    public static RedirectionPaymentRequest aRedirectionPaymentRequest(){
        return RedirectionPaymentRequest.builder()
                .withAmount( aPaylineAmount() )
                .withBrowser( aBrowser() )
                .withBuyer( aBuyer() )
                .withContractConfiguration( aContractConfiguration() )
                .withEnvironment( anEnvironment() )
                .withOrder( anOrder() )
                .withPartnerConfiguration( aPartnerConfiguration() )
                .withTransactionId( aTransactionId() )
                .build();
    }

    /**
     * Generate a valid {@link RequestConfiguration}.
     */
    public static RequestConfiguration aRequestConfiguration(){
        return new RequestConfiguration( aContractConfiguration(), anEnvironment(), aPartnerConfiguration() );
    }

    /**
     * Generate a valid {@link RetrievePluginConfigurationRequest}.
     */
    public static RetrievePluginConfigurationRequest aRetrievePluginConfigurationRequest(){
        return aRetrievePluginConfigurationRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link RetrievePluginConfigurationRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static RetrievePluginConfigurationRequest.RetrieveConfigurationRequestBuilder aRetrievePluginConfigurationRequestBuilder(){
        return RetrievePluginConfigurationRequest.RetrieveConfigurationRequestBuilder.aRetrieveConfigurationRequest()
                .withContractConfiguration( aContractConfiguration() )
                .withEnvironment( anEnvironment() )
                .withPartnerConfiguration( aPartnerConfiguration() )
                .withPluginConfiguration( aPluginConfiguration() );
    }

    /**
     * Generate a sample {@link Signature}.
     */
    public static Signature aSignature(){
        Signature signature = new Signature("a-key-id", Algorithm.RSA_SHA256, null, "(request-target)" );
        Signer signer = new Signer( aPrivateKey(), signature );
        try {
            signature = signer.sign( "POST", "/some/path", new HashMap<>() );
        } catch (Exception e) {
            // This would happen in a testing context: spare the exception throwing, the test case will probably fail anyway.
            return null;
        }

        return signature;
    }

    /**
     * @return a valid transaction ID.
     */
    public static String aTransactionId(){
        return "123456789012345678901";
    }

    /**
     * Generate a valid {@link TransactionStatusRequest}.
     */
    public static TransactionStatusRequest aTransactionStatusRequest(){
        return TransactionStatusRequest.TransactionStatusRequestBuilder.aNotificationRequest()
                .withAmount( aPaylineAmount() )
                .withBuyer( aBuyer() )
                .withContractConfiguration( aContractConfiguration() )
                .withEnvironment( anEnvironment() )
                .withOrder( anOrder() )
                .withPartnerConfiguration( aPartnerConfiguration() )
                .withTransactionId( aTransactionId() )
                .build();
    }

    /**
     * Generate a unique identifier that matches the API expectations.
     */
    public static String aUniqueIdentifier(){
        return "MONEXT" +  new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    /**
     * @return a valid user agent.
     */
    public static String aUserAgent(){
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0";
    }
}
