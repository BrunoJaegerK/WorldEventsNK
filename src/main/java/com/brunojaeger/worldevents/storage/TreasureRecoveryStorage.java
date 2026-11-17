package com.brunojaeger.worldevents.storage;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.utils.Utils;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.world.EventLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class TreasureRecoveryStorage {
    private static final String FILE_NAME = "active-treasure.properties";

    private TreasureRecoveryStorage() {
    }

    public static boolean save(WorldEventsNK plugin, EventLocation location) {
        File target = new File(plugin.getDataFolder(), FILE_NAME);
        File temporary = new File(plugin.getDataFolder(), FILE_NAME + ".tmp");
        Properties properties = new Properties();
        properties.setProperty("world", location.getLevel().getName());
        properties.setProperty("x", Integer.toString(location.getX()));
        properties.setProperty("y", Integer.toString(location.getY() + 1));
        properties.setProperty("z", Integer.toString(location.getZ()));
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IOException("No se pudo crear la carpeta de datos.");
            }
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                properties.store(output, "WorldEventsNK pending treasure cleanup");
                output.getFD().sync();
            }
            try {
                Files.move(temporary.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException error) {
            plugin.getLogger().error("No se pudo guardar el registro de recuperación del tesoro.", error);
            temporary.delete();
            return false;
        }
    }

    public static void clear(WorldEventsNK plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("No se pudo borrar el registro de recuperación del tesoro.");
        }
    }

    public static void recover(WorldEventsNK plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.isFile()) return;

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
            String worldName = properties.getProperty("world");
            int x = Integer.parseInt(properties.getProperty("x"));
            int y = Integer.parseInt(properties.getProperty("y"));
            int z = Integer.parseInt(properties.getProperty("z"));
            Level level = plugin.getServer().getLevelByName(worldName);
            if (level == null) {
                plugin.getLogger().warning("No se pudo recuperar el cofre pendiente: el mundo "
                        + worldName + " no está cargado. Se conservará el registro para el próximo inicio.");
                return;
            }
            if (level.getBlock(x, y, z).getId() == Block.CHEST) {
                level.setBlock(x, y, z, Block.get(Block.AIR), false, true);
                plugin.getLogger().info("Se eliminó un cofre de evento interrumpido en "
                        + worldName + " (" + x + ", " + y + ", " + z + ").");
            }
            clear(plugin);
        } catch (IOException | RuntimeException error) {
            plugin.getLogger().error("No se pudo procesar el registro de recuperación del tesoro.", error);
        }
    }
}
