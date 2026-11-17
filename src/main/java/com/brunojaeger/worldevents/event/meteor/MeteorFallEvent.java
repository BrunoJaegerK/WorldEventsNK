package com.brunojaeger.worldevents.event.meteor;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.util.ArrayList;
import java.util.List;

public final class MeteorFallEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final RewardManager rewards;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private int remainingSeconds;
    private int coreX, coreY, coreZ;
    private boolean coreClaimed;

    public MeteorFallEvent(WorldEventsNK plugin, SafeLocationFinder locations, RewardManager rewards) {
        this.plugin = plugin;
        this.locations = locations;
        this.rewards = rewards;
    }

    @Override public String getId() { return "meteorfall"; }
    @Override public EventState getState() { return state; }

    @Override
    public boolean start() {
        state = EventState.PREPARING;
        location = locations.find(getId());
        if (location == null || !createPermanentImpact()) {
            state = EventState.FAILED;
            return false;
        }
        coreClaimed = false;
        remainingSeconds = Math.max(30, plugin.getConfig().getInt("events.duration-seconds", 600));
        state = EventState.ACTIVE;
        plugin.getServer().broadcastMessage("§6[WorldEvents] §e¡Un meteorito ha impactado en "
                + location.getLevel().getName() + " §7(X: " + location.getX() + ", Z: " + location.getZ() + ")!");
        location.getLevel().addSound(new Vector3(location.getX() + 0.5, location.getY(), location.getZ() + 0.5),
                cn.nukkit.level.Sound.RANDOM_EXPLODE, 1.0f, 0.7f);
        return true;
    }

    /**
     * Valida todo el volumen antes de modificar terreno. Si detecta construcciones,
     * chunks descargados o denegaciones de protección, cancela sin aplicar cambios.
     */
    private boolean createPermanentImpact() {
        Level level = location.getLevel();
        int radius = Math.max(2, Math.min(6, plugin.getConfig().getInt("meteorfall.radius", 4)));
        int depth = Math.max(1, Math.min(5, plugin.getConfig().getInt("meteorfall.depth", 2)));
        int maxChanges = Math.max(1, plugin.getConfig().getInt("performance.max-block-changes-per-event", 150));
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
                    int replacement = distanceSquared >= (radius - 1) * (radius - 1)
                            ? Block.COBBLESTONE : Block.AIR;
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
        } catch (Throwable error) {
            rollback(level, applied);
            plugin.getLogger().warning("Excepcion aplicando MeteorFall: " + error.getMessage());
            return reject("se revirtieron los cambios tras una excepcion");
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
                level.setBlock(change.x, change.y, change.z,
                        Block.get(change.oldId, change.oldMeta), false, true);
            } catch (Throwable error) {
                plugin.getLogger().critical("No se pudo revertir un bloque tras fallo de MeteorFall: " + error.getMessage());
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
        if (state != EventState.ACTIVE || coreClaimed || location == null || player == null || block == null) return false;
        if (player.getLevel() != location.getLevel() || block.getLevel() != location.getLevel()) return false;
        if (block.getId() != Block.OBSIDIAN || block.getFloorX() != coreX
                || block.getFloorY() != coreY || block.getFloorZ() != coreZ) return false;
        coreClaimed = true;
        rewards.give(getId(), player);
        player.sendMessage(plugin.getMessages().get("meteor-core-claimed"));
        return true;
    }

    @Override
    public void tick() {
        if (state == EventState.ACTIVE && --remainingSeconds <= 0) state = EventState.COMPLETED;
    }

    @Override public void stop() { if (state == EventState.ACTIVE) state = EventState.COMPLETED; }
    @Override public boolean isFinished() { return state == EventState.COMPLETED || state == EventState.FAILED; }

    private record BlockChange(int x, int y, int z, int oldId, int oldMeta, int newId) { }
}
