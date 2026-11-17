package com.brunojaeger.worldevents.core;

import com.brunojaeger.worldevents.event.WorldEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;

public final class EventRegistry {
    private final Map<String, Supplier<WorldEvent>> factories = new LinkedHashMap<>();

    public void register(String id, Supplier<WorldEvent> factory) {
        if (id == null || id.isBlank() || factory == null) {
            throw new IllegalArgumentException("El identificador y la fábrica del evento son obligatorios.");
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        if (factories.containsKey(normalized)) {
            throw new IllegalStateException("Ya existe un evento registrado con el ID '" + normalized + "'.");
        }
        factories.put(normalized, factory);
    }

    public WorldEvent create(String id) {
        if (id == null) return null;
        Supplier<WorldEvent> factory = factories.get(id.toLowerCase(Locale.ROOT));
        return factory == null ? null : factory.get();
    }

    public boolean contains(String id) {
        return id != null && factories.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public boolean unregister(String id) {
        if (id == null) return false;
        String normalized = id.toLowerCase(Locale.ROOT);
        if ("meteorfall".equals(normalized) || "losttreasure".equals(normalized)) return false;
        return factories.remove(normalized) != null;
    }

    public Map<String, Supplier<WorldEvent>> getFactories() {
        return Map.copyOf(factories);
    }
}
