package com.eyegil.gara.admin.web;

import com.eyegil.gara.security.service.CredentialsService;
import com.eyegil.gara.security.service.LoginAttemptGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP Basic with the Gara account for the admin UI, the admin API and the actuator endpoints. The health
 * endpoint stays public so load balancers and orchestrators can probe it. Wrong passwords count toward the client's
 * lockout.
 */
@RequiredArgsConstructor
public class AdminBasicAuthFilter extends OncePerRequestFilter {

    private static final String BASIC = "Basic ";
    private static final int TOO_MANY_REQUESTS = 429;
    private static final String CHALLENGE = "Basic realm=\"Gara admin\", charset=\"UTF-8\"";

    private final CredentialsService credentialsService;
    private final LoginAttemptGuard loginAttemptGuard;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String client = request.getRemoteAddr();
        if (loginAttemptGuard.isLockedOut(client)) {
            response.sendError(TOO_MANY_REQUESTS, "Too many failed attempts; try again later.");
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isAuthenticated(authorization)) {
            loginAttemptGuard.recordSuccess(client);
            chain.doFilter(request, response);
            return;
        }
        if (authorization != null) {
            loginAttemptGuard.recordFailure(client);
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, CHALLENGE);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean isAuthenticated(String authorization) {
        if (authorization == null || !authorization.startsWith(BASIC)) {
            return false;
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(authorization.substring(BASIC.length()).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }
        int colon = decoded.indexOf(':');
        return colon > 0 && credentialsService.matches(decoded.substring(0, colon), decoded.substring(colon + 1));
    }
}
