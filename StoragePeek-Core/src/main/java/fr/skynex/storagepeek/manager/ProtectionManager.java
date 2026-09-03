package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.hook.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ProtectionManager {

    private record NamedHook(String pluginName, ProtectionHook hook) {}

    private final List<NamedHook> activeHooks = new ArrayList<>();

    public ProtectionManager() {
        reloadHooks();
    }

    public void reloadHooks() {
        activeHooks.clear();
        registerHook("WorldGuard", "fr.skynex.storagepeek.hook.WorldGuardHook");
        registerHook("Lands", "fr.skynex.storagepeek.hook.LandsHook");
        registerHook("GriefPrevention", "fr.skynex.storagepeek.hook.GriefPreventionHook");
        registerHook("BentoBox", "fr.skynex.storagepeek.hook.BentoBoxHook");
        registerHook("SuperiorSkyblock2", "fr.skynex.storagepeek.hook.SuperiorSkyblockHook");
        registerHook("Towny", "fr.skynex.storagepeek.hook.TownyHook");
        registerHook("LWC", "fr.skynex.storagepeek.hook.LWCHook");
        registerHook("Residence", "fr.skynex.storagepeek.hook.ResidenceHook");
        registerHook("GriefDefender", "fr.skynex.storagepeek.hook.GriefDefenderHook");
        registerHook("PlotSquared", "fr.skynex.storagepeek.hook.PlotSquaredHook");
    }

    private void registerHook(String pluginName, String className) {
        if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            try {
                ProtectionHook hook = (ProtectionHook) Class.forName(className)
                        .getDeclaredConstructor().newInstance();
                activeHooks.add(new NamedHook(pluginName, hook));
            } catch (Throwable t) {
                Bukkit.getLogger().warning("[StoragePeek] Failed to load protection hook for " + pluginName + ": " + t.getMessage());
            }
        }
    }

    public boolean canAccess(Player player, Location loc) {
        StoragePeek plugin = StoragePeek.getInstance();
        if (plugin != null) {
            if (!plugin.isProtectionHooksEnabled()) {
                return true;
            }
            if (player.hasPermission("storagepeek.bypass.protection")) {
                return true;
            }

        }

        for (NamedHook namedHook : activeHooks) {
            if (plugin != null && !plugin.isProtectionHookEnabled(namedHook.pluginName())) {
                continue;
            }
            try {
                if (!namedHook.hook().canAccess(player, loc)) {
                    return false;
                }
            } catch (Throwable ignored) {}
        }
        return true;
    }
}

