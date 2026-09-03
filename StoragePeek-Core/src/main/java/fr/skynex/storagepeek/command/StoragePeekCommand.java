package fr.skynex.storagepeek.command;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.manager.MessageManager;

import fr.skynex.storagepeek.session.PeekSession;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class StoragePeekCommand implements CommandExecutor, TabCompleter {

    private final StoragePeek plugin;

    public StoragePeekCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("storagepeek.reload") && !sender.hasPermission("storagepeek.admin")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadConfigurationCache();
            messageManager.reloadConfig();

            plugin.reloadRaycastTasks();

            sender.sendMessage(messageManager.getMessage("reload-success"));
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getMessage("only-players"));
                return true;
            }
            if (!player.hasPermission("storagepeek.toggle")) {
                player.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            NamespacedKey disabledKey = plugin.getDisabledKey();
            if (pdc.has(disabledKey, PersistentDataType.BYTE)) {
                pdc.remove(disabledKey);
                plugin.getDisabledPlayers().remove(player.getUniqueId());
                player.sendMessage(messageManager.getMessage("toggle-enabled"));
            } else {
                pdc.set(disabledKey, PersistentDataType.BYTE, (byte) 1);
                plugin.getDisabledPlayers().add(player.getUniqueId());
                player.sendMessage(messageManager.getMessage("toggle-disabled"));
                PeekSession session = plugin.getActiveSessions().remove(player.getUniqueId());
                if (session != null) {
                    session.cleanup(true);
                }
            }
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("themes")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getMessage("only-players"));
                return true;
            }
            if (!player.hasPermission("storagepeek.themes")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            plugin.openThemesMenu(player);
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("theme")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getMessage("only-players"));
                return true;
            }
            String wanted = args[1].toLowerCase().trim();
            List<String> validThemes = Arrays.asList("default", "ender", "rich", "aqua", "nether", "neon", "cyberpunk",
                    "rainbow");
            if (!validThemes.contains(wanted)) {
                player.sendMessage(
                        "§cInvalid theme! Choose from: default, ender, rich, aqua, nether, neon, cyberpunk, rainbow");
                return true;
            }
            if (!wanted.equals("default") && !player.hasPermission("storagepeek.theme." + wanted)) {
                player.sendMessage(messageManager.getMessage("theme-no-permission").replace("{theme}", wanted));
                return true;
            }
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            pdc.set(plugin.getThemeKey(), PersistentDataType.STRING, wanted);
            player.sendMessage(messageManager.getMessage("theme-updated").replace("{theme}", wanted));
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("filter")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getMessage("only-players"));
                return true;
            }
            if (!player.hasPermission("storagepeek.filter") && !player.hasPermission("storagepeek.admin")) {
                player.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            String wanted = args[1].toUpperCase().trim();
            if (args.length > 1 && args[1].equalsIgnoreCase("rarity")) {
                PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
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
                plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.3f);
                return true;
            }
            try {
                PeekSession.FilterType filter = PeekSession.FilterType.valueOf(wanted);
                PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
                if (session != null) {
                    session.setActiveFilter(filter);
                    player.sendMessage(
                            messageManager.getMessage("filter-updated").replace("{filter}", wanted.toLowerCase()));
                    plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                } else {
                    player.sendMessage("§cYou must be looking at a container to apply a filter.");
                }
            } catch (Exception ex) {
                player.sendMessage(
                        "§cInvalid filter type! Choose from: ALL, RESOURCES, FOOD, EQUIPMENT, or /sp filter rarity <MYTHIC|LEGENDARY|EPIC|RARE|UNCOMMON|COMMON>");
            }
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("dashboard")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messageManager.getMessage("only-players"));
                return true;
            }
            int radius = 25;
            if (args.length > 1) {
                try {
                    radius = Math.min(64, Math.max(5, Integer.parseInt(args[1])));
                } catch (NumberFormatException ignored) {
                }
            }
            if (plugin.getStorageDashboardGUI() != null)
                plugin.getStorageDashboardGUI().openDashboard(player, radius);
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("purge")) {
            if (!sender.hasPermission("storagepeek.purge") && !sender.hasPermission("storagepeek.admin")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            int purged = plugin.purgeOrphanedEntities();
            sender.sendMessage(
                    "§aPurged " + purged + " orphaned StoragePeek display entities across all loaded chunks.");
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

            List<org.bukkit.block.Block> containers = ((fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider
                    .get())
                    .findNearbyContainers(player.getLocation(), 32.0, mat);

            if (containers.isEmpty()) {
                player.sendMessage("§cNo nearby containers containing " + mat.name() + " were found within 32 blocks.");
                return true;
            }

            org.bukkit.block.Block nearest = containers.get(0);
            player.sendMessage("§aFound " + containers.size() + " container(s) with " + mat.name()
                    + "! Pointing compass arrow to nearest container.");
            plugin.getRaycastTask().setCompassTarget(player, nearest.getLocation().add(0.5, 0.5, 0.5));
            plugin.startGPSWaypointTask(player, nearest);
            plugin.playConfigSound(player, "sort", Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.8f, 1.2f);
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
                Inventory enderChest = player.getEnderChest();
                int deposited = 0;
                for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (item == null || item.getType() == Material.AIR)
                        continue;
                    if (enderChest.contains(item.getType())) {
                        java.util.HashMap<Integer, ItemStack> remaining = enderChest.addItem(item);
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
                    player.sendMessage("§a[StoragePeek] Deposited " + deposited
                            + " items directly into your VaultX Virtual Vault!");
                    plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                } else {
                    player.sendMessage(
                            "§e[StoragePeek] No matching items found to deposit into your VaultX Virtual Vault.");
                }
                return true;
            }

            int radius = 16;
            if (args.length > 1) {
                try {
                    radius = Math.min(32, Math.max(1, Integer.parseInt(args[1])));
                } catch (NumberFormatException ignored) {
                }
            }

            int depositedCount = plugin.handleSmartBaseDeposit(player, radius);
            if (depositedCount > 0) {
                player.sendMessage(
                        "§a[StoragePeek] Deposited " + depositedCount + " matching items into nearby containers!");
                plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
            } else {
                player.sendMessage(
                        "§e[StoragePeek] No matching container slots found nearby for items in your inventory.");
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
                try {
                    vaultNum = Math.max(1, Integer.parseInt(args[1]));
                } catch (NumberFormatException ignored) {
                }
            }
            if (plugin.getVaultXHook() != null && !plugin.getVaultXHook().hasVaultPermission(player, vaultNum)) {
                player.sendMessage("§c🔒 Vault #" + vaultNum + " is locked! Purchase or unlock it via VaultX.");
                plugin.playConfigSound(player, "sort", Sound.BLOCK_CHEST_LOCKED, 0.8f, 1.0f);
                return true;
            }
            Inventory vaultInv = plugin.getVaultXHook() != null
                    ? plugin.getVaultXHook().getPlayerEnderChestOrVault(player, vaultNum)
                    : player.getEnderChest();
            boolean success = fr.skynex.storagepeek.api.StoragePeekProvider.get() != null
                    && fr.skynex.storagepeek.api.StoragePeekProvider.get().openVirtualPeekSession(player, vaultInv,
                            "§6🏦 VaultX Vault #" + vaultNum);
            if (success) {
                player.sendMessage("§a[StoragePeek] Displaying 3D virtual preview for VaultX Vault #" + vaultNum + "!");
                plugin.playConfigSound(player, "sort", Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
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
            if (targetBlock == null || (!plugin.getHookManager().isCustomContainer(targetBlock)
                    && !plugin.getRaycastTask().getAllowedBlocks().contains(targetBlock.getType()))) {
                player.sendMessage("§cYou must be looking at a valid container block within 5 blocks!");
                return true;
            }
            List<fr.skynex.storagepeek.manager.ContainerHistoryManager.AccessLog> logs = plugin
                    .getContainerHistoryManager().getLogs(targetBlock.getLocation());
            if (logs.isEmpty()) {
                player.sendMessage(messageManager.getMessage("history-empty"));
                return true;
            }
            StringBuilder sb = new StringBuilder("§6§l📜 CONTAINER ACCESS HISTORY\n");
            long now = System.currentTimeMillis();
            for (fr.skynex.storagepeek.manager.ContainerHistoryManager.AccessLog log : logs) {
                long secAgo = Math.max(1, (now - log.timestamp()) / 1000);
                String agoStr = secAgo < 60 ? secAgo + "s ago" : (secAgo / 60) + "m ago";
                sb.append("§7• §e").append(log.playerName()).append(" §7- ").append(log.action()).append(" §8(")
                        .append(agoStr).append(")\n");
            }
            final String historyText = sb.toString().trim();

            org.bukkit.Location loc = targetBlock.getLocation().add(0.5, 1.35, 0.5);
            org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc, org.bukkit.entity.TextDisplay.class,
                    ent -> {
                        plugin.tagDisplayEntity(ent);
                        ent.setVisibleByDefault(false);
                        ent.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                        ent.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                        ent.setDefaultBackground(true);
                        ent.setBackgroundColor(org.bukkit.Color.fromARGB(200, 20, 20, 30));
                        ent.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
                        ent.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                .deserialize(historyText));
                        org.bukkit.util.Transformation t = ent.getTransformation();
                        t.getScale().set(0.7f, 0.7f, 0.7f);
                        ent.setTransformation(t);
                    });
            player.showEntity(plugin, textDisplay);
            plugin.playConfigSound(player, "sort", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
            player.sendMessage("§a[StoragePeek] Displaying 3D audit hologram above container!");

            fr.skynex.storagepeek.util.FoliaScheduler.runLater(plugin, player, () -> {
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
            PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
            if (session == null) {
                player.sendMessage("§cYou must be looking at a container to use search!");
                return true;
            }
            if (args.length < 2 || args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("clear")) {
                session.setSearchQuery(null);
                player.sendMessage(messageManager.getMessage("search-cleared"));
                plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            } else {
                String query = args[1].trim();
                session.setSearchQuery(query);
                player.sendMessage(messageManager.getMessage("search-updated").replace("{query}", query));
                plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f);
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
            PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
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
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            session.setCurrentPage(targetPage);
            player.sendMessage(
                    messageManager.getMessage("page-updated").replace("{page}", String.valueOf(targetPage + 1)));
            plugin.playConfigSound(player, "sort", Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.5f, 1.2f);
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
            if (targetBlock == null || (!plugin.getHookManager().isCustomContainer(targetBlock)
                    && !plugin.getRaycastTask().getAllowedBlocks().contains(targetBlock.getType()))) {
                player.sendMessage("§cYou must be looking at a valid container block within 5 blocks!");
                return true;
            }

            if (!(targetBlock.getState() instanceof org.bukkit.block.TileState tileState)) {
                player.sendMessage("§cThis block type cannot store persistent labels.");
                return true;
            }

            NamespacedKey labelKey = plugin.getLabelKey();
            if (args[0].equalsIgnoreCase("unlabel") || args.length == 1) {
                tileState.getPersistentDataContainer().remove(labelKey);
                tileState.update();
                player.sendMessage("§a[StoragePeek] Removed 3D label from container!");
                plugin.playConfigSound(player, "sort", Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.2f);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                String labelText = sb.toString().trim().replace("&", "§");
                tileState.getPersistentDataContainer().set(labelKey, PersistentDataType.STRING, labelText);
                tileState.update();
                player.sendMessage("§a[StoragePeek] Set 3D container label to: §f" + labelText);
                plugin.playConfigSound(player, "sort", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
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
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            Material bgMat = (mainHand != null && mainHand.getType() != Material.AIR) ? mainHand.getType()
                    : Material.BLACK_STAINED_GLASS;

            plugin.getConfig().set("themes.custom." + themeName + ".background-material", bgMat.name());
            plugin.getConfig().set("themes.custom." + themeName + ".particle-type", "END_ROD");
            plugin.getConfig().set("themes.custom." + themeName + ".glow-color", "255,215,0");
            plugin.saveConfig();
            plugin.loadConfigurationCache();

            player.sendMessage("§a[StoragePeek] Created custom 3D theme '§e" + themeName
                    + "§a' using background block §f" + bgMat.name() + "§a!");
            plugin.playConfigSound(player, "sort", Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 1.2f);
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
                } catch (NumberFormatException ignored) {
                }
            }

            plugin.displayBaseStatsHologram(player, radius);
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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "toggle", "themes", "theme", "filter", "vault",
                    "dashboard", "history", "search", "page", "label", "unlabel", "createtheme", "stats", "find",
                    "deposit", "purge");
            return subCommands.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("deposit")) {
            List<String> depositOptions = Arrays.asList("vault", "16", "32");
            return depositOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("theme")) {
            List<String> validThemes = Arrays.asList("default", "ender", "rich", "aqua", "nether", "neon", "cyberpunk",
                    "rainbow");
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
    }
}
