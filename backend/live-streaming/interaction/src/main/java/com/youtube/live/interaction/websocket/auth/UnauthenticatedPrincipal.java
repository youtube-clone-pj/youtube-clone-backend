package com.youtube.live.interaction.websocket.auth;

import java.security.Principal;

public class UnauthenticatedPrincipal implements Principal {

    @Override
    public String getName() {
        return "anonymous";
    }
}
