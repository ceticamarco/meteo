package com.ceticamarco.cache;

import com.ceticamarco.Result.IResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

public class Cache {
    private final int timeToLive;
    private record cacheT(IResult value, LocalDateTime timestamp) {}
    private final HashMap<String, cacheT> cache = new HashMap<>();

    /**
     * Returns true whether 'time-to-live' parameter is greater than zero
     * @return boolean value
     */
    private boolean isCacheEnabled() {
        return this.timeToLive > 0;
    }

    public Cache(int ttl) {
        this.timeToLive = ttl;
    }

    /**
     * Given a key and a value, store it in the cache
     */
    public void addValue(String key, IResult value) {
        if(!isCacheEnabled()) { return; }

        var timestamp = LocalDateTime.now();
        var cachedValue = new cacheT(value, timestamp);
        this.cache.put(key.toLowerCase(), cachedValue);
    }

    /**
     * Given a key, retrieve its value if and only if
     * it exists in the cache and it is not expired
     */
    public Optional<IResult> getValue(String key) {
        if(!isCacheEnabled()) { return Optional.empty(); }

        cacheT cachedValue = this.cache.get(key.toLowerCase());
        if(cachedValue == null) { return Optional.empty(); }

        var timestamp = cachedValue.timestamp;
        var offset = LocalDateTime.now().minusHours(this.timeToLive);

        // Return nothing if the cached element is expired
        if(timestamp.isBefore(offset)) { return Optional.empty(); }

        return Optional.of(cachedValue.value);
    }

    /**
     * Given a key, remove it regardless of whether
     * it exists or not in the cache
     */
    public void delValue(String key) {
        this.cache.remove(key);
    }
}
