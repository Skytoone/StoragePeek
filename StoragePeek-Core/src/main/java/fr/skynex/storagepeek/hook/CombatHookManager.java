package fr.skynex.storagepeek.hook;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public class CombatHookManager {

    private final StoragePeek plugin;

    // Reflection caches
    private boolean combatLogXChecked = false;
    private Plugin clxPlugin = null;
    private Method clxGetCombatManager = null;
    private Method clxIsInCombat = null;
    private Object clxCombatManagerInstance = null;

    private boolean combatTagPlusChecked = false;
    private Plugin ctpPlugin = null;
    private Method ctpGetTagManager = null;
    private Method ctpIsTagged = null;
    private Object ctpTagManagerInstance = null;

    private boolean pvpManagerChecked = false;
    private Plugin pmPlugin = null;
    private Method pmGetPlayerHandler = null;
    private Method pmGet = null;
    private Method pmIsInCombat = null;
    private Object pmPlayerHandlerInstance = null;

    private boolean deluxeCombatChecked = false;
    private Plugin dcPlugin = null;
    private Constructor<?> dcApiConstructor = null;
    private Method dcIsInCombat = null;
    private Object dcApiInstance = null;

    public CombatHookManager(StoragePeek plugin) {
        this.plugin = plugin;
    }

    /**
     * Evaluates whether a player is considered in combat, checking permissions,
     * local PVP timers, and hooked third-party combat plugins.
     *
     * @param player the player to evaluate
     * @return true if the player is currently in combat, false otherwise
     */
    public boolean isPlayerInCombat(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (player.hasPermission("storagepeek.bypass.combat")) {
            return false;
        }
        if (!plugin.getConfigManager().isCombatCullingEnabled()) {
            return false;
        }
        if (plugin.getPlayerListener() != null && plugin.getPlayerListener().isInLocalCombat(player)) {
            return true;
        }
        if (plugin.getConfigManager().isCombatCullingHookPlugins()) {
            if (isHookedCombat(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dynamically checks external combat plugins via cached reflection handles.
     */
    public boolean isHookedCombat(Player player) {
        // CombatLogX
        if (!combatLogXChecked) {
            clxPlugin = Bukkit.getPluginManager().getPlugin("CombatLogX");
            if (clxPlugin != null && clxPlugin.isEnabled()) {
                try {
                    clxGetCombatManager = clxPlugin.getClass().getMethod("getCombatManager");
                    clxCombatManagerInstance = clxGetCombatManager.invoke(clxPlugin);
                    if (clxCombatManagerInstance != null) {
                        clxIsInCombat = clxCombatManagerInstance.getClass().getMethod("isInCombat", Player.class);
                    }
                } catch (Throwable ignored) {}
            }
            combatLogXChecked = true;
        }
        if (clxPlugin != null && clxPlugin.isEnabled() && clxIsInCombat != null && clxCombatManagerInstance != null) {
            try {
                Object inCombatObj = clxIsInCombat.invoke(clxCombatManagerInstance, player);
                if (inCombatObj instanceof Boolean && (Boolean) inCombatObj) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // CombatTagPlus
        if (!combatTagPlusChecked) {
            ctpPlugin = Bukkit.getPluginManager().getPlugin("CombatTagPlus");
            if (ctpPlugin != null && ctpPlugin.isEnabled()) {
                try {
                    ctpGetTagManager = ctpPlugin.getClass().getMethod("getTagManager");
                    ctpTagManagerInstance = ctpGetTagManager.invoke(ctpPlugin);
                    if (ctpTagManagerInstance != null) {
                        ctpIsTagged = ctpTagManagerInstance.getClass().getMethod("isTagged", UUID.class);
                    }
                } catch (Throwable ignored) {}
            }
            combatTagPlusChecked = true;
        }
        if (ctpPlugin != null && ctpPlugin.isEnabled() && ctpIsTagged != null && ctpTagManagerInstance != null) {
            try {
                Object isTaggedObj = ctpIsTagged.invoke(ctpTagManagerInstance, player.getUniqueId());
                if (isTaggedObj instanceof Boolean && (Boolean) isTaggedObj) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // PvPManager
        if (!pvpManagerChecked) {
            pmPlugin = Bukkit.getPluginManager().getPlugin("PvPManager");
            if (pmPlugin != null && pmPlugin.isEnabled()) {
                try {
                    pmGetPlayerHandler = pmPlugin.getClass().getMethod("getPlayerHandler");
                    pmPlayerHandlerInstance = pmGetPlayerHandler.invoke(pmPlugin);
                    if (pmPlayerHandlerInstance != null) {
                        pmGet = pmPlayerHandlerInstance.getClass().getMethod("get", Player.class);
                    }
                } catch (Throwable ignored) {}
            }
            pvpManagerChecked = true;
        }
        if (pmPlugin != null && pmPlugin.isEnabled() && pmGet != null && pmPlayerHandlerInstance != null) {
            try {
                Object pvpPlayer = pmGet.invoke(pmPlayerHandlerInstance, player);
                if (pvpPlayer != null) {
                    if (pmIsInCombat == null) {
                        pmIsInCombat = pvpPlayer.getClass().getMethod("isInCombat");
                    }
                    Object inCombatObj = pmIsInCombat.invoke(pvpPlayer);
                    if (inCombatObj instanceof Boolean && (Boolean) inCombatObj) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // DeluxeCombat
        if (!deluxeCombatChecked) {
            dcPlugin = Bukkit.getPluginManager().getPlugin("DeluxeCombat");
            if (dcPlugin != null && dcPlugin.isEnabled()) {
                try {
                    Class<?> apiClass = Class.forName("nl.marcorius.deluxecombat.api.DeluxeCombatAPI");
                    dcApiConstructor = apiClass.getDeclaredConstructor();
                    dcApiInstance = dcApiConstructor.newInstance();
                    dcIsInCombat = apiClass.getMethod("isInCombat", Player.class);
                } catch (Throwable ignored) {}
            }
            deluxeCombatChecked = true;
        }
        if (dcPlugin != null && dcPlugin.isEnabled() && dcIsInCombat != null && dcApiInstance != null) {
            try {
                Object inCombatObj = dcIsInCombat.invoke(dcApiInstance, player);
                if (inCombatObj instanceof Boolean && (Boolean) inCombatObj) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }

    public void resetReflectionCache() {
        combatLogXChecked = false;
        combatTagPlusChecked = false;
        pvpManagerChecked = false;
        deluxeCombatChecked = false;
    }
}
