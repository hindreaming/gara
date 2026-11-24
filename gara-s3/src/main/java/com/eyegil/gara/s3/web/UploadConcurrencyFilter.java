package com.eyegil.gara.s3.web;

import com.eyegil.gara.s3.config.S3Property;
import com.eyegil.gara.s3.dto.S3Error;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.util.S3Xml;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Caps how many S3 uploads one instance handles at once, because each request body is held in memory until it is
 * committed. Requests over the cap wait briefly, then get SlowDown, which S3 clients retry.
 */
public class UploadConcurrencyFilter extends OncePerRequestFilter {

    private final Semaphore slots;
    private final S3Property s3Property;
    private final ReservedPaths reservedPaths;
    private final S3Xml s3Xml;

    public UploadConcurrencyFilter(S3Property s3Property, ReservedPaths reservedPaths, S3Xml s3Xml) {
        this.slots = new Semaphore(s3Property.maxConcurrentUploads(), true);
        this.s3Property = s3Property;
        this.reservedPaths = reservedPaths;
        this.s3Xml = s3Xml;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return !("PUT".equals(method) || "POST".equals(method)) || reservedPaths.isReserved(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!acquire()) {
            S3ErrorCode errorCode = S3ErrorCode.SLOW_DOWN;
            response.setStatus(errorCode.getStatus().value());
            response.setContentType(MediaType.APPLICATION_XML_VALUE);
            response.getOutputStream().write(s3Xml.write(
                    new S3Error(errorCode.getCode(), "Too many uploads in progress.", request.getRequestURI())));
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            slots.release();
        }
    }

    private boolean acquire() {
        try {
            return slots.tryAcquire(s3Property.uploadWait().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
