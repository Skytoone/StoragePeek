package fr.skynex.storagepeek.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player changes pages in a 3D container preview session.
 */
public class StoragePeekPageChangeEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final int oldPage;
    private int newPage;
    private boolean cancelled = false;

    public StoragePeekPageChangeEvent(@NotNull Player player, int oldPage, int newPage) {
        this.player = player;
        this.oldPage = oldPage;
        this.newPage = newPage;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public int getOldPage() {
        return oldPage;
    }

    public int getNewPage() {
        return newPage;
    }

    public void setNewPage(int newPage) {
        this.newPage = newPage;
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
