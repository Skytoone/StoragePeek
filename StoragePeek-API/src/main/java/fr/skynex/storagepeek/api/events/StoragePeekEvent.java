package fr.skynex.storagepeek.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base abstract Bukkit event class for all StoragePeek events.
 */
public abstract class StoragePeekEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public StoragePeekEvent() {
        super();
    }

    public StoragePeekEvent(boolean isAsync) {
        super(isAsync);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
