package com.af.keycloak.orderby;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class OrderByResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "orderby-users";

    @Override
    public void close() {
        System.out.println("OrderByResourceProviderFactory closed.");
    }

    @Override
    public OrderByResourceProvider create(KeycloakSession session) {
        return new OrderByResourceProviderImpl(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Scope config) {
        System.out.println("OrderByResourceProviderFactory initialized with config: " + config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        System.out.println("OrderByResourceProviderFactory post-initialization with factory: " + factory);
    }
}