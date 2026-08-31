package fr.skynex.storagepeek.api.events;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when an item from a container is being rendered in 3D in front of a player.
 * Allows modifying the scale, custom display name, or glowing color of the rendered item.
 */
public class StoragePeekRenderItemEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final ItemStack itemStack;
    private final int slot;
    private boolean cancelled = false;

    private float customScaleMultiplier = 1.0f;
    private Color glowColor = null;

    public StoragePeekRenderItemEvent(@NotNull Player player, @NotNull ItemStack itemStack, int slot) {
        this.player = player;
        this.itemStack = itemStack;
        this.slot = slot;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getSlot() {
        return slot;
    }

    public float getCustomScaleMultiplier() {
        return customScaleMultiplier;
    }

    public void setCustomScaleMultiplier(float customScaleMultiplier) {
        this.customScaleMultiplier = customScaleMultiplier;
    }

    @Nullable
    public Color getGlowColor() {
        return glowColor;
    }

    public void setGlowColor(@Nullable Color glowColor) {
        this.glowColor = glowColor;
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
