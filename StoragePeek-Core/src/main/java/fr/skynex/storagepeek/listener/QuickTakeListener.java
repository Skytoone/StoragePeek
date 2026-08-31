package fr.skynex.storagepeek.listener;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class QuickTakeListener implements Listener {

    private final StoragePeek plugin;

    public QuickTakeListener(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND)
            return;
        Player player = event.getPlayer();
        if (!player.isSneaking())
            return;

        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null)
            return;
        if (!session.isValid()) {
            plugin.getActiveSessions().remove(player.getUniqueId());
            session.cleanup(true);
            return;
        }
        if (!session.isFrozen())
            return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            boolean isDeposit = plugin.getLeftClickAction().equals("DEPOSIT");
            handleQuickAction(player, session, event, false, isDeposit);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            boolean isDeposit = plugin.getRightClickAction().equals("DEPOSIT");
            handleQuickAction(player, session, event, true, isDeposit);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND)
            return;
        // PlayerInteractEntityEvent is inherently a Right Click.
        handleEntityClick(event.getPlayer(), event.getRightClicked(), event, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        // Damage event is a Left Click.
        if (event.getDamager() instanceof Player player) {
            handleEntityClick(player, event.getEntity(), event, false);
        }
    }

    private void handleEntityClick(Player player, org.bukkit.entity.Entity target, org.bukkit.event.Cancellable event, boolean isRightClick) {
        if (!player.isSneaking())
            return;

        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null)
            return;
        if (!session.isValid()) {
            plugin.getActiveSessions().remove(player.getUniqueId());
            session.cleanup(true);
            return;
        }
        if (!session.isFrozen())
            return;

        if (target.equals(session.getInteractionEntity())) {
            boolean isDeposit = isRightClick ? plugin.getRightClickAction().equals("DEPOSIT") : plugin.getLeftClickAction().equals("DEPOSIT");
            handleQuickAction(player, session, event, isRightClick, isDeposit);
        }
    }

    private void handleQuickAction(Player player, PeekSession session, org.bukkit.event.Cancellable event, boolean isRightClickActual, boolean isDepositAction) {
        int slot = session.getTargetSlot();
        if (slot == -1) {
            if (isRightClickActual) {
                event.setCancelled(true);
                handleSmartDeposit(player, session);
            }
            return;
        }

        event.setCancelled(true);

        Inventory inv = session.getInventory();
        if (inv == null)
            return;

        if (isDepositAction) {
            handleQuickDeposit(player, inv, slot);
        } else {
            handleQuickTake(player, inv, slot);
        }

        if (session.getBlock() != null) {
            plugin.getContainerHistoryManager().recordAccess(session.getBlock().getLocation(), player.getName(), "Quick action on slot " + (slot + 1));
        }

        session.saveHandInventory();
        session.update(true); // Sync view immediately
    }

    private void handleQuickDeposit(Player player, Inventory inv, int slot) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR)
            return;

        ItemStack chestItem = inv.getItem(slot);
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());

        // CASE 1: Targeted slot is empty. Move entire stack into it.
        if (chestItem == null || chestItem.getType() == Material.AIR) {
            fr.skynex.storagepeek.api.events.StoragePeekQuickDepositEvent depositEvent =
                new fr.skynex.storagepeek.api.events.StoragePeekQuickDepositEvent(player, session != null ? session.getBlock() : null, session != null ? session.getEntity() : null, hand, slot);
            org.bukkit.Bukkit.getPluginManager().callEvent(depositEvent);
            if (depositEvent.isCancelled()) return;

            inv.setItem(slot, hand.clone());
            player.getInventory().setItemInMainHand(null);
            plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
            return;
        }

        // CASE 2: Items stack. Fill it up.
        if (hand.isSimilar(chestItem)) {
            fr.skynex.storagepeek.api.events.StoragePeekQuickDepositEvent depositEvent =
                new fr.skynex.storagepeek.api.events.StoragePeekQuickDepositEvent(player, session != null ? session.getBlock() : null, session != null ? session.getEntity() : null, hand, slot);
            org.bukkit.Bukkit.getPluginManager().callEvent(depositEvent);
            if (depositEvent.isCancelled()) return;

            int max = chestItem.getType().getMaxStackSize();
            int current = chestItem.getAmount();
            if (current >= max) return; // Slot is full

            int adding = Math.min(hand.getAmount(), max - current);
            chestItem.setAmount(current + adding);
            inv.setItem(slot, chestItem);

            if (hand.getAmount() > adding) {
                hand.setAmount(hand.getAmount() - adding);
                player.getInventory().setItemInMainHand(hand);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
        }
        // CASE 3: Different items. Let's swap them! Extremely handy UX!
        else {
            fr.skynex.storagepeek.api.events.StoragePeekSwapItemEvent swapEvent =
                new fr.skynex.storagepeek.api.events.StoragePeekSwapItemEvent(player, session != null ? session.getBlock() : null, session != null ? session.getEntity() : null, hand, chestItem, slot);
            org.bukkit.Bukkit.getPluginManager().callEvent(swapEvent);
            if (swapEvent.isCancelled()) return;

            ItemStack toChest = hand.clone();
            ItemStack toHand = chestItem.clone();

            inv.setItem(slot, toChest);
            player.getInventory().setItemInMainHand(toHand);
            plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
        }
    }

    private void handleQuickTake(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() == Material.AIR)
            return;

        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        fr.skynex.storagepeek.api.events.StoragePeekQuickTakeEvent quickTakeEvent = 
            new fr.skynex.storagepeek.api.events.StoragePeekQuickTakeEvent(player, session != null ? session.getBlock() : null, session != null ? session.getEntity() : null, item, slot);
        org.bukkit.Bukkit.getPluginManager().callEvent(quickTakeEvent);
        if (quickTakeEvent.isCancelled()) {
            return;
        }

        Map<Integer, ItemStack> leftOver = player.getInventory().addItem(item.clone());

        if (leftOver.isEmpty()) {
            inv.setItem(slot, null);
            plugin.playConfigSound(player, "take", Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
        } else {
            ItemStack leftoverStack = leftOver.get(0);
            if (leftoverStack != null) {
                int leftoverAmount = leftoverStack.getAmount();
                if (leftoverAmount < item.getAmount()) {
                    ItemStack remaining = item.clone();
                    remaining.setAmount(leftoverAmount);
                    inv.setItem(slot, remaining);
                    plugin.playConfigSound(player, "take", Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!session.isValid()) {
            plugin.getActiveSessions().remove(player.getUniqueId());
            session.cleanup(true);
            return;
        }
        if (!session.isFrozen()) {
            return;
        }

        Inventory inv = session.getInventory();
        if (inv == null) {
            return;
        }

        event.setCancelled(true);

        int targetSlot = session.getTargetSlot();
        if (targetSlot != -1) {
            ItemStack item = inv.getItem(targetSlot);
            if (item != null && item.getType() != Material.AIR) {
                session.toggleFilter(item.getType());
                plugin.playConfigSound(player, "hover", Sound.BLOCK_LEVER_CLICK, 0.2f, 1.5f);
                session.update(true);
                return;
            }
        }

        session.clearFilter();

        org.bukkit.event.inventory.InventoryType type = inv.getType();
        if (type == org.bukkit.event.inventory.InventoryType.CHEST ||
            type == org.bukkit.event.inventory.InventoryType.BARREL ||
            type == org.bukkit.event.inventory.InventoryType.SHULKER_BOX ||
            type == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {

            sortInventory(inv);
            session.triggerSortAnimation();
            plugin.playConfigSound(player, "sort", Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.5f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getMessage("sorted-success"));
        }
        session.saveHandInventory();
        session.update(true); // Sync view immediately
    }

    private void sortInventory(Inventory inv) {
        ItemStack[] contents = inv.getContents();
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        // Merge similar items
        java.util.List<ItemStack> merged = new java.util.ArrayList<>();
        for (ItemStack item : items) {
            for (ItemStack existing : merged) {
                if (existing.isSimilar(item)) {
                    int maxStack = existing.getType().getMaxStackSize();
                    int currentAmount = existing.getAmount();
                    if (currentAmount < maxStack) {
                        int add = Math.min(item.getAmount(), maxStack - currentAmount);
                        existing.setAmount(currentAmount + add);
                        item.setAmount(item.getAmount() - add);
                        if (item.getAmount() <= 0) {
                            break;
                        }
                    }
                }
            }
            if (item.getAmount() > 0) {
                merged.add(item);
            }
        }

        // Sort alphabetically by material name, then by amount descending
        merged.sort((a, b) -> {
            int comp = a.getType().name().compareTo(b.getType().name());
            if (comp != 0) return comp;
            return Integer.compare(b.getAmount(), a.getAmount());
        });

        // Clear and fill
        inv.clear();
        for (int i = 0; i < merged.size(); i++) {
            inv.setItem(i, merged.get(i));
        }
    }

    private void handleSmartDeposit(Player player, PeekSession session) {
        Inventory containerInv = session.getInventory();
        if (containerInv == null) return;

        Inventory playerInv = player.getInventory();
        boolean depositedAny = false;

        // Get all types already present in the container
        java.util.Set<Material> existingTypes = new java.util.HashSet<>();
        for (ItemStack item : containerInv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                existingTypes.add(item.getType());
            }
        }

        if (existingTypes.isEmpty()) {
            return; // Container is empty, nothing to match
        }

        // Loop through player's main inventory (slots 0 to 35)
        for (int i = 0; i < 36; i++) {
            ItemStack pItem = playerInv.getItem(i);
            if (pItem == null || pItem.getType() == Material.AIR) continue;

            if (existingTypes.contains(pItem.getType())) {
                int originalAmount = pItem.getAmount();
                
                // First try to merge with existing similar item stacks in container
                int remaining = depositIntoExisting(containerInv, pItem);
                if (remaining < originalAmount) {
                    depositedAny = true;
                    if (remaining <= 0) {
                        playerInv.setItem(i, null);
                        continue;
                    } else {
                        pItem.setAmount(remaining);
                    }
                }

                // If still remaining, try to put into first empty slot of the container
                if (remaining > 0) {
                    int firstEmpty = containerInv.firstEmpty();
                    if (firstEmpty != -1) {
                        containerInv.setItem(firstEmpty, pItem.clone());
                        playerInv.setItem(i, null);
                        depositedAny = true;
                    }
                }
            }
        }

        if (depositedAny) {
            plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
            player.sendMessage(plugin.getMessageManager().getMessage("smart-deposit-success"));
            session.saveHandInventory();
            session.update(true);
        }
    }

    private int depositIntoExisting(Inventory inv, ItemStack toAdd) {
        int amount = toAdd.getAmount();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.isSimilar(toAdd)) {
                int max = item.getType().getMaxStackSize();
                int current = item.getAmount();
                if (current < max) {
                    int add = Math.min(amount, max - current);
                    item.setAmount(current + add);
                    inv.setItem(i, item);
                    amount -= add;
                    if (amount <= 0) {
                        break;
                    }
                }
            }
        }
        return amount;
    }

    private void spawnItemTransferTrail(Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null) return;
        org.bukkit.World world = from.getWorld();
        org.bukkit.util.Vector vec = to.toVector().subtract(from.toVector());
        double length = vec.length();
        if (length < 0.1) return;
        org.bukkit.util.Vector step = vec.clone().normalize().multiply(0.3);
        int points = (int) (length / 0.3);

        for (int i = 0; i < Math.min(20, points); i++) {
            Location pLoc = from.clone().add(step.clone().multiply(i));
            world.spawnParticle(org.bukkit.Particle.CRIT, pLoc, 2, 0.05, 0.05, 0.05, 0.02);
        }
    }
}
