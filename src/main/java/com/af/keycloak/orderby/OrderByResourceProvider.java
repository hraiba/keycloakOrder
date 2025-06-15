package com.af.keycloak.orderby;

import org.keycloak.services.resource.RealmResourceProvider;

public interface OrderByResourceProvider extends RealmResourceProvider{

    Object getResource();

    @Override
    default void close() {}
}
