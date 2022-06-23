package com.payline.payment.equens.utils.http;

import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Utility class for test purpose related to HTTP calls.
 */
class HttpTestUtils {

    /**
     * Mock an HTTP Response with the given elements.
     *
     * @param statusCode The status code (ex: 200)
     * @param statusMessage The status message (ex: "OK")
     * @param content The response content/body
     * @return A mocked HTTP response
     */
    static CloseableHttpResponse mockHttpResponse(int statusCode, String statusMessage, String content, Header[] headers ){
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn( new BasicStatusLine( new ProtocolVersion("HTTP", 1, 1), statusCode, statusMessage) )
                .when( response ).getStatusLine();
        doReturn( new StringEntity( content, StandardCharsets.UTF_8 ) ).when( response ).getEntity();
        if( headers != null && headers.length >= 1 ){
            doReturn( headers ).when( response ).getAllHeaders();
        } else {
            doReturn( new Header[]{} ).when( response ).getAllHeaders();
        }
        return response;
    }

    /**
     * Mock a StringResponse with the given elements (no headers).
     *
     * @param statusCode The HTTP status code (ex: 200, 403)
     * @param statusMessage The HTTP status message (ex: "OK", "Forbidden")
     * @param content The response content as a string
     * @return A mocked StringResponse
     */
    public static StringResponse mockStringResponse( int statusCode, String statusMessage, String content ){
        return mockStringResponse( statusCode, statusMessage, content, null );
    }

    /**
     * Mock a StringResponse with the given elements.
     *
     * @param statusCode The HTTP status code (ex: 200, 403)
     * @param statusMessage The HTTP status message (ex: "OK", "Forbidden")
     * @param content The response content as a string
     * @param headers The response headers
     * @return A mocked StringResponse
     */
    public static StringResponse mockStringResponse( int statusCode, String statusMessage, String content, Map<String, String> headers ){
        final StringResponse.StringResponseBuilder builder = StringResponse.builder();
        if (content != null && !content.isEmpty()) {
            builder.content(content);
        }
        if (headers != null && headers.size() > 0) {
            builder.headers(headers);
        }
        if (statusCode >= 100 && statusCode < 600) {
            builder.statusCode(statusCode);
        }
        if (statusMessage != null && !statusMessage.isEmpty()) {
            builder.statusMessage(statusMessage);
        }
        return builder.build();
    }

}
