package com.af.keycloak.userorderby;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.representations.idm.UserRepresentation;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.regex.Pattern;

@Provider
public class UserOrderByFilter implements ContainerRequestFilter, ContainerResponseFilter {
    
    private static final Logger logger = Logger.getLogger(UserOrderByFilter.class);
    private static final String ORDERBY_PROPERTY = "CUSTOM_ORDERBY";
    private static final String ORDERBY_PARAM = "ORDERBY_PARAM";
    private static final Pattern USERS_ENDPOINT_PATTERN = 
        Pattern.compile(".*/admin/realms/[^/]+/users.*");
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getPath();
        
        // Check if this is a GET request to users endpoint with orderby parameter
        if (shouldIntercept(requestContext, path, uriInfo)) {
            logger.infof("Intercepting request: %s with orderby parameter", path);
            
            String orderBy = uriInfo.getQueryParameters().getFirst("orderby");
            requestContext.setProperty(ORDERBY_PROPERTY, true);
            requestContext.setProperty(ORDERBY_PARAM, orderBy);
            
            // Validate orderby parameter
            if (!isValidOrderByParam(orderBy)) {
                Response errorResponse = Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid orderby parameter: " + orderBy))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
                requestContext.abortWith(errorResponse);
                return;
            }
        }
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext, 
                      ContainerResponseContext responseContext) throws IOException {
        
        // Only process if we marked this request for custom handling
        if (requestContext.getProperty(ORDERBY_PROPERTY) == null) {
            return;
        }
        
        String orderBy = (String) requestContext.getProperty(ORDERBY_PARAM);
        
        try {
            // Get the original response entity
            Object entity = responseContext.getEntity();
            
            if (entity instanceof List) {
                @SuppressWarnings("unchecked")
                List<UserRepresentation> users = (List<UserRepresentation>) entity;
                
                // Apply custom ordering
                List<UserRepresentation> orderedUsers = applyOrdering(users, orderBy);
                
                // Replace the response entity
                responseContext.setEntity(orderedUsers);
                logger.infof("Applied orderby '%s' to %d users", orderBy, orderedUsers.size());
            }
            
        } catch (Exception e) {
            logger.errorf("Error applying orderby: %s", e.getMessage());
            // Don't break the original response, just log the error
        }
    }
    
    private boolean shouldIntercept(ContainerRequestContext requestContext, 
                                  String path, UriInfo uriInfo) {
        return "GET".equals(requestContext.getMethod()) &&
               USERS_ENDPOINT_PATTERN.matcher(path).matches() &&
               uriInfo.getQueryParameters().containsKey("orderby");
    }
    
    private boolean isValidOrderByParam(String orderBy) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return false;
        }
        
        // Define allowed fields for ordering
        Set<String> allowedFields = Set.of(
            "username", "firstName", "lastName", "email", 
            "createdTimestamp", "enabled"
        );
        
        // Handle desc/asc modifiers
        String field = orderBy.toLowerCase().trim();
        if (field.endsWith(" desc") || field.endsWith(" asc")) {
            field = field.substring(0, field.lastIndexOf(" ")).trim();
        }
        
        return allowedFields.contains(field);
    }
    
    private List<UserRepresentation> applyOrdering(List<UserRepresentation> users, 
                                                  String orderBy) {
        if (users == null || users.isEmpty()) {
            return users;
        }
        
        String field = orderBy.toLowerCase().trim();
        boolean descending = false;
        
        // Check for desc/asc modifiers
        if (field.endsWith(" desc")) {
            descending = true;
            field = field.substring(0, field.lastIndexOf(" ")).trim();
        } else if (field.endsWith(" asc")) {
            field = field.substring(0, field.lastIndexOf(" ")).trim();
        }
        
        Comparator<UserRepresentation> comparator = createComparator(field);
        
        if (descending) {
            comparator = comparator.reversed();
        }
        
        return users.stream()
                   .sorted(comparator)
                   .collect(Collectors.toList());
    }
    
    private Comparator<UserRepresentation> createComparator(String field) {
        switch (field) {
            case "username":
                return Comparator.comparing(user -> 
                    user.getUsername() != null ? user.getUsername().toLowerCase() : "");
            case "firstname":
                return Comparator.comparing(user -> 
                    user.getFirstName() != null ? user.getFirstName().toLowerCase() : "");
            case "lastname":
                return Comparator.comparing(user -> 
                    user.getLastName() != null ? user.getLastName().toLowerCase() : "");
            case "email":
                return Comparator.comparing(user -> 
                    user.getEmail() != null ? user.getEmail().toLowerCase() : "");
            case "createdtimestamp":
                return Comparator.comparing(user -> 
                    user.getCreatedTimestamp() != null ? user.getCreatedTimestamp() : 0L);
            case "enabled":
                return Comparator.comparing(user -> 
                    user.isEnabled() != null ? user.isEnabled() : false);
            default:
                return Comparator.comparing(user -> 
                    user.getUsername() != null ? user.getUsername().toLowerCase() : "");
        }
    }
}