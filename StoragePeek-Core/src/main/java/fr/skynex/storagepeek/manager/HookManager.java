package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.hook.*;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.loot.Lootable;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import java.util.Random;

import java.util.ArrayList;
import java.util.List;

public class HookManager {

    private final List<ContainerHook> activeHooks = new ArrayList<>();
    private final List<fr.skynex.storagepeek.api.provider.CustomContainerProvider> customContainerProviders = new java.util.concurrent.CopyOnWriteArrayList<>();

    public HookManager() {
        registerHook("Oraxen", "fr.skynex.storagepeek.hook.OraxenHook");
        registerHook("Nexo", "fr.skynex.storagepeek.hook.NexoHook");
        registerHook("ItemsAdder", "fr.skynex.storagepeek.hook.ItemsAdderHook");
        registerHook("CraftEngine", "fr.skynex.storagepeek.hook.CraftEngineHook");
    }

    public void registerCustomContainerProvider(fr.skynex.storagepeek.api.provider.CustomContainerProvider provider) {
        if (provider != null && !customContainerProviders.contains(provider)) {
            customContainerProviders.add(provider);
        }
    }

    public void unregisterCustomContainerProvider(fr.skynex.storagepeek.api.provider.CustomContainerProvider provider) {
        if (provider != null) {
            customContainerProviders.remove(provider);
        }
    }

    private void registerHook(String pluginName, String className) {
        if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            try {
                ContainerHook hook = (ContainerHook) Class.forName(className)
                        .getDeclaredConstructor().newInstance();
                activeHooks.add(hook);
            } catch (Throwable t) {
                Bukkit.getLogger().warning("[StoragePeek] Failed to load container hook for " + pluginName + ": " + t.getMessage());
            }
        }
    }

    public boolean isCustomContainer(Block block) {
        for (fr.skynex.storagepeek.api.provider.CustomContainerProvider provider : customContainerProviders) {
            try {
                if (provider.isCustomContainer(block)) return true;
            } catch (Throwable ignored) {}
        }
        for (ContainerHook hook : activeHooks) {
            try {
                if (hook.isCustomContainer(block)) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public boolean isCustomFurniture(Entity entity) {
        for (fr.skynex.storagepeek.api.provider.CustomContainerProvider provider : customContainerProviders) {
            try {
                if (provider.isCustomFurniture(entity)) return true;
            } catch (Throwable ignored) {}
        }
        for (ContainerHook hook : activeHooks) {
            try {
                if (hook.isCustomFurniture(entity)) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public Inventory getInventory(Block block, Player player) {
        for (fr.skynex.storagepeek.api.provider.CustomContainerProvider provider : customContainerProviders) {
            try {
                if (provider.isCustomContainer(block)) {
                    Inventory inv = provider.getContainerInventory(block, player);
                    if (inv != null) return inv;
                }
            } catch (Throwable ignored) {}
        }
        for (ContainerHook hook : activeHooks) {
            try {
                Inventory inv = hook.getInventory(block);
                if (inv != null) return inv;
            } catch (Throwable ignored) {}
        }
        
        BlockState state = block.getState();
        if (state instanceof Lootable lootable) {
            try {
                LootTable table = lootable.getLootTable();
                if (table != null) {
                    if (state instanceof org.bukkit.block.Container container) {
                        AttributeInstance luckAttr = player != null ? player.getAttribute(Attribute.LUCK) : null;
                        float luckValue = luckAttr != null ? (float) luckAttr.getValue() : 0.0f;
                        
                        LootContext context = new LootContext.Builder(block.getLocation())
                                .luck(luckValue)
                                .killer(player)
                                .build();

                        // FILL THE SNAPSHOT INVENTORY INSTEAD OF THE LIVE DETACHED INVENTORY
                        table.fillInventory(container.getSnapshotInventory(), new Random(), context);
                        lootable.setLootTable(null);
                        state.update(true, false);
                        
                        // Refresh state to retrieve the newly persisted block contents
                        state = block.getState();
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (state instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        return null;
    }

    public Inventory getInventory(Entity entity, Player player) {
        for (fr.skynex.storagepeek.api.provider.CustomContainerProvider provider : customContainerProviders) {
            try {
                if (provider.isCustomFurniture(entity)) {
                    Inventory inv = provider.getFurnitureInventory(entity, player);
                    if (inv != null) return inv;
                }
            } catch (Throwable ignored) {}
        }
        for (ContainerHook hook : activeHooks) {
            try {
                Inventory inv = hook.getInventory(entity);
                if (inv != null) return inv;
            } catch (Throwable ignored) {}
        }

        if (entity instanceof Lootable lootable) {
            try {
                LootTable table = lootable.getLootTable();
                if (table != null) {
                    AttributeInstance luckAttr = player != null ? player.getAttribute(Attribute.LUCK) : null;
                    float luckValue = luckAttr != null ? (float) luckAttr.getValue() : 0.0f;

                    LootContext context = new LootContext.Builder(entity.getLocation())
                            .luck(luckValue)
                            .killer(player)
                            .build();
                    if (entity instanceof InventoryHolder holder) {
                        table.fillInventory(holder.getInventory(), new Random(), context);
                        lootable.setLootTable(null);
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (entity instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        return null;
    }
}
