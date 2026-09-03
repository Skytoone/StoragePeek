package fr.skynex.storagepeek.session;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class PeekSession {

    public enum FilterType {
        ALL, RESOURCES, FOOD, EQUIPMENT
    }

    private final StoragePeek plugin;
    private final Player player;
    private final Block block;
    private final Entity entity;
    private final EquipmentSlot handSlot;
    private final Inventory inventory;
    private final Location containerCenter;

    private final PeekSessionTheme themeHelper;
    private final PeekSessionDisplayManager displayManager;

    private final double spacing;
    private final float distance;
    private final int syncFreq;

    private final float textScale;
    private final float textYOffset;
    private final float textZOffset;

    private final Location centerCache = new Location(null, 0, 0, 0);
    private final Location baseCenterCache = new Location(null, 0, 0, 0);
    private final Location entityCenterCache = new Location(null, 0, 0, 0);
    private final Vector rightVec = new Vector();
    private final Vector upVec = new Vector();
    private final Vector normalVec = new Vector();
    private final Vector tempVec = new Vector();
    private final Location tempLoc = new Location(null, 0, 0, 0);
    private final Location tempLoc2 = new Location(null, 0, 0, 0);
    private final Location eyeCache = new Location(null, 0, 0, 0);
    private final Vector eyeVecCache = new Vector();

    private int updateCounter = 0;
    private double smoothedDistance = 1.8;
    private boolean focusModeEnabled;
    private boolean frozen = false;
    private boolean animationsEnabled;
    private boolean hoverNameplateEnabled;
    private float hoverNameplateScale;
    private Color hoverNameplateBgColor;
    private Material highlightMaterial;
    private double lookX = 0;
    private double lookY = 0;
    private Material filterMaterial = null;
    private int teleportDuration = 3;
    private double distanceSmoothing = 0.15;
    private static final java.util.Set<Material> RESOURCE_MATERIALS = java.util.EnumSet.noneOf(Material.class);
    private static final java.util.Set<Material> EQUIPMENT_MATERIALS = java.util.EnumSet.noneOf(Material.class);

    static {
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("GOLD") 
                    || name.contains("IRON") || name.contains("COAL") || name.contains("COPPER") 
                    || name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("NETHERITE")
                    || name.contains("AMETHYST") || name.contains("QUARTZ")) {
                RESOURCE_MATERIALS.add(mat);
            }
            if (name.contains("SWORD") || name.contains("PICKAXE") || name.contains("AXE") 
                    || name.contains("SHOVEL") || name.contains("HOE") || name.contains("HELMET") 
                    || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") 
                    || name.contains("SHIELD") || name.contains("BOW") || name.contains("CROSSBOW") 
                    || name.contains("TRIDENT") || name.contains("SHEARS") || name.contains("FISHING_ROD")) {
                EQUIPMENT_MATERIALS.add(mat);
            }
        }
    }

    private final org.joml.Vector3f jomlLx = new org.joml.Vector3f();
    private final org.joml.Vector3f jomlLy = new org.joml.Vector3f();
    private final org.joml.Vector3f jomlLz = new org.joml.Vector3f();

    private final org.joml.Quaternionf tiltRotation = new org.joml.Quaternionf();

    private FilterType activeFilter = FilterType.ALL;
    private String rarityFilter = null;
    private String searchQuery = null;
    private int currentPage = 0;
    private int sortAnimationTicks = 0;
    private int columns;

    public PeekSession(Player player, Block block, Entity entity) {
        this.plugin = StoragePeek.getInstance();
        this.player = player;
        this.block = block;
        this.entity = entity;
        this.handSlot = null;
        this.inventory = findInventory();
        this.containerCenter = resolveContainerCenter();
        this.spacing = plugin.getSlotSpacing();
        this.distance = plugin.getDisplayDistance();
        this.smoothedDistance = this.distance;
        this.syncFreq = plugin.getSyncFrequency();

        this.textScale = plugin.getTextScale();
        this.textYOffset = plugin.getTextYOffset();
        this.textZOffset = plugin.getTextZOffset();

        this.themeHelper = new PeekSessionTheme(plugin);
        this.displayManager = new PeekSessionDisplayManager(plugin, this);

        loadThemeConfig();
        setupDynamics();
        spawnDisplays();
        if (block != null) {
            sendContainerAnimation(player, block, true);
        }
    }

    public PeekSession(Player player, Block block, Entity entity, Inventory virtualInventory, String virtualTitle) {
        this.plugin = StoragePeek.getInstance();
        this.player = player;
        this.block = block;
        this.entity = entity;
        this.handSlot = null;
        this.inventory = virtualInventory != null ? virtualInventory : findInventory();
        this.containerCenter = resolveContainerCenter();
        this.spacing = plugin.getSlotSpacing();
        this.distance = plugin.getDisplayDistance();
        this.smoothedDistance = this.distance;
        this.syncFreq = plugin.getSyncFrequency();

        this.textScale = plugin.getTextScale();
        this.textYOffset = plugin.getTextYOffset();
        this.textZOffset = plugin.getTextZOffset();

        this.themeHelper = new PeekSessionTheme(plugin);
        this.displayManager = new PeekSessionDisplayManager(plugin, this);

        loadThemeConfig();
        setupDynamics();
        this.frozen = true;
        spawnDisplays();
    }

    public PeekSession(Player player, EquipmentSlot handSlot) {
        this.plugin = StoragePeek.getInstance();
        this.player = player;
        this.block = null;
        this.entity = null;
        this.handSlot = handSlot;
        this.inventory = findInventory();
        this.containerCenter = resolveContainerCenter();
        this.spacing = plugin.getSlotSpacing();
        this.distance = plugin.getDisplayDistance();
        this.smoothedDistance = this.distance;
        this.syncFreq = plugin.getSyncFrequency();

        this.textScale = plugin.getTextScale();
        this.textYOffset = plugin.getTextYOffset();
        this.textZOffset = plugin.getTextZOffset();

        this.themeHelper = new PeekSessionTheme(plugin);
        this.displayManager = new PeekSessionDisplayManager(plugin, this);

        loadThemeConfig();
        setupDynamics();
        this.frozen = true;
        spawnDisplays();
    }

    public EquipmentSlot getHandSlot() { return handSlot; }
    public String getRarityFilter() { return rarityFilter; }
    public void setRarityFilter(String rarityFilter) {
        this.rarityFilter = rarityFilter;
        syncInventory();
    }

    private void loadThemeConfig() {
        themeHelper.loadThemeConfig(plugin);
        this.focusModeEnabled = plugin.isFocusModeEnabled();
        this.animationsEnabled = plugin.isAnimationsEnabled();
        this.hoverNameplateEnabled = plugin.isHoverNameplateEnabled();
        this.hoverNameplateScale = plugin.getHoverNameplateScale();
        this.hoverNameplateBgColor = plugin.getHoverNameplateBgColor();
        this.highlightMaterial = plugin.getHighlightMaterial();
        this.teleportDuration = plugin.getTeleportDuration();
        this.distanceSmoothing = plugin.getDistanceSmoothing();
    }

    private void setupDynamics() {
        if (inventory == null) return;
        columns = switch (inventory.getType()) {
            case DISPENSER, DROPPER, FURNACE, BLAST_FURNACE, SMOKER, CRAFTING -> 3;
            case HOPPER, BREWING -> 5;
            default -> 9;
        };
        themeHelper.setupDynamics(plugin, player, block, entity, handSlot, inventory);
    }

    private Inventory findInventory() {
        if (handSlot != null) {
            ItemStack item = handSlot == EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
            if (item != null && item.getType().name().contains("SHULKER_BOX")) {
                if (item.getItemMeta() instanceof BlockStateMeta bsm) {
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

    private void spawnDisplays() {
        updateDisplayCenter();
        displayManager.spawnDisplays(centerCache, inventory, columns, spacing, themeHelper.getBackgroundMaterial(),
                animationsEnabled, hoverNameplateEnabled, hoverNameplateScale, hoverNameplateBgColor, highlightMaterial,
                teleportDuration, textScale, textYOffset, textZOffset, handSlot, block, entity, player, searchQuery, currentPage);

        if (handSlot != null) {
            displayManager.updateAllBillboards(Display.Billboard.FIXED);
            buildAxes();
        }
    }

    public void update(boolean moved) {
        if (inventory == null) return;
        updateCounter++;
        if (focusModeEnabled) {
            boolean isSneaking = player.isSneaking();
            if (isSneaking && !frozen) {
                updateDisplayCenter();
                frozen = true;

                player.getLocation(tempLoc);
                tempLoc.setY(tempLoc.getY() + player.getEyeHeight());
                Vector dirToPlayer = tempLoc.toVector().subtract(centerCache.toVector()).normalize();
                double doubleX = dirToPlayer.getX();
                double doubleZ = dirToPlayer.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-doubleX, doubleZ));
                float pitch = (float) Math.toDegrees(Math.asin(-dirToPlayer.getY()));

                centerCache.setYaw(yaw);
                centerCache.setPitch(pitch);

                if (displayManager.getAnchor() != null && displayManager.getAnchor().isValid()) {
                    displayManager.getAnchor().teleport(centerCache);
                }
                displayManager.updateAllBillboards(Display.Billboard.FIXED);
                buildAxes();
                displayManager.setChildDisplayRotations(yaw, 0f);
            } else if (!isSneaking && frozen) {
                frozen = false;
                displayManager.resetHoverEffects(player, block, entity, inventory, hoverNameplateEnabled, hoverNameplateScale);
                displayManager.updateAllBillboards(Display.Billboard.CENTER);
                displayManager.setChildDisplayRotations(0f, 0f);
                if (displayManager.getAnchor() != null && displayManager.getAnchor().isValid()) {
                    displayManager.getAnchor().setInterpolationDelay(0);
                    displayManager.getAnchor().setInterpolationDuration(3);
                    Transformation t = displayManager.getAnchor().getTransformation();
                    t.getLeftRotation().identity();
                    displayManager.getAnchor().setTransformation(t);
                }
            }
        }

        if (moved) {
            if (!frozen) {
                updateDisplayCenter();
            } else {
                calculateLookOffsets();
            }
        } else {
            if (!frozen) {
                centerCache.setWorld(baseCenterCache.getWorld());
                centerCache.setX(baseCenterCache.getX());
                centerCache.setY(baseCenterCache.getY());
                centerCache.setZ(baseCenterCache.getZ());
                centerCache.setYaw(baseCenterCache.getYaw());
                centerCache.setPitch(baseCenterCache.getPitch());
            }
        }

        if (frozen) {
            int rows = (int) Math.ceil((double) inventory.getSize() / columns);
            double halfWidth = (columns * spacing) / 2.0;
            double halfHeight = (rows * spacing) / 2.0;
            double maxTiltAngle = Math.toRadians(12.0);
            float yawTilt = halfWidth > 0.001 ? (float) (-lookX / halfWidth * maxTiltAngle) : 0f;
            float pitchTilt = halfHeight > 0.001 ? (float) (lookY / halfHeight * maxTiltAngle) : 0f;
            yawTilt = Math.max(-0.22f, Math.min(0.22f, yawTilt));
            pitchTilt = Math.max(-0.22f, Math.min(0.22f, pitchTilt));
            tiltRotation.rotationXYZ(pitchTilt, yawTilt, 0.0f);
        } else {
            tiltRotation.identity();
        }

        if (frozen && (displayManager.getInteractionEntity() == null || !displayManager.getInteractionEntity().isValid())) {
            displayManager.spawnInteractionEntity(centerCache, columns, spacing, inventory, player);
        } else if (!frozen && displayManager.getInteractionEntity() != null) {
            displayManager.getInteractionEntity().remove();
            displayManager.setInteractionEntity(null);
        }

        if (displayManager.getInteractionEntity() != null && displayManager.getInteractionEntity().isValid()) {
            displayManager.getInteractionEntity().teleport(centerCache);
        }

        double breathing = frozen ? 0 : Math.sin(System.currentTimeMillis() / 400.0) * 0.03;
        tempVec.copy(upVec).multiply(breathing);
        centerCache.add(tempVec);

        if (plugin.isThemesEnabled()) {
            PeekSessionTheme.Theme t = themeHelper.getTheme();
            if (t == PeekSessionTheme.Theme.ENDER && themeHelper.isEnderParticles()) {
                player.spawnParticle(plugin.getEnderParticleType(), centerCache, plugin.getEnderParticleCount(), 0.2, 0.2, 0.2, 0.1);
            } else if (t == PeekSessionTheme.Theme.RICH && themeHelper.isRichParticles()) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(plugin.getRichParticleType(), centerCache, plugin.getRichParticleCount(), 0.3, 0.3, 0.3, 0.05);
                }
            } else if (t == PeekSessionTheme.Theme.AQUA && updateCounter % 2 == 0) {
                player.spawnParticle(Particle.BUBBLE, centerCache, 2, 0.25, 0.25, 0.25, 0.01);
            } else if (t == PeekSessionTheme.Theme.NETHER && updateCounter % 2 == 0) {
                player.spawnParticle(Particle.FLAME, centerCache, 1, 0.25, 0.25, 0.25, 0.02);
            } else if (t == PeekSessionTheme.Theme.NEON && updateCounter % 2 == 0) {
                player.spawnParticle(Particle.GLOW, centerCache, 1, 0.25, 0.25, 0.25, 0.01);
            } else if (t == PeekSessionTheme.Theme.CYBERPUNK && updateCounter % 2 == 0) {
                player.spawnParticle(Particle.WARPED_SPORE, centerCache, 2, 0.25, 0.25, 0.25, 0.01);
            } else if (t == PeekSessionTheme.Theme.RAINBOW && updateCounter % 2 == 0) {
                player.spawnParticle(Particle.CHERRY_LEAVES, centerCache, 1, 0.3, 0.3, 0.3, 0.02);
            }
        }

        if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive() && updateCounter % 15 == 0) {
            PeekSessionTheme.Theme t = themeHelper.getTheme();
            if (t == PeekSessionTheme.Theme.RICH || t == PeekSessionTheme.Theme.ENDER) {
                Location loc = block != null ? block.getLocation() : (entity != null ? entity.getLocation() : null);
                if (loc != null) {
                    Color beamColor = (t == PeekSessionTheme.Theme.RICH) ? Color.fromRGB(255, 215, 0) : Color.fromRGB(170, 0, 255);
                    plugin.getLootGlowHook().spawnLootGlowBeaconBeam(loc, beamColor);
                }
            }
        }

        if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive() && updateCounter % 6 == 0 && inventory != null) {
            int mythicCount = 0;
            int legendaryCount = 0;
            for (ItemStack st : inventory.getContents()) {
                if (st != null && st.getType() != Material.AIR) {
                    String r = plugin.getLootGlowHook().getItemRarity(st);
                    if ("MYTHIC".equalsIgnoreCase(r)) mythicCount++;
                    else if ("LEGENDARY".equalsIgnoreCase(r)) legendaryCount++;
                }
            }
            if (mythicCount >= 3 || legendaryCount >= 5) {
                Location loc = block != null ? block.getLocation() : (entity != null ? entity.getLocation() : null);
                if (loc != null) {
                    String highest = mythicCount >= 3 ? "MYTHIC" : "LEGENDARY";
                    plugin.getLootGlowHook().spawnMythicVaultAura(loc, highest);
                }
            }
        }

        if (plugin.isFillIndicatorEnabled() && updateCounter % 8 == 0 && block != null && inventory != null) {
            int max = inventory.getSize();
            int current = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    current++;
                }
            }
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            if (current == 0) {
                block.getWorld().spawnParticle(Particle.SMOKE, center, 2, 0.4, 0.4, 0.4, 0.01);
            } else if (current == max) {
                block.getWorld().spawnParticle(Particle.WAX_OFF, center, 3, 0.4, 0.4, 0.4, 0.05);
            }
        }

        if (sortAnimationTicks > 0) {
            sortAnimationTicks--;
            float progress = (6f - sortAnimationTicks) / 6f;
            float angle = (float) (progress * Math.PI * 2);
            for (PeekSessionDisplayManager.ItemEntry entry : displayManager.getItemEntries()) {
                if (entry.display() != null && entry.display().isValid()) {
                    Transformation t = entry.display().getTransformation();
                    t.getLeftRotation().rotationY(angle);
                    float scaleFactor = 1.0f + (float) Math.sin(progress * Math.PI) * 0.35f;
                    float size = (entry.slot() == displayManager.getHoveredSlot() ? 0.22f : 0.15f) * scaleFactor;
                    t.getScale().set(size, size, size);
                    entry.display().setInterpolationDelay(0);
                    entry.display().setInterpolationDuration(1);
                    entry.display().setTransformation(t);
                }
            }
            if (sortAnimationTicks == 0) {
                displayManager.updateAllScales(textScale);
            }
        }

        if (displayManager.getAnchor() != null && displayManager.getAnchor().isValid()) {
            displayManager.getAnchor().teleport(centerCache);
            if (frozen) {
                displayManager.getAnchor().setInterpolationDelay(0);
                displayManager.getAnchor().setInterpolationDuration(3);
                Transformation t = displayManager.getAnchor().getTransformation();
                t.getLeftRotation().set(tiltRotation);
                displayManager.getAnchor().setTransformation(t);
            }
        }

        if (frozen) {
            int currentTarget = getTargetSlot();
            if (currentTarget != displayManager.getHoveredSlot()) {
                displayManager.applyHoverEffects(currentTarget, player, block, entity, inventory, hoverNameplateEnabled, hoverNameplateScale);
            }
        }

        if (++updateCounter % syncFreq == 0) {
            syncInventory();
        }
    }

    public void syncInventory() {
        if (inventory == null) return;
        int size = inventory.getSize();
        int filledCount = 0;

        for (PeekSessionDisplayManager.ItemEntry entry : displayManager.getItemEntries()) {
            ItemStack rawItem = (entry.slot() < size) ? inventory.getItem(entry.slot()) : null;
            if (rawItem != null && rawItem.getType() != Material.AIR) {
                filledCount++;
            }
            ItemStack cur = (rawItem != null && matchesFilter(rawItem, activeFilter)) ? rawItem : null;
            ItemStack disp = entry.display().getItemStack();

            boolean curEmpty = cur == null || cur.getType() == Material.AIR;
            boolean dispEmpty = disp == null || disp.getType() == Material.AIR;

            if (curEmpty) {
                if (!dispEmpty) {
                    entry.display().setItemStack(null);
                    displayManager.updateFakeAmount(entry.slot(), 0, (float) entry.xOff(), (float) entry.yOff(), centerCache, player, textScale, textYOffset, textZOffset, animationsEnabled, inventory);
                    displayManager.destroyDurabilityBar(entry.slot());
                }
            } else {
                if (cur != null && (dispEmpty || !cur.isSimilar(disp)
                        || (disp != null && cur.getAmount() != disp.getAmount())
                        || (cur.getType().getMaxDurability() > 0 && disp != null
                                && getDamage(cur) != getDamage(disp)))) {
                    entry.display().setItemStack(cur.clone());

                    boolean matches = (filterMaterial == null) || (cur.getType() == filterMaterial);
                    float targetScale = matches ? (entry.slot() == displayManager.getHoveredSlot() ? 0.22f : 0.15f) : 0.05f;
                    Transformation t = entry.display().getTransformation();
                    t.getScale().set(targetScale, targetScale, targetScale);
                    entry.display().setTransformation(t);

                    displayManager.updateFakeAmount(entry.slot(), cur.getAmount(), (float) entry.xOff(), (float) entry.yOff(), centerCache, player, textScale, textYOffset, textZOffset, animationsEnabled, inventory);
                    displayManager.updateDurabilityBar(entry.slot(), cur, (float) entry.xOff(), (float) entry.yOff(), centerCache, player, animationsEnabled);
                }
            }
        }

        if (displayManager.getFillIndicator() != null && displayManager.getFillIndicator().isValid() && plugin.isFillIndicatorEnabled()) {
            String titleStr = (block != null) ? block.getType().name() : (entity != null ? entity.getType().name() : "Container");
            titleStr = titleStr.replace("_", " ");
            titleStr = Character.toUpperCase(titleStr.charAt(0)) + titleStr.substring(1).toLowerCase();

            net.kyori.adventure.text.TextComponent.Builder builder = net.kyori.adventure.text.Component.text()
                    .append(net.kyori.adventure.text.Component.text(titleStr, net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                    .append(net.kyori.adventure.text.Component.text(" - " + filledCount + " / " + size, net.kyori.adventure.text.format.NamedTextColor.GRAY));

            if (activeFilter != FilterType.ALL) {
                builder.append(net.kyori.adventure.text.Component.text(" (" + activeFilter.name() + ")", net.kyori.adventure.text.format.NamedTextColor.AQUA));
            }
            if (filledCount == 0) {
                builder.append(net.kyori.adventure.text.Component.text(" [Empty]", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
            } else if (filledCount == size) {
                builder.append(net.kyori.adventure.text.Component.text(" [Full]", net.kyori.adventure.text.format.NamedTextColor.GOLD));
            }

            displayManager.getFillIndicator().text(builder.build());
        }
    }

    private boolean matchesFilter(ItemStack item, FilterType filter) {
        if (item == null || item.getType() == Material.AIR) return true;
        if (rarityFilter != null && !rarityFilter.isEmpty()) {
            if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive()) {
                String r = plugin.getLootGlowHook().getItemRarity(item);
                if (r == null || !r.equalsIgnoreCase(rarityFilter)) {
                    return false;
                }
            }
        }
        if (filter == FilterType.ALL) return true;
        
        Material type = item.getType();
        
        switch (filter) {
            case RESOURCES -> { return RESOURCE_MATERIALS.contains(type); }
            case FOOD -> {
                return type.isEdible() || type == Material.CAKE || type == Material.COOKIE 
                    || type == Material.MILK_BUCKET || type == Material.HONEY_BOTTLE;
            }
            case EQUIPMENT -> { return EQUIPMENT_MATERIALS.contains(type); }
            default -> { return true; }
        }
    }

    private int getDamage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        if (item.getItemMeta() instanceof Damageable d) return d.getDamage();
        return 0;
    }

    public FilterType getActiveFilter() { return activeFilter; }
    public void setActiveFilter(FilterType activeFilter) {
        this.activeFilter = activeFilter;
        syncInventory();
    }

    public void triggerSortAnimation() { this.sortAnimationTicks = 6; }

    public void cleanup(boolean immediate) {
        if (block != null) {
            sendContainerAnimation(player, block, false);
        }
        if (immediate || !animationsEnabled) {
            displayManager.removeEntities();
        } else {
            displayManager.animateScaleDown();
            FoliaScheduler.runLater(plugin, player, displayManager::removeEntities, 4L);
        }
    }

    private void sendContainerAnimation(Player player, Block block, boolean open) {
        if (block == null || !plugin.isContainerAnimationsEnabled()) return;
        if (block.getState() instanceof org.bukkit.block.Lidded lidded) {
            if (open) lidded.open();
            else lidded.close();
        }

        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            Material type = block.getType();
            String name = type.name();
            Sound sound = null;
            if (name.contains("ENDER_CHEST")) sound = open ? Sound.BLOCK_ENDER_CHEST_OPEN : Sound.BLOCK_ENDER_CHEST_CLOSE;
            else if (name.contains("SHULKER_BOX")) sound = open ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_SHULKER_BOX_CLOSE;
            else if (name.contains("BARREL")) sound = open ? Sound.BLOCK_BARREL_OPEN : Sound.BLOCK_BARREL_CLOSE;
            else if (name.contains("CHEST")) sound = open ? Sound.BLOCK_CHEST_OPEN : Sound.BLOCK_CHEST_CLOSE;
            if (sound != null) {
                player.playSound(block.getLocation(), sound, 0.5f, 1.0f);
            }
        }
    }

    public Block getBlock() { return block; }
    public Entity getEntity() { return entity; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        if (frozen) {
            displayManager.updateAllBillboards(Display.Billboard.FIXED);
        } else {
            displayManager.updateAllBillboards(Display.Billboard.CENTER);
        }
    }

    public void refresh() {
        cleanup(false);
        spawnDisplays();
        update(true);
    }

    public Inventory getInventory() { return inventory; }
    public Player getPlayer() { return player; }

    public void saveHandInventory() {
        if (handSlot == null) return;
        ItemStack item = handSlot == EquipmentSlot.HAND ? 
            player.getInventory().getItemInMainHand() : 
            player.getInventory().getItemInOffHand();
        if (item != null && item.getType().name().contains("SHULKER_BOX")) {
            if (item.getItemMeta() instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulkerBox) {
                    shulkerBox.getInventory().setContents(inventory.getContents());
                    bsm.setBlockState(shulkerBox);
                    item.setItemMeta(bsm);
                    if (handSlot == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.getInventory().setItemInOffHand(item);
                    }
                }
            }
        }
    }

    public Location getContainerCenter() {
        if (containerCenter != null) return containerCenter;
        if (entity != null) {
            entity.getLocation(entityCenterCache);
            entityCenterCache.add(0, entity.getHeight() / 2.0, 0);
            return entityCenterCache;
        }
        return null;
    }

    private Location resolveContainerCenter() {
        if (inventory instanceof org.bukkit.inventory.DoubleChestInventory dci) {
            org.bukkit.block.DoubleChest doubleChest = dci.getHolder();
            if (doubleChest != null) {
                Location loc = doubleChest.getLocation();
                if (doubleChest.getLeftSide() instanceof org.bukkit.block.BlockState leftState && 
                    doubleChest.getRightSide() instanceof org.bukkit.block.BlockState rightState) {
                    Location lLoc = leftState.getLocation().add(0.5, 0.5, 0.5);
                    Location rLoc = rightState.getLocation().add(0.5, 0.5, 0.5);
                    return new Location(loc.getWorld(), 
                        (lLoc.getX() + rLoc.getX()) / 2.0,
                        (lLoc.getY() + rLoc.getY()) / 2.0,
                        (lLoc.getZ() + rLoc.getZ()) / 2.0
                    );
                }
                return loc;
            }
        }
        if (block != null) {
            return block.getLocation().add(0.5, 0.5, 0.5);
        }
        return null;
    }

    private void updateDisplayCenter() {
        player.getLocation(tempLoc);
        tempLoc.setY(tempLoc.getY() + player.getEyeHeight());
        double ex = tempLoc.getX();
        double ey = tempLoc.getY();
        double ez = tempLoc.getZ();

        double yawRad = Math.toRadians(tempLoc.getYaw());
        double pitchRad = Math.toRadians(tempLoc.getPitch());
        double cosPitch = Math.cos(pitchRad);

        Vector dir = tempVec;
        dir.setX(-Math.sin(yawRad) * cosPitch);
        dir.setY(-Math.sin(pitchRad));
        dir.setZ(Math.cos(yawRad) * cosPitch);

        Location center = getContainerCenter();
        if (center != null) {
            dir.setX(center.getX() - ex);
            dir.setY(center.getY() - ey);
            dir.setZ(center.getZ() - ez);
            dir.normalize();
        }

        double targetDistance = distance;
        double offset = Math.max(0.25, columns * spacing * 0.45);

        RayTraceResult blockResult = player.getWorld().rayTraceBlocks(tempLoc, dir, distance,
                org.bukkit.FluidCollisionMode.NEVER, true);
        if (blockResult != null && blockResult.getHitBlock() != null) {
            Vector hitPos = blockResult.getHitPosition();
            double dx = hitPos.getX() - ex;
            double dy = hitPos.getY() - ey;
            double dz = hitPos.getZ() - ez;
            targetDistance = Math.max(0.4, Math.sqrt(dx * dx + dy * dy + dz * dz) - offset);
        }

        if (entity != null) {
            eyeVecCache.setX(ex).setY(ey).setZ(ez);
            RayTraceResult entResult = entity.getBoundingBox().rayTrace(eyeVecCache, dir, distance);
            if (entResult != null) {
                double entDist = Math.max(0.4, entResult.getHitPosition().distance(eyeVecCache) - offset);
                targetDistance = Math.min(targetDistance, entDist);
            } else {
                entity.getLocation(tempLoc2);
                double rawDist = Math.max(0.4, tempLoc2.distance(tempLoc) - offset);
                targetDistance = Math.min(targetDistance, rawDist);
            }
        }

        smoothedDistance += (targetDistance - smoothedDistance) * distanceSmoothing;
        baseCenterCache.setWorld(tempLoc.getWorld());
        baseCenterCache.setX(ex + dir.getX() * smoothedDistance);
        baseCenterCache.setY(ey + dir.getY() * smoothedDistance);
        baseCenterCache.setZ(ez + dir.getZ() * smoothedDistance);

        if (dir.getY() < -0.4) {
            baseCenterCache.add(0, 0.12, 0);
        }

        if (block != null && block.getType().name().contains("SHULKER_BOX")) {
            baseCenterCache.add(0, 0.35, 0);
        }

        centerCache.setWorld(baseCenterCache.getWorld());
        centerCache.setX(baseCenterCache.getX());
        centerCache.setY(baseCenterCache.getY());
        centerCache.setZ(baseCenterCache.getZ());
        centerCache.setYaw(baseCenterCache.getYaw());
        centerCache.setPitch(baseCenterCache.getPitch());
    }

    private void buildAxes() {
        player.getLocation(tempLoc);
        tempLoc.setY(tempLoc.getY() + player.getEyeHeight());

        double nx = tempLoc.getX() - centerCache.getX();
        double ny = tempLoc.getY() - centerCache.getY();
        double nz = tempLoc.getZ() - centerCache.getZ();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (len < 0.0001) return;
        nx /= len; ny /= len; nz /= len;

        normalVec.setX(nx); normalVec.setY(ny); normalVec.setZ(nz);

        boolean lookingVertical = Math.abs(ny) > 0.95;
        if (lookingVertical) {
            double rad = Math.toRadians(tempLoc.getYaw());
            rightVec.setX(-Math.cos(rad));
            rightVec.setY(0);
            rightVec.setZ(-Math.sin(rad));
        } else {
            rightVec.setX(nz); rightVec.setY(0); rightVec.setZ(-nx);
        }
        rightVec.normalize();

        double rx = rightVec.getX();
        double ry = rightVec.getY();
        double rz = rightVec.getZ();

        upVec.setX(ny * rz - nz * ry);
        upVec.setY(nz * rx - nx * rz);
        upVec.setZ(nx * ry - ny * rx);
        upVec.normalize();
    }

    private void calculateLookOffsets() {
        if (!frozen) {
            lookX = 0; lookY = 0;
            return;
        }

        player.getLocation(eyeCache);
        eyeCache.setY(eyeCache.getY() + player.getEyeHeight());

        double ex = eyeCache.getX();
        double ey = eyeCache.getY();
        double ez = eyeCache.getZ();

        double yawRad = Math.toRadians(eyeCache.getYaw());
        double pitchRad = Math.toRadians(eyeCache.getPitch());
        double cosPitch = Math.cos(pitchRad);
        double dx = -Math.sin(yawRad) * cosPitch;
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * cosPitch;

        double cx = centerCache.getX();
        double cy = centerCache.getY();
        double cz = centerCache.getZ();

        double nx = normalVec.getX();
        double ny = normalVec.getY();
        double nz = normalVec.getZ();

        jomlLx.set(1, 0, 0);
        jomlLy.set(0, 1, 0);
        jomlLz.set(0, 0, 1);
        tiltRotation.transform(jomlLx);
        tiltRotation.transform(jomlLy);
        tiltRotation.transform(jomlLz);

        double xPrimeX = jomlLx.x * rightVec.getX() + jomlLx.y * upVec.getX() + jomlLx.z * nx;
        double xPrimeY = jomlLx.x * rightVec.getY() + jomlLx.y * upVec.getY() + jomlLx.z * ny;
        double xPrimeZ = jomlLx.x * rightVec.getZ() + jomlLx.y * upVec.getZ() + jomlLx.z * nz;

        double yPrimeX = jomlLy.x * rightVec.getX() + jomlLy.y * upVec.getX() + jomlLy.z * nx;
        double yPrimeY = jomlLy.x * rightVec.getY() + jomlLy.y * upVec.getY() + jomlLy.z * ny;
        double yPrimeZ = jomlLy.x * rightVec.getZ() + jomlLy.y * upVec.getZ() + jomlLy.z * nz;

        double zPrimeX = jomlLz.x * rightVec.getX() + jomlLz.y * upVec.getX() + jomlLz.z * nx;
        double zPrimeY = jomlLz.x * rightVec.getY() + jomlLz.y * upVec.getY() + jomlLz.z * ny;
        double zPrimeZ = jomlLz.x * rightVec.getZ() + jomlLz.y * upVec.getZ() + jomlLz.z * nz;

        double denom = zPrimeX * dx + zPrimeY * dy + zPrimeZ * dz;
        if (Math.abs(denom) < 0.0001) {
            lookX = 0; lookY = 0;
            return;
        }

        double t = ((cx - ex) * zPrimeX + (cy - ey) * zPrimeY + (cz - ez) * zPrimeZ) / denom;
        if (t < 0) {
            lookX = 0; lookY = 0;
            return;
        }

        double hx = ex + dx * t;
        double hy = ey + dy * t;
        double hz = ez + dz * t;

        double rx = hx - cx;
        double ry = hy - cy;
        double rz = hz - cz;

        lookX = rx * xPrimeX + ry * xPrimeY + rz * xPrimeZ;
        lookY = rx * yPrimeX + ry * yPrimeY + rz * yPrimeZ;
    }

    public int getTargetSlot() {
        if (inventory == null) return -1;
        int rows = (int) Math.ceil((double) inventory.getSize() / columns);
        double halfWidth = (columns * spacing) / 2.0;
        double halfHeight = (rows * spacing) / 2.0;

        if (Math.abs(lookX) > halfWidth || Math.abs(lookY) > halfHeight) return -1;

        int col = (int) Math.floor((lookX + halfWidth) / spacing);
        int row = (int) Math.floor((halfHeight - lookY) / spacing);

        if (col < 0 || col >= columns || row < 0 || row >= rows) return -1;

        int slot = row * columns + col;
        return (slot < inventory.getSize()) ? slot : -1;
    }

    public void toggleFilter(Material material) {
        if (filterMaterial == material) {
            filterMaterial = null;
        } else {
            filterMaterial = material;
        }
        displayManager.updateAllScales(textScale);
    }

    public void clearFilter() {
        if (filterMaterial != null) {
            filterMaterial = null;
            displayManager.updateAllScales(textScale);
        }
    }

    public Material getFilterMaterial() {
        return filterMaterial;
    }

    public Interaction getInteractionEntity() {
        return displayManager.getInteractionEntity();
    }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        refresh();
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, currentPage);
        refresh();
    }

    public boolean isValid() {
        if (displayManager.isCleanedUp()) return false;
        if (handSlot != null) {
            if (!player.isSneaking()) return false;
            ItemStack item = handSlot == EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
            if (item == null || !item.getType().name().contains("SHULKER_BOX")) {
                return false;
            }
            return true;
        }
        if (block != null) {
            if (block.getType() == Material.AIR) return false;
            if (!plugin.getRaycastTask().getAllowedBlocks().contains(block.getType())
                    && !plugin.getHookManager().isCustomContainer(block)) {
                return false;
            }
            if (!plugin.getProtectionManager().canAccess(player, block.getLocation())) {
                return false;
            }
        }
        if (entity != null) {
            if (!entity.isValid()) return false;
            if (!plugin.getRaycastTask().getAllowedEntities().contains(entity.getType())
                    && !plugin.getHookManager().isCustomFurniture(entity)) {
                return false;
            }
            if (!plugin.getProtectionManager().canAccess(player, entity.getLocation())) {
                return false;
            }
        }
        return true;
    }
}
