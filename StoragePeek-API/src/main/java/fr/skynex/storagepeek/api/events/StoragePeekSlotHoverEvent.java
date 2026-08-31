package fr.skynex.storagepeek.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player hovers over a 3D item slot in preview mode.
 */
public class StoragePeekSlotHoverEvent extends StoragePeekEvent {

    private final Player player;
    private final Block block;
    private final Entity entity;
    private final ItemStack hoveredItem;
    private final int oldSlot;
    private final int newSlot;

    public StoragePeekSlotHoverEvent(@NotNull Player player, @Nullable Block block, @Nullable Entity entity, @Nullable ItemStack hoveredItem, int oldSlot, int newSlot) {
        this.player = player;
        this.block = block;
        this.entity = entity;
        this.hoveredItem = hoveredItem;
        this.oldSlot = oldSlot;
        this.newSlot = newSlot;
    }

    @NotNull public Player getPlayer() { return player; }
    @Nullable public Block getBlock() { return block; }
    @Nullable public Entity getEntity() { return entity; }
    @Nullable public ItemStack getHoveredItem() { return hoveredItem; }
    public int getOldSlot() { return oldSlot; }
    public int getNewSlot() { return newSlot; }
}
