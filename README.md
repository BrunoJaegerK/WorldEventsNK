# WorldEventsNK

Plugin de eventos dinamicos para **NukkitPetteriM1Edition (PM1E)**.

- Autor: BrunoJaegerK
- Java: 17
- Dependencia de servidor: `cn.nukkit:Nukkit:PM1E` (provided)
- Eventos 1.0.0: MeteorFall y LostTreasure

## Estado funcional actual

### MeteorFall
- Busca ubicaciones en chunks ya cargados.
- Valida el volumen completo antes de modificar terreno.
- Rechaza ubicaciones con bloques no naturales o denegadas por un proveedor de proteccion.
- Crea un crater y nucleo de obsidiana permanentes.
- No restaura el terreno al finalizar.

### LostTreasure
- Coloca un cofre en una ubicacion valida.
- Publica pistas progresivas de coordenadas.
- El primer jugador cercano que reclama el tesoro recibe la recompensa principal.
- El cofre no puede romperse normalmente durante el evento.
- El cofre se elimina al reclamarse, expirar o detener el evento, si sigue siendo el bloque del plugin.

### Integraciones
- EconomyAPI es opcional y se detecta mediante reflexion usando su paquete PM1E conocido.
- La proteccion de regiones se extiende mediante `RegionProtectionProvider`.
- Los adaptadores especificos de terceros deben implementarse contra la API real del plugin de proteccion. No se anuncia compatibilidad automatica con WorldGuard ni con otros plugins.

## Comandos
- `/we` — ayuda.
- `/we list` — eventos disponibles.
- `/we status` — estado actual.
- `/we start <meteorfall|losttreasure|random>` — iniciar un evento.
- `/we stop` — detener el evento.
- `/we claim` — reclamar el tesoro si estas dentro del radio.
- `/we reload` — recargar configuracion y mensajes.

## Compilacion
Requiere JDK 17 y Maven 3.9+.

```bash
mvn clean package
```

El JAR se genera en `target/WorldEventsNK.jar`. El workflow de GitHub Actions compila primero la rama `release` del repositorio PM1E del autor y despues el plugin.

## Limites conocidos de esta etapa
- Los eventos buscan ubicaciones en chunks cargados; no fuerzan la generacion de chunks.
- El meteorito modifica permanentemente el terreno. No ejecutar sobre zonas donde no se quiera alterar el mapa.
- La interfaz de proteccion es extensible, pero requiere un adaptador real para cada plugin externo.
- La persistencia de estadisticas y la recuperacion de eventos tras un cierre abrupto no estan implementadas todavia.
- Las recompensas de objetos y dinero se leen de `rewards.yml`; el dinero se omite si EconomyAPI no esta disponible.

