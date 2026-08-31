package fr.skynex.storagepeek.manager;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager tracking container access and item withdrawal history per container block.
 */
public class ContainerHistoryManager {

    public record AccessLog(String playerName, String action, long timestamp) {}

    private final Map<Location, LinkedList<AccessLog>> historyMap = new ConcurrentHashMap<>();

    /**
     * Records a player access or item transfer event for a container location.
     */
    public void recordAccess(@NotNull Location loc, @NotNull String playerName, @NotNull String action) {
        Location blockLoc = loc.getBlock().getLocation();
        LinkedList<AccessLog> logs = historyMap.computeIfAbsent(blockLoc, k -> new LinkedList<>());
        synchronized (logs) {
            logs.addFirst(new AccessLog(playerName, action, System.currentTimeMillis()));
            if (logs.size() > 5) {
                logs.removeLast();
            }
        }
    }

    /**
     * Retrieves the recent access history logs for a container block location.
     */
    @NotNull
    public List<AccessLog> getLogs(@NotNull Location loc) {
        Location blockLoc = loc.getBlock().getLocation();
        LinkedList<AccessLog> logs = historyMap.get(blockLoc);
        if (logs == null) return Collections.emptyList();
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    /**
     * Clears history for a broken container block.
     */
    public void clearHistory(@NotNull Location loc) {
        historyMap.remove(loc.getBlock().getLocation());
    }
}
