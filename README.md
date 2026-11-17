# WorldEventsNK

Plugin de eventos dinámicos para **NukkitPetteriM1Edition (PM1E)**.

- Autor: BrunoJaegerK
- Java: 17
- Dependencia de servidor: `cn.nukkit:Nukkit:PM1E` (provided)
- Eventos iniciales: MeteorFall y LostTreasure

## Funcionalidades

### MeteorFall
- Busca ubicaciones en chunks cargados cercanos a jugadores.
- Valida el volumen completo antes de modificar el terreno.
- Rechaza bloques no naturales, chunks descargados y regiones denegadas.
- Aplica un cráter y núcleo de obsidiana permanentes.
- Revierte los cambios si Nukkit no puede aplicar el impacto completo.
- El núcleo entrega una recompensa única durante el evento.

### LostTreasure
- Coloca un cofre en una ubicación válida.
- Publica pistas progresivas y cada vez más precisas.
- El primer jugador cercano que reclama el tesoro recibe la recompensa principal.
- El cofre no puede romperse normalmente durante el evento.
- El cofre se elimina al reclamarse, expirar o detener el evento.
- Un registro persistente permite limpiar cofres huérfanos después de un cierre inesperado, sin forzar la carga de chunks.

### Sistemas compartidos
- Todos los mundos cargados son elegibles por defecto; se pueden excluir mundos.
- EconomyAPI es opcional y se detecta mediante reflexión.
- Protección de regiones extensible mediante `RegionProtectionProvider`.
- Estadísticas persistentes de eventos y recompensas por jugador.
- Programación automática y comandos administrativos.
- Configuración YAML en español.
- Las operaciones del mundo se ejecutan en el hilo principal de Nukkit.

## Comandos

- `/we` — ayuda.
- `/we list` — eventos disponibles.
- `/we status` — estado actual.
- `/we start <meteorfall|losttreasure|random>` — iniciar evento.
- `/we stop` — detener evento.
- `/we claim` — reclamar el tesoro si estás dentro del radio.
- `/we stats` — estadísticas propias o generales desde consola.
- `/we stats <jugador>` — estadísticas de un jugador (admin).
- `/we reload` — recargar configuración.

## Configuración y economía

Los eventos se configuran en `config.yml` y `events.yml`. Las recompensas se ajustan en `rewards.yml`. Si EconomyAPI no está instalada o falla, se omite únicamente el pago monetario; los premios en objetos siguen funcionando.

## Protección externa

Un plugin de protección puede implementar `RegionProtectionProvider` y registrarlo en:

```java
WorldEventsNK.getInstance().getProtectionManager().registerProvider(provider);
```

El adaptador debe devolver `false` cuando no pueda confirmar que la modificación está permitida. WorldEventsNK no declara compatibilidad automática con plugins de regiones sin un adaptador probado.

## Compilación

Requiere JDK 17 y Maven. El workflow de GitHub Actions compila primero la rama `release` de `BrunoJaegerK/NukkitPetteriM1Edition`, instala la API en Maven y después construye el plugin.

```bash
mvn clean package
```

El JAR se genera en `target/WorldEventsNK.jar`.

## Consideraciones

MeteorFall es permanente y debe utilizarse en zonas donde se permita modificar terreno natural. El registro de recuperación de LostTreasure elimina cofres temporales huérfanos; no reanuda una partida interrumpida ni repite premios. Las estadísticas no registran datos históricos anteriores a la instalación de esta versión.
