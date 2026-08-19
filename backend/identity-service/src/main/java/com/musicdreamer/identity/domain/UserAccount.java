package com.musicdreamer.identity.domain;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.util.Objects;

public record UserAccount(
        long id,
        String username,
        String email,
        String passwordHash,
        Role role,
        AccountStatus status,
        long version) {

    public UserAccount {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(status, "status");
    }

    public UserAccount activate() {
        return new UserAccount(id, username, email, passwordHash, role, AccountStatus.ACTIVE, version + 1);
    }

    public UserAccount lock() {
        return new UserAccount(id, username, email, passwordHash, role, AccountStatus.LOCKED, version + 1);
    }
}
