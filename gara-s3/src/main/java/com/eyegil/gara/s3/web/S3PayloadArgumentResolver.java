package com.eyegil.gara.s3.web;

import com.eyegil.gara.s3.dto.S3Payload;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.eyegil.gara.s3.util.AwsChunkedDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Reads an S3 request body into an {@link S3Payload}: strips {@code aws-chunked} framing and checks the body
 * against {@code x-amz-content-sha256} (a SigV4 signature covers that header, so this ties the body to the
 * signature) and {@code Content-MD5} when the client sent them.
 */
public class S3PayloadArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String CONTENT_SHA256 = "x-amz-content-sha256";
    private static final String DECODED_LENGTH = "x-amz-decoded-content-length";
    private static final String STREAMING_PREFIX = "STREAMING-";
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return S3Payload.class.equals(parameter.getParameterType());
    }

    @Override
    public S3Payload resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                     NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws IOException {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        byte[] raw = request.getInputStream().readAllBytes();
        String declaredSha256 = request.getHeader(CONTENT_SHA256);

        byte[] body;
        if (declaredSha256 != null && declaredSha256.startsWith(STREAMING_PREFIX)) {
            body = AwsChunkedDecoder.decode(raw);
            requireDecodedLength(request.getHeader(DECODED_LENGTH), body.length);
        } else {
            body = raw;
            if (declaredSha256 != null && SHA256_HEX.matcher(declaredSha256).matches()
                    && !declaredSha256.equalsIgnoreCase(HexFormat.of().formatHex(sha256(body)))) {
                throw new S3Exception(S3ErrorCode.CONTENT_SHA256_MISMATCH);
            }
        }

        String contentMd5 = request.getHeader("Content-MD5");
        if (contentMd5 != null && !contentMd5.equals(Base64.getEncoder().encodeToString(DigestUtils.md5Digest(body)))) {
            throw new S3Exception(S3ErrorCode.BAD_DIGEST);
        }
        return new S3Payload(body);
    }

    private void requireDecodedLength(String declared, int actual) {
        if (declared != null && Long.parseLong(declared.trim()) != actual) {
            throw new S3Exception(S3ErrorCode.INVALID_REQUEST, "Decoded body length does not match x-amz-decoded-content-length.");
        }
    }

    private static byte[] sha256(byte[] body) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(body);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
