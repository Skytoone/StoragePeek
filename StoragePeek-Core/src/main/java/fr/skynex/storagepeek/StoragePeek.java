package fr.skynex.storagepeek;

import fr.skynex.storagepeek.manager.HookManager;
import fr.skynex.storagepeek.manager.MessageManager;
import fr.skynex.storagepeek.manager.ProtectionManager;

import fr.skynex.storagepeek.session.PeekSession;
import fr.skynex.storagepeek.task.RaycastTask;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;

public final class StoragePeek extends JavaPlugin {

    private static StoragePeek instance;
    private final Map<UUID, PeekSession> activeSessions = new HashMap<>();
    private final java.util.Set<UUID> disabledPlayers = new java.util.HashSet<>();
    private RaycastTask raycastTask;
    private final Map<UUID, fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask> raycastTasks = new HashMap<>();
    private ProtectionManager protectionManager;
    private HookManager hookManager;
    private MessageManager messageManager;
    private NamespacedKey disabledKey;
    private NamespacedKey displayKey;
    private fr.skynex.storagepeek.listener.PlayerListener playerListener;
    private fr.skynex.storagepeek.visualizer.VisualizerManager visualizerManager;
    private fr.skynex.storagepeek.visualizer.LootChestGlowTask lootChestGlowTask;

    // Configuration values (Cached for performance)
    private double maxDistance;
    private double slotSpacing;
    private float displayDistance;
    private int syncFrequency;
    private float textScale;
    private float textYOffset;
    private float textZOffset;
    private org.bukkit.Material defaultBackground;
    private boolean themesEnabled;
    private boolean enderThemeEnabled;
    private org.bukkit.Material enderBackground;
    private boolean enderParticles;
    private boolean richThemeEnabled;
    private org.bukkit.Material richBackground;
    private boolean richParticles;
    private int richThreshold;
    private final java.util.Set<org.bukkit.Material> preciousMaterials = new java.util.HashSet<>();
    private boolean focusModeEnabled;
    private String shulkerBgType;
    private final java.util.Map<String, org.bukkit.Material> customBackgrounds = new java.util.HashMap<>();
    private boolean animationsEnabled;
    private boolean containerAnimations;
    private boolean hoverNameplateEnabled;
    private float hoverNameplateScale;
    private org.bukkit.Color hoverNameplateBgColor;
    private org.bukkit.Material highlightMaterial;
    private int teleportDuration;
    private double distanceSmoothing;
    private boolean combatCullingEnabled;
    private boolean combatCullingHookPlugins;
    private double combatCullingCooldown;
    private boolean hideWhenEmpty;
    private boolean autoEnableOnJoin;

    // Additional configuration values
    private org.bukkit.Particle enderParticleType;
    private int enderParticleCount;
    private org.bukkit.Particle richParticleType;
    private int richParticleCount;
    private boolean quantityLabelsEnabled;
    private boolean durabilityBarsEnabled;
    private org.bukkit.Material durabilityColorHigh;
    private org.bukkit.Material durabilityColorMedium;
    private org.bukkit.Material durabilityColorLow;

    // Disabled worlds and Quick Actions click config
    private final java.util.Set<String> disabledWorlds = new java.util.HashSet<>();
    private final java.util.Set<org.bukkit.World> disabledWorldsCache = new java.util.HashSet<>();
    private String leftClickAction;
    private String rightClickAction;

    private boolean protectionHooksEnabled;
    private final java.util.Map<String, Boolean> protectionHookFlags = new java.util.HashMap<>();


    private fr.skynex.storagepeek.listener.ExhibitionFrameListener exhibitionFrameListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfiguration();
        
        this.disabledKey = new NamespacedKey(this, "disabled");
        this.displayKey = new NamespacedKey(this, "display");
        this.messageManager = new MessageManager(this);
        this.protectionManager = new ProtectionManager();
        this.hookManager = new HookManager();
        this.visualizerManager = new fr.skynex.storagepeek.visualizer.VisualizerManager(this);
        loadConfigurationCache();

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



        this.lootChestGlowTask = new fr.skynex.storagepeek.visualizer.LootChestGlowTask(this);
        this.lootChestGlowTask.runTaskTimer(this, 20L, 20L);

        raycastTask = new RaycastTask();
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
        getCommand("storagepeek").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("storagepeek.admin")) {
                    sender.sendMessage(messageManager.getMessage("no-permission"));
                    return true;
                }
                reloadConfig();
                loadConfigurationCache();
                messageManager.reloadConfig();
                
                raycastTasks.values().forEach(task -> {
                    if (task != null) {
                        task.cancel();
                    }
                });
                raycastTasks.clear();
                
                raycastTask = new RaycastTask();
                for (Player player : getServer().getOnlinePlayers()) {
                    startRaycastTask(player);
                }
                
                sender.sendMessage(messageManager.getMessage("reload-success"));
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage(messageManager.getMessage("only-players"));
                    return true;
                }
                if (!player.hasPermission("storagepeek.toggle")) {
                    player.sendMessage(messageManager.getMessage("no-permission"));
                    return true;
                }
                org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
                if (pdc.has(disabledKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    pdc.remove(disabledKey);
                    disabledPlayers.remove(player.getUniqueId());
                    player.sendMessage(messageManager.getMessage("toggle-enabled"));
                } else {
                    pdc.set(disabledKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                    disabledPlayers.add(player.getUniqueId());
                    player.sendMessage(messageManager.getMessage("toggle-disabled"));
                    PeekSession session = activeSessions.remove(player.getUniqueId());
                    if (session != null) {
                        session.cleanup(true);
                    }
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("themes")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage(messageManager.getMessage("only-players"));
                    return true;
                }
                if (!player.hasPermission("storagepeek.themes")) {
                    sender.sendMessage(messageManager.getMessage("no-permission"));
                    return true;
                }
                openThemesMenu(player);
                return true;
            } else if (args.length > 1 && args[0].equalsIgnoreCase("theme")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage(messageManager.getMessage("only-players"));
                    return true;
                }
                String wanted = args[1].toLowerCase().trim();
                List<String> validThemes = Arrays.asList("default", "ender", "rich", "aqua", "nether", "neon", "cyberpunk", "rainbow");
                if (!validThemes.contains(wanted)) {
                    player.sendMessage("§cInvalid theme! Choose from: default, ender, rich, aqua, nether, neon, cyberpunk, rainbow");
                    return true;
                }
                if (!wanted.equals("default") && !player.hasPermission("storagepeek.theme." + wanted)) {
                    player.sendMessage(messageManager.getMessage("theme-no-permission").replace("{theme}", wanted));
                    return true;
                }
                org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
                NamespacedKey themeKey = new NamespacedKey(this, "theme");
                pdc.set(themeKey, org.bukkit.persistence.PersistentDataType.STRING, wanted);
                player.sendMessage(messageManager.getMessage("theme-updated").replace("{theme}", wanted));
                return true;
            } else if (args.length > 1 && args[0].equalsIgnoreCase("filter")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage(messageManager.getMessage("only-players"));
                    return true;
                }
                if (!player.hasPermission("storagepeek.filter") && !player.hasPermission("storagepeek.admin")) {
                    player.sendMessage(messageManager.getMessage("no-permission"));
                    return true;
                }
                String wanted = args[1].toUpperCase().trim();
                try {
                    fr.skynex.storagepeek.session.PeekSession.FilterType filter = 
                        fr.skynex.storagepeek.session.PeekSession.FilterType.valueOf(wanted);
                    
                    PeekSession session = activeSessions.get(player.getUniqueId());
                    if (session != null) {
                        session.setActiveFilter(filter);
                        player.sendMessage(messageManager.getMessage("filter-updated").replace("{filter}", wanted.toLowerCase()));
                        playConfigSound(player, "sort", org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                    } else {
                        player.sendMessage("§cYou must be looking at a container to apply a filter.");
                    }
                } catch (Exception ex) {
                    player.sendMessage("§cInvalid filter type! Choose from: ALL, RESOURCES, FOOD, EQUIPMENT");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("purge")) {
                if (!sender.hasPermission("storagepeek.admin")) {
                    sender.sendMessage(messageManager.getMessage("no-permission"));
                    return true;
                }
                int purged = purgeOrphanedEntities();
                sender.sendMessage("§aPurged " + purged + " orphaned StoragePeek display entities across all loaded chunks.");
                return true;
            }
            sender.sendMessage(messageManager.getMessage("usage-reload"));
            sender.sendMessage(messageManager.getMessage("usage-toggle"));
            sender.sendMessage(messageManager.getMessage("usage-themes"));
            sender.sendMessage(messageManager.getMessage("usage-filter"));
            sender.sendMessage("§e/storagepeek purge §7- Purge orphaned display entities.");
            return true;
        });

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
        if (raycastTask != null) {
            raycastTask.cleanupAllCompassArrows();
        }
        if (visualizerManager != null) {
            visualizerManager.shutdown();
        }
        if (lootChestGlowTask != null) {
            lootChestGlowTask.cancel();
        }
        activeSessions.values().forEach(session -> session.cleanup(true));
        activeSessions.clear();
        disabledPlayers.clear();
        raycastTasks.values().forEach(task -> {
            if (task != null) {
                task.cancel();
            }
        });
        raycastTasks.clear();
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

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public RaycastTask getRaycastTask() {
        return raycastTask;
    }

    public NamespacedKey getDisabledKey() {
        return disabledKey;
    }

    public fr.skynex.storagepeek.listener.PlayerListener getPlayerListener() {
        return playerListener;
    }

    public void playConfigSound(Player player, String soundPath, Sound defaultSound, float defaultVolume, float defaultPitch) {
        if (!getConfig().getBoolean("sounds.enabled", true)) {
            return;
        }
        String typeStr = getConfig().getString("sounds." + soundPath + ".type");
        Sound sound = defaultSound;
        if (typeStr != null) {
            try {
                String formatted = typeStr.toLowerCase().trim();
                NamespacedKey key = null;
                if (formatted.contains(":")) {
                    key = NamespacedKey.fromString(formatted);
                } else {
                    key = NamespacedKey.minecraft(formatted.replace('_', '.'));
                }

                Sound registrySound = key != null ? org.bukkit.Registry.SOUNDS.get(key) : null;
                if (registrySound != null) {
                    sound = registrySound;
                } else {
                    @SuppressWarnings("deprecation")
                    Sound legacySound = Sound.valueOf(typeStr.toUpperCase().trim());
                    sound = legacySound;
                }
            } catch (Exception ignored) {}
        }
        double volume = getConfig().getDouble("sounds." + soundPath + ".volume", defaultVolume);
        double pitch = getConfig().getDouble("sounds." + soundPath + ".pitch", defaultPitch);
        player.playSound(player.getLocation(), sound, (float) volume, (float) pitch);
    }

    public void loadConfigurationCache() {
        this.maxDistance = getConfig().getDouble("max-distance", 5.0);
        this.slotSpacing = getConfig().getDouble("slot-spacing", 0.18);
        this.displayDistance = (float) getConfig().getDouble("display-distance", 1.8);
        this.syncFrequency = getConfig().getInt("sync-frequency", 5);
        this.textScale = (float) getConfig().getDouble("text-scale", 0.2);
        this.textYOffset = (float) getConfig().getDouble("text-y-offset", -0.05);
        this.textZOffset = (float) getConfig().getDouble("text-z-offset", 0.08);

        String bgName = getConfig().getString("default-background", "BLACK_STAINED_GLASS");
        try {
            this.defaultBackground = org.bukkit.Material.valueOf(bgName);
        } catch (Exception e) {
            this.defaultBackground = org.bukkit.Material.BLACK_STAINED_GLASS;
        }

        this.themesEnabled = getConfig().getBoolean("themes.enabled", true);
        this.enderThemeEnabled = getConfig().getBoolean("themes.ender.enabled", true);
        
        String enderBgName = getConfig().getString("themes.ender.background", "OBSIDIAN");
        this.enderBackground = org.bukkit.Material.matchMaterial(enderBgName);
        if (this.enderBackground == null) {
            this.enderBackground = org.bukkit.Material.OBSIDIAN;
        }
        this.enderParticles = getConfig().getBoolean("themes.ender.particles", true);

        this.richThemeEnabled = getConfig().getBoolean("themes.rich.enabled", true);
        String richBgName = getConfig().getString("themes.rich.background", "GOLD_BLOCK");
        this.richBackground = org.bukkit.Material.matchMaterial(richBgName);
        if (this.richBackground == null) {
            this.richBackground = org.bukkit.Material.GOLD_BLOCK;
        }
        this.richParticles = getConfig().getBoolean("themes.rich.particles", true);
        this.richThreshold = getConfig().getInt("themes.rich.threshold", 32);

        this.preciousMaterials.clear();
        for (String name : getConfig().getStringList("themes.rich.items")) {
            org.bukkit.Material m = org.bukkit.Material.matchMaterial(name);
            if (m != null) {
                this.preciousMaterials.add(m);
            }
        }

        this.focusModeEnabled = getConfig().getBoolean("focus-mode", true);
        this.shulkerBgType = getConfig().getString("shulker-background-type", "STAINED_GLASS");

        this.customBackgrounds.clear();
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection("container-backgrounds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String matName = section.getString(key);
                if (matName != null) {
                    org.bukkit.Material m = org.bukkit.Material.matchMaterial(matName);
                    if (m != null) {
                        this.customBackgrounds.put(key.toUpperCase(), m);
                    }
                }
            }
        }

        this.animationsEnabled = getConfig().getBoolean("holograms.animations-enabled", true);
        this.containerAnimations = getConfig().getBoolean("holograms.container-animations", true);
        this.hoverNameplateEnabled = getConfig().getBoolean("holograms.hover-nameplate-enabled", true);
        this.hoverNameplateScale = (float) getConfig().getDouble("holograms.hover-nameplate-scale", 0.18);
        
        String colorStr = getConfig().getString("holograms.hover-nameplate-bg-color", "160,20,20,20");
        this.hoverNameplateBgColor = parseColor(colorStr, org.bukkit.Color.fromARGB(160, 20, 20, 20));

        String matStr = getConfig().getString("holograms.hover-highlight-material", "WHITE_STAINED_GLASS");
        this.highlightMaterial = org.bukkit.Material.matchMaterial(matStr);
        if (this.highlightMaterial == null) {
            this.highlightMaterial = org.bukkit.Material.WHITE_STAINED_GLASS;
        }

        this.teleportDuration = getConfig().getInt("holograms.teleport-duration", 3);
        this.distanceSmoothing = getConfig().getDouble("holograms.distance-smoothing", 0.15);

        this.combatCullingEnabled = getConfig().getBoolean("combat-culling.enabled", true);
        this.combatCullingHookPlugins = getConfig().getBoolean("combat-culling.hook-plugins", true);
        this.combatCullingCooldown = getConfig().getDouble("combat-culling.pvp-cooldown-seconds", 10.0);
        this.hideWhenEmpty = getConfig().getBoolean("hide-when-empty", false);
        this.autoEnableOnJoin = getConfig().getBoolean("auto-enable-on-join", false);
        if (this.autoEnableOnJoin) {
            this.disabledPlayers.clear();
            for (Player player : getServer().getOnlinePlayers()) {
                if (disabledKey != null) {
                    player.getPersistentDataContainer().remove(disabledKey);
                }
            }
        }

        // Parse custom particle configurations
        String enderPartStr = getConfig().getString("themes.ender.particle-type", "PORTAL");
        try {
            this.enderParticleType = org.bukkit.Particle.valueOf(enderPartStr.toUpperCase().trim());
        } catch (Exception e) {
            this.enderParticleType = org.bukkit.Particle.PORTAL;
        }
        this.enderParticleCount = getConfig().getInt("themes.ender.particle-count", 3);

        String richPartStr = getConfig().getString("themes.rich.particle-type", "WAX_OFF");
        try {
            this.richParticleType = org.bukkit.Particle.valueOf(richPartStr.toUpperCase().trim());
        } catch (Exception e) {
            this.richParticleType = org.bukkit.Particle.WAX_OFF;
        }
        this.richParticleCount = getConfig().getInt("themes.rich.particle-count", 1);

        // Parse label and durability toggles
        this.quantityLabelsEnabled = getConfig().getBoolean("holograms.quantity-labels-enabled", true);
        this.durabilityBarsEnabled = getConfig().getBoolean("holograms.durability-bars-enabled", true);

        // Parse custom durability materials
        String colHighStr = getConfig().getString("holograms.durability-colors.high", "LIME_CONCRETE");
        this.durabilityColorHigh = org.bukkit.Material.matchMaterial(colHighStr);
        if (this.durabilityColorHigh == null) {
            this.durabilityColorHigh = org.bukkit.Material.LIME_CONCRETE;
        }

        String colMedStr = getConfig().getString("holograms.durability-colors.medium", "YELLOW_CONCRETE");
        this.durabilityColorMedium = org.bukkit.Material.matchMaterial(colMedStr);
        if (this.durabilityColorMedium == null) {
            this.durabilityColorMedium = org.bukkit.Material.YELLOW_CONCRETE;
        }

        String colLowStr = getConfig().getString("holograms.durability-colors.low", "RED_CONCRETE");
        this.durabilityColorLow = org.bukkit.Material.matchMaterial(colLowStr);
        if (this.durabilityColorLow == null) {
            this.durabilityColorLow = org.bukkit.Material.RED_CONCRETE;
        }

        // Parse disabled worlds
        this.disabledWorlds.clear();
        this.disabledWorldsCache.clear();
        for (String worldName : getConfig().getStringList("disabled-worlds")) {
            if (worldName != null) {
                String nameLower = worldName.toLowerCase().trim();
                this.disabledWorlds.add(nameLower);
                org.bukkit.World world = getServer().getWorld(nameLower);
                if (world != null) {
                    this.disabledWorldsCache.add(world);
                }
            }
        }

        // Parse quick actions click config
        this.leftClickAction = getConfig().getString("quick-actions.left-click", "TAKE").toUpperCase().trim();
        this.rightClickAction = getConfig().getString("quick-actions.right-click", "DEPOSIT").toUpperCase().trim();

        // Parse protection hooks config
        this.protectionHooksEnabled = getConfig().getBoolean("protection-hooks.enabled", true);
        this.protectionHookFlags.clear();
        org.bukkit.configuration.ConfigurationSection protSection = getConfig().getConfigurationSection("protection-hooks");
        if (protSection != null) {
            for (String key : protSection.getKeys(false)) {
                if (!key.equalsIgnoreCase("enabled")) {
                    this.protectionHookFlags.put(key.toLowerCase(), protSection.getBoolean(key, true));
                }
            }
        }
    }

    private org.bukkit.Color parseColor(String str, org.bukkit.Color defaultColor) {
        if (str == null) return defaultColor;
        String[] parts = str.split(",");
        if (parts.length == 4) {
            try {
                int a = Integer.parseInt(parts[0].trim());
                int r = Integer.parseInt(parts[1].trim());
                int g = Integer.parseInt(parts[2].trim());
                int b = Integer.parseInt(parts[3].trim());
                return org.bukkit.Color.fromARGB(a, r, g, b);
            } catch (NumberFormatException ignored) {}
        } else if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return org.bukkit.Color.fromRGB(r, g, b);
            } catch (NumberFormatException ignored) {}
        }
        return defaultColor;
    }

    public boolean isProtectionHooksEnabled() { return protectionHooksEnabled; }
    public boolean isProtectionHookEnabled(String pluginName) {
        if (!protectionHooksEnabled) return false;
        return protectionHookFlags.getOrDefault(pluginName.toLowerCase(), true);
    }

    public double getMaxDistance() { return maxDistance; }

    public double getSlotSpacing() { return slotSpacing; }
    public float getDisplayDistance() { return displayDistance; }
    public int getSyncFrequency() { return syncFrequency; }
    public float getTextScale() { return textScale; }
    public float getTextYOffset() { return textYOffset; }
    public float getTextZOffset() { return textZOffset; }
    public org.bukkit.Material getDefaultBackground() { return defaultBackground; }
    public boolean isThemesEnabled() { return themesEnabled; }
    public boolean isEnderThemeEnabled() { return enderThemeEnabled; }
    public org.bukkit.Material getEnderBackground() { return enderBackground; }
    public boolean isEnderParticles() { return enderParticles; }
    public boolean isRichThemeEnabled() { return richThemeEnabled; }
    public org.bukkit.Material getRichBackground() { return richBackground; }
    public boolean isRichParticles() { return richParticles; }
    public int getRichThreshold() { return richThreshold; }
    public java.util.Set<org.bukkit.Material> getPreciousMaterials() { return preciousMaterials; }
    public boolean isFocusModeEnabled() { return focusModeEnabled; }
    public String getShulkerBgType() { return shulkerBgType; }
    public java.util.Map<String, org.bukkit.Material> getCustomBackgrounds() { return customBackgrounds; }
    public boolean isAnimationsEnabled() { return animationsEnabled; }
    public boolean isContainerAnimationsEnabled() { return containerAnimations; }
    public boolean isHoverNameplateEnabled() { return hoverNameplateEnabled; }
    public float getHoverNameplateScale() { return hoverNameplateScale; }
    public org.bukkit.Color getHoverNameplateBgColor() { return hoverNameplateBgColor; }
    public org.bukkit.Material getHighlightMaterial() { return highlightMaterial; }
    public int getTeleportDuration() { return teleportDuration; }
    public double getDistanceSmoothing() { return distanceSmoothing; }
    public boolean isCombatCullingEnabled() { return combatCullingEnabled; }
    public boolean isCombatCullingHookPlugins() { return combatCullingHookPlugins; }
    public double getCombatCullingCooldown() { return combatCullingCooldown; }
    public boolean isHideWhenEmpty() { return hideWhenEmpty; }
    public boolean isAutoEnableOnJoin() { return autoEnableOnJoin; }

    public org.bukkit.Particle getEnderParticleType() { return enderParticleType; }
    public int getEnderParticleCount() { return enderParticleCount; }
    public org.bukkit.Particle getRichParticleType() { return richParticleType; }
    public int getRichParticleCount() { return richParticleCount; }
    public boolean isQuantityLabelsEnabled() { return quantityLabelsEnabled; }
    public boolean isDurabilityBarsEnabled() { return durabilityBarsEnabled; }
    public org.bukkit.Material getDurabilityColorHigh() { return durabilityColorHigh; }
    public org.bukkit.Material getDurabilityColorMedium() { return durabilityColorMedium; }
    public org.bukkit.Material getDurabilityColorLow() { return durabilityColorLow; }

    public java.util.Set<String> getDisabledWorlds() { return disabledWorlds; }
    public String getLeftClickAction() { return leftClickAction; }
    public String getRightClickAction() { return rightClickAction; }

    public void addDisabledWorldToCache(org.bukkit.World world) {
        if (world != null && disabledWorlds.contains(world.getName().toLowerCase().trim())) {
            disabledWorldsCache.add(world);
        }
    }

    public void removeDisabledWorldFromCache(org.bukkit.World world) {
        if (world != null) {
            disabledWorldsCache.remove(world);
        }
    }

    public java.util.Set<org.bukkit.World> getDisabledWorldsCache() {
        return disabledWorldsCache;
    }



    public void startRaycastTask(Player player) {
        UUID uuid = player.getUniqueId();
        stopRaycastTask(player);

        long interval = getConfig().getLong("raycast-frequency", 2L);
        if (interval < 1) interval = 1L;

        fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask task = fr.skynex.storagepeek.util.FoliaScheduler.runTimer(
            this,
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
        fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask task = raycastTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void openThemesMenu(Player player) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("StoragePeek - Themes"));
        
        gui.setItem(0, createGuiItem(Material.GLASS, "§fDefault", "§7Standard translucent stained glass."));
        gui.setItem(1, createGuiItem(Material.ENDER_PEARL, "§dEnder", "§7Obsidian background with portal particles."));
        gui.setItem(2, createGuiItem(Material.GOLD_INGOT, "§6Rich", "§7Gold block background with gold sparkle particles."));
        gui.setItem(3, createGuiItem(Material.PRISMARINE_SHARD, "§bAqua", "§7Dark prismarine background with bubbles."));
        gui.setItem(4, createGuiItem(Material.NETHERRACK, "§cNether", "§7Netherrack background with flames."));
        gui.setItem(5, createGuiItem(Material.GLOW_INK_SAC, "§aNeon", "§7Dark background with neon glow particles."));
        gui.setItem(6, createGuiItem(Material.WARPED_FUNGUS, "§3Cyberpunk", "§7Warped wart background with warped spores."));
        gui.setItem(7, createGuiItem(Material.RED_DYE, "§dRainbow", "§7Glazed terracotta background with cherry petals."));

        player.openInventory(gui);
    }

    private org.bukkit.inventory.ItemStack createGuiItem(org.bukkit.Material mat, String name, String description) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(name));
            meta.lore(Arrays.asList(net.kyori.adventure.text.Component.text(description)));
            item.setItemMeta(meta);
        }
        return item;
    }

    public NamespacedKey getDisplayKey() {
        return displayKey;
    }

    public void tagDisplayEntity(org.bukkit.entity.Entity entity) {
        if (entity != null) {
            entity.setPersistent(false);
            if (displayKey != null) {
                entity.getPersistentDataContainer().set(displayKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            }
        }
    }

    public int purgeOrphanedEntities() {
        int count = 0;
        if (displayKey == null) return count;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntitiesByClasses(
                    org.bukkit.entity.ItemDisplay.class,
                    org.bukkit.entity.BlockDisplay.class,
                    org.bukkit.entity.TextDisplay.class,
                    org.bukkit.entity.Interaction.class)) {
                if (entity.getPersistentDataContainer().has(displayKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    entity.remove();
                    count++;
                }
            }
        }
        return count;
    }
}
