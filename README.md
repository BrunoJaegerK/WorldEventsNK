# WorldEventsNK

Plugin de eventos dinámicos para **NukkitPetteriM1Edition (PM1E)**.

- Autor: BrunoJaegerK
- Java: 17
- API: PM1E `release` (dependencia `provided`)
- Eventos 1.0.0: MeteorFall y LostTreasure

## MeteorFall
- Busca ubicaciones en mundos cargados y cerca de jugadores activos.
- Valida el volumen completo antes de modificar el terreno.
- Rechaza construcciones, bloques no naturales y regiones denegadas.
- Crea un cráter permanente con minerales y núcleo de obsidiana.
- Permite reclamar una recompensa única interactuando con el núcleo durante el evento.
- Revierte los cambios si Nukkit no puede aplicar el impacto completo.

## LostTreasure
- Coloca un cofre temporal con identificador persistente en su BlockEntity.
- Publica pistas progresivas.
- El primer jugador cercano que reclama recibe la recompensa principal.
- Protege el cofre frente a rotura e interacción normal.
- Limpia el cofre al reclamar, expirar, detener o recuperar un cierre abrupto.

## Sistemas
- Todos los mundos cargados son elegibles; se pueden excluir mundos.
- EconomyAPI es opcional y se detecta por reflexión.
- Protección extensible mediante `RegionProtectionProvider`.
- Estadísticas persistentes de reclamaciones por jugador y globales.
- Recompensas configurables en `rewards.yml`; los objetos que no caben en el inventario se dejan en el suelo junto al jugador.
- Configuración y mensajes en YAML.

## Comandos
- `/we list`, `/we status`
- `/we start <meteorfall|losttreasure|random>`
- `/we stop`, `/we claim`, `/we reload`
- `/we stats [jugador]`

## Protección externa
Un plugin de regiones puede implementar `RegionProtectionProvider` y registrarlo mediante `WorldEventsNK.getProtectionManager().registerProvider(...)`. No se declara compatibilidad automática con plugins de terceros sin un adaptador probado.

## Compilación
Requiere JDK 17 y Maven. GitHub Actions instala la API de la rama `release` de `BrunoJaegerK/NukkitPetteriM1Edition` y construye el plugin.

```bash
mvn clean package
```

JAR: `target/WorldEventsNK.jar`.

**Importante:** MeteorFall modifica permanentemente terreno natural. Revisa el radio, la profundidad y los mundos habilitados antes de activarlo en un mapa de producción.
