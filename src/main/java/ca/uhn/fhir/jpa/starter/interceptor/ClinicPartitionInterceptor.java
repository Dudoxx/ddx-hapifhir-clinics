package ca.uhn.fhir.jpa.starter.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Clinic-based Multi-Tenant Partition Interceptor
 * 
 * This interceptor manages partition assignment based on clinic identification.
 * Each clinic (tenant) is isolated in its own partition.
 * 
 * OFFICIAL FIX (Nov 2025): Handles SystemRequestDetails for internal operations
 * like subscription cache warmup and IG installation.
 * 
 * Tenant identification methods:
 * 1. X-Clinic-ID header (primary method)
 * 2. Subdomain (e.g., hamburg.fhir.dudoxx.com)
 * 3. URL path (e.g., /fhir/clinic/hamburg/Patient)
 * 
 * Clinic-to-Partition mapping:
 * - ddx-hamburg-clinic -> Partition 1
 * - ddx-berlin-clinic  -> Partition 2
 * - ddx-munich-clinic  -> Partition 3
 * 
 * Usage:
 *   X-Clinic-ID: ddx-hamburg-clinic
 *   Authorization: Bearer ddx-api-token-2024
 */
@Component
@Interceptor
public class ClinicPartitionInterceptor {

    private static final Logger ourLog = LoggerFactory.getLogger(ClinicPartitionInterceptor.class);
    
	// Clinic ID to Partition ID mapping
	private static final Map<String, Integer> CLINIC_PARTITION_MAP = new HashMap<>();

    static {
        // System partition (for background jobs and subscriptions)
        CLINIC_PARTITION_MAP.put("default", 0);
        
        // Register clinics with their partition IDs
        CLINIC_PARTITION_MAP.put("ddx-hamburg-clinic", 1);
        CLINIC_PARTITION_MAP.put("ddx-berlin-clinic", 2);
        CLINIC_PARTITION_MAP.put("ddx-munich-clinic", 3);
        CLINIC_PARTITION_MAP.put("ddx-frankfurt-clinic", 4);
        CLINIC_PARTITION_MAP.put("ddx-cologne-clinic", 5);
        
        // Shared resources partition
        CLINIC_PARTITION_MAP.put("ddx-shared-clinic", 6);
    }
    
    /**
     * Identify the partition for read operations
     * 
     * OFFICIAL FIX (Nov 2025): Use DEFAULT partition for system operations instead of ALL_PARTITIONS.
     * This prevents "HAPI-1220: This server is not configured to support search against all partitions" errors
     * during subscription cache warmup and search parameter cache initialization.
     * 
     * @param theRequestDetails The request details (null for system operations, SystemRequestDetails for internal ops)
     * @return The partition ID to use
     */
    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
    public RequestPartitionId identifyPartitionForRead(RequestDetails theRequestDetails) {
        // System operations (null RequestDetails) - use DEFAULT partition
        if (theRequestDetails == null) {
            ourLog.debug("PARTITION READ: NULL RequestDetails - returning DEFAULT partition");
            return RequestPartitionId.defaultPartition();
        }
        
        // System operations (SystemRequestDetails) - use DEFAULT partition
        // This includes subscription cache warmup and search parameter cache initialization
        if (theRequestDetails instanceof SystemRequestDetails) {
            ourLog.debug("PARTITION READ: SystemRequestDetails detected - returning DEFAULT partition");
            return RequestPartitionId.defaultPartition();
        }
        
        // Extract clinic ID from request headers for normal client API calls
        String clinicId = extractClinicId(theRequestDetails);
        if (clinicId == null) {
            // No clinic ID provided - use DEFAULT partition (for public endpoints like /metadata)
            ourLog.debug("PARTITION READ: No clinic ID - returning DEFAULT partition");
            return RequestPartitionId.defaultPartition();
        }
        
        // Normal client request with valid clinic ID - enforce partition isolation
        ourLog.debug("PARTITION READ: Clinic ID {} found - enforcing partition", clinicId);
        return identifyPartition(theRequestDetails, "READ");
    }
    
    /**
     * Identify the partition for create operations
     * 
     * @param theRequestDetails The request details
     * @return The partition ID to use
     */
    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
    public RequestPartitionId identifyPartitionForCreate(RequestDetails theRequestDetails) {
        // System operations (IG installation, subscriptions) - use default partition
        if (theRequestDetails == null || theRequestDetails instanceof SystemRequestDetails) {
            ourLog.debug("System operation detected for CREATE - using DEFAULT partition");
            return RequestPartitionId.defaultPartition();
        }
        
        // Extract clinic ID for normal operations
        String clinicId = extractClinicId(theRequestDetails);
        if (clinicId == null) {
            // No partition specified - use DEFAULT partition for system resources
            ourLog.debug("No clinic ID for CREATE operation - using DEFAULT partition (0)");
            return RequestPartitionId.defaultPartition();
        }
        
        return identifyPartition(theRequestDetails, "CREATE");
    }
    
    /**
     * Identify the partition for any operation (fallback)
     * 
     * @param theRequestDetails The request details
     * @return The partition ID to use
     */
    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_ANY)
    public RequestPartitionId identifyPartitionForAny(RequestDetails theRequestDetails) {
        // System operations - use DEFAULT partition
        if (theRequestDetails == null || theRequestDetails instanceof SystemRequestDetails) {
            ourLog.debug("System operation detected for ANY - returning DEFAULT partition");
            return RequestPartitionId.defaultPartition();
        }
        
        // Extract clinic ID
        String clinicId = extractClinicId(theRequestDetails);
        if (clinicId == null) {
            ourLog.debug("No clinic ID for ANY operation - returning DEFAULT partition");
            return RequestPartitionId.defaultPartition();
        }
        
        return identifyPartition(theRequestDetails, "ANY");
    }
    
    /**
     * Core partition identification logic
     * 
     * @param theRequestDetails The request details
     * @param operation The operation type (for logging)
     * @return The partition ID
     * @throws AuthenticationException if clinic ID is invalid
     */
    private RequestPartitionId identifyPartition(RequestDetails theRequestDetails, String operation) {
        String clinicId = extractClinicId(theRequestDetails);
        
        if (clinicId == null) {
            ourLog.warn("No clinic ID found in request for {} operation", operation);
            throw new AuthenticationException("Missing clinic identification. Please provide X-Clinic-ID header.");
        }
        
        Integer partitionId = CLINIC_PARTITION_MAP.get(clinicId);
        
        if (partitionId == null) {
            ourLog.warn("Unknown clinic ID: {}", clinicId);
            throw new AuthenticationException("Unknown clinic ID: " + clinicId);
        }
        
        ourLog.debug("Assigned partition {} for clinic {} ({})", partitionId, clinicId, operation);
        
        return RequestPartitionId.fromPartitionId(partitionId);
    }
    
    /**
     * Get partition ID for a clinic - used by system operations
     * 
     * @param clinicId The clinic ID
     * @return The partition ID or null if not found
     */
    public static Integer getPartitionIdForClinic(String clinicId) {
        return CLINIC_PARTITION_MAP.get(clinicId);
    }
    
    /**
     * Extract clinic ID from request
     * Priority: Header > Subdomain > Path parameter
     * 
     * @param theRequestDetails The request details
     * @return The clinic ID or null if not found
     */
    private String extractClinicId(RequestDetails theRequestDetails) {
        // Method 1: Check X-Clinic-ID header (primary method)
        String clinicId = theRequestDetails.getHeader("X-Clinic-ID");
        if (clinicId != null && !clinicId.isEmpty()) {
            ourLog.debug("Clinic ID from header: {}", clinicId);
            return clinicId.toLowerCase().trim();
        }
        
        // Method 2: Check subdomain (if available through servlet request)
        try {
            Object servletRequestObj = theRequestDetails.getAttribute("jakarta.servlet.http.HttpServletRequest");
            if (servletRequestObj instanceof HttpServletRequest) {
                HttpServletRequest servletRequest = (HttpServletRequest) servletRequestObj;
                String serverName = servletRequest.getServerName();
                if (serverName != null && !serverName.equals("localhost")) {
                    // Extract subdomain (e.g., hamburg from hamburg.fhir.dudoxx.com)
                    String[] parts = serverName.split("\\.");
                    if (parts.length > 2) {
                        String subdomain = parts[0];
                        clinicId = "ddx-" + subdomain + "-clinic";
                        ourLog.debug("Clinic ID from subdomain: {}", clinicId);
                        return clinicId.toLowerCase();
                    }
                }
            }
        } catch (Exception e) {
            ourLog.debug("Could not extract subdomain: {}", e.getMessage());
        }
        
        // Method 3: Check URL path parameter
        String requestPath = theRequestDetails.getRequestPath();
        if (requestPath != null && requestPath.contains("/clinic/")) {
            // Extract from path like: /fhir/clinic/hamburg/Patient
            String[] pathParts = requestPath.split("/");
            for (int i = 0; i < pathParts.length - 1; i++) {
                if ("clinic".equals(pathParts[i]) && i + 1 < pathParts.length) {
                    String clinicName = pathParts[i + 1];
                    clinicId = "ddx-" + clinicName + "-clinic";
                    ourLog.debug("Clinic ID from path: {}", clinicId);
                    return clinicId.toLowerCase();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get all registered clinic IDs
     * 
     * @return Map of clinic IDs to partition IDs
     */
    public static Map<String, Integer> getRegisteredClinics() {
        return new HashMap<>(CLINIC_PARTITION_MAP);
    }
    
    /**
     * Register a new clinic dynamically
     * 
     * @param clinicId The clinic ID
     * @param partitionId The partition ID
     */
    public static void registerClinic(String clinicId, Integer partitionId) {
        CLINIC_PARTITION_MAP.put(clinicId.toLowerCase(), partitionId);
        ourLog.info("Registered new clinic: {} -> Partition {}", clinicId, partitionId);
    }
}
