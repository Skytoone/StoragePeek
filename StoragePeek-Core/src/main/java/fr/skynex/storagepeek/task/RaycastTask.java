package fr.skynex.storagepeek.task;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RaycastTask extends BukkitRunnable {

    private final Set<Material> allowedBlocks = new HashSet<>();
    private final Set<EntityType> allowedEntities = new HashSet<>();

    private static class PlayerState {
        double x, y, z;
        float yaw, pitch;
    }

    private final Map<UUID, PlayerState> lastStates = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPermissionWarning = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Location> compassTargets = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ItemDisplay> compassArrows = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> compassCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean entitiesEnabled;

    public void setCompassTarget(Player player, Location target) {
        if (target == null) {
            compassTargets.remove(player.getUniqueId());
            cleanupCompassArrow(player.getUniqueId());
        } else {
            compassTargets.put(player.getUniqueId(), target);
        }
    }

    public Location getCompassTarget(Player player) {
        return compassTargets.get(player.getUniqueId());
    }

    public void cleanupCompassArrow(UUID uuid) {
        ItemDisplay display = compassArrows.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public void cleanupAllCompassArrows() {
        for (ItemDisplay display : compassArrows.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        compassArrows.clear();
        compassTargets.clear();
    }

    public void tickLootTrackerCompass(Player player) {
        UUID uuid = player.getUniqueId();
        Location target = compassTargets.get(uuid);
        if (target == null) return;

        if (!player.isOnline() || !player.getWorld().equals(target.getWorld()) || player.getLocation().distanceSquared(target) < 4.0) {
            setCompassTarget(player, null);
            return;
        }

        ItemDisplay arrow = compassArrows.get(uuid);
        Location headLoc = player.getEyeLocation().add(0, 0.8, 0);
        org.bukkit.util.Vector dir = target.clone().subtract(headLoc).toVector().normalize();
        Location arrowLoc = headLoc.clone();
        arrowLoc.setDirection(dir);

        if (arrow == null || !arrow.isValid()) {
            ItemDisplay newArrow = headLoc.getWorld().spawn(headLoc, ItemDisplay.class, ent -> {
                StoragePeek.getInstance().tagDisplayEntity(ent);
                ent.setItemStack(new ItemStack(Material.AMETHYST_SHARD));
                ent.setBillboard(Display.Billboard.FIXED);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setTeleportDuration(1);
                Transformation t = ent.getTransformation();
                t.getScale().set(0.35f, 0.35f, 0.35f);
                ent.setTransformation(t);
            });
            player.showEntity(StoragePeek.getInstance(), newArrow);
            compassArrows.put(uuid, newArrow);
        } else {
            arrow.teleport(arrowLoc);
        }
    }

    public RaycastTask() {
        loadConfig();
    }

    public void loadConfig() {
        StoragePeek plugin = StoragePeek.getInstance();
        allowedBlocks.clear();
        allowedEntities.clear();

        List<String> blockNames = plugin.getConfig().getStringList("containers");
        for (String name : blockNames) {
            try {
                allowedBlocks.add(Material.valueOf(name));
            } catch (Exception ignored) {
            }
        }

        entitiesEnabled = plugin.getConfig().getBoolean("enable-entities", true);
        List<String> entityNames = plugin.getConfig().getStringList("allowed-entities");
        for (String name : entityNames) {
            try {
                allowedEntities.add(EntityType.valueOf(name));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void run() {
        compassArrows.keySet().removeIf(uuid -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                org.bukkit.entity.ItemDisplay display = compassArrows.get(uuid);
                if (display != null) {
                    display.remove();
                }
                return true;
            }
            return false;
        });

        StoragePeek plugin = StoragePeek.getInstance();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            runForPlayer(player);
        }
    }

    public void runForPlayer(Player player) {
        tickLootTrackerCompass(player);
        StoragePeek plugin = StoragePeek.getInstance();
        double maxDist = plugin.getMaxDistance();
        UUID uuid = player.getUniqueId();

        // Skip and cleanup if player has toggled StoragePeek off (Fast cache lookup)
        if (plugin.getDisabledPlayers().contains(uuid)) {
            PeekSession session = plugin.getActiveSessions().remove(uuid);
            if (session != null)
                session.cleanup(true);
            return;
        }

        // Skip and cleanup if player is in a disabled world
        if (plugin.getDisabledWorldsCache().contains(player.getWorld())) {
            PeekSession session = plugin.getActiveSessions().remove(uuid);
            if (session != null)
                session.cleanup(true);
            return;
        }

        PeekSession currentSession = plugin.getActiveSessions().get(uuid);

        if (currentSession != null) {
            if (!currentSession.isValid()) {
                plugin.getActiveSessions().remove(uuid);
                currentSession.cleanup(true);
                currentSession = null;
            }
        }

        // If player has a session, we must check if they entered combat or opened an inventory
        if (currentSession != null) {
            if (isInCombat(player)) {
                PeekSession session = plugin.getActiveSessions().remove(uuid);
                if (session != null)
                    session.cleanup(true);
                return;
            }

            InventoryType openType = player.getOpenInventory().getType();
            if (openType != InventoryType.CRAFTING && openType != InventoryType.CREATIVE
                    && openType != InventoryType.PLAYER) {
                PeekSession session = plugin.getActiveSessions().remove(uuid);
                if (session != null)
                    session.cleanup(true);
                return;
            }

            // Check world / distance
            Location containerLoc = null;
            if (currentSession.getBlock() != null) {
                containerLoc = currentSession.getBlock().getLocation();
            } else if (currentSession.getEntity() != null) {
                containerLoc = currentSession.getEntity().getLocation();
            }
            if (containerLoc != null) {
                if (!containerLoc.getWorld().equals(player.getWorld())) {
                    PeekSession removed = plugin.getActiveSessions().remove(uuid);
                    if (removed != null)
                        removed.cleanup(true);
                    return;
                } else {
                    double dx = containerLoc.getX() - player.getX();
                    double dy = containerLoc.getY() - player.getY();
                    double dz = containerLoc.getZ() - player.getZ();
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > (maxDist * 2.0) * (maxDist * 2.0)) {
                        PeekSession removed = plugin.getActiveSessions().remove(uuid);
                        if (removed != null)
                            removed.cleanup(true);
                        return;
                    }
                }
            }
        }

        // CRITICAL GARBAGE COLLECTION BYPASS:
        // Query directly from net.minecraft primitives without generating transient
        // Vector/Location payloads
        double curX = player.getX();
        double curY = player.getY();
        double curZ = player.getZ();
        float curYaw = player.getYaw();
        float curPitch = player.getPitch();

        PlayerState state = lastStates.get(uuid);
        if (state == null) {
            state = new PlayerState();
            lastStates.put(uuid, state);
        }

        // Check delta magnitudes before burning CPU cycles on raytracing
        boolean posMoved = Math.abs(state.x - curX) > 0.005 || Math.abs(state.y - curY) > 0.005
                || Math.abs(state.z - curZ) > 0.005;
        boolean rotMoved = Math.abs(state.yaw - curYaw) > 0.05f || Math.abs(state.pitch - curPitch) > 0.05f;

        if (!posMoved && !rotMoved) {
            if (currentSession != null) {
                currentSession.update(false);
            }
            return;
        }

        // Commit snapshot changes to fast lookup map
        state.x = curX;
        state.y = curY;
        state.z = curZ;
        state.yaw = curYaw;
        state.pitch = curPitch;

        Location eyeLoc = player.getEyeLocation();
        org.bukkit.util.Vector dir = eyeLoc.getDirection();

        double effectiveMaxDist = plugin.getPerformanceManager() != null ? plugin.getPerformanceManager().getAdaptiveMaxRaycastDistance(player, maxDist) : maxDist;

        RayTraceResult result;
        if (entitiesEnabled && !allowedEntities.isEmpty()) {
            result = player.getWorld().rayTrace(
                    eyeLoc,
                    dir,
                    effectiveMaxDist,
                    FluidCollisionMode.NEVER,
                    true,
                    0.1,
                    ent -> allowedEntities.contains(ent.getType()) && ent != player);
        } else {
            result = player.getWorld().rayTraceBlocks(eyeLoc, dir, effectiveMaxDist, FluidCollisionMode.NEVER, true);
        }

        Block targetBlock = result != null ? result.getHitBlock() : null;
        Entity targetEntity = result != null ? result.getHitEntity() : null;

        boolean hasBlock = targetBlock != null && (allowedBlocks.contains(targetBlock.getType())
                || plugin.getHookManager().isCustomContainer(targetBlock));
        boolean hasEntity = targetEntity != null && (allowedEntities.contains(targetEntity.getType())
                || plugin.getHookManager().isCustomFurniture(targetEntity));

        if (hasBlock || hasEntity) {
            Location targetLoc = (targetBlock != null) ? targetBlock.getLocation()
                    : (targetEntity != null ? targetEntity.getLocation() : null);

            if (targetLoc == null)
                return;

            boolean sameContainer = isSameContainer(currentSession, targetBlock, targetEntity);

            // Granular checks only for new containers (deferred checks)
            if (!sameContainer) {
                // Check combat first
                if (isInCombat(player)) {
                    if (currentSession != null) {
                        currentSession.cleanup(false);
                        plugin.getActiveSessions().remove(uuid);
                    }
                    return;
                }

                // Check inventory open next
                InventoryType openType = player.getOpenInventory().getType();
                if (openType != InventoryType.CRAFTING && openType != InventoryType.CREATIVE
                        && openType != InventoryType.PLAYER) {
                    if (currentSession != null) {
                        currentSession.cleanup(false);
                        plugin.getActiveSessions().remove(uuid);
                    }
                    return;
                }

                // Check permission
                if (!hasContainerPermission(player, targetBlock, targetEntity)) {
                    if (currentSession != null) {
                        currentSession.cleanup(false);
                        plugin.getActiveSessions().remove(uuid);
                    }
                    long now = System.currentTimeMillis();
                    long lastWarning = lastPermissionWarning.getOrDefault(uuid, 0L);
                    if (now - lastWarning > 3000L) {
                        player.sendMessage(plugin.getMessageManager().getMessage("container-no-permission"));
                        lastPermissionWarning.put(uuid, now);
                    }
                    return;
                }

                // Protection check (Factions, Lands, etc.)
                if (!plugin.getProtectionManager().canAccess(player, targetLoc)) {
                    if (currentSession != null) {
                        currentSession.cleanup(false);
                        plugin.getActiveSessions().remove(uuid);
                    }
                    long now = System.currentTimeMillis();
                    long lastWarning = lastPermissionWarning.getOrDefault(uuid, 0L);
                    if (now - lastWarning > 3000L) {
                        String protMsg = plugin.getMessageManager().getMessage("container-no-protection-access");
                        if (protMsg != null && !protMsg.isEmpty() && !protMsg.startsWith("Message not found")) {
                            player.sendMessage(protMsg);
                        }
                        lastPermissionWarning.put(uuid, now);
                    }
                    return;
                }

            }

            if (plugin.isHideWhenEmpty()) {
                boolean isEmpty = (sameContainer && currentSession != null) ? 
                        (currentSession.getInventory() == null || currentSession.getInventory().isEmpty()) :
                        isInventoryEmpty(player, targetBlock, targetEntity);
                if (isEmpty) {
                    if (currentSession != null) {
                        currentSession.cleanup(false);
                        plugin.getActiveSessions().remove(uuid);
                    }
                    return;
                }
            }

            if (currentSession == null) {
                PeekSession session = new PeekSession(player, targetBlock, targetEntity);
                if (session.getInventory() != null) {
                    plugin.getActiveSessions().put(uuid, session);
                }
            } else {
                if (sameContainer) {
                    if (currentSession.getInventory() != null) {
                        currentSession.update(true);
                    } else {
                        currentSession.cleanup(true);
                        plugin.getActiveSessions().remove(uuid);
                    }
                } else {
                    if (currentSession.isFrozen()) {
                        if (currentSession.getInventory() != null) {
                            currentSession.update(true);
                        } else {
                            currentSession.cleanup(true);
                            plugin.getActiveSessions().remove(uuid);
                        }
                        return;
                    }
                    currentSession.cleanup(true);
                    PeekSession session = new PeekSession(player, targetBlock, targetEntity);
                    if (session.getInventory() != null) {
                        plugin.getActiveSessions().put(uuid, session);
                    } else {
                        plugin.getActiveSessions().remove(uuid);
                    }
                }
            }
        } else {
            org.bukkit.inventory.EquipmentSlot shulkerSlot = null;
            if (player.isSneaking()) {
                org.bukkit.inventory.ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem != null && handItem.getType().name().contains("SHULKER_BOX")) {
                    shulkerSlot = org.bukkit.inventory.EquipmentSlot.HAND;
                } else {
                    org.bukkit.inventory.ItemStack offHandItem = player.getInventory().getItemInOffHand();
                    if (offHandItem != null && offHandItem.getType().name().contains("SHULKER_BOX")) {
                        shulkerSlot = org.bukkit.inventory.EquipmentSlot.OFF_HAND;
                    }
                }
            }

            if (shulkerSlot != null) {
                boolean sameContainer = currentSession != null && currentSession.getHandSlot() == shulkerSlot;

                if (currentSession == null) {
                    PeekSession session = new PeekSession(player, shulkerSlot);
                    if (session.getInventory() != null) {
                        plugin.getActiveSessions().put(uuid, session);
                    }
                } else {
                    if (sameContainer) {
                        currentSession.update(true);
                    } else {
                        currentSession.cleanup(true);
                        PeekSession session = new PeekSession(player, shulkerSlot);
                        if (session.getInventory() != null) {
                            plugin.getActiveSessions().put(uuid, session);
                        } else {
                            plugin.getActiveSessions().remove(uuid);
                        }
                    }
                }
            } else {
                if (currentSession != null) {
                    if (currentSession.isFrozen()) {
                        currentSession.update(true);
                        return;
                    }
                    currentSession.cleanup(false);
                    plugin.getActiveSessions().remove(uuid);
                }
            }
        }
    }

    public void clearCache(UUID uuid) {
        lastStates.remove(uuid);
        lastPermissionWarning.remove(uuid);
        compassTargets.remove(uuid);
        compassCooldowns.remove(uuid);
        cleanupCompassArrow(uuid);
    }

    private boolean isInventoryEmpty(Player player, Block block, Entity entity) {
        StoragePeek plugin = StoragePeek.getInstance();
        Inventory inv = null;
        if (block != null) {
            inv = plugin.getHookManager().getInventory(block, player);
            if (inv == null && block.getType() == Material.ENDER_CHEST) {
                inv = player.getEnderChest();
            }
        } else if (entity != null) {
            inv = plugin.getHookManager().getInventory(entity, player);
        }

        return inv == null || inv.isEmpty();
    }

    private boolean isInCombat(Player player) {
        return StoragePeek.getInstance().getCombatHookManager().isPlayerInCombat(player);
    }

    private boolean hasContainerPermission(Player player, Block block, Entity entity) {
        // NOTE: storagepeek.bypass.protection is intentionally NOT checked here.
        // It is only a protection-layer bypass (used in ProtectionManager.canAccess),
        // not a container-visibility bypass. This prevents storagepeek.peek.* wildcard
        // from implicitly granting bypass.protection via LuckPerms and skipping island checks.
        if (player.hasPermission("storagepeek.admin")
                || player.hasPermission("storagepeek.peek.*")
                || player.hasPermission("storagepeek.peek")) {
            return true;
        }

        String containerType = "";
        if (block != null) {
            containerType = block.getType().name().toLowerCase();
        } else if (entity != null) {
            containerType = entity.getType().name().toLowerCase();
        }

        if (player.hasPermission("storagepeek.peek." + containerType)) {
            return true;
        }

        if (containerType.contains("shulker") && player.hasPermission("storagepeek.peek.shulker_box")) {
            return true;
        }

        return false;
    }


    public java.util.Set<Material> getAllowedBlocks() {
        return allowedBlocks;
    }

    public java.util.Set<EntityType> getAllowedEntities() {
        return allowedEntities;
    }

    private boolean isSameContainer(PeekSession session, Block targetBlock, Entity targetEntity) {
        if (session == null) {
            return false;
        }
        if (targetEntity != null && targetEntity.equals(session.getEntity())) {
            return true;
        }
        if (targetBlock != null) {
            if (targetBlock.equals(session.getBlock())) {
                return true;
            }
            Inventory inv = session.getInventory();
            if (inv instanceof org.bukkit.inventory.DoubleChestInventory dci) {
                org.bukkit.block.DoubleChest doubleChest = dci.getHolder();
                if (doubleChest != null) {
                    if (doubleChest.getLeftSide() instanceof org.bukkit.block.BlockState leftState && targetBlock.equals(leftState.getBlock())) {
                        return true;
                    }
                    if (doubleChest.getRightSide() instanceof org.bukkit.block.BlockState rightState && targetBlock.equals(rightState.getBlock())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
