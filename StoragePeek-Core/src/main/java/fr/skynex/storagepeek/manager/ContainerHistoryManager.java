package fr.skynex.storagepeek.manager;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Manager tracking container access and item withdrawal history per container block.
 * Bounded to a maximum of 500 active container locations via LRU eviction to prevent memory leaks.
 */
public class ContainerHistoryManager {

    private static final int MAX_CONTAINER_ENTRIES = 500;

    public record AccessLog(String playerName, String action, long timestamp) {}

    private final Map<Location, LinkedList<AccessLog>> historyMap = Collections.synchronizedMap(
        new LinkedHashMap<Location, LinkedList<AccessLog>>(100, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Location, LinkedList<AccessLog>> eldest) {
                return size() > MAX_CONTAINER_ENTRIES;
            }
        }
    );

    /**
     * Records a player access or item transfer event for a container location.
     */
    public void recordAccess(@NotNull Location loc, @NotNull String playerName, @NotNull String action) {
        Location blockLoc = loc.getBlock().getLocation();
        LinkedList<AccessLog> logs;
        synchronized (historyMap) {
            logs = historyMap.computeIfAbsent(blockLoc, k -> new LinkedList<>());
        }
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
        LinkedList<AccessLog> logs;
        synchronized (historyMap) {
            logs = historyMap.get(blockLoc);
        }
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
