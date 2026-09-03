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
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

public final class StoragePeek extends JavaPlugin {

    private static StoragePeek instance;
    private final Map<UUID, PeekSession> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<UUID> disabledPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private RaycastTask raycastTask;
    private final Map<UUID, fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask> raycastTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private ProtectionManager protectionManager;
    private HookManager hookManager;
    private MessageManager messageManager;
    private NamespacedKey disabledKey;
    private NamespacedKey displayKey;
    private fr.skynex.storagepeek.listener.PlayerListener playerListener;
    private fr.skynex.storagepeek.visualizer.VisualizerManager visualizerManager;
    private fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask lootChestGlowTaskHandle;
    private fr.skynex.storagepeek.manager.ContainerHistoryManager containerHistoryManager;
    private fr.skynex.storagepeek.hook.LootGlowHook lootGlowHook;

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
        
        this.disabledKey = new NamespacedKey(this, "disabled");
        this.displayKey = new NamespacedKey(this, "display");
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



        this.lootChestGlowTaskHandle = fr.skynex.storagepeek.util.FoliaScheduler.runTimer(this, null, () -> new fr.skynex.storagepeek.visualizer.LootChestGlowTask(this).run(), 20L, 20L);

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

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new fr.skynex.storagepeek.hook.StoragePeekPAPIExpansion(this).register();
            getLogger().info("Successfully registered PlaceholderAPI expansion (%storagepeek_...)!");
        }

        try {
            org.bukkit.command.PluginCommand spCommand = getCommand("storagepeek");
            if (spCommand != null) {
                spCommand.setExecutor((sender, command, label, args) -> {
                    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                        if (!sender.hasPermission("storagepeek.reload") && !sender.hasPermission("storagepeek.admin")) {
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
                        if (args.length > 1 && args[1].equalsIgnoreCase("rarity")) {
                            PeekSession session = activeSessions.get(player.getUniqueId());
                            if (session == null) {
                                player.sendMessage("§cYou must be looking at a container to apply a rarity filter.");
                                return true;
                            }
                            String rarity = args.length > 2 ? args[2].toUpperCase() : "RESET";
                            if ("RESET".equals(rarity) || "CLEAR".equals(rarity) || "ALL".equals(rarity)) {
                                session.setRarityFilter(null);
                                player.sendMessage("§a[StoragePeek] Rarity filter reset!");
                            } else {
                                session.setRarityFilter(rarity);
                                player.sendMessage("§a[StoragePeek] Set 3D rarity filter to §e" + rarity + "§a!");
                            }
                            playConfigSound(player, "sort", org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.3f);
                            return true;
                        }
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
                            player.sendMessage("§cInvalid filter type! Choose from: ALL, RESOURCES, FOOD, EQUIPMENT, or /sp filter rarity <MYTHIC|LEGENDARY|EPIC|RARE|UNCOMMON|COMMON>");
                        }
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("dashboard")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        int radius = 25;
                        if (args.length > 1) {
                            try { radius = Math.min(64, Math.max(5, Integer.parseInt(args[1]))); } catch (NumberFormatException ignored) {}
                        }
                        if (storageDashboardGUI != null) storageDashboardGUI.openDashboard(player, radius);
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("purge")) {
                        if (!sender.hasPermission("storagepeek.purge") && !sender.hasPermission("storagepeek.admin")) {
                            sender.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        int purged = purgeOrphanedEntities();
                        sender.sendMessage("§aPurged " + purged + " orphaned StoragePeek display entities across all loaded chunks.");
                        return true;
                    } else if (args.length > 1 && args[0].equalsIgnoreCase("find")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.find") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        String wantedName = args[1].toUpperCase().trim();
                        Material mat = Material.matchMaterial(wantedName);
                        if (mat == null) {
                            player.sendMessage("§cInvalid item material! Example: /sp find DIAMOND");
                            return true;
                        }

                        List<org.bukkit.block.Block> containers = ((fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get())
                            .findNearbyContainers(player.getLocation(), 32.0, mat);

                        if (containers.isEmpty()) {
                            player.sendMessage("§cNo nearby containers containing " + mat.name() + " were found within 32 blocks.");
                            return true;
                        }

                        org.bukkit.block.Block nearest = containers.get(0);
                        player.sendMessage("§aFound " + containers.size() + " container(s) with " + mat.name() + "! Pointing compass arrow to nearest container.");
                        getRaycastTask().setCompassTarget(player, nearest.getLocation().add(0.5, 0.5, 0.5));
                        startGPSWaypointTask(player, nearest);
                        playConfigSound(player, "sort", Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.8f, 1.2f);
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("deposit")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.deposit") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        if (args.length > 1 && args[1].equalsIgnoreCase("vault")) {
                            org.bukkit.inventory.Inventory enderChest = player.getEnderChest();
                            int deposited = 0;
                            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                                org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
                                if (item == null || item.getType() == Material.AIR) continue;
                                if (enderChest.contains(item.getType())) {
                                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> remaining = enderChest.addItem(item);
                                    if (remaining.isEmpty()) {
                                        deposited += item.getAmount();
                                        player.getInventory().setItem(slot, null);
                                    } else {
                                        int dep = item.getAmount() - remaining.get(0).getAmount();
                                        if (dep > 0) {
                                            deposited += dep;
                                            player.getInventory().setItem(slot, remaining.get(0));
                                        }
                                    }
                                }
                            }
                            if (deposited > 0) {
                                player.sendMessage("§a[StoragePeek] Deposited " + deposited + " items directly into your VaultX Virtual Vault!");
                                playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                            } else {
                                player.sendMessage("§e[StoragePeek] No matching items found to deposit into your VaultX Virtual Vault.");
                            }
                            return true;
                        }

                        int radius = 16;
                        if (args.length > 1) {
                            try {
                                radius = Math.min(32, Math.max(1, Integer.parseInt(args[1])));
                            } catch (NumberFormatException ignored) {}
                        }

                        int depositedCount = handleSmartBaseDeposit(player, radius);
                        if (depositedCount > 0) {
                            player.sendMessage("§a[StoragePeek] Deposited " + depositedCount + " matching items into nearby containers!");
                            playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                        } else {
                            player.sendMessage("§e[StoragePeek] No matching container slots found nearby for items in your inventory.");
                        }
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("vault")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.vault") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        int vaultNum = 1;
                        if (args.length > 1) {
                            try { vaultNum = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
                        }
                        if (vaultXHook != null && !vaultXHook.hasVaultPermission(player, vaultNum)) {
                            player.sendMessage("§c🔒 Vault #" + vaultNum + " is locked! Purchase or unlock it via VaultX.");
                            playConfigSound(player, "sort", Sound.BLOCK_CHEST_LOCKED, 0.8f, 1.0f);
                            return true;
                        }
                        org.bukkit.inventory.Inventory vaultInv = vaultXHook != null ? vaultXHook.getPlayerEnderChestOrVault(player, vaultNum) : player.getEnderChest();
                        boolean success = fr.skynex.storagepeek.api.StoragePeekProvider.get() != null && fr.skynex.storagepeek.api.StoragePeekProvider.get().openVirtualPeekSession(player, vaultInv, "§6🏦 VaultX Vault #" + vaultNum);
                        if (success) {
                            player.sendMessage("§a[StoragePeek] Displaying 3D virtual preview for VaultX Vault #" + vaultNum + "!");
                            playConfigSound(player, "sort", Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
                        }
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("history")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.history") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
                        if (targetBlock == null || (!getHookManager().isCustomContainer(targetBlock) && !getRaycastTask().getAllowedBlocks().contains(targetBlock.getType()))) {
                            player.sendMessage("§cYou must be looking at a valid container block within 5 blocks!");
                            return true;
                        }
                        java.util.List<fr.skynex.storagepeek.manager.ContainerHistoryManager.AccessLog> logs = containerHistoryManager.getLogs(targetBlock.getLocation());
                        if (logs.isEmpty()) {
                            player.sendMessage(messageManager.getMessage("history-empty"));
                            return true;
                        }
                        StringBuilder sb = new StringBuilder("§6§l📜 CONTAINER ACCESS HISTORY\n");
                        long now = System.currentTimeMillis();
                        for (fr.skynex.storagepeek.manager.ContainerHistoryManager.AccessLog log : logs) {
                            long secAgo = Math.max(1, (now - log.timestamp()) / 1000);
                            String agoStr = secAgo < 60 ? secAgo + "s ago" : (secAgo / 60) + "m ago";
                            sb.append("§7• §e").append(log.playerName()).append(" §7- ").append(log.action()).append(" §8(").append(agoStr).append(")\n");
                        }
                        final String historyText = sb.toString().trim();

                        Location loc = targetBlock.getLocation().add(0.5, 1.35, 0.5);
                        org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc, org.bukkit.entity.TextDisplay.class, ent -> {
                            tagDisplayEntity(ent);
                            ent.setVisibleByDefault(false);
                            ent.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                            ent.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                            ent.setDefaultBackground(true);
                            ent.setBackgroundColor(org.bukkit.Color.fromARGB(200, 20, 20, 30));
                            ent.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
                            ent.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(historyText));
                            org.bukkit.util.Transformation t = ent.getTransformation();
                            t.getScale().set(0.7f, 0.7f, 0.7f);
                            ent.setTransformation(t);
                        });
                        player.showEntity(this, textDisplay);
                        playConfigSound(player, "sort", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
                        player.sendMessage("§a[StoragePeek] Displaying 3D audit hologram above container!");

                        fr.skynex.storagepeek.util.FoliaScheduler.runLater(this, player, () -> {
                            if (textDisplay.isValid()) {
                                textDisplay.remove();
                            }
                        }, 200L);
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("search")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.search") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        PeekSession session = activeSessions.get(player.getUniqueId());
                        if (session == null) {
                            player.sendMessage("§cYou must be looking at a container to use search!");
                            return true;
                        }
                        if (args.length < 2 || args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("clear")) {
                            session.setSearchQuery(null);
                            player.sendMessage(messageManager.getMessage("search-cleared"));
                            playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                        } else {
                            String query = args[1].trim();
                            session.setSearchQuery(query);
                            player.sendMessage(messageManager.getMessage("search-updated").replace("{query}", query));
                            playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f);
                        }
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("page")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.page") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        PeekSession session = activeSessions.get(player.getUniqueId());
                        if (session == null) {
                            player.sendMessage("§cYou must be looking at a container to change pages!");
                            return true;
                        }
                        int targetPage = session.getCurrentPage();
                        if (args.length > 1) {
                            if (args[1].equalsIgnoreCase("next")) {
                                targetPage++;
                            } else if (args[1].equalsIgnoreCase("prev") || args[1].equalsIgnoreCase("previous")) {
                                targetPage = Math.max(0, targetPage - 1);
                            } else {
                                try {
                                    targetPage = Math.max(0, Integer.parseInt(args[1]) - 1);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        session.setCurrentPage(targetPage);
                        player.sendMessage(messageManager.getMessage("page-updated").replace("{page}", String.valueOf(targetPage + 1)));
                        playConfigSound(player, "sort", Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.5f, 1.2f);
                        return true;
                    } else if (args.length > 0 && (args[0].equalsIgnoreCase("label") || args[0].equalsIgnoreCase("unlabel"))) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.label") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
                        if (targetBlock == null || (!getHookManager().isCustomContainer(targetBlock) && !getRaycastTask().getAllowedBlocks().contains(targetBlock.getType()))) {
                            player.sendMessage("§cYou must be looking at a valid container block within 5 blocks!");
                            return true;
                        }

                        if (!(targetBlock.getState() instanceof org.bukkit.block.TileState tileState)) {
                            player.sendMessage("§cThis block type cannot store persistent labels.");
                            return true;
                        }

                        NamespacedKey labelKey = new NamespacedKey(this, "custom_label");
                        if (args[0].equalsIgnoreCase("unlabel") || args.length == 1) {
                            tileState.getPersistentDataContainer().remove(labelKey);
                            tileState.update();
                            player.sendMessage("§a[StoragePeek] Removed 3D label from container!");
                            playConfigSound(player, "sort", Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.2f);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 1; i < args.length; i++) {
                                sb.append(args[i]).append(" ");
                            }
                            String labelText = sb.toString().trim().replace("&", "§");
                            tileState.getPersistentDataContainer().set(labelKey, org.bukkit.persistence.PersistentDataType.STRING, labelText);
                            tileState.update();
                            player.sendMessage("§a[StoragePeek] Set 3D container label to: §f" + labelText);
                            playConfigSound(player, "sort", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
                        }
                        return true;
                    } else if (args.length > 1 && args[0].equalsIgnoreCase("createtheme")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.createtheme") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        String themeName = args[1].toLowerCase().trim();
                        org.bukkit.inventory.ItemStack mainHand = player.getInventory().getItemInMainHand();
                        Material bgMat = (mainHand != null && mainHand.getType() != Material.AIR) ? mainHand.getType() : Material.BLACK_STAINED_GLASS;

                        getConfig().set("themes.custom." + themeName + ".background-material", bgMat.name());
                        getConfig().set("themes.custom." + themeName + ".particle-type", "END_ROD");
                        getConfig().set("themes.custom." + themeName + ".glow-color", "255,215,0");
                        saveConfig();
                        loadConfigurationCache();

                        player.sendMessage("§a[StoragePeek] Created custom 3D theme '§e" + themeName + "§a' using background block §f" + bgMat.name() + "§a!");
                        playConfigSound(player, "sort", Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 1.2f);
                        return true;
                    } else if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(messageManager.getMessage("only-players"));
                            return true;
                        }
                        if (!player.hasPermission("storagepeek.stats") && !player.hasPermission("storagepeek.admin")) {
                            player.sendMessage(messageManager.getMessage("no-permission"));
                            return true;
                        }
                        int radius = 32;
                        if (args.length > 1) {
                            try {
                                radius = Math.min(64, Math.max(1, Integer.parseInt(args[1])));
                            } catch (NumberFormatException ignored) {}
                        }

                        displayBaseStatsHologram(player, radius);
                        return true;
                    }
                    sender.sendMessage(messageManager.getMessage("usage-reload"));
                    sender.sendMessage(messageManager.getMessage("usage-toggle"));
                    sender.sendMessage(messageManager.getMessage("usage-themes"));
                    sender.sendMessage(messageManager.getMessage("usage-filter"));
                    sender.sendMessage(messageManager.getMessage("usage-history"));
                    sender.sendMessage(messageManager.getMessage("usage-search"));
                    sender.sendMessage(messageManager.getMessage("usage-page"));
                    sender.sendMessage("§e/storagepeek label <text> §7- Attach persistent 3D label to targeted container.");
                    sender.sendMessage("§e/storagepeek createtheme <name> §7- Create new 3D theme using held block.");
                    sender.sendMessage("§e/storagepeek stats [radius] §7- Display 3D base storage statistics dashboard.");
                    sender.sendMessage("§e/storagepeek find <item> §7- Point compass arrow to nearby chest containing item.");
                    sender.sendMessage("§e/storagepeek deposit [radius] §7- Auto-deposit matching items into nearby chests.");
                    sender.sendMessage("§e/storagepeek purge §7- Purge orphaned display entities.");
                    return true;
                });

                spCommand.setTabCompleter((sender, command, alias, args) -> {
                    if (args.length == 1) {
                        List<String> subCommands = Arrays.asList("reload", "toggle", "themes", "theme", "filter", "vault", "dashboard", "history", "search", "page", "label", "unlabel", "createtheme", "stats", "find", "deposit", "purge");
                        return subCommands.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("deposit")) {
                        List<String> depositOptions = Arrays.asList("vault", "16", "32");
                        return depositOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("theme")) {
                        List<String> validThemes = Arrays.asList("default", "ender", "rich", "aqua", "nether", "neon", "cyberpunk", "rainbow");
                        return validThemes.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("filter")) {
                        List<String> validFilters = Arrays.asList("ALL", "RESOURCES", "FOOD", "EQUIPMENT", "rarity");
                        return validFilters.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                    }
                    if (args.length == 3 && args[0].equalsIgnoreCase("filter") && args[1].equalsIgnoreCase("rarity")) {
                        List<String> rarities = Arrays.asList("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON", "RESET");
                        return rarities.stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).toList();
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("search")) {
                        List<String> searchOptions = Arrays.asList("reset", "clear");
                        return searchOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("page")) {
                        List<String> pageOptions = Arrays.asList("next", "prev", "1", "2");
                        return pageOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                    }
                    return Collections.emptyList();
                });
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
        if (raycastTask != null) {
            raycastTask.cleanupAllCompassArrows();
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

    public fr.skynex.storagepeek.hook.LootGlowHook getLootGlowHook() {
        return lootGlowHook;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public fr.skynex.storagepeek.manager.ContainerHistoryManager getContainerHistoryManager() {
        return containerHistoryManager;
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
        playConfigSoundAt(player, player != null ? player.getLocation() : null, soundPath, defaultSound, defaultVolume, defaultPitch);
    }

    public void playConfigSoundAt(Player player, Location loc, String soundPath, Sound defaultSound, float defaultVolume, float defaultPitch) {
        if (player == null || !player.isOnline()) return;
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
        Location soundLoc = (loc != null && loc.getWorld() != null) ? loc : player.getLocation();
        player.playSound(soundLoc, sound, (float) volume, (float) pitch);
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
        if (protectionManager != null) protectionManager.reloadHooks();
        if (hookManager != null) hookManager.reloadHooks();
        if (lootGlowHook != null) lootGlowHook.init();
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

    private List<org.bukkit.block.Block> findContainersInRadius(Location pLoc, int radius, Player player) {
        List<org.bukkit.block.Block> containers = new java.util.ArrayList<>();
        org.bukkit.World world = pLoc.getWorld();
        if (world == null) return containers;

        int minChunkX = (pLoc.getBlockX() - radius) >> 4;
        int maxChunkX = (pLoc.getBlockX() + radius) >> 4;
        int minChunkZ = (pLoc.getBlockZ() - radius) >> 4;
        int maxChunkZ = (pLoc.getBlockZ() + radius) >> 4;

        double radiusSq = (double) radius * radius;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                org.bukkit.Chunk chunk = world.getChunkAt(cx, cz);
                for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                    org.bukkit.block.Block block = state.getBlock();
                    if (block.getLocation().distanceSquared(pLoc) <= radiusSq) {
                        if (getHookManager().isCustomContainer(block) || getRaycastTask().getAllowedBlocks().contains(block.getType())) {
                            if (protectionManager.canAccess(player, block.getLocation())) {
                                containers.add(block);
                            }
                        }
                    }
                }
            }
        }
        return containers;
    }

    private int handleSmartBaseDeposit(Player player, int radius) {
        int totalDeposited = 0;
        org.bukkit.Location pLoc = player.getLocation();
        if (pLoc.getWorld() == null) return 0;

        List<org.bukkit.block.Block> containers = findContainersInRadius(pLoc, radius, player);

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;

            for (org.bukkit.block.Block containerBlock : containers) {
                org.bukkit.inventory.Inventory containerInv = getHookManager().getInventory(containerBlock, player);
                if (containerInv == null) continue;

                if (containerInv.contains(item.getType())) {
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> remaining = containerInv.addItem(item);
                    if (remaining.isEmpty()) {
                        totalDeposited += item.getAmount();
                        player.getInventory().setItem(slot, null);
                        containerBlock.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, containerBlock.getLocation().add(0.5, 1.0, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
                        if (lootGlowHook != null && lootGlowHook.isActive()) {
                            lootGlowHook.triggerMagnetAbsorptionEffect(player.getLocation(), containerBlock.getLocation().add(0.5, 0.5, 0.5));
                        }
                        break;
                    } else {
                        int deposited = item.getAmount() - remaining.get(0).getAmount();
                        if (deposited > 0) {
                            totalDeposited += deposited;
                            player.getInventory().setItem(slot, remaining.get(0));
                            containerBlock.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, containerBlock.getLocation().add(0.5, 1.0, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
                            if (lootGlowHook != null && lootGlowHook.isActive()) {
                                lootGlowHook.triggerMagnetAbsorptionEffect(player.getLocation(), containerBlock.getLocation().add(0.5, 0.5, 0.5));
                            }
                        }
                    }
                }
            }
        }
        return totalDeposited;
    }

    private void displayBaseStatsHologram(Player player, int radius) {
        Location pLoc = player.getLocation();
        if (pLoc.getWorld() == null) return;

        int totalChests = 0;
        int totalSlotsUsed = 0;
        int totalSlotsCapacity = 0;
        int totalItemCount = 0;
        double totalEcoValue = 0.0;
        Material mostValuableMaterial = Material.AIR;
        double highestItemVal = 0.0;

        fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
            (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();

        List<org.bukkit.block.Block> containers = findContainersInRadius(pLoc, radius, player);

        for (org.bukkit.block.Block block : containers) {
            org.bukkit.inventory.Inventory inv = getHookManager().getInventory(block, player);
            if (inv != null) {
                totalChests++;
                totalSlotsCapacity += inv.getSize();
                double chestValue = apiImpl.getContainerTotalValue(block, player);
                totalEcoValue += chestValue;

                for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        totalSlotsUsed++;
                        totalItemCount += item.getAmount();
                        double val = apiImpl.getItemValue(item);
                        if (val > highestItemVal) {
                            highestItemVal = val;
                            mostValuableMaterial = item.getType();
                        }
                    }
                }
            }
        }

        int fillPercent = (totalSlotsCapacity > 0) ? (totalSlotsUsed * 100 / totalSlotsCapacity) : 0;
        String mostValuableName = mostValuableMaterial == Material.AIR ? "None" : mostValuableMaterial.name();

        String statsText = String.format(
            "§6§l📊 BASE STORAGE STATISTICS (Radius: %dm)\n" +
            "§7• 📦 Total Containers: §e%d\n" +
            "§7• 💎 Total Items Stored: §a%d §7(%d%% Capacity)\n" +
            "§7• 👑 Top Material: §b%s\n" +
            "§7• 🪙 Base Total Economic Value: §a$%.2f",
            radius, totalChests, totalItemCount, fillPercent, mostValuableName, totalEcoValue
        );

        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2.2));
        org.bukkit.entity.TextDisplay statsHolo = spawnLoc.getWorld().spawn(spawnLoc, org.bukkit.entity.TextDisplay.class, ent -> {
            tagDisplayEntity(ent);
            ent.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            ent.setDefaultBackground(true);
            ent.setBackgroundColor(org.bukkit.Color.fromARGB(200, 15, 15, 25));
            ent.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(statsText));
            ent.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        });

        player.showEntity(this, statsHolo);
        spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, spawnLoc, 20, 0.5, 0.5, 0.5, 0.05);
        playConfigSound(player, "sort", Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.8f, 1.2f);

        fr.skynex.storagepeek.util.FoliaScheduler.runLater(this, player, () -> {
            if (statsHolo.isValid()) {
                statsHolo.remove();
            }
        }, 300L); // 15 seconds
    }

    public void startGPSWaypointTask(Player player, org.bukkit.block.Block targetBlock) {
        if (player == null || targetBlock == null || targetBlock.getWorld() == null) return;
        Location targetLoc = targetBlock.getLocation().add(0.5, 1.5, 0.5);
        
        org.bukkit.entity.TextDisplay waypoint = targetLoc.getWorld().spawn(targetLoc, org.bukkit.entity.TextDisplay.class, ent -> {
            tagDisplayEntity(ent);
            ent.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§e📍 [GPS TARGET CHEST]\n§f" + targetBlock.getType().name()));
            ent.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            ent.setBackgroundColor(org.bukkit.Color.fromARGB(180, 20, 20, 20));
        });

        fr.skynex.storagepeek.util.FoliaScheduler.RepeatingTask task = fr.skynex.storagepeek.util.FoliaScheduler.runTimer(this, player, () -> {
            if (!player.isOnline()) return;
            Location pLoc = player.getLocation().add(0, 1.0, 0);
            org.bukkit.util.Vector vec = targetLoc.toVector().subtract(pLoc.toVector());
            double length = vec.length();
            if (length < 0.8) return;
            org.bukkit.util.Vector dir = vec.normalize().multiply(0.4);
            int points = (int) (length / 0.4);

            for (int i = 0; i < Math.min(20, points); i++) {
                Location p = pLoc.clone().add(dir.clone().multiply(i));
                p.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, p, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }, 1L, 10L);

        fr.skynex.storagepeek.util.FoliaScheduler.runLater(this, player, () -> {
            task.cancel();
            if (waypoint.isValid()) waypoint.remove();
        }, 300L);
    }
}
