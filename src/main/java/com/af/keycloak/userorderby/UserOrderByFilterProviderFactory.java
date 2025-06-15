package com.af.keycloak.userorderby;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public class UserOrderByFilterProviderFactory implements ProviderFactory<UserOrderByFilterProvider> {
    
    public static final String PROVIDER_ID = "user-orderby-filter";
    
    @Override
    public UserOrderByFilterProvider create(KeycloakSession session) {
        return new UserOrderByFilterProviderImpl(session);
    }
    
    @Override
    public void init(Config.Scope config) {
        // Initialization logic - can be used for configuration
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post-initialization logic
    }
    
    @Override
    public void close() {
        // Cleanup logic
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}