package fr.skynex.storagepeek.api.transform;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for evaluating custom 3D display transformations for items in previews.
 */
@FunctionalInterface
public interface CustomDisplayTransform {

    /**
     * Evaluates custom 3D scale, rotation, offset, and glow color for an item.
     *
     * @param player Target viewing player
     * @param item Target ItemStack
     * @param slot Slot index
     * @return Custom DisplayTransform or null to use default transform
     */
    @Nullable
    DisplayTransform evaluate(@NotNull Player player, @NotNull ItemStack item, int slot);
}
