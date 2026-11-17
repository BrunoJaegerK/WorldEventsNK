package com.brunojaeger.worldevents.event.meteor;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.FlameParticle;
import cn.nukkit.level.particle.HugeExplodeParticle;
import cn.nukkit.level.particle.SmokeParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.level.format.FullChunk;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.statistics.StatisticsManager;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.util.ArrayList;
import java.util.List;

public final class MeteorFallEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final RewardManager rewards;
    private final StatisticsManager statistics;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private int remainingSeconds;
    private int warningDuration;
    private int coreX;
    private int coreY;
    private int coreZ;
    private boolean coreClaimed;

    public MeteorFallEvent(WorldEventsNK plugin, SafeLocationFinder locations,
                           RewardManager rewards, StatisticsManager statistics) {
        this.plugin = plugin;
        this.locations = locations;
        this.rewards = rewards;
        this.statistics = statistics;
    }

    @Override public String getId() { return "meteorfall"; }
    @Override public EventState getState() { return state; }

    @Override
    public boolean start() {
        state = EventState.PREPARING;
        location = locations.find(getId());
        if (location == null) {
            state = EventState.FAILED;
            return false;
        }
        coreClaimed = false;
        remainingSeconds = Math.max(0, Math.min(120,
                plugin.getConfig().getInt("meteorfall.warning-seconds", 10)));
        warningDuration = remainingSeconds;
        state = EventState.PREPARING;
        plugin.getServer().broadcastMessage(plugin.getMessages().format("meteorfall-warning",
                "{world}", location.getLevel().getName(),
                "{x}", Integer.toString(location.getX()),
                "{z}", Integer.toString(location.getZ()),
                "{seconds}", Integer.toString(remainingSeconds)));
        if (visualEffectsEnabled()) {
            levelSound(Sound.FIREWORK_LAUNCH, 0.8f, 0.7f);
            renderMeteorTrail(0);
        }
        if (remainingSeconds == 0) return impact();
        return true;
    }

    /**
     * Primero valida todo el volumen. Si un bloque no es terreno natural, el chunk no
     * esta cargado o un proveedor deniega un punto, no se modifica ningun bloque.
     */
    private boolean createPermanentImpact() {
        Level level = location.getLevel();
        int radius = Math.max(2, Math.min(6, plugin.getConfig().getInt("meteorfall.radius", 4)));
        int depth = Math.max(1, Math.min(5, plugin.getConfig().getInt("meteorfall.depth", 2)));
        int maxChanges = Math.max(1,
                plugin.getConfig().getInt("performance.max-block-changes-per-event", 150));
        List<BlockChange> changes = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radius * radius) continue;
                int x = location.getX() + dx;
                int z = location.getZ() + dz;
                FullChunk chunk = level.getChunkIfLoaded(x >> 4, z >> 4);
                if (chunk == null) return reject("el area incluye chunks no cargados");

                int surfaceY = chunk.getHighestBlockAt(x & 15, z & 15, false);
                if (surfaceY <= level.getMinBlockY() + depth + 1) return reject("altura insuficiente");
                int localDepth = Math.max(1, (int) Math.round(
                        depth * (1.0 - Math.sqrt(distanceSquared) / (radius + 1.0))));

                for (int y = surfaceY - localDepth + 1; y <= surfaceY; y++) {
                    Block original = level.getBlock(x, y, z, false);
                    if (original == null || !isNatural(original.getId())) {
                        return reject("se encontro una construccion o bloque no natural");
                    }
                    if (!plugin.getProtectionManager().canModify(level, x, y, z, getId())) {
                        return reject("un proveedor de proteccion denego el area");
                    }
                    int replacement;
                    if (distanceSquared >= (radius - 1) * (radius - 1)) {
                        replacement = Block.COBBLESTONE;
                    } else if (y == surfaceY - localDepth + 1 && dx == 1 && dz == 0) {
                        replacement = Block.DIAMOND_ORE;
                    } else if (y == surfaceY - localDepth + 1 && dx == -1 && dz == 0) {
                        replacement = Block.GOLD_ORE;
                    } else if (y == surfaceY - localDepth + 1 && dx == 0 && dz == 1) {
                        replacement = Block.IRON_ORE;
                    } else {
                        replacement = Block.AIR;
                    }
                    changes.add(new BlockChange(x, y, z, original.getId(), original.getDamage(), replacement));
                    if (changes.size() > maxChanges) return reject("se excedio el limite de cambios");
                }
            }
        }

        int centerX = location.getX();
        int centerZ = location.getZ();
        int centerSurface = level.getChunkIfLoaded(centerX >> 4, centerZ >> 4)
                .getHighestBlockAt(centerX & 15, centerZ & 15, false);
        int candidateCoreY = centerSurface - depth;
        Block coreOriginal = level.getBlock(centerX, candidateCoreY, centerZ, false);
        if (coreOriginal == null || !isNatural(coreOriginal.getId())
                || !plugin.getProtectionManager().canModify(level, centerX, candidateCoreY, centerZ, getId())) {
            return reject("no se puede colocar el nucleo del meteorito de forma segura");
        }
        changes.add(new BlockChange(centerX, candidateCoreY, centerZ,
                coreOriginal.getId(), coreOriginal.getDamage(), Block.OBSIDIAN));
        if (changes.size() > maxChanges) return reject("se excedio el limite de cambios");

        List<BlockChange> applied = new ArrayList<>();
        try {
            for (BlockChange change : changes) {
                if (!level.setBlock(change.x, change.y, change.z, Block.get(change.newId), false, true)) {
                    rollback(level, applied);
                    return reject("Nukkit no pudo aplicar todos los cambios; se revirtio el impacto");
                }
                applied.add(change);
            }
        } catch (RuntimeException error) {
            rollback(level, applied);
            plugin.getLogger().error("Excepcion al aplicar MeteorFall; se intento revertir el terreno.", error);
            return reject("una excepcion interrumpio el impacto");
        }
        coreX = centerX;
        coreY = candidateCoreY;
        coreZ = centerZ;
        return true;
    }

    private void rollback(Level level, List<BlockChange> applied) {
        for (int i = applied.size() - 1; i >= 0; i--) {
            BlockChange change = applied.get(i);
            try {
                boolean restored = level.setBlock(change.x, change.y, change.z,
                        Block.get(change.oldId, change.oldMeta), false, true);
                if (!restored) {
                    plugin.getLogger().warning("Nukkit no pudo restaurar el bloque ("
                            + change.x + ", " + change.y + ", " + change.z + ") durante el rollback.");
                }
            } catch (RuntimeException error) {
                plugin.getLogger().error("No se pudo restaurar el bloque (" + change.x + ", "
                        + change.y + ", " + change.z + ") durante el rollback.", error);
            }
        }
    }

    private boolean reject(String reason) {
        plugin.getLogger().warning("MeteorFall cancelado sin modificar el terreno: " + reason + ".");
        return false;
    }

    private boolean isNatural(int id) {
        return id == Block.GRASS || id == Block.DIRT || id == Block.STONE
                || id == Block.SAND || id == Block.GRAVEL || id == Block.NETHERRACK
                || id == Block.END_STONE || id == Block.CLAY_BLOCK;
    }

    public boolean claimCore(Player player, Block block) {
        if (state != EventState.ACTIVE || coreClaimed || player == null || block == null || location == null) return false;
        if (player.getLevel() != location.getLevel() || block.getLevel() != location.getLevel()) return false;
        if (block.getId() != Block.OBSIDIAN || block.getFloorX() != coreX
                || block.getFloorY() != coreY || block.getFloorZ() != coreZ) return false;
        coreClaimed = true;
        statistics.recordMeteorCoreClaim(player);
        rewards.give(getId(), player);
        player.sendMessage(plugin.getMessages().get("meteor-core-claimed"));
        return true;
    }

    private boolean visualEffectsEnabled() {
        return plugin.getConfig().getBoolean("meteorfall.visual-effects", true);
    }

    private void levelSound(Sound sound, float volume, float pitch) {
        if (location != null && location.getLevel() != null) {
            location.getLevel().addSound(new Vector3(location.getX() + 0.5,
                    location.getY() + 1.0, location.getZ() + 0.5), sound, volume, pitch);
        }
    }

    /** Dibuja una estela descendente de fuego y humo durante la cuenta regresiva. */
    private void renderMeteorTrail(int elapsedSeconds) {
        if (location == null || location.getLevel() == null) return;
        Level level = location.getLevel();
        double progress = warningDuration <= 0 ? 1.0
                : Math.max(0.0, Math.min(1.0, elapsedSeconds / (double) warningDuration));
        double y = location.getY() + 24.0 - (progress * 21.0);
        double centerX = location.getX() + 0.5;
        double centerZ = location.getZ() + 0.5;
        Vector3 head = new Vector3(centerX, y, centerZ);
        level.addParticle(new FlameParticle(head));
        level.addParticle(new FlameParticle(new Vector3(centerX + 0.35, y - 0.4, centerZ)));
        level.addParticle(new FlameParticle(new Vector3(centerX - 0.35, y - 0.4, centerZ)));
        level.addParticle(new SmokeParticle(new Vector3(centerX, y - 1.0, centerZ), 2));
        level.addParticle(new SmokeParticle(new Vector3(centerX, y - 2.0, centerZ), 2));
    }

    private boolean impact() {
        if (!createPermanentImpact()) {
            state = EventState.FAILED;
            return false;
        }
        remainingSeconds = Math.max(30, plugin.getConfig().getInt("meteorfall.duration-seconds",
                plugin.getConfig().getInt("events.duration-seconds", 600)));
        state = EventState.ACTIVE;
        if (visualEffectsEnabled()) {
            Level level = location.getLevel();
            Vector3 impactPoint = new Vector3(location.getX() + 0.5, location.getY() + 1.0, location.getZ() + 0.5);
            level.addParticle(new HugeExplodeParticle(impactPoint));
            level.addSound(impactPoint, Sound.RANDOM_EXPLODE, 1.5f, 0.8f);
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2.0 * i) / 12.0;
                Vector3 burst = new Vector3(location.getX() + 0.5 + Math.cos(angle) * 2.0,
                        location.getY() + 1.2, location.getZ() + 0.5 + Math.sin(angle) * 2.0);
                level.addParticle(new FlameParticle(burst));
                level.addParticle(new SmokeParticle(burst, 2));
            }
        }
        plugin.getServer().broadcastMessage(plugin.getMessages().format("event-started",
                "{event}", "MeteorFall", "{world}", location.getLevel().getName(),
                "{x}", Integer.toString(location.getX()), "{z}", Integer.toString(location.getZ())));
        return true;
    }

    @Override
    public void tick() {
        if (state == EventState.PREPARING) {
            if (remainingSeconds > 0) remainingSeconds--;
            if (remainingSeconds <= 0) impact();
            else if (visualEffectsEnabled()) renderMeteorTrail(warningDuration - remainingSeconds);
        } else if (state == EventState.ACTIVE && --remainingSeconds <= 0) {
            state = EventState.COMPLETED;
        }
    }

    @Override public void stop() {
        if (state == EventState.ACTIVE || state == EventState.PREPARING) state = EventState.COMPLETED;
    }
    @Override public boolean isFinished() { return state == EventState.COMPLETED || state == EventState.FAILED; }
    @Override public int getRemainingSeconds() { return Math.max(0, remainingSeconds); }
    @Override public EventLocation getLocation() { return location; }

    private record BlockChange(int x, int y, int z, int oldId, int oldMeta, int newId) {
    }
}
