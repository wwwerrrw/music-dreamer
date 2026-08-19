package com.musicdreamer.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CredentialProtectionTest {

    @Test
    void bcryptHashAcceptsOnlyTheOriginalPassword() {
        BCryptCredentials credentials = new BCryptCredentials(4);

        String hash = credentials.hash("correct-password");

        assertThat(hash).doesNotContain("correct-password");
        assertThat(credentials.matches("correct-password", hash)).isTrue();
        assertThat(credentials.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void sha256TokenDigestMatchesIndependentFixedVector() {
        Sha256TokenDigester digester = new Sha256TokenDigester();

        assertThat(digester.digest("activation-token"))
                .isEqualTo("d066a4f4c1bea1ab8d47372ded551bc3f1d06dcba0608d179c4a6d2844fec548");
    }

    @Test
    void secureTokenGeneratorReturnsDistinctUrlSafe256BitTokens() {
        SecureRawTokenGenerator generator = new SecureRawTokenGenerator();

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).isNotEqualTo(first);
    }
}
