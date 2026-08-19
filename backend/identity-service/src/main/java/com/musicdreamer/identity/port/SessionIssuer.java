package com.musicdreamer.identity.port;

import com.musicdreamer.identity.application.LoginSession;
import com.musicdreamer.identity.domain.UserAccount;
import java.time.Instant;

public interface SessionIssuer {

    LoginSession issue(UserAccount account, Instant issuedAt);
}
