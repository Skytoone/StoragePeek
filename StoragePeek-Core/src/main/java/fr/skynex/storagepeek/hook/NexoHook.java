package fr.skynex.storagepeek.hook;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;

public class NexoHook implements ContainerHook {
    @Override
    public boolean isCustomContainer(Block block) {
        return NexoBlocks.isCustomBlock(block);
    }

    @Override
    public boolean isCustomFurniture(Entity entity) {
        return NexoFurniture.isFurniture(entity);
    }

    @Override
    public Inventory getInventory(Block block) {
        return null;
    }

    @Override
    public Inventory getInventory(Entity entity) {
        return null;
    }
}
