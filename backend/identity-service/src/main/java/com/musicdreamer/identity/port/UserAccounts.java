package com.musicdreamer.identity.port;

import com.musicdreamer.identity.domain.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserAccounts {

    Optional<UserAccount> findByLogin(String login);

    Optional<UserAccount> findById(long userId);

    List<UserAccount> findAll();

    void save(UserAccount account);

    UserAccount createPending(String username, String email, String passwordHash);
}
