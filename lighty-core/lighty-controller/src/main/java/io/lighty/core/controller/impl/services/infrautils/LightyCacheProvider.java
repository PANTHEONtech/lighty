package io.lighty.core.controller.impl.services.infrautils;

import org.opendaylight.infrautils.caches.*;

public class LightyCacheProvider implements CacheProvider {
    @Override
    public <K, V> Cache<K, V> newCache(CacheConfig<K, V> cacheConfig, CachePolicy initialPolicy) {
        return null;
    }

    @Override
    public <K, V> Cache<K, V> newCache(CacheConfig<K, V> cacheConfig) {
        return null;
    }

    @Override
    public <K, V, E extends Exception> CheckedCache<K, V, E> newCheckedCache(CheckedCacheConfig<K, V, E> cacheConfig, CachePolicy initialPolicy) {
        return null;
    }

    @Override
    public <K, V, E extends Exception> CheckedCache<K, V, E> newCheckedCache(CheckedCacheConfig<K, V, E> cacheConfig) {
        return null;
    }
}
