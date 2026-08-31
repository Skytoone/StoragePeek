package fr.skynex.storagepeek.hook;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CraftEngineHook implements ContainerHook {

    @Override
    public boolean isCustomContainer(Block block) {
        try {
            return CraftEngineBlocks.isCustomBlock(block);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean isCustomFurniture(Entity entity) {
        try {
            return CraftEngineFurniture.isFurniture(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Inventory getInventory(Block block) {
        // CraftEngine blocks usually use vanilla InventoryHolder
        return null;
    }

    @Override
    public Inventory getInventory(Entity entity) {
        try {
            if (CraftEngineFurniture.isFurniture(entity)) {
                if (entity instanceof InventoryHolder holder) {
                    return holder.getInventory();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
