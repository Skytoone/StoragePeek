package fr.skynex.storagepeek.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class FoliaScheduler {
    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            // io.papermc.paper.threadedregions.RegionizedServer only exists on actual Folia.
            // Paper 1.21+ ships the scheduler API classes (e.g. ScheduledTask) without being Folia,
            // so checking ScheduledTask alone gives a false positive on Paper servers.
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        IS_FOLIA = folia;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            if (entity != null) {
                entity.getScheduler().run(plugin, t -> task.run(), null);
            } else {
                Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            if (entity != null) {
                entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
            } else {
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runLaterGlobal(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public interface RepeatingTask {
        void cancel();
    }

    public static RepeatingTask runTimer(Plugin plugin, Entity entity, Runnable task, long delayTicks,
            long periodTicks) {
        if (IS_FOLIA) {
            long initialDelay = Math.max(1, delayTicks);
            long period = Math.max(1, periodTicks);
            if (entity != null) {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = entity.getScheduler()
                        .runAtFixedRate(plugin, t -> task.run(), null, initialDelay, period);
                return () -> scheduledTask.cancel();
            } else {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = Bukkit
                        .getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), initialDelay, period);
                return () -> scheduledTask.cancel();
            }
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return () -> bukkitTask.cancel();
        }
    }
}
