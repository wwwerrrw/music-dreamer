package com.musicdreamer.identity.application;

import java.util.Objects;

public record RegistrationCommand(String username, String email, String rawPassword) {

    public RegistrationCommand {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(rawPassword, "rawPassword");
    }
}
