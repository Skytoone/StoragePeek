package fr.skynex.storagepeek.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a StoragePeek preview session for a player closes.
 */
public class StoragePeekCloseEvent extends StoragePeekEvent {

    private final Player player;
    private final Block targetBlock;

    public StoragePeekCloseEvent(@NotNull Player player, @Nullable Block targetBlock) {
        this.player = player;
        this.targetBlock = targetBlock;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Block getTargetBlock() {
        return targetBlock;
    }
}
