package fr.skynex.storagepeek.api;

import fr.skynex.storagepeek.api.provider.CustomContainerProvider;
import fr.skynex.storagepeek.api.theme.CustomTheme;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Official StoragePeek API interface for controlling storage previews, sessions, custom containers, custom themes, and player preferences.
 */
public interface StoragePeekAPI {

    /**
     * Checks if a player currently has an active StoragePeek preview session.
     *
     * @param player Target player
     * @return True if an active preview session is open
     */
    boolean isSessionActive(@NotNull Player player);

    /**
     * Retrieves the target block of the player's active StoragePeek session, if any.
     *
     * @param player Target player
     * @return Target container block or null if no session active
     */
    @Nullable
    Block getActiveSessionBlock(@NotNull Player player);

    /**
     * Checks if StoragePeek previews are disabled for a player.
     *
     * @param player Target player
     * @return True if disabled
     */
    boolean isStoragePeekDisabled(@NotNull Player player);

    /**
     * Sets whether StoragePeek previews are disabled for a player.
     *
     * @param player Target player
     * @param disabled True to disable previews, false to enable
     */
    void setStoragePeekDisabled(@NotNull Player player, boolean disabled);

    /**
     * Gets all player UUIDs who currently have StoragePeek disabled.
     *
     * @return Immutable set of disabled player UUIDs
     */
    @NotNull
    Set<UUID> getDisabledPlayerUUIDs();

    /**
     * Manually opens a StoragePeek preview session for a player on a target block.
     *
     * @param player Target player
     * @param block Target container block
     * @return True if session was successfully opened
     */
    boolean openPeekSession(@NotNull Player player, @NotNull Block block);

    /**
     * Closes any active StoragePeek preview session for a player.
     *
     * @param player Target player
     */
    void closePeekSession(@NotNull Player player);

    /**
     * Registers a custom container provider for external block containers or furniture.
     *
     * @param provider Custom container provider implementation
     */
    void registerContainerProvider(@NotNull CustomContainerProvider provider);

    /**
     * Unregisters a previously registered custom container provider.
     *
     * @param provider Custom container provider implementation
     */
    void unregisterContainerProvider(@NotNull CustomContainerProvider provider);

    /**
     * Registers a custom cosmetic theme for preview displays.
     *
     * @param theme Custom theme definition
     */
    void registerCustomTheme(@NotNull CustomTheme theme);

    /**
     * Unregisters a custom cosmetic theme by its ID.
     *
     * @param themeId Theme ID string
     */
    void unregisterCustomTheme(@NotNull String themeId);

    /**
     * Freezes or unfreezes a player's active preview session display.
     *
     * @param player Target player
     * @param frozen True to freeze display orientation, false to unfreeze
     * @return True if player had an active session and state was set
     */
    boolean setSessionFrozen(@NotNull Player player, boolean frozen);

    /**
     * Checks if a player's preview session is currently frozen.
     *
     * @param player Target player
     * @return True if frozen
     */
    boolean isSessionFrozen(@NotNull Player player);

    /**
     * Forces a refresh of the player's active preview session display entities.
     *
     * @param player Target player
     */
    void refreshSession(@NotNull Player player);

    /**
     * Sets the active item filter for a player's preview session.
     *
     * @param player Target player
     * @param filterName Name of filter (ALL, RESOURCES, FOOD, EQUIPMENT, etc.)
     * @return True if filter was successfully set
     */
    boolean setSessionFilter(@NotNull Player player, @NotNull String filterName);
}
