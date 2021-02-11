package com.payline.payment.equens.utils.http;

import com.payline.payment.equens.bean.business.psu.Psu;
import com.payline.payment.equens.bean.business.psu.PsuCreateRequest;
import com.payline.payment.equens.bean.business.psu.PsuCreateResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.utils.constant.PartnerConfigurationKeys;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * HTTP client in charge of requesting the PSU Management API.
 */
public class PsuHttpClient extends EquensHttpClient {

    // --- Singleton Holder pattern + initialization BEGIN
    PsuHttpClient() {
    }
    private static class Holder {
        private static final PsuHttpClient instance = new PsuHttpClient();
    }
    public static PsuHttpClient getInstance() {
        return PsuHttpClient.Holder.instance;
    }
    // --- Singleton Holder pattern + initialization END

    @Override
    protected String appName() {
        return "PSU";
    }

    /**
     * Create a PSU.
     *
     * @param psuCreateRequest The PSU creation request
     * @param requestConfiguration The request configuration
     * @return The API response, normally containing the created PSU data
     */
    public Psu createPsu(PsuCreateRequest psuCreateRequest, RequestConfiguration requestConfiguration ){
        // Service full URL
        String url = requestConfiguration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.API_URL_PSU_PSUS);
        if (url == null) {
            throw new InvalidDataException("Missing API psus url in PartnerConfiguration");
        }

        // Init. headers with Authorization (access token)
        List<Header> headers = this.initHeaders( requestConfiguration );

        // Misc headers
        headers.add( new BasicHeader( HEADER_REQUEST_ID, UUID.randomUUID().toString() ) );

        // Body
        StringEntity body = new StringEntity(jsonService.toJson(psuCreateRequest), StandardCharsets.UTF_8 );
        headers.add( new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json") );

        // Send
        StringResponse response = this.post( url, headers, body );

        // Handle potential errors
        if( !response.isSuccess() || response.getContent() == null ){
            throw this.handleError( response );
        }

        return jsonService.fromJson(response.getContent(), PsuCreateResponse.class).getPsu();
    }

}
