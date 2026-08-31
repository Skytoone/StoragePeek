package fr.skynex.storagepeek.api.valuation;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for evaluating monetary or economic item prices.
 */
@FunctionalInterface
public interface ItemValuer {

    /**
     * Calculates the unit economic value of an item stack.
     *
     * @param item Target ItemStack
     * @return Economic value per unit (e.g. 10.0$)
     */
    double getValue(@NotNull ItemStack item);
}
