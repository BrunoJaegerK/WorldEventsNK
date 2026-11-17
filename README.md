# WorldEventsNK

Plugin de eventos dinamicos para **NukkitPetteriM1Edition (PM1E)**.

- Autor: BrunoJaegerK
- Java: 17
- Dependencia: `cn.nukkit:Nukkit:PM1E` (provided)
- Eventos iniciales: MeteorFall y LostTreasure

## Eventos

### MeteorFall
- Busca una ubicacion segura en un mundo con jugadores conectados.
- Solo evalua chunks que ya estan cargados; no genera terreno para buscar ubicaciones.
- Valida el volumen completo antes de cambiar bloques.
- Rechaza volumenes con bloques no naturales y ubicaciones denegadas por proveedores de proteccion.
- Genera un crater permanente, con borde de roca, vetas de hierro/oro/diamante y nucleo de obsidiana.
- Si falla una escritura durante el impacto, intenta restaurar los bloques modificados.

### LostTreasure
- Coloca un cofre con un identificador persistente propio.
- Publica pistas progresivas.
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
- `/we list` — eventos disponibles.
- `/we status` — evento activo.
- `/we start <meteorfall|losttreasure|random>` — iniciar un evento.
- `/we stop` — detener el evento.
- `/we claim` — reclamar el tesoro dentro del radio.
- `/we stats` — estadisticas propias o totales desde consola.
- `/we stats <jugador>` — estadisticas de un jugador conectado.
- `/we reload` — recargar configuracion y mensajes.

## Datos persistentes

- `pending-event.yml`: ubicacion, fase y marcador del cofre temporal para recuperacion.
- `statistics.yml`: reclamaciones por UUID y total acumulado.
- `rewards.yml`: activa/desactiva todas las recompensas con `enabled` y configura cantidades de objetos y rangos monetarios.

La reclamacion se registra en disco antes de entregar el premio para impedir duplicaciones tras un cierre abrupto. Como contrapartida, un cierre en el instante exacto entre el registro y la entrega puede dejar una reclamacion contabilizada sin que el jugador haya recibido todos los premios.

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
