package com.alibaba.rcm;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.AbstractResourceContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ResourceContainerMonitor {
    private static Map<Long, ResourceContainer> tenantContainerMap = new ConcurrentHashMap<>();
    private static AtomicLong idGen = new AtomicLong(0);

    public static long register(ResourceContainer resourceContainer) {
        long id = idGen.getAndIncrement();
        tenantContainerMap.put(id, resourceContainer);
        return id;
    }

    public static void deregister(long id) {
        tenantContainerMap.remove(id);
    }

    public static ResourceContainer getContainerById(long id) {
        return tenantContainerMap.get(id);
    }

    public static List<Long> getAllContainerIds() {
        return new ArrayList<>(tenantContainerMap.keySet());
    }

    public static List<Constraint> getConstraintsById(long id) {
        AbstractResourceContainer resourceContainer = (AbstractResourceContainer) tenantContainerMap.get(id);
        return StreamSupport
                .stream(resourceContainer.getConstraints().spliterator(), false)
                .collect(Collectors.toList());
    }

    public long getContainerConsumedAmount(long id) {
        return 0;
    }

}
