# Plan de pruebas de WorldEventsNK

Este documento separa las pruebas automatizadas de las que necesitan una instancia real de NukkitPetteriM1Edition. No marques un escenario como validado hasta ejecutarlo en servidor.

## 1. Compilación y pruebas automatizadas

- [ ] Ejecutar `mvn clean verify` con JDK 17.
- [ ] Confirmar que GitHub Actions instala PM1E desde la rama `release` y compila el JAR.
- [ ] Ejecutar los tests de `EventRegistry`: registro, IDs duplicados, creación y retirada de eventos externos.
- [ ] Verificar que el artefacto `target/WorldEventsNK.jar` contiene `plugin.yml` y todos los YAML de configuración.

## 2. Instalación y configuración

- [ ] Instalar el JAR en una copia de pruebas de PM1E y confirmar que habilita sin errores.
- [ ] Confirmar que se crean `config.yml`, `events.yml`, `rewards.yml`, `integrations.yml` y `lang/es_ES.yml`.
- [ ] Desactivar `economy.enabled` en `integrations.yml`; confirmar que las recompensas de objetos siguen funcionando.
- [ ] Instalar EconomyAPI y comprobar el depósito de una cantidad de prueba.
- [ ] Cambiar `economy.provider`/ `economy.class` a una integración compatible y confirmar que la conexión se detecta o falla de forma controlada.
- [ ] Desactivar y volver a activar `protection.enabled`; comprobar el comportamiento configurado.
- [ ] Ejecutar `/we reload` tras cambiar cada archivo y confirmar que los ajustes se actualizan sin reiniciar.

## 3. MeteorFall

- [ ] Probar advertencia de 0, 1, 10 y 120 segundos.
- [ ] Confirmar que la estela de fuego/humo desciende hacia las coordenadas anunciadas.
- [ ] Confirmar sonidos/partículas de impacto y que `meteorfall.visual-effects: false` los desactiva.
- [ ] Verificar el cráter, borde, vetas y núcleo en terreno natural.
- [ ] Confirmar que no modifica construcciones ni bloques no naturales.
- [ ] Simular un proveedor de protección que deniegue un bloque del área; verificar que no se aplica ningún cambio.
- [ ] Forzar una escritura fallida si el entorno de pruebas lo permite y comprobar el rollback.
- [ ] Reclamar el núcleo dos veces y desde dos jugadores; solo una reclamación debe entregar recompensa.
- [ ] Detener durante PREPARING; no debe crear cráter.
- [ ] Detener durante ACTIVE; el terreno permanente debe permanecer.

## 4. LostTreasure

- [ ] Confirmar que coloca un cofre real con marcador persistente.
- [ ] Verificar partículas/sonidos de aparición y pistas; comprobar `losttreasure.visual-effects: false`.
- [ ] Verificar pistas con radios 96, 32 y 0, además del intervalo y máximo configurados.
- [ ] Romper el cofre con un jugador normal; la rotura debe cancelarse.
- [ ] Reclamar dentro y fuera del radio; solo debe aceptar dentro.
- [ ] Intentar reclamar dos veces y con dos jugadores; solo una reclamación debe ser aceptada.
- [ ] Llenar el inventario y comprobar que los objetos sobrantes se dejan en el suelo.
- [ ] Expirar y detener el evento; comprobar que solo se elimina el cofre marcado por WorldEventsNK.
- [ ] Reiniciar el servidor con el evento activo; comprobar recuperación/limpieza del cofre.
- [ ] Descargar el chunk del cofre durante la limpieza pendiente y volver a cargarlo; comprobar reintento.
- [ ] Colocar un cofre ajeno en la coordenada registrada con marcador distinto; confirmar que no se elimina.

## 5. Scheduler, historial y estadísticas

- [ ] Confirmar que no hay eventos automáticos por debajo del mínimo de jugadores.
- [ ] Confirmar pesos, eventos deshabilitados, cooldowns y prevención de eventos consecutivos.
- [ ] Verificar que el cooldown se conserva tras reiniciar.
- [ ] Completar, detener y provocar un fallo de evento; `/we history` debe mostrar una sola entrada por ejecución.
- [ ] Verificar que los resultados terminales no borran el timestamp de inicio usado para cooldowns.
- [ ] Reclamar ambos tipos de recompensa y comprobar contadores por jugador y totales.
- [ ] Consultar estadísticas de un jugador desconectado y el top de tesoros.
- [ ] Reiniciar y confirmar persistencia de historial y estadísticas.

## 6. Mundos y rendimiento

- [ ] Probar Overworld, Nether y End, si están cargados y habilitados.
- [ ] Probar mundo excluido y distancias mínima/máxima desde spawn.
- [ ] Confirmar que la búsqueda no genera chunks.
- [ ] Probar terreno irregular, agua, lava, hojas, nieve y superficies no admitidas.
- [ ] Probar varios jugadores y varios mundos con carga normal.
- [ ] Verificar que los efectos visuales no generan carga excesiva con muchos jugadores.

## 7. Adaptadores de protección

- [ ] Implementar un adaptador contra la API concreta del plugin de regiones instalado.
- [ ] Probar regiones propias, ajenas, sin dueño y regiones con permisos denegados.
- [ ] Simular proveedor no disponible y excepción; con `fail-safe: true` la modificación debe denegarse.
- [ ] Deshabilitar el plugin adaptador y confirmar que retira su provider en `onDisable`.

## Criterio de aceptación

El plugin se considera listo para producción cuando CI compila y pasa los tests automatizados, y todos los escenarios aplicables de servidor real se han ejecutado sin pérdida de bloques, duplicación de recompensas, eliminación de cofres ajenos ni errores de consola.
