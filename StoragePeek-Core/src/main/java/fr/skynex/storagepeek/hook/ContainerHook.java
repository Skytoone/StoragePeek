package fr.skynex.storagepeek.hook;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;

public interface ContainerHook {
    boolean isCustomContainer(Block block);
    boolean isCustomFurniture(Entity entity);
    Inventory getInventory(Block block);
    Inventory getInventory(Entity entity);
}
