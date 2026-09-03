package fr.skynex.storagepeek.gui;

import fr.skynex.storagepeek.StoragePeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageDashboardGUI implements Listener {

    private final StoragePeek plugin;
    private final Map<Integer, Block> slotToBlock = new HashMap<>();

    public StorageDashboardGUI(StoragePeek plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private Component text(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }

    public void openDashboard(Player player, int radius) {
        Inventory inv = Bukkit.createInventory(null, 54, text("§8📦 Base Storage Dashboard"));
        slotToBlock.clear();

        List<Block> containers = findContainersInRadius(player.getLocation(), radius, player);
        int slot = 0;

        for (Block block : containers) {
            if (slot >= 45) break;

            Inventory containerInv = plugin.getHookManager().getInventory(block, player);
            int itemCount = 0;
            int totalSlots = containerInv != null ? containerInv.getSize() : 27;
            int filledSlots = 0;

            if (containerInv != null) {
                for (ItemStack is : containerInv.getContents()) {
                    if (is != null && is.getType() != Material.AIR) {
                        itemCount += is.getAmount();
                        filledSlots++;
                    }
                }
            }

            int fillPercent = (int) (((double) filledSlots / totalSlots) * 100);
            double totalValue = fr.skynex.storagepeek.api.StoragePeekProvider.get() != null ? fr.skynex.storagepeek.api.StoragePeekProvider.get().getContainerTotalValue(block, player) : 0.0;
            boolean canAccess = plugin.getProtectionManager().canAccess(player, block.getLocation());

            ItemStack icon = new ItemStack(block.getType());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(text("§6" + block.getType().name() + " §7@ (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")"));
                List<Component> lore = new ArrayList<>();
                lore.add(text("§7Fill Level: §e" + fillPercent + "% §7(" + filledSlots + "/" + totalSlots + " slots)"));
                lore.add(text("§7Total Items: §b" + itemCount));
                if (totalValue > 0) {
                    lore.add(text("§7Value ($): §a$" + String.format("%.2f", totalValue)));
                }
                lore.add(text("§7Status: " + (canAccess ? "§a🔓 Unlocked" : "§c🔒 Locked")));
                lore.add(Component.empty());
                lore.add(text("§e▶ Left-Click: §7Start GPS Particle Waypoint"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }

            inv.setItem(slot, icon);
            slotToBlock.put(slot, block);
            slot++;
        }

        // Fill empty slots with glass pane
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(text(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // VaultX Wealth Summary Item in Slot 49
        double vaultBal = plugin.getVaultXHook() != null ? plugin.getVaultXHook().getPlayerBalance(player) : 0.0;
        ItemStack vaultItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta vMeta = vaultItem.getItemMeta();
        if (vMeta != null) {
            vMeta.displayName(text("§6🏦 VaultX Wealth & Economy Summary"));
            List<Component> vLore = new ArrayList<>();
            vLore.add(text("§7Player Balance: §a$" + String.format("%.2f", vaultBal)));
            vLore.add(text("§7Virtual Vault Status: §bActive"));
            vLore.add(text("§e▶ Use /sp vault <n> to peek virtual vaults 3D"));
            vMeta.lore(vLore);
            vaultItem.setItemMeta(vMeta);
        }
        inv.setItem(49, vaultItem);

        player.openInventory(inv);
        plugin.playConfigSound(player, "hover", Sound.BLOCK_LEVER_CLICK, 0.5f, 1.2f);
    }

    private List<Block> findContainersInRadius(org.bukkit.Location pLoc, int radius, Player player) {
        List<Block> containers = new ArrayList<>();
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
                    Block block = state.getBlock();
                    if (block.getLocation().distanceSquared(pLoc) <= radiusSq) {
                        if (plugin.getHookManager().isCustomContainer(block) || plugin.getRaycastTask().getAllowedBlocks().contains(block.getType())) {
                            containers.add(block);
                        }
                    }
                }
            }
        }
        return containers;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!title.equals("§8📦 Base Storage Dashboard")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        if (slotToBlock.containsKey(slot)) {
            Block target = slotToBlock.get(slot);
            if (target != null) {
                player.closeInventory();
                plugin.startGPSWaypointTask(player, target);
                player.sendMessage(plugin.getMessageManager().getMessage("find-target-found")
                        .replace("%x%", String.valueOf(target.getX()))
                        .replace("%y%", String.valueOf(target.getY()))
                        .replace("%z%", String.valueOf(target.getZ())));
            }
        }
    }
}
