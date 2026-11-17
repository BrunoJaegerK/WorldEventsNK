# WorldEventsNK

Plugin de eventos dinámicos para Nukkit PetteriM1 Edition (PM1E), Java 17.

## Alcance 1.0.0

- MeteorFall: impacto permanente limitado a bloques naturales reconocidos.
- LostTreasure: cofre de evento con reclamación única mediante `/we claim`.
- Selección entre todos los mundos cargados, con exclusiones configurables.
- API genérica de proveedores de protección de regiones.
- EconomyAPI opcional; los eventos siguen funcionando sin economía.
- Persistencia local y limpieza de entidades/recursos temporales.

## Compilar

El proyecto está preparado para compilar contra `cn.nukkit:Nukkit:PM1E` desde el repositorio OpenCollab. Ejecuta `mvn clean package` con Java 17.

## Comandos

- `/we` — ayuda.
- `/we list` — eventos disponibles.
- `/we status` — estado actual.
- `/we start <meteorfall|losttreasure|random>` — iniciar evento.
- `/we stop` — detener el evento activo.
- `/we claim` — reclamar el tesoro estando cerca.
- `/we reload` — recargar configuración.

Autor: BrunoJaegerK.
