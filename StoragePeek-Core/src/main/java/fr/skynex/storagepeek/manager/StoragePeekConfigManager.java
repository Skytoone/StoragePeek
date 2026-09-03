package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StoragePeekConfigManager {

    private final StoragePeek plugin;

    // Configuration values (Cached for performance)
    private double maxDistance;
    private double slotSpacing;
    private float displayDistance;
    private int syncFrequency;
    private float textScale;
    private float textYOffset;
    private float textZOffset;
    private Material defaultBackground;
    private boolean themesEnabled;
    private boolean enderThemeEnabled;
    private Material enderBackground;
    private boolean enderParticles;
    private boolean richThemeEnabled;
    private Material richBackground;
    private boolean richParticles;
    private int richThreshold;
    private final Set<Material> preciousMaterials = new HashSet<>();
    private boolean focusModeEnabled;
    private String shulkerBgType;
    private final Map<String, Material> customBackgrounds = new HashMap<>();
    private boolean animationsEnabled;
    private boolean containerAnimations;
    private boolean hoverNameplateEnabled;
    private float hoverNameplateScale;
    private Color hoverNameplateBgColor;
    private Material highlightMaterial;
    private int teleportDuration;
    private double distanceSmoothing;
    private boolean combatCullingEnabled;
    private boolean combatCullingHookPlugins;
    private double combatCullingCooldown;
    private boolean hideWhenEmpty;
    private boolean autoEnableOnJoin;
    private boolean fillIndicatorEnabled;

    // Additional configuration values
    private Particle enderParticleType;
    private int enderParticleCount;
    private Particle richParticleType;
    private int richParticleCount;
    private boolean quantityLabelsEnabled;
    private boolean durabilityBarsEnabled;
    private Material durabilityColorHigh;
    private Material durabilityColorMedium;
    private Material durabilityColorLow;

    // Disabled worlds and Quick Actions click config
    private final Set<String> disabledWorlds = new HashSet<>();
    private final Set<World> disabledWorldsCache = new HashSet<>();
    private String leftClickAction;
    private String rightClickAction;

    private boolean protectionHooksEnabled;
    private final Map<String, Boolean> protectionHookFlags = new HashMap<>();

    public StoragePeekConfigManager(StoragePeek plugin) {
        this.plugin = plugin;
    }

    public void loadConfigurationCache() {
        this.maxDistance = plugin.getConfig().getDouble("max-distance", 5.0);
        this.slotSpacing = plugin.getConfig().getDouble("slot-spacing", 0.18);
        this.displayDistance = (float) plugin.getConfig().getDouble("display-distance", 1.8);
        this.syncFrequency = plugin.getConfig().getInt("sync-frequency", 5);
        this.textScale = (float) plugin.getConfig().getDouble("text-scale", 0.2);
        this.textYOffset = (float) plugin.getConfig().getDouble("text-y-offset", -0.05);
        this.textZOffset = (float) plugin.getConfig().getDouble("text-z-offset", 0.08);

        String bgName = plugin.getConfig().getString("default-background", "BLACK_STAINED_GLASS");
        try {
            this.defaultBackground = Material.valueOf(bgName);
        } catch (Exception e) {
            this.defaultBackground = Material.BLACK_STAINED_GLASS;
        }

        this.themesEnabled = plugin.getConfig().getBoolean("themes.enabled", true);
        this.enderThemeEnabled = plugin.getConfig().getBoolean("themes.ender.enabled", true);

        String enderBgName = plugin.getConfig().getString("themes.ender.background", "OBSIDIAN");
        this.enderBackground = Material.matchMaterial(enderBgName);
        if (this.enderBackground == null) {
            this.enderBackground = Material.OBSIDIAN;
        }
        this.enderParticles = plugin.getConfig().getBoolean("themes.ender.particles", true);

        this.richThemeEnabled = plugin.getConfig().getBoolean("themes.rich.enabled", true);
        String richBgName = plugin.getConfig().getString("themes.rich.background", "GOLD_BLOCK");
        this.richBackground = Material.matchMaterial(richBgName);
        if (this.richBackground == null) {
            this.richBackground = Material.GOLD_BLOCK;
        }
        this.richParticles = plugin.getConfig().getBoolean("themes.rich.particles", true);
        this.richThreshold = plugin.getConfig().getInt("themes.rich.threshold", 32);

        this.preciousMaterials.clear();
        for (String name : plugin.getConfig().getStringList("themes.rich.items")) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                this.preciousMaterials.add(m);
            }
        }

        this.focusModeEnabled = plugin.getConfig().getBoolean("focus-mode", true);
        this.shulkerBgType = plugin.getConfig().getString("shulker-background-type", "STAINED_GLASS");

        this.customBackgrounds.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("container-backgrounds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String matName = section.getString(key);
                if (matName != null) {
                    Material m = Material.matchMaterial(matName);
                    if (m != null) {
                        this.customBackgrounds.put(key.toUpperCase(), m);
                    }
                }
            }
        }

        this.animationsEnabled = plugin.getConfig().getBoolean("holograms.animations-enabled", true);
        this.containerAnimations = plugin.getConfig().getBoolean("holograms.container-animations", true);
        this.hoverNameplateEnabled = plugin.getConfig().getBoolean("holograms.hover-nameplate-enabled", true);
        this.hoverNameplateScale = (float) plugin.getConfig().getDouble("holograms.hover-nameplate-scale", 0.18);

        String colorStr = plugin.getConfig().getString("holograms.hover-nameplate-bg-color", "160,20,20,20");
        this.hoverNameplateBgColor = parseColor(colorStr, Color.fromARGB(160, 20, 20, 20));

        String matStr = plugin.getConfig().getString("holograms.hover-highlight-material", "WHITE_STAINED_GLASS");
        this.highlightMaterial = Material.matchMaterial(matStr);
        if (this.highlightMaterial == null) {
            this.highlightMaterial = Material.WHITE_STAINED_GLASS;
        }

        this.teleportDuration = plugin.getConfig().getInt("holograms.teleport-duration", 3);
        this.distanceSmoothing = plugin.getConfig().getDouble("holograms.distance-smoothing", 0.15);

        this.combatCullingEnabled = plugin.getConfig().getBoolean("combat-culling.enabled", true);
        this.combatCullingHookPlugins = plugin.getConfig().getBoolean("combat-culling.hook-plugins", true);
        this.combatCullingCooldown = plugin.getConfig().getDouble("combat-culling.pvp-cooldown-seconds", 10.0);
        this.hideWhenEmpty = plugin.getConfig().getBoolean("hide-when-empty", false);
        this.autoEnableOnJoin = plugin.getConfig().getBoolean("auto-enable-on-join", false);
        if (this.autoEnableOnJoin) {
            plugin.getDisabledPlayers().clear();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDisabledKey() != null) {
                    player.getPersistentDataContainer().remove(plugin.getDisabledKey());
                }
            }
        }

        // Parse custom particle configurations
        String enderPartStr = plugin.getConfig().getString("themes.ender.particle-type", "PORTAL");
        try {
            this.enderParticleType = Particle.valueOf(enderPartStr.toUpperCase().trim());
        } catch (Exception e) {
            this.enderParticleType = Particle.PORTAL;
        }
        this.enderParticleCount = plugin.getConfig().getInt("themes.ender.particle-count", 3);

        String richPartStr = plugin.getConfig().getString("themes.rich.particle-type", "WAX_OFF");
        try {
            this.richParticleType = Particle.valueOf(richPartStr.toUpperCase().trim());
        } catch (Exception e) {
            this.richParticleType = Particle.WAX_OFF;
        }
        this.richParticleCount = plugin.getConfig().getInt("themes.rich.particle-count", 1);

        // Parse label and durability toggles
        this.fillIndicatorEnabled = plugin.getConfig().getBoolean("visualizers.fill-indicator", true)
                && plugin.getConfig().getBoolean("holograms.fill-indicator-enabled", true);
        this.quantityLabelsEnabled = plugin.getConfig().getBoolean("holograms.quantity-labels-enabled", true);
        this.durabilityBarsEnabled = plugin.getConfig().getBoolean("holograms.durability-bars-enabled", true);

        // Parse custom durability materials
        String colHighStr = plugin.getConfig().getString("holograms.durability-colors.high", "LIME_CONCRETE");
        this.durabilityColorHigh = Material.matchMaterial(colHighStr);
        if (this.durabilityColorHigh == null) {
            this.durabilityColorHigh = Material.LIME_CONCRETE;
        }

        String colMedStr = plugin.getConfig().getString("holograms.durability-colors.medium", "YELLOW_CONCRETE");
        this.durabilityColorMedium = Material.matchMaterial(colMedStr);
        if (this.durabilityColorMedium == null) {
            this.durabilityColorMedium = Material.YELLOW_CONCRETE;
        }

        String colLowStr = plugin.getConfig().getString("holograms.durability-colors.low", "RED_CONCRETE");
        this.durabilityColorLow = Material.matchMaterial(colLowStr);
        if (this.durabilityColorLow == null) {
            this.durabilityColorLow = Material.RED_CONCRETE;
        }

        // Parse disabled worlds
        this.disabledWorlds.clear();
        this.disabledWorldsCache.clear();
        for (String worldName : plugin.getConfig().getStringList("disabled-worlds")) {
            if (worldName != null) {
                String nameLower = worldName.toLowerCase().trim();
                this.disabledWorlds.add(nameLower);
                World world = plugin.getServer().getWorld(nameLower);
                if (world != null) {
                    this.disabledWorldsCache.add(world);
                }
            }
        }

        // Parse quick actions click config
        this.leftClickAction = plugin.getConfig().getString("quick-actions.left-click", "TAKE").toUpperCase().trim();
        this.rightClickAction = plugin.getConfig().getString("quick-actions.right-click", "DEPOSIT").toUpperCase().trim();

        // Parse protection hooks config
        this.protectionHooksEnabled = plugin.getConfig().getBoolean("protection-hooks.enabled", true);
        this.protectionHookFlags.clear();
        ConfigurationSection protSection = plugin.getConfig().getConfigurationSection("protection-hooks");
        if (protSection != null) {
            for (String key : protSection.getKeys(false)) {
                if (!key.equalsIgnoreCase("enabled")) {
                    this.protectionHookFlags.put(key.toLowerCase(), protSection.getBoolean(key, true));
                }
            }
        }
        if (plugin.getProtectionManager() != null) plugin.getProtectionManager().reloadHooks();
        if (plugin.getHookManager() != null) plugin.getHookManager().reloadHooks();
        if (plugin.getLootGlowHook() != null) plugin.getLootGlowHook().init();
        if (plugin.getCombatHookManager() != null) plugin.getCombatHookManager().resetReflectionCache();
    }

    private Color parseColor(String str, Color defaultColor) {
        if (str == null) return defaultColor;
        String[] parts = str.split(",");
        if (parts.length == 4) {
            try {
                int a = Integer.parseInt(parts[0].trim());
                int r = Integer.parseInt(parts[1].trim());
                int g = Integer.parseInt(parts[2].trim());
                int b = Integer.parseInt(parts[3].trim());
                return Color.fromARGB(a, r, g, b);
            } catch (NumberFormatException ignored) {}
        } else if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
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
    public Material getDefaultBackground() { return defaultBackground; }
    public boolean isThemesEnabled() { return themesEnabled; }
    public boolean isEnderThemeEnabled() { return enderThemeEnabled; }
    public Material getEnderBackground() { return enderBackground; }
    public boolean isEnderParticles() { return enderParticles; }
    public boolean isRichThemeEnabled() { return richThemeEnabled; }
    public Material getRichBackground() { return richBackground; }
    public boolean isRichParticles() { return richParticles; }
    public int getRichThreshold() { return richThreshold; }
    public Set<Material> getPreciousMaterials() { return preciousMaterials; }
    public boolean isFocusModeEnabled() { return focusModeEnabled; }
    public String getShulkerBgType() { return shulkerBgType; }
    public Map<String, Material> getCustomBackgrounds() { return customBackgrounds; }
    public boolean isAnimationsEnabled() { return animationsEnabled; }
    public boolean isContainerAnimationsEnabled() { return containerAnimations; }
    public boolean isHoverNameplateEnabled() { return hoverNameplateEnabled; }
    public float getHoverNameplateScale() { return hoverNameplateScale; }
    public Color getHoverNameplateBgColor() { return hoverNameplateBgColor; }
    public Material getHighlightMaterial() { return highlightMaterial; }
    public int getTeleportDuration() { return teleportDuration; }
    public double getDistanceSmoothing() { return distanceSmoothing; }
    public boolean isCombatCullingEnabled() { return combatCullingEnabled; }
    public boolean isCombatCullingHookPlugins() { return combatCullingHookPlugins; }
    public double getCombatCullingCooldown() { return combatCullingCooldown; }
    public boolean isHideWhenEmpty() { return hideWhenEmpty; }
    public boolean isAutoEnableOnJoin() { return autoEnableOnJoin; }

    public Particle getEnderParticleType() { return enderParticleType; }
    public int getEnderParticleCount() { return enderParticleCount; }
    public Particle getRichParticleType() { return richParticleType; }
    public int getRichParticleCount() { return richParticleCount; }
    public boolean isQuantityLabelsEnabled() { return quantityLabelsEnabled; }
    public boolean isDurabilityBarsEnabled() { return durabilityBarsEnabled; }
    public Material getDurabilityColorHigh() { return durabilityColorHigh; }
    public Material getDurabilityColorMedium() { return durabilityColorMedium; }
    public Material getDurabilityColorLow() { return durabilityColorLow; }

    public Set<String> getDisabledWorlds() { return disabledWorlds; }
    public String getLeftClickAction() { return leftClickAction; }
    public String getRightClickAction() { return rightClickAction; }

    public void addDisabledWorldToCache(World world) {
        if (world != null && disabledWorlds.contains(world.getName().toLowerCase().trim())) {
            disabledWorldsCache.add(world);
        }
    }

    public void removeDisabledWorldFromCache(World world) {
        if (world != null) {
            disabledWorldsCache.remove(world);
        }
    }

    public Set<World> getDisabledWorldsCache() {
        return disabledWorldsCache;
    }

    public boolean isFillIndicatorEnabled() {
        return fillIndicatorEnabled;
    }
}
