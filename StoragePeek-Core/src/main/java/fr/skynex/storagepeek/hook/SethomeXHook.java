package fr.skynex.storagepeek.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;

public class SethomeXHook {

    private boolean active = false;
    private Object sethomeApi = null;

    public SethomeXHook() {
        init();
    }

    public void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("SethomeX")) {
            try {
                Class<?> apiProviderClass = Class.forName("fr.skynex.sethomex.api.SethomeXProvider");
                Method getMethod = apiProviderClass.getMethod("get");
                sethomeApi = getMethod.invoke(null);
                if (sethomeApi != null) {
                    active = true;
                    Bukkit.getLogger().info("[StoragePeek] Successfully hooked into SethomeX API!");
                }
            } catch (Throwable t) {
                // Fallback attempt to get plugin instance
                try {
                    org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("SethomeX");
                    if (plugin != null && plugin.isEnabled()) {
                        active = true;
                        Bukkit.getLogger().info("[StoragePeek] Hooked into SethomeX via plugin instance!");
                    }
                } catch (Throwable ignored) {
                    active = false;
                }
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public String getNearbyHomeName(Player player, Location containerLoc) {
        if (!active || player == null || containerLoc == null) return null;
        try {
            if (sethomeApi != null) {
                Method getHomesMethod = sethomeApi.getClass().getMethod("getPlayerHomes", Player.class);
                Object homesMap = getHomesMethod.invoke(sethomeApi, player);
                if (homesMap instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        String homeName = String.valueOf(entry.getKey());
                        Object homeObj = entry.getValue();
                        Method getLocMethod = homeObj.getClass().getMethod("getLocation");
                        Object locObj = getLocMethod.invoke(homeObj);
                        if (locObj instanceof Location homeLoc) {
                            if (homeLoc.getWorld() != null && homeLoc.getWorld().equals(containerLoc.getWorld())) {
                                if (homeLoc.distanceSquared(containerLoc) <= 25.0) { // 5 blocks radius
                                    return homeName;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
