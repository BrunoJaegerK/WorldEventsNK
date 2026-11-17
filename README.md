# WorldEventsNK

Plugin de eventos dinamicos para **NukkitPetteriM1Edition (PM1E)**.

- Autor: BrunoJaegerK
- Java: 17
- Dependencia: `cn.nukkit:Nukkit:PM1E` (provided)
- Eventos iniciales: MeteorFall y LostTreasure

## Eventos

### MeteorFall
- Duracion del estado activo configurable independientemente mediante `meteorfall.duration-seconds`. El impacto en el terreno sigue siendo permanente.
- Busca ubicaciones en todos los mundos cargados y no excluidos, tomando el spawn como referencia para el rango configurado.
- Muestrea el rango completo de distancia al spawn y solo evalua chunks que ya estan cargados; nunca genera terreno para buscar ubicaciones.
- Valida el volumen completo antes de cambiar bloques.
- Rechaza volumenes con bloques no naturales y ubicaciones denegadas por proveedores de proteccion.
- Genera un crater permanente, con borde de roca, vetas de hierro/oro/diamante y nucleo de obsidiana.
- Si falla una escritura durante el impacto, intenta restaurar los bloques modificados.

### LostTreasure
- Duracion del evento configurable mediante `losttreasure.duration-seconds`.
- Coloca un cofre con un identificador persistente propio.
- Publica hasta tres pistas progresivas con radios independientes configurables (`hint-radius-first`, `hint-radius-second`, `hint-radius-third`); la última puede revelar las coordenadas exactas.
- El primer jugador dentro del radio configurado puede reclamar la recompensa principal.
- El cofre no puede romperse normalmente durante el evento.
- Se elimina al reclamar, expirar o detener el evento.
- El registro permite recuperar y limpiar el cofre tras un cierre abrupto, sin borrar cofres ajenos.
- Si el chunk se descarga antes de expirar o detener el evento, la limpieza queda persistida y se reintenta cuando PM1E vuelve a inicializar ese chunk.

## Integraciones

- EconomyAPI es opcional y se detecta por reflexion usando `me.onebone.economyapi.EconomyAPI`.
- La proteccion de regiones usa la interfaz `RegionProtectionProvider`. Consulta [la guía para crear adaptadores](docs/PROTECTION_API.md).
- Un plugin de proteccion externo debe implementar y registrar su adaptador contra su API real. No se anuncia compatibilidad automatica con WorldGuard ni con otros plugins.
- Si EconomyAPI no esta disponible, se omite el dinero y se mantienen las recompensas de objetos.

## Comandos

- `/we` — ayuda.
- `/we list` — eventos registrados, estado habilitado/deshabilitado, peso aleatorio y enfriamiento restante.
- `/we status` — evento activo o cuenta regresiva del siguiente intento automático.
- `/we status` — evento activo.
- `/we history [1-10]` — historial persistente de los últimos eventos con resultado, fecha y ubicación.
- `/we start <meteorfall|losttreasure|random>` — iniciar un evento.
- `/we stop` — detener el evento.
- `/we claim` — reclamar el tesoro dentro del radio.
- `/we stats` — estadisticas propias o totales desde consola.
- `/we stats <jugador>` — estadisticas por nombre, aunque el jugador este desconectado (si ya tiene registros).
- `/we stats top [1-10]` — clasificación persistente de los jugadores con más tesoros reclamados, incluidos los desconectados.
- `/we reload` — recargar configuracion y mensajes.

## Datos persistentes

- `pending-event.yml`: ubicacion, fase y marcador del cofre temporal para recuperacion.
- `statistics.yml`: reclamaciones por UUID y total acumulado.
- `history.yml`: historial limitado de inicios, finalizaciones, detenciones y fallos de eventos.
- `rewards.yml`: activa/desactiva todas las recompensas, configura cantidades de diamantes/esmeraldas y dinero, y permite un objeto adicional con ID, daño, probabilidad y cantidad configurables.
- `events.yml`: habilitación, peso de selección y enfriamiento individual de cada evento.

La reclamacion se registra en disco antes de entregar el premio para impedir duplicaciones tras un cierre abrupto. Como contrapartida, un cierre en el instante exacto entre el registro y la entrega puede dejar una reclamacion contabilizada sin que el jugador haya recibido todos los premios.

## Programación y selección automática

- Cada evento admite `enabled`, `weight` y `cooldown-seconds` en `events.yml`.
- Los enfriamientos se calculan usando el historial persistente y sobreviven reinicios mientras el registro de inicio siga retenido.
- `events.avoid-consecutive-same-event` evita repetir el último evento cuando existe otra opción elegible.
- Los enfriamientos se aplican a `/we start random` y al programador automático; iniciar un evento concreto manualmente es una acción administrativa y omite ese enfriamiento.
- El temporizador automático solo avanza cuando se alcanza el mínimo de jugadores en línea.

## Compilacion

Requiere JDK 17 y Maven 3.9+.

```bash
mvn clean package
```

El JAR se genera en `target/WorldEventsNK.jar`. GitHub Actions construye primero la rama `release` de `BrunoJaegerK/NukkitPetteriM1Edition` y despues compila WorldEventsNK.

## Seguridad y limites

- MeteorFall modifica el mundo permanentemente. Excluye los mundos donde no quieras crateres. Una construccion hecha enteramente con bloques naturales no se puede distinguir del terreno solo mediante IDs de bloque; protege esas zonas con el adaptador de regiones.
- La proteccion generica solo puede respetar regiones de terceros cuando existe un adaptador registrado para ese plugin.
- La seleccion considera todos los mundos cargados y no excluidos. Usa jugadores como referencia cuando los hay y el spawn en mundos sin jugadores; nunca fuerza la generacion de chunks.
- No se ha validado aun el comportamiento visual y de juego dentro de una instancia real de servidor; la compilacion automatizada valida compatibilidad de codigo/API, no una prueba de gameplay.
