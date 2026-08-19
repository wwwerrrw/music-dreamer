package com.musicdreamer.interaction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class InteractionControllerTest {

    @Test
    void ordinaryUserCannotPublishBroadcastMessage() {
        InteractionController controller = new InteractionController(null);
        HttpHeaders headers = headers(new AuthenticatedPrincipal(14L, Role.USER, AccountStatus.ACTIVE));

        assertThatThrownBy(() -> controller.publishMessage(headers,
                new InteractionController.MessageRequest("Notice", "Body")))
                .isInstanceOf(InteractionAuthorizationException.class);
    }

    @Test
    void inactivePrincipalCannotCreatePlaylist() {
        InteractionController controller = new InteractionController(null);
        HttpHeaders headers = headers(new AuthenticatedPrincipal(14L, Role.USER, AccountStatus.LOCKED));

        assertThatThrownBy(() -> controller.createPlaylist(headers,
                new InteractionController.PlaylistRequest("Road Trip", "Songs", "PRIVATE")))
                .isInstanceOf(InteractionAuthorizationException.class);
    }

    private static HttpHeaders headers(AuthenticatedPrincipal principal) {
        return TrustedIdentityHeaders.forPrincipal(new HttpHeaders(), principal);
    }
}
