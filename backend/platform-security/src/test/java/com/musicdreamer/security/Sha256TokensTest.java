package com.musicdreamer.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256TokensTest {

    @Test
    void digestMatchesIndependentFixedVector() {
        assertThat(Sha256Tokens.digest("raw-session-token"))
                .isEqualTo("e6c276c51996dfa4b71f39f34f5f1a5a8f116e29eb538fab6403dd689631c622");
    }
}
