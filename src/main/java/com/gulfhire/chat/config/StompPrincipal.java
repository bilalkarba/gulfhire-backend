package com.gulfhire.chat.config;

import java.security.Principal;

/** STOMP principal whose name is the authenticated user's UUID. */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
