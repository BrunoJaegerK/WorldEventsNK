# API de proveedores económicos

WorldEventsNK incluye soporte opcional para EconomyAPI. Otros plugins pueden conectar una economía sin que WorldEventsNK dependa de sus clases, implementando `EconomyProvider`.

## Dependencia

El plugin adaptador debe declarar WorldEventsNK como dependencia en su `plugin.yml`:

```yaml
name: MyEconomyBridge
main: example.MyEconomyBridge
version: 1.0.0
depend: [WorldEventsNK]
```

## Implementación y registro

```java
import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.economy.EconomyProvider;

public final class MyEconomyBridge extends PluginBase {
    private WorldEventsNK worldEvents;
    private EconomyProvider provider;

    @Override
    public void onEnable() {
        Plugin dependency = getServer().getPluginManager().getPlugin("WorldEventsNK");
        if (!(dependency instanceof WorldEventsNK)) return;
        worldEvents = (WorldEventsNK) dependency;

        provider = new EconomyProvider() {
            @Override
            public String getName() {
                return "MyEconomy";
            }

            @Override
            public boolean isAvailable() {
                return MyEconomyBridge.this.isEnabled() && myEconomyApiIsReady();
            }

            @Override
            public boolean deposit(Player player, double amount) {
                // Llamar a la API real. Devolver true solo tras confirmar el depósito.
                return depositUsingMyEconomy(player, amount);
            }
        };

        if (!worldEvents.getEventManager().getEconomyManager().registerProvider(provider)) {
            getLogger().warning("Ya existe un proveedor económico externo registrado.");
        }
    }

    @Override
    public void onDisable() {
        if (worldEvents != null && provider != null && worldEvents.getEventManager() != null) {
            worldEvents.getEventManager().getEconomyManager().unregisterProvider(provider);
        }
    }

    private boolean myEconomyApiIsReady() {
        // Sustituir por la comprobación de disponibilidad de la API real.
        return true;
    }

    private boolean depositUsingMyEconomy(Player player, double amount) {
        // Sustituir por el depósito real y su comprobación de éxito.
        return false;
    }
}
```

## Contrato y prioridad

- Solo se permite un proveedor externo registrado a la vez.
- El proveedor debe devolver `true` únicamente si confirmó el depósito; no debe usar el resultado de una llamada asíncrona todavía pendiente como éxito.
- Si el proveedor externo informa que no está disponible, WorldEventsNK intenta el proveedor configurado en `integrations.yml` (por defecto EconomyAPI). Si lanza una excepción durante un depósito, la operación se cancela sin fallback porque el depósito podría haberse aplicado antes del error y un segundo proveedor podría duplicarlo.
- `/we reload` recarga la configuración de EconomyAPI sin eliminar el proveedor externo registrado.
- El adaptador debe desregistrarse en `onDisable()`.
- El sistema no puede garantizar transacciones exactamente-una-vez frente a cierres abruptos: el proveedor debe implementar sus propias garantías si su API lo permite.
