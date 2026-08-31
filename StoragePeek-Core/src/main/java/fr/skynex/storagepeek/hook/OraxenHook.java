package fr.skynex.storagepeek.hook;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;

public class OraxenHook implements ContainerHook {
    @Override
    public boolean isCustomContainer(Block block) {
        return OraxenBlocks.isOraxenBlock(block);
    }

    @Override
    public boolean isCustomFurniture(Entity entity) {
        return OraxenFurniture.isFurniture(entity);
    }

    @Override
    public Inventory getInventory(Block block) {
        return null; // Oraxen blocks usually use vanilla InventoryHolder
    }

    @Override
    public Inventory getInventory(Entity entity) {
        return null;
    }
}
