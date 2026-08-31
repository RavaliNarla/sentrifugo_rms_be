package com.bob.commonutil.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CaptchaStore {

    private final Cache<UUID, String> cache;

    public CaptchaStore(Cache<UUID, String> cache) {
        this.cache = cache;
    }

    public void save(UUID id, String text) {
        cache.put(id, text);
    }

    public String get(UUID id) {
        return cache.getIfPresent(id); // auto handles expiry
    }

    public void remove(UUID id) {
        cache.invalidate(id);
    }
}