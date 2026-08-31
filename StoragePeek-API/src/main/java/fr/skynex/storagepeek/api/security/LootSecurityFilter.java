package fr.skynex.storagepeek.api.security;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Filter allowing plugins to restrict or mask item preview visibility for specific players or containers.
 */
@FunctionalInterface
public interface LootSecurityFilter {

    /**
     * Evaluates security permission for a specific item in a container.
     *
     * @param player Viewing player
     * @param block Container block or null
     * @param entity Furniture entity or null
     * @param item Target ItemStack
     * @return SecurityResult determining allow, hide, or mask behavior
     */
    @NotNull
    SecurityResult evaluate(@NotNull Player player, @Nullable Block block, @Nullable Entity entity, @NotNull ItemStack item);
}
