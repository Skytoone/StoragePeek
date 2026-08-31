package fr.skynex.storagepeek.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player changes their StoragePeek item filter.
 */
public class StoragePeekFilterChangeEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final String oldFilter;
    private String newFilter;
    private boolean cancelled = false;

    public StoragePeekFilterChangeEvent(@NotNull Player player, @NotNull String oldFilter, @NotNull String newFilter) {
        this.player = player;
        this.oldFilter = oldFilter;
        this.newFilter = newFilter;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getOldFilter() {
        return oldFilter;
    }

    @NotNull
    public String getNewFilter() {
        return newFilter;
    }

    public void setNewFilter(@NotNull String newFilter) {
        this.newFilter = newFilter;
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
