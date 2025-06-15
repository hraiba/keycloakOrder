package com.af.keycloak.userorderby;

import org.keycloak.provider.Provider;

public interface UserOrderByFilterProvider extends Provider {
    void processOrderByRequest();
}