package fr.skynex.storagepeek.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player starts looking at a container block and a StoragePeek session is being created.
 */
public class StoragePeekOpenEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final Block targetBlock;
    private boolean cancelled = false;

    public StoragePeekOpenEvent(@NotNull Player player, @NotNull Block targetBlock) {
        this.player = player;
        this.targetBlock = targetBlock;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Block getTargetBlock() {
        return targetBlock;
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
