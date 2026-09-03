package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.task.RaycastTask;
import fr.skynex.storagepeek.util.FoliaScheduler;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RaycastTaskManager {

    private final StoragePeek plugin;
    private RaycastTask raycastTask;
    private final Map<UUID, FoliaScheduler.RepeatingTask> raycastTasks = new ConcurrentHashMap<>();

    public RaycastTaskManager(StoragePeek plugin) {
        this.plugin = plugin;
        this.raycastTask = new RaycastTask();
    }

    public RaycastTask getRaycastTask() {
        return raycastTask;
    }

    public void startRaycastTask(Player player) {
        UUID uuid = player.getUniqueId();
        stopRaycastTask(player);

        long interval = plugin.getConfig().getLong("raycast-frequency", 2L);
        if (interval < 1) interval = 1L;

        FoliaScheduler.RepeatingTask task = FoliaScheduler.runTimer(
            plugin,
            player,
            () -> {
                if (player.isOnline()) {
                    raycastTask.runForPlayer(player);
                }
            },
            1L,
            interval
        );
        raycastTasks.put(uuid, task);
    }

    public void stopRaycastTask(Player player) {
        FoliaScheduler.RepeatingTask task = raycastTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void reloadRaycastTasks() {
        cleanupAll();
        this.raycastTask = new RaycastTask();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            startRaycastTask(player);
        }
    }

    public void cleanupAll() {
        if (raycastTask != null) {
            raycastTask.cleanupAllCompassArrows();
        }
        raycastTasks.values().forEach(task -> {
            if (task != null) {
                task.cancel();
            }
        });
        raycastTasks.clear();
    }
}
