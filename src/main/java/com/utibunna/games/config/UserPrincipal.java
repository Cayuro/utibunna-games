package com.utibunna.games.config;

import java.security.Principal;

/** Minimal Principal whose name is the authenticated userId (UUID string) forwarded by the gateway. */
public record UserPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
