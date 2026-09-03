package fr.skynex.storagepeek.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

public class VaultXHook {

    private boolean active = false;
    private Object economyProvider = null;

    public VaultXHook() {
        init();
    }

    public void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault") || Bukkit.getPluginManager().isPluginEnabled("VaultX")) {
            try {
                Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(econClass);
                if (rsp != null) {
                    economyProvider = rsp.getProvider();
                    active = true;
                    Bukkit.getLogger().info("[StoragePeek] Successfully hooked into VaultX Economy API!");
                }
            } catch (Throwable t) {
                active = false;
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public double getPlayerBalance(Player player) {
        if (!active || economyProvider == null || player == null) return 0.0;
        try {
            Method getBalMethod = economyProvider.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            Object res = getBalMethod.invoke(economyProvider, player);
            return res instanceof Number n ? n.doubleValue() : 0.0;
        } catch (Throwable t) {
            try {
                Method getBalMethod = economyProvider.getClass().getMethod("getBalance", String.class);
                Object res = getBalMethod.invoke(economyProvider, player.getName());
                return res instanceof Number n ? n.doubleValue() : 0.0;
            } catch (Throwable ignored) {
                return 0.0;
            }
        }
    }

    public boolean hasVaultPermission(Player player, int vaultNumber) {
        if (player == null) return false;
        if (player.hasPermission("storagepeek.admin") || player.hasPermission("vaultx.admin") || player.isOp()) return true;
        return player.hasPermission("vaultx.vault." + vaultNumber) || player.hasPermission("vault.vault." + vaultNumber) || player.hasPermission("vaultx.use");
    }

    public Inventory getPlayerEnderChestOrVault(Player player, int vaultNumber) {
        if (player == null) return null;
        if (vaultNumber <= 1) {
            return player.getEnderChest();
        }
        // Return EnderChest as fallback for virtual vaults or custom inventory
        return player.getEnderChest();
    }
}
