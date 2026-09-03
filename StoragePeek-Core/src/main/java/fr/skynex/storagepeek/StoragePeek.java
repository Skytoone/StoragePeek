package fr.skynex.storagepeek;

import fr.skynex.storagepeek.manager.HookManager;
import fr.skynex.storagepeek.manager.MessageManager;
import fr.skynex.storagepeek.manager.ProtectionManager;

import fr.skynex.storagepeek.session.PeekSession;
import fr.skynex.storagepeek.task.RaycastTask;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.List;

public final class StoragePeek extends JavaPlugin {

    private static StoragePeek instance;
    private final Map<UUID, PeekSession> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<UUID> disabledPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private fr.skynex.storagepeek.manager.RaycastTaskManager raycastTaskManager;
    private fr.skynex.storagepeek.manager.DisplayEntityManager displayEntityManager;
    private ProtectionManager protectionManager;
    private HookManager hookManager;
    private MessageManager messageManager;
    private NamespacedKey disabledKey;
    private NamespacedKey displayKey;
    private NamespacedKey themeKey;
    private NamespacedKey labelKey;
    private fr.skynex.storagepeek.listener.PlayerListener playerListener;
    private fr.skynex.storagepeek.visualizer.VisualizerManager visualizerManager;
    private fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask lootChestGlowTaskHandle;
    private fr.skynex.storagepeek.manager.ContainerHistoryManager containerHistoryManager;
    private fr.skynex.storagepeek.hook.LootGlowHook lootGlowHook;

    private fr.skynex.storagepeek.manager.StoragePeekConfigManager configManager;
    private fr.skynex.storagepeek.hook.CombatHookManager combatHookManager;
    private fr.skynex.storagepeek.manager.BaseStorageManager baseStorageManager;
    private fr.skynex.storagepeek.manager.SoundManager soundManager;

    private fr.skynex.storagepeek.hook.VaultXHook vaultXHook;
    private fr.skynex.storagepeek.hook.SethomeXHook sethomeXHook;
    private fr.skynex.storagepeek.manager.AdaptivePerformanceManager performanceManager;

    public fr.skynex.storagepeek.hook.VaultXHook getVaultXHook() {
        return vaultXHook;
    }

    public fr.skynex.storagepeek.hook.SethomeXHook getSethomeXHook() {
        return sethomeXHook;
    }

    public fr.skynex.storagepeek.manager.AdaptivePerformanceManager getPerformanceManager() {
        return performanceManager;
    }


    private fr.skynex.storagepeek.listener.ExhibitionFrameListener exhibitionFrameListener;
    private fr.skynex.storagepeek.gui.StorageDashboardGUI storageDashboardGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfiguration();
        
        this.configManager = new fr.skynex.storagepeek.manager.StoragePeekConfigManager(this);
        this.combatHookManager = new fr.skynex.storagepeek.hook.CombatHookManager(this);
        this.baseStorageManager = new fr.skynex.storagepeek.manager.BaseStorageManager(this);
        this.soundManager = new fr.skynex.storagepeek.manager.SoundManager(this);
        this.displayEntityManager = new fr.skynex.storagepeek.manager.DisplayEntityManager(this);
        this.raycastTaskManager = new fr.skynex.storagepeek.manager.RaycastTaskManager(this);
        this.disabledKey = new NamespacedKey(this, "disabled");
        this.displayKey = new NamespacedKey(this, "display");
        this.themeKey = new NamespacedKey(this, "theme");
        this.labelKey = new NamespacedKey(this, "custom_label");
        this.messageManager = new MessageManager(this);
        this.protectionManager = new ProtectionManager();
        this.hookManager = new HookManager();
        this.lootGlowHook = new fr.skynex.storagepeek.hook.LootGlowHook();
        this.vaultXHook = new fr.skynex.storagepeek.hook.VaultXHook();
        this.sethomeXHook = new fr.skynex.storagepeek.hook.SethomeXHook();
        this.performanceManager = new fr.skynex.storagepeek.manager.AdaptivePerformanceManager(this);
        this.containerHistoryManager = new fr.skynex.storagepeek.manager.ContainerHistoryManager();
        this.visualizerManager = new fr.skynex.storagepeek.visualizer.VisualizerManager(this);
        this.storageDashboardGUI = new fr.skynex.storagepeek.gui.StorageDashboardGUI(this);
        this.configManager.loadConfigurationCache();

        fr.skynex.storagepeek.api.StoragePeekProvider.setInstance(new fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl(this));

        int cleaned = purgeOrphanedEntities();
        if (cleaned > 0) {
            getLogger().info("Cleaned up " + cleaned + " orphaned display entities on startup.");
        }

        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getPersistentDataContainer().has(disabledKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                disabledPlayers.add(player.getUniqueId());
            }
        }

        // bStats Metrics
        int pluginId = 31024;
        new org.bstats.bukkit.Metrics(this, pluginId);

        // Update Checker
        if (getConfig().getBoolean("check-updates", true)) {
            new fr.skynex.storagepeek.util.UpdateChecker(this, 134712).getVersion(version -> {
                if (this.getPluginMeta().getVersion().equals(version)) {
                    getLogger().info("The plugin is up to date.");
                } else {
                    getLogger().warning("A new update is available (" + version
                            + ")! Download it here: https://www.spigotmc.org/resources/134712");
                }
            });
        }



        this.lootChestGlowTaskHandle = fr.skynex.storagepeek.util.FoliaScheduler.runTimer(this, null, () -> new fr.skynex.storagepeek.visualizer.LootChestGlowTask(this).run(), 20L, 20L);

        for (Player player : getServer().getOnlinePlayers()) {
            startRaycastTask(player);
        }

        this.playerListener = new fr.skynex.storagepeek.listener.PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new fr.skynex.storagepeek.listener.QuickTakeListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.skynex.storagepeek.visualizer.VisualizerListener(this, visualizerManager), this);
        getServer().getPluginManager().registerEvents(new fr.skynex.storagepeek.listener.TransferVisualizerListener(this), this);
        this.exhibitionFrameListener = new fr.skynex.storagepeek.listener.ExhibitionFrameListener(this);
        getServer().getPluginManager().registerEvents(exhibitionFrameListener, this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new fr.skynex.storagepeek.hook.StoragePeekPAPIExpansion(this).register();
            getLogger().info("Successfully registered PlaceholderAPI expansion (%storagepeek_...)!");
        }

        try {
            org.bukkit.command.PluginCommand spCommand = getCommand("storagepeek");
            if (spCommand != null) {
                fr.skynex.storagepeek.command.StoragePeekCommand cmd = new fr.skynex.storagepeek.command.StoragePeekCommand(this);
                spCommand.setExecutor(cmd);
                spCommand.setTabCompleter(cmd);
            } else {
                getLogger().warning("Could not register /storagepeek command executor: getCommand(\"storagepeek\") returned null.");
            }
        } catch (UnsupportedOperationException e) {
            getLogger().warning("Could not register /storagepeek command executor: " + e.getMessage());
        }

        getLogger().info("StoragePeek v" + getPluginMeta().getVersion() + " enabled successfully!");
    }

    private void loadConfiguration() {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }



    @Override
    public void onDisable() {
        if (exhibitionFrameListener != null) {
            exhibitionFrameListener.cleanupAll();
        }
        if (raycastTaskManager != null) {
            raycastTaskManager.cleanupAll();
        }
        if (visualizerManager != null) {
            visualizerManager.shutdown();
        }
        if (lootChestGlowTaskHandle != null) {
            lootChestGlowTaskHandle.cancel();
        }
        activeSessions.values().forEach(session -> session.cleanup(true));
        activeSessions.clear();
        disabledPlayers.clear();
        fr.skynex.storagepeek.session.PeekSessionDisplayManager.clearBlockDataCache();
        fr.skynex.storagepeek.api.StoragePeekProvider.setInstance(null);
    }

    public static StoragePeek getInstance() {
        return instance;
    }

    public Map<UUID, PeekSession> getActiveSessions() {
        return activeSessions;
    }

    public java.util.Set<UUID> getDisabledPlayers() {
        return disabledPlayers;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public fr.skynex.storagepeek.hook.LootGlowHook getLootGlowHook() {
        return lootGlowHook;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public fr.skynex.storagepeek.manager.ContainerHistoryManager getContainerHistoryManager() {
        return containerHistoryManager;
    }

    public fr.skynex.storagepeek.manager.RaycastTaskManager getRaycastTaskManager() {
        return raycastTaskManager;
    }

    public fr.skynex.storagepeek.manager.DisplayEntityManager getDisplayEntityManager() {
        return displayEntityManager;
    }

    public NamespacedKey getDisabledKey() {
        return disabledKey;
    }

    public NamespacedKey getThemeKey() {
        return themeKey;
    }

    public NamespacedKey getLabelKey() {
        return labelKey;
    }

    public fr.skynex.storagepeek.gui.StorageDashboardGUI getStorageDashboardGUI() {
        return storageDashboardGUI;
    }

    public fr.skynex.storagepeek.listener.PlayerListener getPlayerListener() {
        return playerListener;
    }

    public fr.skynex.storagepeek.manager.SoundManager getSoundManager() {
        return soundManager;
    }

    public void playConfigSound(Player player, String soundPath, Sound defaultSound, float defaultVolume, float defaultPitch) {
        if (soundManager != null) soundManager.playConfigSound(player, soundPath, defaultSound, defaultVolume, defaultPitch);
    }

    public void playConfigSoundAt(Player player, Location loc, String soundPath, Sound defaultSound, float defaultVolume, float defaultPitch) {
        if (soundManager != null) soundManager.playConfigSoundAt(player, loc, soundPath, defaultSound, defaultVolume, defaultPitch);
    }

    public fr.skynex.storagepeek.manager.StoragePeekConfigManager getConfigManager() {
        return configManager;
    }

    public fr.skynex.storagepeek.hook.CombatHookManager getCombatHookManager() {
        return combatHookManager;
    }

    public void loadConfigurationCache() {
        if (configManager != null) {
            configManager.loadConfigurationCache();
        }
    }

    public boolean isProtectionHooksEnabled() { return configManager.isProtectionHooksEnabled(); }
    public boolean isProtectionHookEnabled(String pluginName) { return configManager.isProtectionHookEnabled(pluginName); }
    public double getMaxDistance() { return configManager.getMaxDistance(); }
    public double getSlotSpacing() { return configManager.getSlotSpacing(); }
    public float getDisplayDistance() { return configManager.getDisplayDistance(); }
    public int getSyncFrequency() { return configManager.getSyncFrequency(); }
    public float getTextScale() { return configManager.getTextScale(); }
    public float getTextYOffset() { return configManager.getTextYOffset(); }
    public float getTextZOffset() { return configManager.getTextZOffset(); }
    public org.bukkit.Material getDefaultBackground() { return configManager.getDefaultBackground(); }
    public boolean isThemesEnabled() { return configManager.isThemesEnabled(); }
    public boolean isEnderThemeEnabled() { return configManager.isEnderThemeEnabled(); }
    public org.bukkit.Material getEnderBackground() { return configManager.getEnderBackground(); }
    public boolean isEnderParticles() { return configManager.isEnderParticles(); }
    public boolean isRichThemeEnabled() { return configManager.isRichThemeEnabled(); }
    public org.bukkit.Material getRichBackground() { return configManager.getRichBackground(); }
    public boolean isRichParticles() { return configManager.isRichParticles(); }
    public int getRichThreshold() { return configManager.getRichThreshold(); }
    public java.util.Set<org.bukkit.Material> getPreciousMaterials() { return configManager.getPreciousMaterials(); }
    public boolean isFocusModeEnabled() { return configManager.isFocusModeEnabled(); }
    public String getShulkerBgType() { return configManager.getShulkerBgType(); }
    public java.util.Map<String, org.bukkit.Material> getCustomBackgrounds() { return configManager.getCustomBackgrounds(); }
    public boolean isAnimationsEnabled() { return configManager.isAnimationsEnabled(); }
    public boolean isContainerAnimationsEnabled() { return configManager.isContainerAnimationsEnabled(); }
    public boolean isHoverNameplateEnabled() { return configManager.isHoverNameplateEnabled(); }
    public float getHoverNameplateScale() { return configManager.getHoverNameplateScale(); }
    public org.bukkit.Color getHoverNameplateBgColor() { return configManager.getHoverNameplateBgColor(); }
    public org.bukkit.Material getHighlightMaterial() { return configManager.getHighlightMaterial(); }
    public int getTeleportDuration() { return configManager.getTeleportDuration(); }
    public double getDistanceSmoothing() { return configManager.getDistanceSmoothing(); }
    public boolean isCombatCullingEnabled() { return configManager.isCombatCullingEnabled(); }
    public boolean isCombatCullingHookPlugins() { return configManager.isCombatCullingHookPlugins(); }
    public double getCombatCullingCooldown() { return configManager.getCombatCullingCooldown(); }
    public boolean isHideWhenEmpty() { return configManager.isHideWhenEmpty(); }
    public boolean isAutoEnableOnJoin() { return configManager.isAutoEnableOnJoin(); }
    public org.bukkit.Particle getEnderParticleType() { return configManager.getEnderParticleType(); }
    public int getEnderParticleCount() { return configManager.getEnderParticleCount(); }
    public org.bukkit.Particle getRichParticleType() { return configManager.getRichParticleType(); }
    public int getRichParticleCount() { return configManager.getRichParticleCount(); }
    public boolean isQuantityLabelsEnabled() { return configManager.isQuantityLabelsEnabled(); }
    public boolean isDurabilityBarsEnabled() { return configManager.isDurabilityBarsEnabled(); }
    public org.bukkit.Material getDurabilityColorHigh() { return configManager.getDurabilityColorHigh(); }
    public org.bukkit.Material getDurabilityColorMedium() { return configManager.getDurabilityColorMedium(); }
    public org.bukkit.Material getDurabilityColorLow() { return configManager.getDurabilityColorLow(); }
    public java.util.Set<String> getDisabledWorlds() { return configManager.getDisabledWorlds(); }
    public String getLeftClickAction() { return configManager.getLeftClickAction(); }
    public String getRightClickAction() { return configManager.getRightClickAction(); }

    public void addDisabledWorldToCache(org.bukkit.World world) {
        configManager.addDisabledWorldToCache(world);
    }

    public void removeDisabledWorldFromCache(org.bukkit.World world) {
        configManager.removeDisabledWorldFromCache(world);
    }

    public java.util.Set<org.bukkit.World> getDisabledWorldsCache() {
        return configManager.getDisabledWorldsCache();
    }

    public boolean isFillIndicatorEnabled() {
        return configManager.isFillIndicatorEnabled();
    }

    public void openThemesMenu(Player player) {
        fr.skynex.storagepeek.gui.ThemeGUI.openThemesMenu(player);
    }

    public NamespacedKey getDisplayKey() {
        return displayKey;
    }

    public RaycastTask getRaycastTask() {
        return raycastTaskManager != null ? raycastTaskManager.getRaycastTask() : null;
    }

    public void reloadRaycastTasks() {
        if (raycastTaskManager != null) raycastTaskManager.reloadRaycastTasks();
    }

    public void startRaycastTask(Player player) {
        if (raycastTaskManager != null) raycastTaskManager.startRaycastTask(player);
    }

    public void stopRaycastTask(Player player) {
        if (raycastTaskManager != null) raycastTaskManager.stopRaycastTask(player);
    }

    public void tagDisplayEntity(org.bukkit.entity.Entity entity) {
        if (displayEntityManager != null) displayEntityManager.tagDisplayEntity(entity);
    }

    public int purgeOrphanedEntities() {
        return displayEntityManager != null ? displayEntityManager.purgeOrphanedEntities() : 0;
    }

    public List<org.bukkit.block.Block> findContainersInRadius(Location pLoc, int radius, Player player) {
        return baseStorageManager.findContainersInRadius(pLoc, radius, player);
    }

    public int handleSmartBaseDeposit(Player player, int radius) {
        return baseStorageManager.handleSmartBaseDeposit(player, radius);
    }

    public void displayBaseStatsHologram(Player player, int radius) {
        baseStorageManager.displayBaseStatsHologram(player, radius);
    }

    public void startGPSWaypointTask(Player player, org.bukkit.block.Block targetBlock) {
        baseStorageManager.startGPSWaypointTask(player, targetBlock);
    }
}
