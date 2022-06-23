package com.payline.payment.equens.utils.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple POJO supporting the core elements of an HTTP response, in a more readable format (especially the content).
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StringResponse {

    private String content;
    private Map<String, String> headers = new HashMap<>();
    private int statusCode;
    private String statusMessage;

    public String getContent() {
        return content;
    }

    public String getHeader(String name){
        if( headers != null && name != null ){
            return headers.get( name.toLowerCase() );
        }
        return null;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isSuccess(){
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Safely extract the elements of a {@link StringResponse} from a {@link HttpResponse}.
     * @param httpResponse the HTTP response
     * @return The corresponding StringResponse, or null if the input cannot be read or contains incomplete data.
     */
    public static StringResponse fromHttpResponse(HttpResponse httpResponse) {
        StringResponse instance = null;

        if( httpResponse != null && httpResponse.getStatusLine() != null ){
            instance = new StringResponse();
            instance.statusCode = httpResponse.getStatusLine().getStatusCode();
            instance.statusMessage = httpResponse.getStatusLine().getReasonPhrase();

            try {
                instance.content = EntityUtils.toString(httpResponse.getEntity());
            } catch (IOException e) {
                instance.content = null;
            }

            Header[] rawHeaders = httpResponse.getAllHeaders();
            for (final Header rawHeader : rawHeaders) {
                instance.headers.put(rawHeader.getName().toLowerCase(), rawHeader.getValue());
            }
        }

        return instance;
    }

    @Override
    public String toString() {
        String ln = System.lineSeparator();
        String str = "HTTP " + this.getStatusCode() + " " + this.getStatusMessage() + ln;

        final List<String> strHeaders = new ArrayList<>();
        this.headers.forEach( (key, value) -> strHeaders.add( key + ": " + value ) );
        str += String.join(ln, strHeaders);

        if( this.content != null ){
            str += ln + this.content;
        }

        return str;
    }
}
