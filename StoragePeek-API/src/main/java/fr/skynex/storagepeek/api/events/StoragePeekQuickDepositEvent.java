package fr.skynex.storagepeek.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired before a player quick-deposits an item into a container slot via 3D preview.
 */
public class StoragePeekQuickDepositEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final Block block;
    private final Entity entity;
    private final ItemStack depositedItem;
    private final int slot;
    private boolean cancelled = false;

    public StoragePeekQuickDepositEvent(@NotNull Player player, @Nullable Block block, @Nullable Entity entity, @NotNull ItemStack depositedItem, int slot) {
        this.player = player;
        this.block = block;
        this.entity = entity;
        this.depositedItem = depositedItem;
        this.slot = slot;
    }

    @NotNull public Player getPlayer() { return player; }
    @Nullable public Block getBlock() { return block; }
    @Nullable public Entity getEntity() { return entity; }
    @NotNull public ItemStack getDepositedItem() { return depositedItem; }
    public int getSlot() { return slot; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
}
