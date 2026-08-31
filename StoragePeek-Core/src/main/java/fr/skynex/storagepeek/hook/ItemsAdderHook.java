package fr.skynex.storagepeek.hook;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ItemsAdderHook implements ContainerHook {
    @Override
    public boolean isCustomContainer(Block block) {
        return CustomBlock.byAlreadyPlaced(block) != null;
    }

    @Override
    public boolean isCustomFurniture(Entity entity) {
        return CustomFurniture.byAlreadySpawned(entity) != null;
    }

    @Override
    public Inventory getInventory(Block block) {
        return null;
    }

    @Override
    public Inventory getInventory(Entity entity) {
        CustomFurniture cf = CustomFurniture.byAlreadySpawned(entity);
        if (cf != null && entity instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        return null;
    }
}
