package com.musicdreamer.identity.application;

import com.musicdreamer.identity.port.SessionRevoker;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class AccountAdministrationService {

    private final UserAccounts users;
    private final SessionRevoker sessions;

    public AccountAdministrationService(UserAccounts users, SessionRevoker sessions) {
        this.users = Objects.requireNonNull(users, "users");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Transactional
    public void lockUser(AuthenticatedPrincipal actor, long userId) {
        requireActiveAdmin(actor);
        var user = users.findById(userId).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.USER_NOT_FOUND));
        users.save(user.lock());
        sessions.revokeAllForUser(userId);
    }

    @Transactional
    public void restoreUser(AuthenticatedPrincipal actor, long userId) {
        requireActiveAdmin(actor);
        var user = users.findById(userId).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.USER_NOT_FOUND));
        if (user.status() != com.musicdreamer.security.AccountStatus.LOCKED) {
            throw new IdentityFailure(IdentityFailure.Reason.ACCOUNT_NOT_LOCKED);
        }
        users.save(user.restore());
        sessions.revokeAllForUser(userId);
    }

    @Transactional
    public void promoteToSinger(AuthenticatedPrincipal actor, long userId) {
        requireActiveAdmin(actor);
        var user = users.findById(userId).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.USER_NOT_FOUND));
        if (user.role() != Role.USER) {
            throw new IdentityFailure(IdentityFailure.Reason.ROLE_CONFLICT);
        }
        users.save(user.promoteToSinger());
        sessions.revokeAllForUser(userId);
    }

    public List<UserSummary> listUsers(AuthenticatedPrincipal actor) {
        requireActiveAdmin(actor);
        return users.findAll().stream()
                .map(user -> new UserSummary(
                        user.id(),
                        user.username(),
                        user.email(),
                        user.role(),
                        user.status(),
                        user.version()))
                .toList();
    }

    private static void requireActiveAdmin(AuthenticatedPrincipal actor) {
        if (!Objects.requireNonNull(actor, "actor").hasActiveRole(Role.ADMIN)) {
            throw new IdentityFailure(IdentityFailure.Reason.FORBIDDEN);
        }
    }
}
