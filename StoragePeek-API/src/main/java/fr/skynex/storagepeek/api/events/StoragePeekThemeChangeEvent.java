package fr.skynex.storagepeek.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player changes their active StoragePeek cosmetic theme.
 */
public class StoragePeekThemeChangeEvent extends StoragePeekEvent implements Cancellable {

    private final Player player;
    private final String oldTheme;
    private String newTheme;
    private boolean cancelled = false;

    public StoragePeekThemeChangeEvent(@NotNull Player player, @NotNull String oldTheme, @NotNull String newTheme) {
        this.player = player;
        this.oldTheme = oldTheme;
        this.newTheme = newTheme;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getOldTheme() {
        return oldTheme;
    }

    @NotNull
    public String getNewTheme() {
        return newTheme;
    }

    public void setNewTheme(@NotNull String newTheme) {
        this.newTheme = newTheme;
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
