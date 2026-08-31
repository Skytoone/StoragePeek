package fr.skynex.storagepeek.visualizer;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizerManager {

    private final StoragePeek plugin;
    private final Map<Location, VisualizerSession> activeSessions = new ConcurrentHashMap<>();
    private BukkitRunnable tickTask;

    public VisualizerManager(StoragePeek plugin) {
        this.plugin = plugin;
        startTicking();
    }

    private void startTicking() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<Location, VisualizerSession>> it = activeSessions.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Location, VisualizerSession> entry = it.next();
                    VisualizerSession session = entry.getValue();
                    
                    // Remove distant/offline viewers
                    session.getViewers().removeIf(uuid -> {
                        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                        if (player == null || !player.isOnline()) {
                            return true;
                        }
                        Location pLoc = player.getLocation();
                        Location bLoc = entry.getKey();
                        if (!pLoc.getWorld().equals(bLoc.getWorld()) || pLoc.distanceSquared(bLoc) > 100) {
                            session.removeViewer(player);
                            return true;
                        }
                        return false;
                    });

                    // Cleanup if session is invalid or has no viewers
                    if (!session.isValid() || session.getViewers().isEmpty()) {
                        session.cleanup();
                        it.remove();
                        continue;
                    }
                    
                    try {
                        session.update();
                    } catch (Throwable t) {
                        plugin.getLogger().warning("[StoragePeek] Error updating visualizer session: " + t.getMessage());
                        session.cleanup();
                        it.remove();
                    }
                }
            }
        };
        tickTask.runTaskTimer(plugin, 1L, 2L); // Tick every 2 ticks
    }

    public void addPlayerToSession(Block block, Player player, Inventory inventory) {
        if (!plugin.getConfig().getBoolean("visualizers.enabled", true)) {
            return;
        }

        Location loc = block.getLocation();
        VisualizerSession session = activeSessions.get(loc);
        if (session == null) {
            session = new VisualizerSession(block, inventory);
            activeSessions.put(loc, session);
        }
        session.addViewer(player);
    }

    public void removePlayerFromSession(Block block, Player player) {
        Location loc = block.getLocation();
        VisualizerSession session = activeSessions.get(loc);
        if (session != null) {
            session.removeViewer(player);
        }
    }

    public void removePlayerFromAll(Player player) {
        UUID uuid = player.getUniqueId();
        for (VisualizerSession session : activeSessions.values()) {
            if (session.getViewers().contains(uuid)) {
                session.removeViewer(player);
            }
        }
    }

    public void clearSession(Block block) {
        Location loc = block.getLocation();
        VisualizerSession session = activeSessions.remove(loc);
        if (session != null) {
            session.cleanup();
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (VisualizerSession session : activeSessions.values()) {
            session.cleanup();
        }
        activeSessions.clear();
    }
}
