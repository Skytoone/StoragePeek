package fr.skynex.storagepeek.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player toggles their StoragePeek visibility mode on or off.
 */
public class StoragePeekToggleEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final boolean newState;
    private boolean cancelled = false;

    public StoragePeekToggleEvent(@NotNull Player player, boolean newState) {
        this.player = player;
        this.newState = newState;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the new state of StoragePeek previews for the player.
     *
     * @return True if StoragePeek will be enabled, false if disabled.
     */
    public boolean getNewState() {
        return newState;
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
