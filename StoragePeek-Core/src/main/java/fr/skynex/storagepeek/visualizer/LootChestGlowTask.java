package fr.skynex.storagepeek.visualizer;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.loot.Lootable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class LootChestGlowTask extends BukkitRunnable {

    private final StoragePeek plugin;

    public LootChestGlowTask(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("visualizers.loot-chest-glow", true)) {
            return;
        }

        Set<Location> glowingChests = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;

            Location pLoc = player.getLocation();
            int px = pLoc.getBlockX();
            int py = pLoc.getBlockY();
            int pz = pLoc.getBlockZ();

            // Scan blocks in a 6-block radius
            int radius = 6;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int bx = px + dx;
                        int by = py + dy;
                        int bz = pz + dz;

                        // Quick bounding check to stay in world limits
                        if (by < player.getWorld().getMinHeight() || by > player.getWorld().getMaxHeight()) {
                            continue;
                        }

                        Block block = player.getWorld().getBlockAt(bx, by, bz);
                        Material type = block.getType();
                        String typeName = type.name();
                        if (typeName.contains("CHEST") || typeName.contains("BARREL") || typeName.contains("SHULKER_BOX")) {
                            Location bLoc = block.getLocation();
                            if (glowingChests.contains(bLoc)) {
                                continue;
                            }

                            if (block.getState() instanceof Lootable lootable) {
                                if (lootable.getLootTable() != null) {
                                    glowingChests.add(bLoc);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spawn particles around found unopened loot containers
        for (Location loc : glowingChests) {
            Location center = loc.clone().add(0.5, 0.6, 0.5);
            // Spawn 3 golden wax off particles
            loc.getWorld().spawnParticle(Particle.WAX_OFF, center, 2, 0.35, 0.35, 0.35, 0.05);
        }
    }
}
