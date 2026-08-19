package com.musicdreamer.security.session;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class SpringRedisSessionDataStore implements RedisSessionDataStore {

    private final StringRedisTemplate redis;
    private final HashOperations<String, String, String> hashes;
    private final SetOperations<String, String> sets;

    public SpringRedisSessionDataStore(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.hashes = redis.opsForHash();
        this.sets = redis.opsForSet();
    }

    @Override
    public void writeHash(String key, Map<String, String> fields, Duration ttl) {
        hashes.putAll(key, fields);
        redis.expire(key, ttl);
    }

    @Override
    public Map<String, String> readHash(String key) {
        return Map.copyOf(hashes.entries(key));
    }

    @Override
    public void addSetMember(String key, String member, Duration ttl) {
        sets.add(key, member);
        redis.expire(key, ttl);
    }

    @Override
    public Set<String> readSet(String key) {
        Set<String> members = sets.members(key);
        return members == null ? Set.of() : Set.copyOf(members);
    }

    @Override
    public void removeSetMember(String key, String member) {
        sets.remove(key, member);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public void delete(Collection<String> keys) {
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
