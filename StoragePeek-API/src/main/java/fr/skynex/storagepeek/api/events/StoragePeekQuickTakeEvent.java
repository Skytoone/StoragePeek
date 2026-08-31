package fr.skynex.storagepeek.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player takes an item from a container preview via quick-take action.
 */
public class StoragePeekQuickTakeEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final Block block;
    private final Entity entity;
    private final ItemStack item;
    private final int slot;
    private boolean cancelled = false;

    public StoragePeekQuickTakeEvent(@NotNull Player player, @Nullable Block block, @Nullable Entity entity, @NotNull ItemStack item, int slot) {
        this.player = player;
        this.block = block;
        this.entity = entity;
        this.item = item;
        this.slot = slot;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Block getBlock() {
        return block;
    }

    @Nullable
    public Entity getEntity() {
        return entity;
    }

    @NotNull
    public ItemStack getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
