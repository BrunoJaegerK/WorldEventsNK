# API para eventos externos

WorldEventsNK permite registrar eventos de otros plugins sin modificar el núcleo. El plugin que aporta el evento debe declarar una dependencia de WorldEventsNK y compilar contra su JAR.

## Registro

En el `plugin.yml` del plugin externo:

```yaml
name: MyWorldEvent
main: example.MyWorldEventPlugin
version: 1.0.0
depend: [WorldEventsNK]
```

Registro desde `onEnable()`:

```java
Plugin dependency = getServer().getPluginManager().getPlugin("WorldEventsNK");
if (!(dependency instanceof WorldEventsNK worldEvents)) {
    getLogger().warning("WorldEventsNK no está disponible.");
    return;
}
worldEvents.getEventManager().getRegistry().register(
    "my-event",
    () -> new MyEvent(this)
);
```

El ID debe ser único. El registro rechaza duplicados para impedir que un plugin externo reemplace accidentalmente MeteorFall o LostTreasure.

## Contrato

La clase debe implementar `WorldEvent`:

- `start()` prepara el evento y devuelve `true` solo si pudo iniciarlo.
- `tick()` se ejecuta una vez por segundo en el hilo del servidor.
- `stop()` debe limpiar los recursos temporales del evento.
- `isFinished()` devuelve `true` al terminar o fallar.
- `getState()` informa `PREPARING`, `ACTIVE`, `COMPLETED` o `FAILED`.
- `getRemainingSeconds()` y `getLocation()` son opcionales, con valores predeterminados.

Los eventos registrados aparecen en `/we list` y pueden iniciarse con `/we start my-event`. Si no hay una sección propia en `events.yml`, el evento queda habilitado por defecto con peso 50 y sin enfriamiento. Para configurarlo, añade:

```yaml
my-event:
  enabled: true
  weight: 20
  cooldown-seconds: 3600
```

Los eventos externos deben respetar las protecciones de regiones y validar las ubicaciones/bloques antes de modificar el mundo. Pueden consultar `worldEvents.getProtectionManager().canModify(...)` y registrar sus propios `RegionProtectionProvider` si necesitan integrar otra protección. El ciclo de vida de sus estructuras temporales y su persistencia son responsabilidad del plugin que los registra.
