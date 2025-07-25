package com.af.keycloak.userorderby;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class UserOrderByFilterProviderSpi implements Spi {
    
    @Override
    public boolean isInternal() {
        return false;
    }
    
    @Override
    public String getName() {
        return "user-orderby-filter";
    }
    
    @Override
    public Class<? extends Provider> getProviderClass() {
        return UserOrderByFilterProvider.class;
    }
    
    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UserOrderByFilterProviderFactory.class;
    }
}