package com.eyegil.gara.security.dto;

import java.util.List;
import java.util.function.Function;

/**
 * What SigV4 verification needs from an HTTP request, without tying the verifier to the Servlet API.
 *
 * @param method   HTTP method
 * @param rawPath  request path exactly as received, still percent-encoded
 * @param rawQuery query string exactly as received, or null
 * @param headers  all values of a header by case-insensitive name; empty when absent
 */
public record SignedRequest(String method, String rawPath, String rawQuery, Function<String, List<String>> headers) {

    public String header(String name) {
        List<String> values = headers.apply(name);
        return values.isEmpty() ? null : values.get(0);
    }
}
