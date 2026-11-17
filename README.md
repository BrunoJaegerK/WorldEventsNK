# WorldEventsNK

Plugin de eventos dinámicos para **Nukkit PetteriM1 Edition (PM1E)**.

- **Autor:** BrunoJaegerK
- **Java:** 17
- **Rama objetivo de la API:** `BrunoJaegerK/NukkitPetteriM1Edition:release`
- **Versión de desarrollo:** 1.0.0-SNAPSHOT

## Eventos iniciales

### MeteorFall
Impacto de meteorito con cráter permanente. La generación valida el área completa antes de cambiar bloques, solo permite bloques incluidos en la lista de terreno natural y respeta regiones protegidas. El núcleo permite reclamar una recompensa una sola vez por evento.

### LostTreasure
Cofre de evento en un mundo cargado, pistas progresivas, reclamación mediante interacción o `/we claim`, premio principal único y expiración.

## Sistemas

- Todos los mundos cargados son elegibles por defecto; se pueden excluir mundos.
- EconomyAPI es opcional y se integra por reflexión.
- API genérica para proveedores externos de protección de regiones.
- Regiones cuboidales configurables como protección básica.
- Programación automática y comandos administrativos.
- Configuración YAML en español.
- Las operaciones de mundo se ejecutan desde el scheduler principal de Nukkit.

## Compilación local

Primero instala la API PM1E de la rama `release`:

```bash
git clone --branch release https://github.com/BrunoJaegerK/NukkitPetteriM1Edition.git
cd NukkitPetteriM1Edition
mvn -DskipTests install
```

Después, desde el proyecto WorldEventsNK:

```bash
mvn clean package
```

El JAR se genera en `target/WorldEventsNK.jar`. GitHub Actions realiza ambos pasos y publica el JAR como artefacto de compilación.

## Comandos

- `/we` — ayuda.
- `/we list` — eventos registrados.
- `/we status` — evento activo.
- `/we start <meteorfall|losttreasure|random>` — iniciar evento.
- `/we stop` — detener evento activo.
- `/we claim` — reclamar el tesoro si estás cerca.
- `/we reload` — recargar configuración.

Los comandos administrativos requieren `worldevents.admin` (operadores por defecto).

## Integración de protección

Un plugin externo puede implementar `RegionProtectionProvider` y registrarlo en:

```java
WorldEventsNK.getInstance().getProtectionManager().registerProvider(provider);
```

El proveedor debe devolver `true` si una coordenada está protegida. Si un proveedor falla y `protection.fail-safe` está activo, la ubicación se considera protegida.

## Meteorito permanente

MeteorFall modifica el terreno de forma definitiva. Antes de habilitarlo en un mapa establecido, revisa `meteorfall.radius`, `meteorfall.depth` y `meteorfall.natural-block-whitelist`. Nunca se deben añadir a esa lista bloques de construcción que quieras preservar.
