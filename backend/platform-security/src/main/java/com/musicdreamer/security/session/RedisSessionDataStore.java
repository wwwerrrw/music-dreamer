package com.musicdreamer.security.session;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RedisSessionDataStore {

    void writeHash(String key, Map<String, String> fields, Duration ttl);

    Map<String, String> readHash(String key);

    void addSetMember(String key, String member, Duration ttl);

    Set<String> readSet(String key);

    void removeSetMember(String key, String member);

    void delete(String key);

    void delete(Collection<String> keys);
}
