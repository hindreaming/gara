package com.eyegil.gara.s3.web;

import com.eyegil.gara.s3.dto.S3Error;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.util.S3Xml;
import com.eyegil.gara.security.dto.SignedRequest;
import com.eyegil.gara.security.exception.SignatureFailure;
import com.eyegil.gara.security.exception.SignatureVerificationException;
import com.eyegil.gara.security.service.LoginAttemptGuard;
import com.eyegil.gara.security.service.SigV4Verifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

/**
 * Requires a valid AWS Signature Version 4 on every S3 request and answers failures with S3 error documents.
 * Wrong keys or signatures count toward the client's lockout; unsigned requests do not.
 */
@Slf4j
@RequiredArgsConstructor
public class S3AuthenticationFilter extends OncePerRequestFilter {

    private final SigV4Verifier sigV4Verifier;
    private final LoginAttemptGuard loginAttemptGuard;
    private final ReservedPaths reservedPaths;
    private final S3Xml s3Xml;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return reservedPaths.isReserved(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String client = request.getRemoteAddr();
        if (loginAttemptGuard.isLockedOut(client)) {
            reject(response, S3ErrorCode.ACCESS_DENIED, "Too many failed attempts; try again later.", request.getRequestURI());
            return;
        }
        SignedRequest signed = new SignedRequest(request.getMethod(), request.getRequestURI(), request.getQueryString(),
                name -> Collections.list(request.getHeaders(name)));
        try {
            sigV4Verifier.verify(signed, Instant.now());
        } catch (SignatureVerificationException e) {
            log.debug("Rejected {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            if (isCredentialGuess(e.getFailure())) {
                loginAttemptGuard.recordFailure(client);
            }
            reject(response, errorCode(e.getFailure()), e.getMessage(), request.getRequestURI());
            return;
        }
        loginAttemptGuard.recordSuccess(client);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, S3ErrorCode errorCode, String message, String resource) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.getOutputStream().write(s3Xml.write(new S3Error(errorCode.getCode(), message, resource)));
    }

    private static boolean isCredentialGuess(SignatureFailure failure) {
        return failure == SignatureFailure.MISMATCH || failure == SignatureFailure.UNKNOWN_ACCESS_KEY;
    }

    private static S3ErrorCode errorCode(SignatureFailure failure) {
        return switch (failure) {
            case MISSING, EXPIRED -> S3ErrorCode.ACCESS_DENIED;
            case MALFORMED -> S3ErrorCode.AUTHORIZATION_HEADER_MALFORMED;
            case UNKNOWN_ACCESS_KEY -> S3ErrorCode.INVALID_ACCESS_KEY_ID;
            case MISMATCH -> S3ErrorCode.SIGNATURE_DOES_NOT_MATCH;
            case CLOCK_SKEW -> S3ErrorCode.REQUEST_TIME_TOO_SKEWED;
        };
    }
}
