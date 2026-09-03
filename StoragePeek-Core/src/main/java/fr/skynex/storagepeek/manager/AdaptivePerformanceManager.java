package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AdaptivePerformanceManager {

    private final StoragePeek plugin;
    private double currentTps = 20.0;
    private boolean lowPerformanceMode = false;

    public AdaptivePerformanceManager(StoragePeek plugin) {
        this.plugin = plugin;
        startTpsMonitor();
    }

    private void startTpsMonitor() {
        fr.skynex.storagepeek.util.FoliaScheduler.runTimer(plugin, null, () -> {
            try {
                double[] tps = Bukkit.getTPS();
                if (tps != null && tps.length > 0) {
                    currentTps = tps[0];
                    lowPerformanceMode = currentTps < 16.5;
                }
            } catch (Throwable ignored) {
                currentTps = 20.0;
                lowPerformanceMode = false;
            }
        }, 100L, 100L); // Check every 5 seconds
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public boolean isLowPerformanceMode() {
        return lowPerformanceMode;
    }

    public double getAdaptiveMaxRaycastDistance(Player player, double baseDistance) {
        if (lowPerformanceMode) {
            return Math.max(2.5, baseDistance * 0.6);
        }
        if (player != null && player.isGliding()) {
            return Math.max(3.0, baseDistance * 0.7);
        }
        return baseDistance;
    }

    public int getAdaptiveParticleMultiplier() {
        if (currentTps < 15.0) return 0;
        if (currentTps < 18.0) return 1;
        return 2;
    }
}
