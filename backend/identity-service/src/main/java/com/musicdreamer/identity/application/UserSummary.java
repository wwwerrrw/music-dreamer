package com.musicdreamer.identity.application;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;

public record UserSummary(
        long id,
        String username,
        String email,
        Role role,
        AccountStatus status,
        long version) {
}
