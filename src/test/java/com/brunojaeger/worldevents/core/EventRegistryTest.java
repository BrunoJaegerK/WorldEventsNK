package com.brunojaeger.worldevents.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventRegistryTest {

    @Test
    void registersIdsCaseInsensitivelyAndRejectsDuplicates() {
        EventRegistry registry = new EventRegistry();

        registry.register("CustomEvent", () -> null);

        assertTrue(registry.contains("customevent"));
        assertTrue(registry.contains("CUSTOMEVENT"));
        assertThrows(IllegalStateException.class,
                () -> registry.register("CUSTOMEVENT", () -> null));
    }

    @Test
    void protectsBuiltInEventsFromExternalUnregister() {
        EventRegistry registry = new EventRegistry();
        registry.register("meteorfall", () -> null);
        registry.register("losttreasure", () -> null);

        assertFalse(registry.unregister("METEORFALL"));
        assertFalse(registry.unregister("losttreasure"));
        assertTrue(registry.contains("meteorfall"));
        assertTrue(registry.contains("losttreasure"));
    }

    @Test
    void unregistersCustomEventsAndHandlesNullIds() {
        EventRegistry registry = new EventRegistry();
        registry.register("custom", () -> null);

        assertTrue(registry.unregister("CUSTOM"));
        assertFalse(registry.contains("custom"));
        assertFalse(registry.unregister(null));
        assertFalse(registry.contains(null));
        assertNull(registry.create(null));
    }

    @Test
    void rejectsInvalidRegistrations() {
        EventRegistry registry = new EventRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(null, () -> null));
        assertThrows(IllegalArgumentException.class, () -> registry.register(" ", () -> null));
        assertThrows(IllegalArgumentException.class, () -> registry.register("custom", null));
    }
}
