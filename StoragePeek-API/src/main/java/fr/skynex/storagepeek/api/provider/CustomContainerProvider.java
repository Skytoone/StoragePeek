package fr.skynex.storagepeek.api.provider;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface allowing third-party plugins to register custom block containers or entity furniture for StoragePeek preview rendering.
 */
public interface CustomContainerProvider {

    /**
     * Checks if the given block is a custom container handled by this provider.
     *
     * @param block Target block
     * @return True if custom container
     */
    boolean isCustomContainer(@NotNull Block block);

    /**
     * Retrieves the custom inventory associated with a block container.
     *
     * @param block Target block
     * @param player Viewing player
     * @return Target Inventory or null if unavailable
     */
    @Nullable
    Inventory getContainerInventory(@NotNull Block block, @NotNull Player player);

    /**
     * Checks if the given entity is a custom furniture container handled by this provider.
     *
     * @param entity Target entity
     * @return True if custom furniture
     */
    default boolean isCustomFurniture(@NotNull Entity entity) {
        return false;
    }

    /**
     * Retrieves the custom inventory associated with a furniture entity.
     *
     * @param entity Target entity
     * @param player Viewing player
     * @return Target Inventory or null if unavailable
     */
    @Nullable
    default Inventory getFurnitureInventory(@NotNull Entity entity, @NotNull Player player) {
        return null;
    }
}
