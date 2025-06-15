package com.af.keycloak.orderby;

import org.keycloak.models.KeycloakSession;

public class OrderByResourceProviderImpl implements OrderByResourceProvider {

    private final KeycloakSession session;

    public OrderByResourceProviderImpl(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        // Implement the logic to return the resource
        return new OrderByResource(session);
    }

    @Override
    public void close() {
        // Implement any cleanup logic if necessary
        System.out.println("OrderByResourceProviderImpl closed.");
    }
}


