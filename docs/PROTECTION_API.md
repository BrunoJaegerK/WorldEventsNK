# API de protección de regiones

WorldEventsNK no depende de una implementación concreta de protección. Los plugins externos pueden conectar su API implementando `RegionProtectionProvider`.

## Registro del proveedor

El plugin adaptador debe declarar WorldEventsNK como dependencia (o cargar después de él) y registrar el proveedor durante `onEnable()`:

```java
import cn.nukkit.level.Level;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.protection.RegionProtectionProvider;

public final class MyProtectionBridge extends PluginBase {
    private RegionProtectionProvider provider;

    @Override
    public void onEnable() {
        Plugin dependency = getServer().getPluginManager().getPlugin("WorldEventsNK");
        if (!(dependency instanceof WorldEventsNK worldEvents)) {
            getLogger().warning("WorldEventsNK no está disponible; el adaptador no se registrará.");
            return;
        }

        provider = new RegionProtectionProvider() {
            @Override
            public String getName() {
                return "MyProtection";
            }

            @Override
            public boolean isAvailable() {
                return MyProtectionBridge.this.isEnabled();
            }

            @Override
            public boolean canModify(Level level, int x, int y, int z, String eventId) {
                // Sustituir por la consulta real de la API de protección.
                // Devolver false si la región no permite la modificación.
                return regionApiAllowsEvent(level, x, y, z, eventId);
            }
        };

        worldEvents.getProtectionManager().registerProvider(provider);
    }

    @Override
    public void onDisable() {
        Plugin dependency = getServer().getPluginManager().getPlugin("WorldEventsNK");
        if (provider != null && dependency instanceof WorldEventsNK worldEvents) {
            worldEvents.getProtectionManager().unregisterProvider(provider);
        }
    }

    private boolean regionApiAllowsEvent(Level level, int x, int y, int z, String eventId) {
        // Implementar usando la API del plugin de protección.
        return true;
    }
}
```

## Reglas del contrato

- `canModify` se consulta para cada bloque que el evento pretende modificar.
- El adaptador debe resolver la región en el mundo y coordenadas recibidas.
- Devuelve `false` si la región deniega el cambio.
- Si la API no puede verificar permisos, devuelve `false`; no asumas que el cambio está permitido.
- Si `isAvailable()` devuelve `false`, WorldEventsNK respeta `protection.fail-safe`: con `true`, cancela la modificación; con `false`, omite ese proveedor.
- Registra el proveedor al habilitar el adaptador y retíralo al deshabilitarlo.

Este contrato es una API de extensión. No constituye por sí mismo una integración automática con WorldGuard u otro plugin: cada adaptador debe compilarse contra la API real del proveedor que conecta.
