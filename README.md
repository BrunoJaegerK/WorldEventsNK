# WorldEventsNK

Plugin de eventos dinámicos para **NukkitPetteriM1Edition (PM1E)**.

- Autor: BrunoJaegerK
- Java: 17
- Dependencia: `cn.nukkit:Nukkit:PM1E` (provided)
- Eventos iniciales: MeteorFall y LostTreasure

## Eventos

### MeteorFall
- Duración del estado activo configurable independientemente mediante `meteorfall.duration-seconds`. El impacto en el terreno sigue siendo permanente.
- Busca ubicaciones en todos los mundos cargados y no excluidos, tomando el spawn como referencia para el rango configurado.
- Muestrea el rango completo de distancia al spawn y solo evalúa chunks que ya están cargados; nunca genera terreno para buscar ubicaciones.
- Valida el volumen completo antes de cambiar bloques.
- Rechaza volúmenes con bloques no naturales y ubicaciones denegadas por proveedores de protección.
- Anuncia el punto de impacto con una fase de advertencia configurable (`meteorfall.warning-seconds`) para dar tiempo a los jugadores a apartarse.
- Durante la advertencia muestra una estela descendente de fuego/humo; al impactar reproduce sonido y partículas de explosión. Se controla con `meteorfall.visual-effects`.
- Genera un cráter permanente, con borde de roca, vetas de hierro/oro/diamante y núcleo de obsidiana.
- El núcleo de obsidiana puede reclamarse una sola vez durante el evento mediante clic derecho; entrega recompensas independientes configuradas en `rewards.yml`.
- Si falla una escritura durante el impacto, intenta restaurar los bloques modificados.

### LostTreasure
- Duración del evento configurable mediante `losttreasure.duration-seconds`.
- Coloca un cofre con un identificador persistente propio.
- Publica hasta tres pistas progresivas con radios independientes configurables (`hint-radius-first`, `hint-radius-second`, `hint-radius-third`); la última puede revelar las coordenadas exactas.
- El cofre tiene un marcador de partículas y señales de sonido al aparecer y al emitirse pistas; se controla con `losttreasure.visual-effects`.
- El primer jugador dentro del radio configurado puede reclamar la recompensa principal.
- El cofre no puede romperse normalmente durante el evento.
- Se elimina al reclamar, expirar o detener el evento.
- El registro permite recuperar y limpiar el cofre tras un cierre abrupto, sin borrar cofres ajenos.
- Si el chunk se descarga antes de expirar o detener el evento, la limpieza queda persistida y se reintenta cuando PM1E vuelve a inicializar ese chunk.

## Extensibilidad

- Otros plugins pueden registrar eventos propios mediante `EventRegistry` y la interfaz `WorldEvent`. Consulta [la API de eventos externos](docs/EVENT_API.md).

## Integraciones

- EconomyAPI es opcional y se detecta por reflexión usando `me.onebone.economyapi.EconomyAPI`.
- Los ajustes de integraciones se leen desde `integrations.yml`; `/we reload` vuelve a cargar economía y protección. En la primera actualización desde versiones que solo usaban `config.yml`, los valores antiguos se migran automáticamente si todavía no existe `integrations.yml`.
- La protección de regiones usa la interfaz `RegionProtectionProvider`. Consulta [la guía para crear adaptadores](docs/PROTECTION_API.md).
- Otros plugins de economía pueden registrar un `EconomyProvider` sin acoplarse a clases internas; consulta [la API económica](docs/ECONOMY_API.md).
- Un plugin de protección externo debe implementar y registrar su adaptador contra su API real. No se anuncia compatibilidad automática con WorldGuard ni con otros plugins.
- Si EconomyAPI no está disponible, se omite el dinero y se mantienen las recompensas de objetos.

## Comandos

- `/we` — ayuda.
- `/we list` — eventos registrados, estado habilitado/deshabilitado, peso aleatorio y enfriamiento restante.
- `/we status` — evento activo o cuenta regresiva del siguiente intento automático.
- `/we history [1-10]` — historial persistente de los últimos eventos con resultado, fecha y ubicación.
- `/we start <meteorfall|losttreasure|random>` — iniciar un evento.
- `/we stop` — detener el evento.
- `/we claim` — reclamar el tesoro dentro del radio.
- `/we stats` — estadísticas propias o totales: tesoros y núcleos de meteorito.
- `/we stats <jugador>` — estadísticas por nombre, aunque el jugador esté desconectado (si ya tiene registros).
- `/we stats top [1-10]` — clasificación persistente de los jugadores con más tesoros reclamados, incluidos los desconectados.
- `/we reload` — recargar configuración y mensajes.

## Datos persistentes

- `pending-event.yml`: ubicación, fase y marcador del cofre temporal para recuperación.
- `statistics.yml`: reclamaciones de tesoros y núcleos por UUID, más totales globales.
- `history.yml`: historial limitado de ejecuciones; el resultado final actualiza la entrada de inicio para evitar duplicar una ejecución en `/we history`.
- `rewards.yml`: activa/desactiva todas las recompensas, configura cantidades de diamantes/esmeraldas y dinero por evento, y permite un objeto adicional con ID, daño, probabilidad y cantidad configurables.
- `events.yml`: habilitación, peso de selección y enfriamiento individual de cada evento.

La reclamación se registra en disco antes de entregar el premio para impedir duplicaciones tras un cierre abrupto. Como contrapartida, un cierre en el instante exacto entre el registro y la entrega puede dejar una reclamación contabilizada sin que el jugador haya recibido todos los premios.

## Programación y selección automática

- Cada evento admite `enabled`, `weight` y `cooldown-seconds` en `events.yml`.
- Los enfriamientos se calculan usando el historial persistente y sobreviven reinicios mientras el registro de inicio siga retenido.
- `events.avoid-consecutive-same-event` evita repetir el último evento cuando existe otra opción elegible.
- Los enfriamientos se aplican a `/we start random` y al programador automático; iniciar un evento concreto manualmente es una acción administrativa y omite ese enfriamiento.
- El temporizador automático solo avanza cuando se alcanza el mínimo de jugadores en línea.

## Pruebas

La suite automatizada actual cubre el registro de eventos (`EventRegistry`). La compilación CI construye contra la rama `release` de PM1E y ejecuta Maven tests. Los escenarios de servidor que deben verificarse manualmente están en [el plan de pruebas](docs/TEST_PLAN.md); no se consideran validados solo por compilar.

## Compilación

Requiere JDK 17 y Maven 3.9+.

```bash
mvn clean package
```

El JAR se genera en `target/WorldEventsNK.jar`. GitHub Actions construye primero la rama `release` de `BrunoJaegerK/NukkitPetteriM1Edition` y después compila WorldEventsNK.

## Seguridad y límites

- MeteorFall modifica el mundo permanentemente. Excluye los mundos donde no quieras cráteres. Una construcción hecha enteramente con bloques naturales no se puede distinguir del terreno solo mediante IDs de bloque; protege esas zonas con el adaptador de regiones.
- La protección genérica solo puede respetar regiones de terceros cuando existe un adaptador registrado para ese plugin.
- La selección considera todos los mundos cargados y no excluidos. Usa jugadores como referencia cuando los hay y el spawn en mundos sin jugadores; nunca fuerza la generación de chunks.
- No se ha validado aún el comportamiento visual y de juego dentro de una instancia real de servidor; la compilación automatizada valida compatibilidad de código/API, no una prueba de gameplay.
