package ca.uhn.fhir.jpa.starter.interceptor;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple API Token Authentication Interceptor
 * 
 * This interceptor checks for a valid API token in the Authorization header.
 * 
 * Usage:
 *   Authorization: Bearer <api-token>
 */
@Component
public class ApiTokenAuthInterceptor extends InterceptorAdapter {

    private static final Logger ourLog = LoggerFactory.getLogger(ApiTokenAuthInterceptor.class);

    @Value("${hapi.fhir.auth.api_token:ddx-api-token-2024}")
    private String validApiToken;

    @Value("${hapi.fhir.auth.enabled:false}")
    private boolean authEnabled;

    @Override
    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        // Skip auth if not enabled
        if (!authEnabled) {
            return true;
        }

        String requestURI = theRequest.getRequestURI();

        // Allow public endpoints without authentication
        if (isPublicEndpoint(requestURI)) {
            ourLog.debug("Allowing public endpoint: {}", requestURI);
            return true;
        }

        // Check Authorization header
        String authHeader = theRequest.getHeader("Authorization");
        
        if (authHeader == null || authHeader.isEmpty()) {
            ourLog.warn("Missing Authorization header for: {}", requestURI);
            throw new AuthenticationException("Missing Authorization header. Use: Authorization: Bearer <api-token>");
        }

        // Check Bearer token format
        if (!authHeader.startsWith("Bearer ")) {
            ourLog.warn("Invalid Authorization header format for: {}", requestURI);
            throw new AuthenticationException("Invalid Authorization header format. Use: Authorization: Bearer <api-token>");
        }

        // Extract and validate token
        String token = authHeader.substring(7).trim();
        
        if (!validApiToken.equals(token)) {
            ourLog.warn("Invalid API token for: {}", requestURI);
            throw new AuthenticationException("Invalid API token");
        }

        ourLog.debug("Successfully authenticated request to: {}", requestURI);
        return true;
    }

    /**
     * Check if the endpoint is public (doesn't require authentication)
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.contains("/metadata") || 
               uri.contains("/actuator/health") ||
               uri.contains("/.well-known/") ||
               uri.contains("/oauth/");
    }
}
