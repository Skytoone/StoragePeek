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
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BaseStorageManager {

    private final StoragePeek plugin;
    private final Map<Material, CachedValue> itemValueCache = new ConcurrentHashMap<>();

    private record CachedValue(double value, long timestamp) {}

    public BaseStorageManager(StoragePeek plugin) {
        this.plugin = plugin;
    }

    public Inventory findInventory(Player player, Block block, Entity entity, EquipmentSlot handSlot) {
        if (handSlot != null) {
            ItemStack item = handSlot == EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
            
            if (item != null && Tag.SHULKER_BOXES.isTagged(item.getType())) {
                if (item.getItemMeta() instanceof BlockStateMeta bsm && bsm.hasBlockState()) {
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulkerBox) {
                        return shulkerBox.getInventory();
                    }
                }
            }
            return null;
        }
        if (block != null) {
            Inventory inv = plugin.getHookManager().getInventory(block, player);
            if (inv != null) return inv;
            if (block.getType() == Material.ENDER_CHEST) return player.getEnderChest();
        } else if (entity != null) {
            Inventory inv = plugin.getHookManager().getInventory(entity, player);
            if (inv != null) return inv;
        }
        return null;
    }

    public Location resolveContainerCenter(Block block, Entity entity, EquipmentSlot handSlot, Player player) {
        if (block != null) {
            if (plugin.getHookManager().getInventory(block, player) instanceof org.bukkit.inventory.DoubleChestInventory dci) {
                org.bukkit.block.DoubleChest doubleChest = dci.getHolder();
                if (doubleChest != null) {
                    return doubleChest.getLocation();
                }
            }
            return block.getLocation().add(0.5, 0.5, 0.5);
        }
        if (entity != null) {
            Location loc = entity.getLocation();
            loc.add(0, entity.getHeight() / 2.0, 0);
            return loc;
        }
        return null;
    }

    public List<Block> findContainersInRadius(Location pLoc, int radius, Player player) {
        List<Block> containers = new ArrayList<>();
        World world = pLoc.getWorld();
        if (world == null) return containers;

        int pX = pLoc.getBlockX();
        int pY = pLoc.getBlockY();
        int pZ = pLoc.getBlockZ();

        int minChunkX = (pX - radius) >> 4;
        int maxChunkX = (pX + radius) >> 4;
        int minChunkZ = (pZ - radius) >> 4;
        int maxChunkZ = (pZ + radius) >> 4;

        double radiusSq = (double) radius * radius;
        Set<Material> allowedBlocks = plugin.getRaycastTask().getAllowedBlocks();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                Chunk chunk = world.getChunkAt(cx, cz);

                BlockState[] tileEntities = chunk.getTileEntities(false);
                for (BlockState state : tileEntities) {
                    Block block = state.getBlock();

                    int dx = block.getX() - pX;
                    int dy = block.getY() - pY;
                    int dz = block.getZ() - pZ;

                    if ((dx * dx + dy * dy + dz * dz) <= radiusSq) {
                        if (plugin.getHookManager().isCustomContainer(block) || allowedBlocks.contains(block.getType())) {
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
        if (containers.isEmpty()) return 0;

        List<ContainerTarget> targetContainers = new ArrayList<>();
        Set<Inventory> visitedInvs = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Block block : containers) {
            Inventory inv = plugin.getHookManager().getInventory(block, player);
            if (inv != null && visitedInvs.add(inv)) {
                targetContainers.add(new ContainerTarget(block, inv));
            }
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR) continue;

            for (ContainerTarget target : targetContainers) {
                Inventory containerInv = target.inventory();
                if (containerInv.contains(item.getType())) {
                    HashMap<Integer, ItemStack> remaining = containerInv.addItem(item);
                    Block containerBlock = target.block();
                    if (remaining.isEmpty()) {
                        totalDeposited += item.getAmount();
                        player.getInventory().setItem(slot, null);
                        triggerDepositEffect(player, containerBlock);
                        break;
                    } else {
                        int deposited = item.getAmount() - remaining.get(0).getAmount();
                        if (deposited > 0) {
                            totalDeposited += deposited;
                            player.getInventory().setItem(slot, remaining.get(0));
                            triggerDepositEffect(player, containerBlock);
                        }
                    }
                }
            }
        }
        return totalDeposited;
    }

    private record ContainerTarget(Block block, Inventory inventory) {}

    private void triggerDepositEffect(Player player, Block containerBlock) {
        Location effectLoc = containerBlock.getLocation().add(0.5, 1.0, 0.5);
        containerBlock.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, effectLoc, 5, 0.2, 0.2, 0.2, 0.05);
        if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive()) {
            plugin.getLootGlowHook().triggerMagnetAbsorptionEffect(player.getLocation(), containerBlock.getLocation().add(0.5, 0.5, 0.5));
        }
    }

    public double getCachedItemValue(fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0.0;
        long now = System.currentTimeMillis();
        CachedValue cached = itemValueCache.get(item.getType());
        if (cached != null && (now - cached.timestamp()) < 15000L) {
            return cached.value() * item.getAmount();
        }
        double unitValue = apiImpl.getItemValue(new ItemStack(item.getType(), 1));
        itemValueCache.put(item.getType(), new CachedValue(unitValue, now));
        return unitValue * item.getAmount();
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
        Set<Inventory> visitedInvs = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Block block : containers) {
            Inventory inv = plugin.getHookManager().getInventory(block, player);
            if (inv != null && visitedInvs.add(inv)) {
                totalChests++;
                totalSlotsCapacity += inv.getSize();

                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        totalSlotsUsed++;
                        totalItemCount += item.getAmount();
                        double val = getCachedItemValue(apiImpl, item);
                        totalEcoValue += val;
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

            World world = pLoc.getWorld();
            double startX = pLoc.getX();
            double startY = pLoc.getY();
            double startZ = pLoc.getZ();
            double dirX = dir.getX();
            double dirY = dir.getY();
            double dirZ = dir.getZ();

            int maxPoints = Math.min(20, points);
            for (int i = 0; i < maxPoints; i++) {
                world.spawnParticle(Particle.END_ROD, startX + dirX * i, startY + dirY * i, startZ + dirZ * i, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }, 1L, 10L);

        FoliaScheduler.runLater(plugin, player, () -> {
            task.cancel();
            if (waypoint.isValid()) waypoint.remove();
        }, 300L);
    }
}

