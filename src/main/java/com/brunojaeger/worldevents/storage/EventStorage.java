package com.brunojaeger.worldevents.storage;

import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;
import java.util.UUID;

/** Registro duradero de la unica estructura temporal de WorldEventsNK 1.0. */
public final class EventStorage {
    private final WorldEventsNK plugin;
    private final Config pending;

    public EventStorage(WorldEventsNK plugin) {
        this.plugin = plugin;
        this.pending = new Config(new File(plugin.getDataFolder(), "pending-event.yml"), Config.YAML);
    }

    public String beginTreasure(Level level, int x, int y, int z) {
        String marker = "WorldEventsNK:" + UUID.randomUUID();
        pending.set("treasure.active", true);
        pending.set("treasure.phase", "placing");
        pending.set("treasure.world", level.getName());
        pending.set("treasure.x", x);
        pending.set("treasure.y", y);
        pending.set("treasure.z", z);
        pending.set("treasure.marker", marker);
        pending.set("treasure.claimed", false);
        return pending.save() ? marker : null;
    }

    public boolean markTreasureActive() {
        pending.set("treasure.phase", "active");
        return pending.save();
    }

    public boolean markTreasureClaimed() {
        if (!pending.getBoolean("treasure.active", false)) return false;
        pending.set("treasure.claimed", true);
        return pending.save();
    }

    public boolean isTreasureClaimed() {
        return pending.getBoolean("treasure.claimed", false);
    }

    public void clearTreasure() {
        pending.set("treasure.active", false);
        pending.set("treasure.phase", "none");
        pending.set("treasure.claimed", false);
        pending.save();
    }

    public boolean markChestEntity(BlockEntityChest chest, String marker) {
        if (chest == null || marker == null) return false;
        chest.setName(marker);
        chest.saveNBT();
        return chest.hasName() && marker.equals(chest.getName());
    }

    public BlockEntityChest createOrGetChestEntity(Level level, int x, int y, int z) {
        BaseFullChunk chunk = level.getChunkIfLoaded(x >> 4, z >> 4);
        if (chunk == null) return null;
        BlockEntity existing = level.getBlockEntity(new Vector3(x, y, z));
        if (existing instanceof BlockEntityChest chest) return chest;
        if (existing != null) return null;

        BlockEntity created = BlockEntity.createBlockEntity(BlockEntity.CHEST, chunk,
                new cn.nukkit.nbt.tag.CompoundTag()
                        .putString("id", BlockEntity.CHEST)
                        .putInt("x", x)
                        .putInt("y", y)
                        .putInt("z", z));
        return created instanceof BlockEntityChest chest ? chest : null;
    }

    public boolean ownsTreasureChest(Level level, int x, int y, int z, String marker) {
        if (level == null || marker == null || !pending.getBoolean("treasure.active", false)) return false;
        if (level.getChunk(x >> 4, z >> 4, false) == null) return false;
        if (level.getBlock(x, y, z, false).getId() != Block.CHEST) return false;
        BlockEntity entity = level.getBlockEntity(new Vector3(x, y, z));
        return entity instanceof BlockEntityChest chest && chest.hasName() && marker.equals(chest.getName())
                && marker.equals(pending.getString("treasure.marker", ""));
    }

    public void removeOwnedTreasureChest(Level level, int x, int y, int z, String marker) {
        if (!ownsTreasureChest(level, x, y, z, marker)) return;
        BlockEntity entity = level.getBlockEntity(new Vector3(x, y, z));
        if (entity != null) entity.close();
        level.setBlock(x, y, z, Block.get(Block.AIR), false, true);
    }

    public void recover() {
        if (!pending.getBoolean("treasure.active", false)) return;
        String worldName = pending.getString("treasure.world", "");
        String marker = pending.getString("treasure.marker", "");
        int x = pending.getInt("treasure.x");
        int y = pending.getInt("treasure.y");
        int z = pending.getInt("treasure.z");

        Level level = plugin.getServer().getLevelByName(worldName);
        if (level == null) {
            plugin.getLogger().warning("Hay una limpieza pendiente en el mundo '" + worldName
                    + "', que no esta cargado. Se conserva el registro para reintentar.");
            return;
        }
        // create=false permite leer un chunk existente sin generar terreno nuevo.
        BaseFullChunk chunk = level.getChunk(x >> 4, z >> 4, false);
        if (chunk == null) {
            plugin.getLogger().warning("No se pudo abrir el chunk del cofre pendiente; se conserva el registro.");
            return;
        }

        // El registro persistente identifica la unica coordenada donde el plugin coloco su cofre.
        // Durante el cierre no hay jugadores que puedan reemplazarlo; se limpia si sigue siendo un cofre.
        if (level.getBlock(x, y, z, false).getId() == Block.CHEST) {
            BlockEntity entity = level.getBlockEntity(new Vector3(x, y, z));
            if (entity != null) entity.close();
            level.setBlock(x, y, z, Block.get(Block.AIR), false, true);
            plugin.getLogger().info("Se retiro un cofre de evento pendiente tras recuperar el estado anterior.");
        }
        clearTreasure();
    }
}
