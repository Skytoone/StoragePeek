package fr.skynex.storagepeek.visualizer;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
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
            if (!player.isOnline() || player.getWorld() == null) continue;

            Location pLoc = player.getLocation();
            int radius = 6;
            double radiusSq = 36.0;

            int minChunkX = (pLoc.getBlockX() - radius) >> 4;
            int maxChunkX = (pLoc.getBlockX() + radius) >> 4;
            int minChunkZ = (pLoc.getBlockZ() - radius) >> 4;
            int maxChunkZ = (pLoc.getBlockZ() + radius) >> 4;

            org.bukkit.World world = player.getWorld();
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (!world.isChunkLoaded(cx, cz)) continue;
                    org.bukkit.Chunk chunk = world.getChunkAt(cx, cz);
                    for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Lootable lootable && lootable.getLootTable() != null) {
                            Location bLoc = state.getLocation();
                            if (bLoc.distanceSquared(pLoc) <= radiusSq) {
                                glowingChests.add(bLoc);
                            }
                        }
                    }
                }
            }
        }

        // Spawn particles around found unopened loot containers
        for (Location loc : glowingChests) {
            Location center = loc.clone().add(0.5, 0.6, 0.5);
            // Spawn 2 golden wax off particles
            loc.getWorld().spawnParticle(Particle.WAX_OFF, center, 2, 0.35, 0.35, 0.35, 0.05);
        }
    }
}
