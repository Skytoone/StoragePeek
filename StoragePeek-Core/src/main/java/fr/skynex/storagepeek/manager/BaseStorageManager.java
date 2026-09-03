package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaseStorageManager {

    private final StoragePeek plugin;

    public BaseStorageManager(StoragePeek plugin) {
        this.plugin = plugin;
    }

    public List<Block> findContainersInRadius(Location pLoc, int radius, Player player) {
        List<Block> containers = new ArrayList<>();
        World world = pLoc.getWorld();
        if (world == null) return containers;

        int minChunkX = (pLoc.getBlockX() - radius) >> 4;
        int maxChunkX = (pLoc.getBlockX() + radius) >> 4;
        int minChunkZ = (pLoc.getBlockZ() - radius) >> 4;
        int maxChunkZ = (pLoc.getBlockZ() + radius) >> 4;

        double radiusSq = (double) radius * radius;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                Chunk chunk = world.getChunkAt(cx, cz);
                for (BlockState state : chunk.getTileEntities()) {
                    Block block = state.getBlock();
                    if (block.getLocation().distanceSquared(pLoc) <= radiusSq) {
                        if (plugin.getHookManager().isCustomContainer(block) || plugin.getRaycastTask().getAllowedBlocks().contains(block.getType())) {
                            if (plugin.getProtectionManager().canAccess(player, block.getLocation())) {
                                containers.add(block);
                            }
                        }
                    }
                }
            }
        }
        return containers;
    }

    public int handleSmartBaseDeposit(Player player, int radius) {
        int totalDeposited = 0;
        Location pLoc = player.getLocation();
        if (pLoc.getWorld() == null) return 0;

        List<Block> containers = findContainersInRadius(pLoc, radius, player);

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            for (Block containerBlock : containers) {
                Inventory containerInv = plugin.getHookManager().getInventory(containerBlock, player);
                if (containerInv == null) continue;

                if (containerInv.contains(item.getType())) {
                    HashMap<Integer, ItemStack> remaining = containerInv.addItem(item);
                    if (remaining.isEmpty()) {
                        totalDeposited += item.getAmount();
                        player.getInventory().setItem(slot, null);
                        containerBlock.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, containerBlock.getLocation().add(0.5, 1.0, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
                        if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive()) {
                            plugin.getLootGlowHook().triggerMagnetAbsorptionEffect(player.getLocation(), containerBlock.getLocation().add(0.5, 0.5, 0.5));
                        }
                        break;
                    } else {
                        int deposited = item.getAmount() - remaining.get(0).getAmount();
                        if (deposited > 0) {
                            totalDeposited += deposited;
                            player.getInventory().setItem(slot, remaining.get(0));
                            containerBlock.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, containerBlock.getLocation().add(0.5, 1.0, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
                            if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive()) {
                                plugin.getLootGlowHook().triggerMagnetAbsorptionEffect(player.getLocation(), containerBlock.getLocation().add(0.5, 0.5, 0.5));
                            }
                        }
                    }
                }
            }
        }
        return totalDeposited;
    }

    public void displayBaseStatsHologram(Player player, int radius) {
        Location pLoc = player.getLocation();
        if (pLoc.getWorld() == null) return;

        int totalChests = 0;
        int totalSlotsUsed = 0;
        int totalSlotsCapacity = 0;
        int totalItemCount = 0;
        double totalEcoValue = 0.0;
        Material mostValuableMaterial = Material.AIR;
        double highestItemVal = 0.0;

        fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
            (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();

        List<Block> containers = findContainersInRadius(pLoc, radius, player);

        for (Block block : containers) {
            Inventory inv = plugin.getHookManager().getInventory(block, player);
            if (inv != null) {
                totalChests++;
                totalSlotsCapacity += inv.getSize();
                double chestValue = apiImpl.getContainerTotalValue(block, player);
                totalEcoValue += chestValue;

                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        totalSlotsUsed++;
                        totalItemCount += item.getAmount();
                        double val = apiImpl.getItemValue(item);
                        if (val > highestItemVal) {
                            highestItemVal = val;
                            mostValuableMaterial = item.getType();
                        }
                    }
                }
            }
        }

        int fillPercent = (totalSlotsCapacity > 0) ? (totalSlotsUsed * 100 / totalSlotsCapacity) : 0;
        String mostValuableName = mostValuableMaterial == Material.AIR ? "None" : mostValuableMaterial.name();

        String statsText = String.format(
            "§6§l📊 BASE STORAGE STATISTICS (Radius: %dm)\n" +
            "§7• 📦 Total Containers: §e%d\n" +
            "§7• 💎 Total Items Stored: §a%d §7(%d%% Capacity)\n" +
            "§7• 👑 Top Material: §b%s\n" +
            "§7• 🪙 Base Total Economic Value: §a$%.2f",
            radius, totalChests, totalItemCount, fillPercent, mostValuableName, totalEcoValue
        );

        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2.2));
        TextDisplay statsHolo = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setDefaultBackground(true);
            ent.setBackgroundColor(Color.fromARGB(200, 15, 15, 25));
            ent.text(LegacyComponentSerializer.legacySection().deserialize(statsText));
            ent.setBrightness(new Display.Brightness(15, 15));
        });

        player.showEntity(plugin, statsHolo);
        spawnLoc.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 20, 0.5, 0.5, 0.5, 0.05);
        plugin.playConfigSound(player, "sort", Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.8f, 1.2f);

        FoliaScheduler.runLater(plugin, player, () -> {
            if (statsHolo.isValid()) {
                statsHolo.remove();
            }
        }, 300L); // 15 seconds
    }

    public void startGPSWaypointTask(Player player, Block targetBlock) {
        if (player == null || targetBlock == null || targetBlock.getWorld() == null) return;
        Location targetLoc = targetBlock.getLocation().add(0.5, 1.5, 0.5);

        TextDisplay waypoint = targetLoc.getWorld().spawn(targetLoc, TextDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.text(LegacyComponentSerializer.legacySection().deserialize("§e📍 [GPS TARGET CHEST]\n§f" + targetBlock.getType().name()));
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setBackgroundColor(Color.fromARGB(180, 20, 20, 20));
        });

        FoliaScheduler.RepeatingTask task = FoliaScheduler.runTimer(plugin, player, () -> {
            if (!player.isOnline()) return;
            Location pLoc = player.getLocation().add(0, 1.0, 0);
            Vector vec = targetLoc.toVector().subtract(pLoc.toVector());
            double length = vec.length();
            if (length < 0.8) return;
            Vector dir = vec.normalize().multiply(0.4);
            int points = (int) (length / 0.4);

            for (int i = 0; i < Math.min(20, points); i++) {
                Location p = pLoc.clone().add(dir.clone().multiply(i));
                p.getWorld().spawnParticle(Particle.END_ROD, p, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }, 1L, 10L);

        FoliaScheduler.runLater(plugin, player, () -> {
            task.cancel();
            if (waypoint.isValid()) waypoint.remove();
        }, 300L);
    }
}
