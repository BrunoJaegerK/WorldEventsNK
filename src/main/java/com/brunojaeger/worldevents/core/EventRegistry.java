package com.brunojaeger.worldevents.core;

import com.brunojaeger.worldevents.event.WorldEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class EventRegistry {
    private final Map<String, Supplier<WorldEvent>> factories = new LinkedHashMap<>();

    public void register(String id, Supplier<WorldEvent> factory) {
        if (id == null || id.isBlank() || factory == null) {
            throw new IllegalArgumentException("El identificador y la fábrica del evento son obligatorios.");
        }
        factories.put(id.toLowerCase(), factory);
    }

    public WorldEvent create(String id) {
        Supplier<WorldEvent> factory = factories.get(id.toLowerCase());
        return factory == null ? null : factory.get();
    }

    public boolean contains(String id) {
        return factories.containsKey(id.toLowerCase());
    }

    public Map<String, Supplier<WorldEvent>> getFactories() {
        return Map.copyOf(factories);
    }
}
