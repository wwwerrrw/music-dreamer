package com.musicdreamer.security.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RedisSessionRepositoryTest {

    private static final String NAMESPACE = "music-dreamer:test:p1";
    private static final Duration TTL = Duration.ofHours(1);

    @Test
    void saveWritesAResolvablePrincipalWithTtlAndUserIndex() {
        InMemoryRedisSessionDataStore dataStore = new InMemoryRedisSessionDataStore();
        RedisSessionRepository repository = new RedisSessionRepository(dataStore, NAMESPACE);

        repository.save(
                "digest-user-17-a",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                TTL);

        assertThat(dataStore.hash("music-dreamer:test:p1:session:digest-user-17-a"))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "userId", "17",
                        "role", "USER",
                        "accountStatus", "ACTIVE"));
        assertThat(dataStore.ttl("music-dreamer:test:p1:session:digest-user-17-a")).isEqualTo(TTL);
        assertThat(dataStore.members("music-dreamer:test:p1:user-sessions:17"))
                .containsExactly("music-dreamer:test:p1:session:digest-user-17-a");
        assertThat(dataStore.ttl("music-dreamer:test:p1:user-sessions:17")).isEqualTo(TTL);
        assertThat(repository.find("digest-user-17-a"))
                .contains(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE));
    }

    @Test
    void deleteRemovesOnlyTheRequestedSessionAndItsIndexEntry() {
        InMemoryRedisSessionDataStore dataStore = new InMemoryRedisSessionDataStore();
        RedisSessionRepository repository = new RedisSessionRepository(dataStore, NAMESPACE);
        repository.save(
                "digest-user-17-a",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                TTL);
        repository.save(
                "digest-user-17-b",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                TTL);
        repository.save(
                "digest-user-23",
                new AuthenticatedPrincipal(23L, Role.SINGER, AccountStatus.ACTIVE),
                TTL);

        repository.delete("digest-user-17-a");

        assertThat(repository.find("digest-user-17-a")).isEmpty();
        assertThat(repository.find("digest-user-17-b")).isPresent();
        assertThat(repository.find("digest-user-23")).isPresent();
        assertThat(dataStore.members("music-dreamer:test:p1:user-sessions:17"))
                .containsExactly("music-dreamer:test:p1:session:digest-user-17-b");
    }

    @Test
    void deleteAllForUserLeavesOtherUsersSessionsIntact() {
        InMemoryRedisSessionDataStore dataStore = new InMemoryRedisSessionDataStore();
        RedisSessionRepository repository = new RedisSessionRepository(dataStore, NAMESPACE);
        repository.save(
                "digest-user-17-a",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                TTL);
        repository.save(
                "digest-user-17-b",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                TTL);
        repository.save(
                "digest-user-23",
                new AuthenticatedPrincipal(23L, Role.SINGER, AccountStatus.ACTIVE),
                TTL);

        repository.deleteAllForUser(17L);

        assertThat(repository.find("digest-user-17-a")).isEmpty();
        assertThat(repository.find("digest-user-17-b")).isEmpty();
        assertThat(repository.find("digest-user-23")).isPresent();
        assertThat(dataStore.members("music-dreamer:test:p1:user-sessions:17")).isEmpty();
        assertThat(dataStore.members("music-dreamer:test:p1:user-sessions:23"))
                .containsExactly("music-dreamer:test:p1:session:digest-user-23");
    }

    @Test
    void malformedStoredSessionsAreRejectedAsMissing() {
        InMemoryRedisSessionDataStore dataStore = new InMemoryRedisSessionDataStore();
        RedisSessionRepository repository = new RedisSessionRepository(dataStore, NAMESPACE);
        dataStore.writeHash(
                "music-dreamer:test:p1:session:missing-status",
                Map.of("userId", "17", "role", "USER"),
                TTL);
        dataStore.writeHash(
                "music-dreamer:test:p1:session:forged-role",
                Map.of("userId", "17", "role", "SUPERUSER", "accountStatus", "ACTIVE"),
                TTL);
        dataStore.writeHash(
                "music-dreamer:test:p1:session:invalid-user-id",
                Map.of("userId", "not-a-number", "role", "USER", "accountStatus", "ACTIVE"),
                TTL);

        assertThat(repository.find("missing-status")).isEmpty();
        assertThat(repository.find("forged-role")).isEmpty();
        assertThat(repository.find("invalid-user-id")).isEmpty();
    }

    private static final class InMemoryRedisSessionDataStore implements RedisSessionDataStore {

        private final Map<String, Map<String, String>> hashes = new LinkedHashMap<>();
        private final Map<String, Set<String>> sets = new LinkedHashMap<>();
        private final Map<String, Duration> ttls = new LinkedHashMap<>();

        @Override
        public void writeHash(String key, Map<String, String> fields, Duration ttl) {
            hashes.put(key, new LinkedHashMap<>(fields));
            ttls.put(key, ttl);
        }

        @Override
        public Map<String, String> readHash(String key) {
            return Map.copyOf(hashes.getOrDefault(key, Map.of()));
        }

        @Override
        public void addSetMember(String key, String member, Duration ttl) {
            sets.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(member);
            ttls.put(key, ttl);
        }

        @Override
        public Set<String> readSet(String key) {
            return Set.copyOf(sets.getOrDefault(key, Set.of()));
        }

        @Override
        public void removeSetMember(String key, String member) {
            sets.getOrDefault(key, Set.of()).remove(member);
        }

        @Override
        public void delete(String key) {
            hashes.remove(key);
            sets.remove(key);
            ttls.remove(key);
        }

        @Override
        public void delete(Collection<String> keys) {
            keys.forEach(this::delete);
        }

        Map<String, String> hash(String key) {
            return readHash(key);
        }

        Set<String> members(String key) {
            return readSet(key);
        }

        Duration ttl(String key) {
            return ttls.get(key);
        }
    }
}
