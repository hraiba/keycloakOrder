package com.af.keycloak.userorderby;

import org.keycloak.models.KeycloakSession;

public class UserOrderByFilterProviderImpl implements UserOrderByFilterProvider {
    
    private final KeycloakSession session;
    
    public UserOrderByFilterProviderImpl(KeycloakSession session) {
        this.session = session;
    }
    
    @Override
    public void processOrderByRequest() {
        // Implementation for any session-specific processing if needed
    }
    
    @Override
    public void close() {
        // Cleanup resources if needed
    }
}