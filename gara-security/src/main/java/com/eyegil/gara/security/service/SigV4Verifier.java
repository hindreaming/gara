package com.eyegil.gara.security.service;

import com.eyegil.gara.security.dto.SignedRequest;
import com.eyegil.gara.security.exception.SignatureFailure;
import com.eyegil.gara.security.exception.SignatureVerificationException;
import com.eyegil.gara.security.util.SigV4Encoding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verifies AWS Signature Version 4 requests, both header-signed ({@code Authorization: AWS4-HMAC-SHA256 ...}) and
 * presigned URLs ({@code X-Amz-Signature} query parameter), against the configured account.
 * Any region and the "s3" service are accepted, so clients can keep their default region.
 */
@Service
@RequiredArgsConstructor
public class SigV4Verifier {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(15);
    private static final Duration MAX_PRESIGN_EXPIRY = Duration.ofDays(7);
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final HexFormat HEX = HexFormat.of();

    private final CredentialsService credentialsService;

    public void verify(SignedRequest request, Instant now) {
        String authorization = request.header("Authorization");
        Map<String, String> query = parseQuery(request.rawQuery());

        if (authorization != null && authorization.startsWith(ALGORITHM + " ")) {
            verifyHeaderSigned(request, authorization, query, now);
        } else if (ALGORITHM.equals(query.get("X-Amz-Algorithm"))) {
            verifyPresigned(request, query, now);
        } else {
            throw failure(SignatureFailure.MISSING, "Request is not signed with AWS Signature Version 4");
        }
    }

    private void verifyHeaderSigned(SignedRequest request, String authorization, Map<String, String> query, Instant now) {
        Map<String, String> fields = parseAuthorization(authorization.substring(ALGORITHM.length() + 1));
        String amzDate = require(request.header("x-amz-date"), "x-amz-date header is required");
        Instant signedAt = parseAmzDate(amzDate);
        if (Duration.between(signedAt, now).abs().compareTo(MAX_CLOCK_SKEW) > 0) {
            throw failure(SignatureFailure.CLOCK_SKEW, "The difference between the request time and the server's time is too large");
        }
        String payloadHash = require(request.header("x-amz-content-sha256"), "x-amz-content-sha256 header is required");
        check(request, query, fields.get("Credential"), fields.get("SignedHeaders"), fields.get("Signature"), amzDate, payloadHash);
    }

    private void verifyPresigned(SignedRequest request, Map<String, String> query, Instant now) {
        String amzDate = require(query.get("X-Amz-Date"), "X-Amz-Date is required");
        Duration expires = Duration.ofSeconds(parseLong(require(query.get("X-Amz-Expires"), "X-Amz-Expires is required")));
        if (expires.compareTo(MAX_PRESIGN_EXPIRY) > 0) {
            throw failure(SignatureFailure.MALFORMED, "X-Amz-Expires must be at most 7 days");
        }
        if (now.isAfter(parseAmzDate(amzDate).plus(expires))) {
            throw failure(SignatureFailure.EXPIRED, "Request has expired");
        }
        Map<String, String> signedQuery = new HashMap<>(query);
        signedQuery.remove("X-Amz-Signature");
        check(request, signedQuery, query.get("X-Amz-Credential"), query.get("X-Amz-SignedHeaders"),
                query.get("X-Amz-Signature"), amzDate, UNSIGNED_PAYLOAD);
    }

    private void check(SignedRequest request, Map<String, String> query, String credential, String signedHeaders,
                       String signature, String amzDate, String payloadHash) {
        require(signedHeaders, "SignedHeaders is missing");
        require(signature, "Signature is missing");
        String[] scope = require(credential, "Credential is missing").split("/");
        if (scope.length != 5 || !"aws4_request".equals(scope[4])) {
            throw failure(SignatureFailure.MALFORMED, "Credential must be accessKey/date/region/service/aws4_request");
        }
        if (!credentialsService.isKnownAccessKey(scope[0])) {
            throw failure(SignatureFailure.UNKNOWN_ACCESS_KEY, "The access key id does not exist");
        }

        String canonicalRequest = String.join("\n",
                request.method(),
                canonicalPath(request.rawPath()),
                canonicalQuery(query),
                canonicalHeaders(request, signedHeaders),
                signedHeaders,
                payloadHash);
        String credentialScope = String.join("/", scope[1], scope[2], scope[3], scope[4]);
        String stringToSign = String.join("\n", ALGORITHM, amzDate, credentialScope, HEX.formatHex(sha256(canonicalRequest)));

        byte[] key = hmac(("AWS4" + credentialsService.secretKey()).getBytes(StandardCharsets.UTF_8), scope[1]);
        key = hmac(key, scope[2]);
        key = hmac(key, scope[3]);
        key = hmac(key, scope[4]);
        byte[] expected = HEX.formatHex(hmac(key, stringToSign)).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, signature.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw failure(SignatureFailure.MISMATCH, "The request signature we calculated does not match the signature you provided");
        }
    }

    private static String canonicalPath(String rawPath) {
        String path = rawPath == null || rawPath.isEmpty() ? "/" : rawPath;
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/", -1)) {
            segments.add(SigV4Encoding.encode(SigV4Encoding.decode(segment)));
        }
        return String.join("/", segments);
    }

    private static String canonicalQuery(Map<String, String> query) {
        return query.entrySet().stream()
                .map(entry -> Map.entry(SigV4Encoding.encode(entry.getKey()), SigV4Encoding.encode(entry.getValue())))
                .sorted(Map.Entry.<String, String>comparingByKey().thenComparing(Map.Entry.comparingByValue()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String canonicalHeaders(SignedRequest request, String signedHeaders) {
        StringBuilder canonical = new StringBuilder();
        for (String name : signedHeaders.split(";")) {
            String value = request.headers().apply(name).stream()
                    .map(v -> v.trim().replaceAll("\\s+", " "))
                    .collect(Collectors.joining(","));
            canonical.append(name).append(':').append(value).append('\n');
        }
        return canonical.toString();
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = SigV4Encoding.decode(equals < 0 ? pair : pair.substring(0, equals));
            String value = equals < 0 ? "" : SigV4Encoding.decode(pair.substring(equals + 1));
            query.put(name, value);
        }
        return query;
    }

    private static Map<String, String> parseAuthorization(String fields) {
        Map<String, String> parsed = new HashMap<>();
        for (String field : fields.split(",")) {
            int equals = field.indexOf('=');
            if (equals > 0) {
                parsed.put(field.substring(0, equals).trim(), field.substring(equals + 1).trim());
            }
        }
        return parsed;
    }

    private static Instant parseAmzDate(String amzDate) {
        try {
            return Instant.from(AMZ_DATE.parse(amzDate));
        } catch (DateTimeParseException e) {
            throw failure(SignatureFailure.MALFORMED, "Invalid date " + amzDate);
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw failure(SignatureFailure.MALFORMED, "Invalid number " + value);
        }
    }

    private static String require(String value, String message) {
        if (value == null || value.isEmpty()) {
            throw failure(SignatureFailure.MALFORMED, message);
        }
        return value;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SignatureVerificationException failure(SignatureFailure failure, String message) {
        return new SignatureVerificationException(failure, message);
    }
}
