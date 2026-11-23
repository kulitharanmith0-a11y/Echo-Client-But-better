package com.blamejared.clumps.client.utils.targeting;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;

public final class EntityQueryCache {
    private final Map<Entity, Long> lastQueryTimes = new WeakHashMap<>();

    public EntityQueryCache() {
    }

    
    public boolean wasQueriedRecently(long now, Entity entity, int interval) {
        Long previous = lastQueryTimes.get(entity);
        return previous != null && now - previous < interval;
    }

    public void recordQuery(long time, Entity entity) {
        lastQueryTimes.put(entity, time);
    }

    public void clear() {
        lastQueryTimes.clear();
    }
}
