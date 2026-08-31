package fr.skynex.storagepeek.session;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;

public class PeekSession {

    private final StoragePeek plugin;
    private final Player player;
    private final Block block;
    private final Entity entity;
    private final org.bukkit.inventory.EquipmentSlot handSlot;
    private final Inventory inventory;
    private final Location containerCenter;

    private final double spacing;
    private final float distance;
    private final int syncFreq;

    // Aesthetic settings from config
    private final float textScale;
    private final float textYOffset;
    private final float textZOffset;
    private Material backgroundMaterial;

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

    private final Map<Integer, TextDisplay> fakeAmounts = new HashMap<>();
    private final Map<Integer, BlockDisplay> durabilityBars = new HashMap<>();
    private final Map<Integer, BlockDisplay> durabilityBgs = new HashMap<>();
    private final Map<Integer, String> lastTextCache = new HashMap<>();

    // Performance Cache
    private static final Map<Material, org.bukkit.block.data.BlockData> blockDataCache = new java.util.concurrent.ConcurrentHashMap<>();

    private enum Theme {
        DEFAULT, ENDER, RICH, AQUA, NETHER, NEON, CYBERPUNK, RAINBOW
    }

    private Theme theme = Theme.DEFAULT;

    public enum FilterType {
        ALL, RESOURCES, FOOD, EQUIPMENT
    }
    private FilterType activeFilter = FilterType.ALL;

    private final Set<Material> preciousMaterials = new HashSet<>();
    private boolean themesEnabled;
    private boolean enderThemeEnabled;
    private Material enderBackground;
    private boolean enderParticles;
    private boolean richThemeEnabled;
    private Material richBackground;
    private boolean richParticles;
    private int richThreshold;
    private final Map<String, Material> customBackgrounds = new HashMap<>();
    private String shulkerBgType;
    private boolean focusModeEnabled;
    private boolean frozen = false;
    private boolean animationsEnabled;
    private boolean hoverNameplateEnabled;
    private float hoverNameplateScale;
    private org.bukkit.Color hoverNameplateBgColor;
    private BlockDisplay hoverHighlight;
    private Material highlightMaterial;
    private double lookX = 0;
    private double lookY = 0;
    private Material filterMaterial = null;
    private int teleportDuration = 3;
    private double distanceSmoothing = 0.15;
    private final org.joml.Quaternionf tiltRotation = new org.joml.Quaternionf();

    private record ItemEntry(ItemDisplay display, double xOff, double yOff, int slot) {
    }

    private final List<ItemEntry> itemEntries = new ArrayList<>();
    private BlockDisplay background;
    private TextDisplay anchor; // The unified passenger vehicle
    private org.bukkit.entity.Interaction interactionEntity;
    private int hoveredSlot = -1;
    private TextDisplay hoverLabel;
    private TextDisplay fillIndicator;
    private TextDisplay taglineBanner;
    private int sortAnimationTicks = 0;
    private boolean cleanedUp = false;
    private boolean isSpawning = false;
    private final List<Entity> entitiesToShow = new ArrayList<>();

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

        this.backgroundMaterial = plugin.getDefaultBackground();

        loadThemeConfig();
        setupDynamics();
        spawnDisplays();
        if (block != null) {
            sendContainerAnimation(player, block, true);
        }
    }

    public PeekSession(Player player, org.bukkit.inventory.EquipmentSlot handSlot) {
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

        this.backgroundMaterial = plugin.getDefaultBackground();

        loadThemeConfig();
        setupDynamics();
        this.frozen = true;
        spawnDisplays();
    }

    public org.bukkit.inventory.EquipmentSlot getHandSlot() {
        return handSlot;
    }

    private void loadThemeConfig() {
        StoragePeek plugin = StoragePeek.getInstance();
        this.themesEnabled = plugin.isThemesEnabled();
        this.enderThemeEnabled = plugin.isEnderThemeEnabled();
        this.enderBackground = plugin.getEnderBackground();
        this.enderParticles = plugin.isEnderParticles();

        this.richThemeEnabled = plugin.isRichThemeEnabled();
        this.richBackground = plugin.getRichBackground();
        this.richParticles = plugin.isRichParticles();
        this.richThreshold = plugin.getRichThreshold();

        this.preciousMaterials.clear();
        this.preciousMaterials.addAll(plugin.getPreciousMaterials());

        this.focusModeEnabled = plugin.isFocusModeEnabled();

        this.shulkerBgType = plugin.getShulkerBgType();
        this.customBackgrounds.clear();
        this.customBackgrounds.putAll(plugin.getCustomBackgrounds());

        this.animationsEnabled = plugin.isAnimationsEnabled();
        this.hoverNameplateEnabled = plugin.isHoverNameplateEnabled();
        this.hoverNameplateScale = plugin.getHoverNameplateScale();
        this.hoverNameplateBgColor = plugin.getHoverNameplateBgColor();
        this.highlightMaterial = plugin.getHighlightMaterial();
        this.teleportDuration = plugin.getTeleportDuration();
        this.distanceSmoothing = plugin.getDistanceSmoothing();
    }

    private void setupDynamics() {
        if (inventory == null)
            return;
        columns = switch (inventory.getType()) {
            case DISPENSER, DROPPER, FURNACE, BLAST_FURNACE, SMOKER, CRAFTING -> 3;
            case HOPPER, BREWING -> 5;
            default -> 9;
        };

        if (!themesEnabled) {
            return;
        }

        // Load custom player theme if set
        org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
        org.bukkit.NamespacedKey themeKey = new org.bukkit.NamespacedKey(plugin, "theme");
        String pTheme = pdc.get(themeKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (pTheme != null) {
            try {
                Theme selected = Theme.valueOf(pTheme.toUpperCase().trim());
                if (selected != Theme.DEFAULT) {
                    this.theme = selected;
                    applyCustomCosmeticTheme();
                    return;
                }
            } catch (Exception ignored) {}
        }

        // Apply Custom Overrides from Config
        String containerKey = (block != null) ? block.getType().name()
                : (entity != null ? entity.getType().name() : null);
        if (containerKey != null && customBackgrounds.containsKey(containerKey)) {
            backgroundMaterial = customBackgrounds.get(containerKey);
        }

        // Shulker Logic (Dynamic Color + Configured Type)
        Material shulkerMaterial = null;
        if (handSlot != null) {
            ItemStack item = handSlot == org.bukkit.inventory.EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
            if (item != null) shulkerMaterial = item.getType();
        } else if (block != null) {
            shulkerMaterial = block.getType();
        }

        if (shulkerMaterial != null) {
            String typeName = shulkerMaterial.name();
            if (typeName.contains("SHULKER_BOX")) {
                String color = typeName.replace("_SHULKER_BOX", "");
                if (color.isEmpty())
                    color = "PURPLE"; // Vanilla SHULKER_BOX is purple
                Material m = Material.matchMaterial(color + "_" + shulkerBgType);
                if (m != null)
                    backgroundMaterial = m;
            } else if (shulkerMaterial == Material.ENDER_CHEST && enderThemeEnabled) {
                theme = Theme.ENDER;
                if (!customBackgrounds.containsKey("ENDER_CHEST")) {
                    backgroundMaterial = enderBackground;
                }
                return;
            }
        }

        // Check for Richness if not already special
        if (theme == Theme.DEFAULT && richThemeEnabled) {
            int preciousCount = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && preciousMaterials.contains(item.getType())) {
                    preciousCount += item.getAmount();
                }
            }
            if (preciousCount >= richThreshold) {
                theme = Theme.RICH;
                backgroundMaterial = richBackground;
            }
        }
    }

    private void applyCustomCosmeticTheme() {
        switch (theme) {
            case ENDER:
                backgroundMaterial = Material.OBSIDIAN;
                break;
            case RICH:
                backgroundMaterial = Material.GOLD_BLOCK;
                break;
            case AQUA:
                backgroundMaterial = Material.DARK_PRISMARINE;
                break;
            case NETHER:
                backgroundMaterial = Material.NETHERRACK;
                break;
            case NEON:
                backgroundMaterial = Material.BLACK_CONCRETE;
                break;
            case CYBERPUNK:
                backgroundMaterial = Material.WARPED_WART_BLOCK;
                break;
            case RAINBOW:
                backgroundMaterial = Material.MAGENTA_GLAZED_TERRACOTTA;
                break;
            default:
                break;
        }
    }

    private Inventory findInventory() {
        StoragePeek plugin = StoragePeek.getInstance();
        if (handSlot != null) {
            ItemStack item = handSlot == org.bukkit.inventory.EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
            if (item != null && item.getType().name().contains("SHULKER_BOX")) {
                if (item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulkerBox) {
                        return shulkerBox.getInventory();
                    }
                }
            }
            return null;
        }
        if (block != null) {
            Inventory inv = plugin.getHookManager().getInventory(block, player);
            if (inv != null)
                return inv;

            if (block.getType() == Material.ENDER_CHEST)
                return player.getEnderChest();
        } else if (entity != null) {
            Inventory inv = plugin.getHookManager().getInventory(entity, player);
            if (inv != null)
                return inv;
        }
        return null;
    }

    private void spawnDisplays() {
        if (inventory == null)
            return;
        isSpawning = true;
        try {
            int size = inventory.getSize();
            int rows = (int) Math.ceil((double) size / columns);

            updateDisplayCenter();

            // Spawn the unseen anchor that ALL other elements will ride on.
            anchor = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER); // Enforce view-plane relative coordinate space
                ent.setTeleportDuration(teleportDuration);
            });
            showEntityToPlayer(anchor);

            float bgWidth = (float) (columns * spacing) + 0.15f;
            float bgHeight = (float) (rows * spacing) + 0.15f;
            org.bukkit.block.data.BlockData bgData = blockDataCache.computeIfAbsent(backgroundMaterial,
                    Bukkit::createBlockData);

            background = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setBlock(bgData);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setVisibleByDefault(false);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setInterpolationDuration(1);
                ent.setTeleportDuration(1);
                Transformation t = ent.getTransformation();
                t.getScale().set(animationsEnabled ? 0f : bgWidth, animationsEnabled ? 0f : bgHeight, 0.01f);
                // Centered background translation relative to origin
                t.getTranslation().set(-bgWidth / 2f, -bgHeight / 2f, -0.05f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(background);
            showEntityToPlayer(background);

            ItemStack[] contents = inventory.getContents();
            for (int i = 0; i < size; i++) {
                double xOff = (i % columns - (columns - 1) / 2.0) * spacing;
                double yOff = ((rows - 1) / 2.0 - i / columns) * spacing;

                ItemStack item = (i < contents.length) ? contents[i] : null;
                float scaleMultiplier = 1.0f;
                org.bukkit.Color customGlowColor = null;

                if (item != null && item.getType() != Material.AIR) {
                    fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
                        (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();
                    
                    for (fr.skynex.storagepeek.api.security.LootSecurityFilter filter : apiImpl.getSecurityFilters()) {
                        fr.skynex.storagepeek.api.security.SecurityResult result = filter.evaluate(player, block, entity, item);
                        if (result.getType() == fr.skynex.storagepeek.api.security.SecurityResult.Type.HIDE) {
                            item = null;
                            break;
                        } else if (result.getType() == fr.skynex.storagepeek.api.security.SecurityResult.Type.MASK) {
                            Material mat = result.getPlaceholderMaterial() != null ? result.getPlaceholderMaterial() : Material.BARRIER;
                            item = new ItemStack(mat);
                            if (result.getCustomName() != null) {
                                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                                if (meta != null) {
                                    meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                            .deserialize(result.getCustomName()));
                                    item.setItemMeta(meta);
                                }
                            }
                            break;
                        }
                    }

                    if (item != null) {
                        fr.skynex.storagepeek.api.events.StoragePeekRenderItemEvent renderEvent =
                            new fr.skynex.storagepeek.api.events.StoragePeekRenderItemEvent(player, item, i);
                        org.bukkit.Bukkit.getPluginManager().callEvent(renderEvent);
                        if (renderEvent.isCancelled()) {
                            item = null;
                        } else {
                            scaleMultiplier = renderEvent.getCustomScaleMultiplier();
                            customGlowColor = renderEvent.getGlowColor();
                        }
                    }
                }

                final ItemStack finalItem = item;
                final float finalScaleMult = scaleMultiplier;
                final org.bukkit.Color finalGlowColor = customGlowColor;

                ItemDisplay display = centerCache.getWorld().spawn(centerCache, ItemDisplay.class, ent -> {
                    plugin.tagDisplayEntity(ent);
                    ent.setItemStack(finalItem);
                    ent.setBillboard(Display.Billboard.CENTER);
                    ent.setVisibleByDefault(false);
                    ent.setBrightness(new Display.Brightness(15, 15));
                    ent.setInterpolationDuration(2); // Silky smooth hovering scaling
                    ent.setTeleportDuration(1);
                    if (finalGlowColor != null) {
                        ent.setGlowing(true);
                        try {
                            ent.setGlowColorOverride(finalGlowColor);
                        } catch (Throwable ignored) {}
                    }
                    Transformation t = ent.getTransformation();
                    float baseScale = (animationsEnabled ? 0f : 0.15f) * finalScaleMult;
                    t.getScale().set(baseScale, baseScale, baseScale);
                    // The magic: absolute viewport relative offset from anchor origin
                    t.getTranslation().set((float) xOff, (float) yOff, 0.0f);
                    ent.setTransformation(t);
                    ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                });
                anchor.addPassenger(display);
                showEntityToPlayer(display);
                itemEntries.add(new ItemEntry(display, xOff, yOff, i));

                if (item != null && item.getAmount() > 1 && plugin.isQuantityLabelsEnabled()) {
                    spawnFakeAmount(i, item.getAmount(), (float) xOff, (float) yOff);
                }
                if (item != null && item.getType().getMaxDurability() > 0 && plugin.isDurabilityBarsEnabled()) {
                    spawnDurabilityBar(i, item, (float) xOff, (float) yOff);
                }
            }

            // Spawn hoverHighlight backplate
            hoverHighlight = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setBlock(blockDataCache.computeIfAbsent(highlightMaterial, Bukkit::createBlockData));
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setVisibleByDefault(false);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setInterpolationDuration(4);
                ent.setTeleportDuration(1);
                Transformation t = ent.getTransformation();
                t.getTranslation().set(-0.08f, -0.08f, -0.04f);
                t.getScale().set(0f, 0f, 0f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(hoverHighlight);
            showEntityToPlayer(hoverHighlight);

            // Spawn hoverLabel nameplate
            hoverLabel = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setInterpolationDuration(4);
                ent.setTeleportDuration(1);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setDefaultBackground(true);
                ent.setBackgroundColor(hoverNameplateBgColor);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.text(net.kyori.adventure.text.Component.empty());
                Transformation t = ent.getTransformation();
                t.getTranslation().set(0f, 0f, 0.10f);
                t.getScale().set(0f, 0f, 0f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(hoverLabel);
            showEntityToPlayer(hoverLabel);

            boolean fillEnabled = plugin.getConfig().getBoolean("visualizers.fill-indicator", true)
                    && plugin.getConfig().getBoolean("holograms.fill-indicator-enabled", true);
            if (fillEnabled) {
                fillIndicator = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                    plugin.tagDisplayEntity(ent);
                    ent.setVisibleByDefault(false);
                    ent.setBillboard(Display.Billboard.CENTER);
                    ent.setBrightness(new Display.Brightness(15, 15));
                    ent.setDefaultBackground(true);
                    ent.setBackgroundColor(org.bukkit.Color.fromARGB(120, 0, 0, 0));
                    ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                    ent.text(net.kyori.adventure.text.Component.empty());
                    Transformation t = ent.getTransformation();
                    t.getTranslation().set(0f, -bgHeight / 2f - 0.22f, 0.05f);
                    ent.setTransformation(t);
                });
                anchor.addPassenger(fillIndicator);
                showEntityToPlayer(fillIndicator);
            }

            fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
                (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();
            String tagline = apiImpl.getContainerTagline(block, entity);
            if (tagline != null && !tagline.isEmpty()) {
                taglineBanner = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                    plugin.tagDisplayEntity(ent);
                    ent.setVisibleByDefault(false);
                    ent.setBillboard(Display.Billboard.CENTER);
                    ent.setBrightness(new Display.Brightness(15, 15));
                    ent.setDefaultBackground(true);
                    ent.setBackgroundColor(org.bukkit.Color.fromARGB(160, 20, 20, 20));
                    ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                    ent.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .deserialize(tagline));
                    Transformation t = ent.getTransformation();
                    t.getTranslation().set(0f, bgHeight / 2f + 0.25f, 0.05f);
                    ent.setTransformation(t);
                });
                anchor.addPassenger(taglineBanner);
                showEntityToPlayer(taglineBanner);
            }
        } finally {
            isSpawning = false;
        }

        if (!entitiesToShow.isEmpty()) {
            StoragePeek plugin = StoragePeek.getInstance();
            fr.skynex.storagepeek.util.FoliaScheduler.runTask(plugin, player, () -> {
                if (player.isOnline()) {
                    for (Entity ent : entitiesToShow) {
                        if (ent.isValid()) {
                            player.showEntity(plugin, ent);
                        }
                    }
                }
                entitiesToShow.clear();
            });
        }

        if (handSlot != null) {
            updateAllBillboards(Display.Billboard.FIXED);
            buildAxes();
        }
 
        // Schedule scale-up animation if enabled
        if (animationsEnabled) {
            animateScaleUp();
        }
    }

    private void spawnFakeAmount(int slot, int amount, float localX, float localY) {
        String format = StoragePeek.getInstance().getMessageManager().getMessage("item-quantity-format");
        String text = format.replace("{amount}", String.valueOf(amount));
        lastTextCache.put(slot, text);

        ItemStack item = (slot < inventory.getSize()) ? inventory.getItem(slot) : null;
        boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);

        TextDisplay textDisplay = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setVisibleByDefault(false);
            ent.setBillboard(frozen ? Display.Billboard.FIXED : Display.Billboard.CENTER);
            ent.setInterpolationDuration(1);
            ent.setTeleportDuration(1);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setDefaultBackground(false);
            ent.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            ent.setShadowed(true);
            ent.setAlignment(TextDisplay.TextAlignment.CENTER);
            ent.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .deserialize(text));
            Transformation t = ent.getTransformation();
            // Local additive stack to align with slot but offset on Z index
            t.getTranslation().set(localX, localY + textYOffset, textZOffset);
            float currentScale = (animationsEnabled || !matches) ? 0f : textScale;
            t.getScale().set(currentScale, currentScale, currentScale);
            ent.setTransformation(t);
        });
        anchor.addPassenger(textDisplay);
        showEntityToPlayer(textDisplay);
        fakeAmounts.put(slot, textDisplay);

        if (animationsEnabled && matches && !isSpawning) {
            fr.skynex.storagepeek.util.FoliaScheduler.runLater(StoragePeek.getInstance(), player, () -> {
                if (textDisplay.isValid() && anchor != null && anchor.isValid()) {
                    textDisplay.setInterpolationDelay(0);
                    textDisplay.setInterpolationDuration(5);
                    Transformation t = textDisplay.getTransformation();
                    t.getScale().set(textScale, textScale, textScale);
                    textDisplay.setTransformation(t);
                }
            }, 1L);
        }
    }

    public void update(boolean moved) {
        if (inventory == null) {
            return;
        }
        if (focusModeEnabled) {
            boolean isSneaking = player.isSneaking();
            if (isSneaking && !frozen) {
                // Refresh HUD position BEFORE freezing so it locks at the crouched eye level,
                // not the standing position from the previous tick.
                updateDisplayCenter();
                frozen = true;

                // Calculate yaw/pitch to face the player at this exact moment
                player.getLocation(tempLoc);
                tempLoc.setY(tempLoc.getY() + player.getEyeHeight());
                Vector dirToPlayer = tempLoc.toVector().subtract(centerCache.toVector()).normalize();
                double doubleX = dirToPlayer.getX();
                double doubleZ = dirToPlayer.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-doubleX, doubleZ));
                float pitch = (float) Math.toDegrees(Math.asin(-dirToPlayer.getY()));

                centerCache.setYaw(yaw);
                centerCache.setPitch(pitch);

                if (anchor != null && anchor.isValid()) {
                    anchor.teleport(centerCache);
                }
                updateAllBillboards(Display.Billboard.FIXED);
                buildAxes(); // Build axes ONCE when transitioning to frozen!
                // Align child entity rotations with the anchor so their local +X
                // matches rightVec (= player's right), preventing the mirror effect.
                setChildDisplayRotations(yaw, 0f);
            } else if (!isSneaking && frozen) {
                frozen = false;
                resetHoverEffects();
                updateAllBillboards(Display.Billboard.CENTER);
                // Reset child rotations: in CENTER mode, yaw doesn't affect billboard
                // but reset to 0 for cleanliness.
                setChildDisplayRotations(0f, 0f);
                if (anchor != null && anchor.isValid()) {
                    anchor.setInterpolationDelay(0);
                    anchor.setInterpolationDuration(3);
                    Transformation t = anchor.getTransformation();
                    t.getLeftRotation().identity();
                    anchor.setTransformation(t);
                }
            }
        }

        if (moved) {
            if (!frozen) {
                updateDisplayCenter();
            } else {
                calculateLookOffsets(); // Only calculate look offsets when frozen!
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

        // Subtle Holographic Tilt
        if (frozen) {
            int rows = (int) Math.ceil((double) inventory.getSize() / columns);
            double halfWidth = (columns * spacing) / 2.0;
            double halfHeight = (rows * spacing) / 2.0;

            // max 12 degrees tilt
            double maxTiltAngle = Math.toRadians(12.0);
            float yawTilt = halfWidth > 0.001 ? (float) (-lookX / halfWidth * maxTiltAngle) : 0f;
            float pitchTilt = halfHeight > 0.001 ? (float) (lookY / halfHeight * maxTiltAngle) : 0f;

            // Constrain angles to avoid extreme spinning
            yawTilt = Math.max(-0.22f, Math.min(0.22f, yawTilt));
            pitchTilt = Math.max(-0.22f, Math.min(0.22f, pitchTilt));

            tiltRotation.rotationXYZ(pitchTilt, yawTilt, 0.0f);
        } else {
            tiltRotation.identity();
        }

        // Manage interaction entity for reliable clicking
        if (frozen && (interactionEntity == null || !interactionEntity.isValid())) {
            spawnInteractionEntity();
        } else if (!frozen && interactionEntity != null) {
            interactionEntity.remove();
            interactionEntity = null;
        }

        if (interactionEntity != null && interactionEntity.isValid()) {
            interactionEntity.teleport(centerCache);
        }

        double breathing = frozen ? 0 : Math.sin(System.currentTimeMillis() / 400.0) * 0.03;
        tempVec.copy(upVec).multiply(breathing);
        centerCache.add(tempVec);

        if (themesEnabled) {
            if (theme == Theme.ENDER && enderParticles) {
                player.spawnParticle(plugin.getEnderParticleType(), centerCache, plugin.getEnderParticleCount(), 0.2,
                        0.2, 0.2, 0.1);
            } else if (theme == Theme.RICH && richParticles) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(plugin.getRichParticleType(), centerCache, plugin.getRichParticleCount(), 0.3,
                            0.3, 0.3, 0.05);
                }
            } else if (theme == Theme.AQUA) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(org.bukkit.Particle.BUBBLE, centerCache, 2, 0.25, 0.25, 0.25, 0.01);
                }
            } else if (theme == Theme.NETHER) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(org.bukkit.Particle.FLAME, centerCache, 1, 0.25, 0.25, 0.25, 0.02);
                }
            } else if (theme == Theme.NEON) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(org.bukkit.Particle.GLOW, centerCache, 1, 0.25, 0.25, 0.25, 0.01);
                }
            } else if (theme == Theme.CYBERPUNK) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(org.bukkit.Particle.WARPED_SPORE, centerCache, 2, 0.25, 0.25, 0.25, 0.01);
                }
            } else if (theme == Theme.RAINBOW) {
                if (updateCounter % 2 == 0) {
                    player.spawnParticle(org.bukkit.Particle.CHERRY_LEAVES, centerCache, 1, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        // Fill Indicator empty/full particle feedback
        if (plugin.getConfig().getBoolean("visualizers.fill-indicator", true) && updateCounter % 8 == 0 && block != null && inventory != null) {
            int max = inventory.getSize();
            int current = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    current++;
                }
            }
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            if (current == 0) {
                block.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, center, 2, 0.4, 0.4, 0.4, 0.01);
            } else if (current == max) {
                block.getWorld().spawnParticle(org.bukkit.Particle.WAX_OFF, center, 3, 0.4, 0.4, 0.4, 0.05);
            }
        }

        // Handle Visual Sorting Swirl animation
        if (sortAnimationTicks > 0) {
            sortAnimationTicks--;
            float progress = (6f - sortAnimationTicks) / 6f;
            float angle = (float) (progress * Math.PI * 2);
            for (ItemEntry entry : itemEntries) {
                if (entry.display() != null && entry.display().isValid()) {
                    Transformation t = entry.display().getTransformation();
                    t.getLeftRotation().rotationY(angle);
                    float scaleFactor = 1.0f + (float) Math.sin(progress * Math.PI) * 0.35f;
                    float size = (entry.slot() == hoveredSlot ? 0.22f : 0.15f) * scaleFactor;
                    t.getScale().set(size, size, size);
                    entry.display().setInterpolationDelay(0);
                    entry.display().setInterpolationDuration(1);
                    entry.display().setTransformation(t);
                }
            }
            if (sortAnimationTicks == 0) {
                updateAllScales();
            }
        }

        // ONE SINGLE TELEPORT.
        // Hardware passengers inherit perfect movement vectors with ZERO packet
        // overhead.
        if (anchor != null && anchor.isValid()) {
            anchor.teleport(centerCache);
            if (frozen) {
                anchor.setInterpolationDelay(0);
                anchor.setInterpolationDuration(3);
                Transformation t = anchor.getTransformation();
                t.getLeftRotation().set(tiltRotation);
                anchor.setTransformation(t);
            }
        }

        // Dynamic Interactive Hover Scaling (Phase 2)
        if (frozen) {
            int currentTarget = getTargetSlot();
            if (currentTarget != hoveredSlot) {
                applyHoverEffects(currentTarget);
                hoveredSlot = currentTarget;
            }
        }

        if (++updateCounter % syncFreq == 0)
            syncInventory();
    }

    private void applyHoverEffects(int newTarget) {
        // Reset old scale
        if (hoveredSlot != -1) {
            for (ItemEntry entry : itemEntries) {
                if (entry.slot() == hoveredSlot && entry.display().isValid()) {
                    ItemStack oldItem = entry.display().getItemStack();
                    boolean matches = (filterMaterial == null)
                            || (oldItem != null && oldItem.getType() == filterMaterial);
                    float targetScale = matches ? 0.15f : 0.05f;
                    Transformation t = entry.display().getTransformation();
                    t.getScale().set(targetScale, targetScale, targetScale);
                    entry.display().setInterpolationDelay(0);
                    entry.display().setInterpolationDuration(4);
                    entry.display().setTransformation(t);
                    break;
                }
            }
        }
        // Scale Up new target
        if (newTarget != -1) {
            boolean hasItem = false;
            ItemStack hoveredItem = (newTarget < inventory.getSize()) ? inventory.getItem(newTarget) : null;
            if (hoveredItem != null && hoveredItem.getType() != Material.AIR) {
                hasItem = true;
            }

            for (ItemEntry entry : itemEntries) {
                if (entry.slot() == newTarget && entry.display().isValid()) {
                    boolean matches = (filterMaterial == null)
                            || (hoveredItem != null && hoveredItem.getType() == filterMaterial);
                    float targetScale = matches ? 0.22f : 0.05f;
                    Transformation t = entry.display().getTransformation();
                    t.getScale().set(targetScale, targetScale, targetScale);
                    entry.display().setInterpolationDelay(0);
                    entry.display().setInterpolationDuration(4);
                    entry.display().setTransformation(t);
                    if (matches) {
                        StoragePeek.getInstance().playConfigSound(player, "hover", Sound.BLOCK_LEVER_CLICK, 0.2f, 1.5f);
                        if (hoveredItem != null) {
                            fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
                                (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();
                            for (fr.skynex.storagepeek.api.audio.SlotHoverSound customSound : apiImpl.getSlotHoverSounds()) {
                                try {
                                    if (customSound.getItemMatcher().test(hoveredItem)) {
                                        player.playSound(player.getLocation(), customSound.getSound(), customSound.getVolume(), customSound.getPitch());
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                    break;
                }
            }

            if (hasItem && hoverNameplateEnabled && hoverLabel != null && hoverLabel.isValid()) {
                net.kyori.adventure.text.Component displayName = getItemDisplayName(hoveredItem);
                hoverLabel.text(displayName);

                double xOff = 0;
                double yOff = 0;
                for (ItemEntry entry : itemEntries) {
                    if (entry.slot() == newTarget) {
                        xOff = entry.xOff();
                        yOff = entry.yOff();
                        break;
                    }
                }

                Transformation t = hoverLabel.getTransformation();
                t.getTranslation().set((float) xOff, (float) (yOff + 0.12f), 0.10f); // Higher and closer to player
                t.getScale().set(hoverNameplateScale, hoverNameplateScale, hoverNameplateScale);
                hoverLabel.setInterpolationDelay(0);
                hoverLabel.setInterpolationDuration(4);
                hoverLabel.setTransformation(t);
            } else if (hoverLabel != null && hoverLabel.isValid()) {
                Transformation t = hoverLabel.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverLabel.setInterpolationDelay(0);
                hoverLabel.setInterpolationDuration(4);
                hoverLabel.setTransformation(t);
            }

            if (hasItem && hoverHighlight != null && hoverHighlight.isValid()) {
                double xOff = 0;
                double yOff = 0;
                for (ItemEntry entry : itemEntries) {
                    if (entry.slot() == newTarget) {
                        xOff = entry.xOff();
                        yOff = entry.yOff();
                        break;
                    }
                }

                Transformation t = hoverHighlight.getTransformation();
                t.getTranslation().set((float) xOff - 0.08f, (float) yOff - 0.08f, -0.04f);
                t.getScale().set(0.16f, 0.16f, 0.002f);
                hoverHighlight.setInterpolationDelay(0);
                hoverHighlight.setInterpolationDuration(4);
                hoverHighlight.setTransformation(t);
            } else if (hoverHighlight != null && hoverHighlight.isValid()) {
                Transformation t = hoverHighlight.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverHighlight.setInterpolationDelay(0);
                hoverHighlight.setInterpolationDuration(4);
                hoverHighlight.setTransformation(t);
            }
        } else {
            if (hoverLabel != null && hoverLabel.isValid()) {
                Transformation t = hoverLabel.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverLabel.setInterpolationDelay(0);
                hoverLabel.setInterpolationDuration(4);
                hoverLabel.setTransformation(t);
            }
            if (hoverHighlight != null && hoverHighlight.isValid()) {
                Transformation t = hoverHighlight.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverHighlight.setInterpolationDelay(0);
                hoverHighlight.setInterpolationDuration(4);
                hoverHighlight.setTransformation(t);
            }
        }
    }

    private net.kyori.adventure.text.Component getItemDisplayName(ItemStack item) {
        if (item == null)
            return net.kyori.adventure.text.Component.empty();
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            
            // Special format for Enchanted Books
            if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta enchantMeta) {
                Map<org.bukkit.enchantments.Enchantment, Integer> stored = enchantMeta.getStoredEnchants();
                if (!stored.isEmpty()) {
                    List<String> list = new ArrayList<>();
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : stored.entrySet()) {
                        String name = e.getKey().getKey().getKey(); // registry key e.g. "mending"
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase().replace('_', ' ');
                        list.add(name + " " + toRoman(e.getValue()));
                    }
                    return net.kyori.adventure.text.Component.text("Enchanted Book (" + String.join(", ", list) + ")",
                            net.kyori.adventure.text.format.NamedTextColor.YELLOW);
                }
            }
            
            // Special format for Written Books
            if ((item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.WRITABLE_BOOK) && meta instanceof org.bukkit.inventory.meta.BookMeta bookMeta) {
                String title = bookMeta.getTitle();
                String author = bookMeta.getAuthor();
                if (title != null && !title.isEmpty()) {
                    String suffix = (author != null && !author.isEmpty()) ? " by " + author : "";
                    return net.kyori.adventure.text.Component.text(title + suffix,
                            net.kyori.adventure.text.format.NamedTextColor.AQUA);
                }
            }

            net.kyori.adventure.text.Component customName = meta.displayName();
            if (customName != null) {
                return customName;
            }
        }
        String raw = item.getType().name();
        String[] words = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty())
                continue;
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return net.kyori.adventure.text.Component.text(sb.toString(),
                net.kyori.adventure.text.format.NamedTextColor.WHITE);
    }

    private String toRoman(int num) {
        if (num == 1) return "I";
        if (num == 2) return "II";
        if (num == 3) return "III";
        if (num == 4) return "IV";
        if (num == 5) return "V";
        return String.valueOf(num);
    }

    private void resetHoverEffects() {
        if (hoveredSlot != -1) {
            applyHoverEffects(-1);
            hoveredSlot = -1;
        }
    }

    private void syncInventory() {
        if (inventory == null)
            return;
        int size = inventory.getSize();

        boolean fillEnabled = plugin.getConfig().getBoolean("visualizers.fill-indicator", true)
                && plugin.getConfig().getBoolean("holograms.fill-indicator-enabled", true);
        if (fillIndicator != null && fillIndicator.isValid() && fillEnabled) {
            int max = inventory.getSize();
            int current = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    current++;
                }
            }
            String titleStr = (block != null) ? block.getType().name() : (entity != null ? entity.getType().name() : "Container");
            titleStr = titleStr.replace("_", " ");
            titleStr = Character.toUpperCase(titleStr.charAt(0)) + titleStr.substring(1).toLowerCase();
            
            String text = "§e" + titleStr + " §7- " + current + " / " + max;
            if (activeFilter != FilterType.ALL) {
                text += " §b(" + activeFilter.name() + ")";
            }
            if (current == 0) {
                text += " §8[Empty]";
            } else if (current == max) {
                text += " §6[Full]";
            }
            
            fillIndicator.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(text));
        }

        for (ItemEntry entry : itemEntries) {
            ItemStack cur = (entry.slot() < size) ? inventory.getItem(entry.slot()) : null;
            if (cur != null && !matchesFilter(cur, activeFilter)) {
                cur = null;
            }
            ItemStack disp = entry.display().getItemStack();

            boolean curEmpty = cur == null || cur.getType() == Material.AIR;
            boolean dispEmpty = disp == null || disp.getType() == Material.AIR;

            if (curEmpty) {
                if (!dispEmpty) {
                    entry.display().setItemStack(null);
                    updateFakeAmount(entry.slot(), 0, (float) entry.xOff(), (float) entry.yOff());
                    destroyDurabilityBar(entry.slot());
                }
            } else {
                // Explicit null checks for cur and disp to satisfy IDE linters
                if (cur != null && (dispEmpty || !cur.isSimilar(disp)
                        || (disp != null && cur.getAmount() != disp.getAmount())
                        || (cur.getType().getMaxDurability() > 0 && disp != null
                                && getDamage(cur) != getDamage(disp)))) {
                    entry.display().setItemStack(cur.clone());

                    // Apply search filter scale immediately on sync to prevent visual bypass
                    boolean matches = (filterMaterial == null) || (cur.getType() == filterMaterial);
                    float targetScale = matches ? (entry.slot() == hoveredSlot ? 0.22f : 0.15f) : 0.05f;
                    Transformation t = entry.display().getTransformation();
                    t.getScale().set(targetScale, targetScale, targetScale);
                    entry.display().setTransformation(t);

                    updateFakeAmount(entry.slot(), cur.getAmount(), (float) entry.xOff(), (float) entry.yOff());
                    updateDurabilityBar(entry.slot(), cur, (float) entry.xOff(), (float) entry.yOff());
                }
            }
        }
    }

    private boolean matchesFilter(ItemStack item, FilterType filter) {
        if (item == null || item.getType() == Material.AIR) return true;
        if (filter == FilterType.ALL) return true;
        
        Material type = item.getType();
        String name = type.name();
        
        switch (filter) {
            case RESOURCES:
                return name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("GOLD") 
                    || name.contains("IRON") || name.contains("COAL") || name.contains("COPPER") 
                    || name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("NETHERITE")
                    || name.contains("AMETHYST") || name.contains("QUARTZ");
            case FOOD:
                return type.isEdible() || type == Material.CAKE || type == Material.COOKIE 
                    || type == Material.MILK_BUCKET || type == Material.HONEY_BOTTLE;
            case EQUIPMENT:
                return name.contains("SWORD") || name.contains("PICKAXE") || name.contains("AXE") 
                    || name.contains("SHOVEL") || name.contains("HOE") || name.contains("HELMET") 
                    || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") 
                    || name.contains("SHIELD") || name.contains("BOW") || name.contains("CROSSBOW") 
                    || name.contains("TRIDENT") || name.contains("SHEARS") || name.contains("FISHING_ROD");
            default:
                return true;
        }
    }

    public FilterType getActiveFilter() {
        return activeFilter;
    }

    public void setActiveFilter(FilterType activeFilter) {
        this.activeFilter = activeFilter;
        syncInventory();
    }

    public void triggerSortAnimation() {
        this.sortAnimationTicks = 6;
    }

    private void updateFakeAmount(int slot, int amount, float localX, float localY) {
        if (!plugin.isQuantityLabelsEnabled()) {
            destroyFakeEntity(slot);
            lastTextCache.put(slot, "");
            return;
        }
        String format = plugin.getMessageManager().getMessage("item-quantity-format");
        String newText = amount > 1 ? format.replace("{amount}", String.valueOf(amount)) : "";
        String lastText = lastTextCache.getOrDefault(slot, "");
        if (newText.equals(lastText))
            return;

        if (amount <= 1) {
            if (fakeAmounts.containsKey(slot)) {
                destroyFakeEntity(slot);
                lastTextCache.put(slot, "");
            }
        } else {
            if (!fakeAmounts.containsKey(slot)) {
                spawnFakeAmount(slot, amount, localX, localY);
            } else {
                TextDisplay display = fakeAmounts.get(slot);
                if (display != null && display.isValid()) {
                    display.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .deserialize(newText));

                    // Update scale based on filter matches
                    ItemStack item = (slot < inventory.getSize()) ? inventory.getItem(slot) : null;
                    boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);
                    float targetScale = matches ? textScale : 0f;
                    Transformation t = display.getTransformation();
                    t.getScale().set(targetScale, targetScale, targetScale);
                    display.setTransformation(t);
                }
                lastTextCache.put(slot, newText);
            }
        }
    }

    private void destroyFakeEntity(int slot) {
        TextDisplay display = fakeAmounts.remove(slot);
        if (display != null) {
            display.remove();
        }
    }

    public void cleanup(boolean immediate) {
        if (block != null) {
            sendContainerAnimation(player, block, false);
        }
        if (immediate || !animationsEnabled) {
            removeEntities();
        } else {
            animateScaleDown();
            fr.skynex.storagepeek.util.FoliaScheduler.runLater(StoragePeek.getInstance(), player, this::removeEntities, 4L);
        }
    }

    private void sendContainerAnimation(Player player, Block block, boolean open) {
        if (block == null || !plugin.isContainerAnimationsEnabled()) return;
        if (block.getState() instanceof org.bukkit.block.Lidded lidded) {
            if (open) {
                lidded.open();
            } else {
                lidded.close();
            }
        }

        // Play sound client-side
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            Material type = block.getType();
            String name = type.name();
            Sound sound = null;
            if (name.contains("ENDER_CHEST")) {
                sound = open ? Sound.BLOCK_ENDER_CHEST_OPEN : Sound.BLOCK_ENDER_CHEST_CLOSE;
            } else if (name.contains("SHULKER_BOX")) {
                sound = open ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_SHULKER_BOX_CLOSE;
            } else if (name.contains("BARREL")) {
                sound = open ? Sound.BLOCK_BARREL_OPEN : Sound.BLOCK_BARREL_CLOSE;
            } else if (name.contains("CHEST")) {
                sound = open ? Sound.BLOCK_CHEST_OPEN : Sound.BLOCK_CHEST_CLOSE;
            }
            if (sound != null) {
                player.playSound(block.getLocation(), sound, 0.5f, 1.0f);
            }
        }
    }

    private void animateScaleDown() {
        if (background != null && background.isValid()) {
            background.setInterpolationDelay(0);
            background.setInterpolationDuration(4);
            Transformation t = background.getTransformation();
            t.getScale().set(0f, 0f, 0.01f);
            background.setTransformation(t);
        }
        if (hoverLabel != null && hoverLabel.isValid()) {
            hoverLabel.setInterpolationDelay(0);
            hoverLabel.setInterpolationDuration(4);
            Transformation t = hoverLabel.getTransformation();
            t.getScale().set(0f, 0f, 0f);
            hoverLabel.setTransformation(t);
        }
        if (hoverHighlight != null && hoverHighlight.isValid()) {
            hoverHighlight.setInterpolationDelay(0);
            hoverHighlight.setInterpolationDuration(4);
            Transformation t = hoverHighlight.getTransformation();
            t.getScale().set(0f, 0f, 0f);
            hoverHighlight.setTransformation(t);
        }
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null && entry.display().isValid()) {
                entry.display().setInterpolationDelay(0);
                entry.display().setInterpolationDuration(4);
                Transformation t = entry.display().getTransformation();
                t.getScale().set(0f, 0f, 0f);
                entry.display().setTransformation(t);
            }
        }
        for (TextDisplay display : fakeAmounts.values()) {
            if (display != null && display.isValid()) {
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(4);
                Transformation t = display.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                display.setTransformation(t);
            }
        }
        for (BlockDisplay bg : durabilityBgs.values()) {
            if (bg != null && bg.isValid()) {
                bg.setInterpolationDelay(0);
                bg.setInterpolationDuration(4);
                Transformation t = bg.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                bg.setTransformation(t);
            }
        }
        for (BlockDisplay bar : durabilityBars.values()) {
            if (bar != null && bar.isValid()) {
                bar.setInterpolationDelay(0);
                bar.setInterpolationDuration(4);
                Transformation t = bar.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                bar.setTransformation(t);
            }
        }
    }

    private void removeEntities() {
        if (cleanedUp)
            return;
        cleanedUp = true;
        if (background != null) {
            background.remove();
            background = null;
        }
        if (anchor != null) {
            anchor.remove();
            anchor = null;
        }
        if (interactionEntity != null) {
            interactionEntity.remove();
            interactionEntity = null;
        }
        if (hoverLabel != null) {
            hoverLabel.remove();
            hoverLabel = null;
        }
        if (fillIndicator != null) {
            fillIndicator.remove();
            fillIndicator = null;
        }
        if (taglineBanner != null) {
            taglineBanner.remove();
            taglineBanner = null;
        }
        if (hoverHighlight != null) {
            hoverHighlight.remove();
            hoverHighlight = null;
        }
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null) {
                entry.display().remove();
            }
        }
        itemEntries.clear();
        for (TextDisplay display : fakeAmounts.values()) {
            if (display != null)
                display.remove();
        }
        fakeAmounts.clear();
        for (BlockDisplay bg : durabilityBgs.values()) {
            if (bg != null)
                bg.remove();
        }
        durabilityBgs.clear();
        for (BlockDisplay bar : durabilityBars.values()) {
            if (bar != null)
                bar.remove();
        }
        durabilityBars.clear();
    }

    private void animateScaleUp() {
        fr.skynex.storagepeek.util.FoliaScheduler.runLater(StoragePeek.getInstance(), player, () -> {
            if (cleanedUp || anchor == null || !anchor.isValid())
                return;

            if (background != null && background.isValid()) {
                int rows = (int) Math.ceil((double) inventory.getSize() / columns);
                float bgWidth = (float) (columns * spacing) + 0.15f;
                float bgHeight = (float) (rows * spacing) + 0.15f;

                background.setInterpolationDelay(0);
                background.setInterpolationDuration(5);
                Transformation t = background.getTransformation();
                t.getScale().set(bgWidth, bgHeight, 0.01f);
                background.setTransformation(t);
            }

            for (ItemEntry entry : itemEntries) {
                if (entry.display() != null && entry.display().isValid()) {
                    entry.display().setInterpolationDelay(0);
                    entry.display().setInterpolationDuration(5);
                    Transformation t = entry.display().getTransformation();
                    t.getScale().set(0.15f, 0.15f, 0.15f);
                    entry.display().setTransformation(t);
                }
            }

            for (Map.Entry<Integer, TextDisplay> entry : fakeAmounts.entrySet()) {
                TextDisplay display = entry.getValue();
                if (display != null && display.isValid()) {
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(5);
                    Transformation t = display.getTransformation();
                    t.getScale().set(textScale, textScale, textScale);
                    display.setTransformation(t);
                }
            }

            for (Map.Entry<Integer, BlockDisplay> entry : durabilityBgs.entrySet()) {
                BlockDisplay bg = entry.getValue();
                if (bg != null && bg.isValid()) {
                    bg.setInterpolationDelay(0);
                    bg.setInterpolationDuration(5);
                    Transformation t = bg.getTransformation();
                    t.getScale().set(0.12f, 0.015f, 0.001f);
                    bg.setTransformation(t);
                }
            }

            for (Map.Entry<Integer, BlockDisplay> entry : durabilityBars.entrySet()) {
                BlockDisplay bar = entry.getValue();
                if (bar != null && bar.isValid()) {
                    ItemStack item = inventory.getItem(entry.getKey());
                    if (item != null && item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable meta) {
                        double percent = (double) (item.getType().getMaxDurability() - meta.getDamage())
                                / item.getType().getMaxDurability();
                        bar.setInterpolationDelay(0);
                        bar.setInterpolationDuration(5);
                        Transformation t = bar.getTransformation();
                        t.getScale().set(0.11f * (float) percent, 0.01f, 0.002f);
                        bar.setTransformation(t);
                    }
                }
            }
        }, 1L);
    }

    public Block getBlock() {
        return block;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        if (frozen) {
            updateAllBillboards(Display.Billboard.FIXED);
        } else {
            updateAllBillboards(Display.Billboard.CENTER);
        }
    }

    public void refresh() {
        cleanup(false);
        spawnDisplays();
        update(true);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public void saveHandInventory() {
        if (handSlot == null) return;
        ItemStack item = handSlot == org.bukkit.inventory.EquipmentSlot.HAND ? 
            player.getInventory().getItemInMainHand() : 
            player.getInventory().getItemInOffHand();
        if (item != null && item.getType().name().contains("SHULKER_BOX")) {
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulkerBox) {
                    shulkerBox.getInventory().setContents(inventory.getContents());
                    bsm.setBlockState(shulkerBox);
                    item.setItemMeta(bsm);
                    if (handSlot == org.bukkit.inventory.EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.getInventory().setItemInOffHand(item);
                    }
                }
            }
        }
    }

    private Location getContainerCenter() {
        if (containerCenter != null) {
            return containerCenter;
        }
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

        org.bukkit.util.RayTraceResult blockResult = player.getWorld().rayTraceBlocks(tempLoc, dir, distance,
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
            org.bukkit.util.RayTraceResult entResult = entity.getBoundingBox().rayTrace(eyeVecCache, dir, distance);
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

        // Vertical safety: lift HUD if looking down to prevent clipping into the
        // floor/bottom of block
        if (dir.getY() < -0.4) {
            baseCenterCache.add(0, 0.12, 0);
        }

        centerCache.setWorld(baseCenterCache.getWorld());
        centerCache.setX(baseCenterCache.getX());
        centerCache.setY(baseCenterCache.getY());
        centerCache.setZ(baseCenterCache.getZ());
        centerCache.setYaw(baseCenterCache.getYaw());
        centerCache.setPitch(baseCenterCache.getPitch());
    }

    private void updateDurabilityBar(int slot, ItemStack item, float localX, float localY) {
        if (!plugin.isDurabilityBarsEnabled()) {
            destroyDurabilityBar(slot);
            return;
        }
        if (item == null || item.getType().getMaxDurability() <= 0) {
            destroyDurabilityBar(slot);
            return;
        }

        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable meta) || meta.getDamage() == 0) {
            destroyDurabilityBar(slot);
            return;
        }

        if (!durabilityBars.containsKey(slot)) {
            spawnDurabilityBar(slot, item, localX, localY);
        } else {
            BlockDisplay bar = durabilityBars.get(slot);
            if (bar != null && bar.isValid()) {
                double percent = (double) (item.getType().getMaxDurability() - meta.getDamage())
                        / item.getType().getMaxDurability();
                Material barColor = getDurabilityColor(percent);
                bar.setBlock(blockDataCache.computeIfAbsent(barColor, Bukkit::createBlockData));
                Transformation t = bar.getTransformation();

                boolean matches = (filterMaterial == null) || (item.getType() == filterMaterial);
                float targetBarX = matches ? 0.11f * (float) percent : 0f;
                float targetBarY = matches ? 0.01f : 0f;
                float targetBarZ = matches ? 0.002f : 0f;
                t.getScale().set(targetBarX, targetBarY, targetBarZ);
                bar.setTransformation(t);
            }

            BlockDisplay bg = durabilityBgs.get(slot);
            if (bg != null && bg.isValid()) {
                Transformation t = bg.getTransformation();
                boolean matches = (filterMaterial == null) || (item.getType() == filterMaterial);
                float targetBgX = matches ? 0.12f : 0f;
                float targetBgY = matches ? 0.015f : 0f;
                float targetBgZ = matches ? 0.001f : 0f;
                t.getScale().set(targetBgX, targetBgY, targetBgZ);
                bg.setTransformation(t);
            }
        }
    }

    private int getDamage(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d)
            return d.getDamage();
        return 0;
    }

    private void spawnDurabilityBar(int slot, ItemStack item, float localX, float localY) {
        if (item == null || item.getType().getMaxDurability() <= 0)
            return;
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable meta) || meta.getDamage() == 0)
            return;

        double percent = (double) (item.getType().getMaxDurability() - meta.getDamage())
                / item.getType().getMaxDurability();
        Material barColor = getDurabilityColor(percent);

        boolean matches = (filterMaterial == null) || (item.getType() == filterMaterial);

        BlockDisplay bg = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setBlock(blockDataCache.computeIfAbsent(Material.BLACK_CONCRETE, Bukkit::createBlockData));
            ent.setBillboard(frozen ? Display.Billboard.FIXED : Display.Billboard.CENTER);
            ent.setVisibleByDefault(false);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            float currentBgX = (animationsEnabled || !matches) ? 0f : 0.12f;
            float currentBgY = (animationsEnabled || !matches) ? 0f : 0.015f;
            float currentBgZ = (animationsEnabled || !matches) ? 0f : 0.001f;
            t.getScale().set(currentBgX, currentBgY, currentBgZ);
            // Composite offset including slot translation + durability fine tuning
            t.getTranslation().set(localX - 0.06f, localY - 0.08f, 0.02f);
            ent.setTransformation(t);
        });
        anchor.addPassenger(bg);
        showEntityToPlayer(bg);
        durabilityBgs.put(slot, bg);

        BlockDisplay bar = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setBlock(blockDataCache.computeIfAbsent(barColor, Bukkit::createBlockData));
            ent.setBillboard(frozen ? Display.Billboard.FIXED : Display.Billboard.CENTER);
            ent.setVisibleByDefault(false);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            float currentBarX = (animationsEnabled || !matches) ? 0f : 0.11f * (float) percent;
            float currentBarY = (animationsEnabled || !matches) ? 0f : 0.01f;
            float currentBarZ = (animationsEnabled || !matches) ? 0f : 0.002f;
            t.getScale().set(currentBarX, currentBarY, currentBarZ);
            t.getTranslation().set(localX - 0.055f, localY - 0.078f, 0.021f);
            ent.setTransformation(t);
        });
        anchor.addPassenger(bar);
        showEntityToPlayer(bar);
        durabilityBars.put(slot, bar);

        if (animationsEnabled && matches && !isSpawning) {
            fr.skynex.storagepeek.util.FoliaScheduler.runLater(StoragePeek.getInstance(), player, () -> {
                if (bg.isValid() && bar.isValid() && anchor != null && anchor.isValid()) {
                    bg.setInterpolationDelay(0);
                    bg.setInterpolationDuration(5);
                    Transformation tBg = bg.getTransformation();
                    tBg.getScale().set(0.12f, 0.015f, 0.001f);
                    bg.setTransformation(tBg);

                    bar.setInterpolationDelay(0);
                    bar.setInterpolationDuration(5);
                    Transformation tBar = bar.getTransformation();
                    tBar.getScale().set(0.11f * (float) percent, 0.01f, 0.002f);
                    bar.setTransformation(tBar);
                }
            }, 1L);
        }
    }

    private Material getDurabilityColor(double percent) {
        if (percent > 0.6)
            return plugin.getDurabilityColorHigh();
        if (percent > 0.3)
            return plugin.getDurabilityColorMedium();
        return plugin.getDurabilityColorLow();
    }

    private void destroyDurabilityBar(int slot) {
        BlockDisplay bg = durabilityBgs.remove(slot);
        if (bg != null)
            bg.remove();
        BlockDisplay bar = durabilityBars.remove(slot);
        if (bar != null)
            bar.remove();
    }

    private void buildAxes() {
        // Billboard.CENTER orients display entities so their local X = right, Y = up
        // (screen-space),
        // Z = toward viewer. We must mirror this coordinate frame server-side for
        // pixel-perfect
        // slot targeting without any visual entity manipulation.
        //
        // n = normalize(eye - center) = "viewDir" pointing FROM entity TO camera.
        // rightBillboard = normalize(cross(worldUp, n))
        // upBillboard = cross(n, rightBillboard)
        player.getLocation(tempLoc);
        tempLoc.setY(tempLoc.getY() + player.getEyeHeight());

        double nx = tempLoc.getX() - centerCache.getX();
        double ny = tempLoc.getY() - centerCache.getY();
        double nz = tempLoc.getZ() - centerCache.getZ();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (len < 0.0001)
            return;
        nx /= len;
        ny /= len;
        nz /= len;

        normalVec.setX(nx);
        normalVec.setY(ny);
        normalVec.setZ(nz);

        boolean lookingVertical = Math.abs(ny) > 0.95;
        if (lookingVertical) {
            // Degenerate case: cross(worldUp, n) is near zero, fall back to yaw-derived
            // right vector from the player's own facing direction.
            double rad = Math.toRadians(tempLoc.getYaw());
            rightVec.setX(-Math.cos(rad));
            rightVec.setY(0);
            rightVec.setZ(-Math.sin(rad));
        } else {
            // rightBillboard = normalize(cross(worldUp=(0,1,0), n=(nx,ny,nz)))
            // = normalize((1*nz-0*ny, 0*nx-0*nz, 0*ny-1*nx))
            // = normalize((nz, 0, -nx))
            // This equals the player's "right" when looking toward the display,
            // which matches child entity local +X when their yaw = anchor yaw.
            rightVec.setX(nz);
            rightVec.setY(0);
            rightVec.setZ(-nx);
        }
        rightVec.normalize();

        double rx = rightVec.getX();
        double ry = rightVec.getY();
        double rz = rightVec.getZ();

        // upBillboard = cross(n, rightBillboard)
        // cross(n, r) = (ny*rz - nz*ry, nz*rx - nx*rz, nx*ry - ny*rx)
        upVec.setX(ny * rz - nz * ry);
        upVec.setY(nz * rx - nx * rz);
        upVec.setZ(nx * ry - ny * rx);
        upVec.normalize();
    }

    /**
     * Sets the yaw/pitch of all child display entities (everything except the anchor).
     * In FIXED billboard mode, each entity's local axes are determined by its own
     * entity rotation. We must sync child yaw to the anchor's yaw so that their
     * local +X aligns with rightVec (= the player's right direction), preventing
     * the horizontal mirror effect.
     */
    private void setChildDisplayRotations(float yaw, float pitch) {
        if (background != null && background.isValid())
            background.setRotation(yaw, pitch);
        if (hoverHighlight != null && hoverHighlight.isValid())
            hoverHighlight.setRotation(yaw, pitch);
        if (hoverLabel != null && hoverLabel.isValid())
            hoverLabel.setRotation(yaw, pitch);
        if (fillIndicator != null && fillIndicator.isValid())
            fillIndicator.setRotation(yaw, pitch);
        for (ItemEntry entry : itemEntries)
            if (entry.display() != null && entry.display().isValid())
                entry.display().setRotation(yaw, pitch);
        for (TextDisplay d : fakeAmounts.values())
            if (d != null && d.isValid())
                d.setRotation(yaw, pitch);
        for (BlockDisplay d : durabilityBgs.values())
            if (d != null && d.isValid())
                d.setRotation(yaw, pitch);
        for (BlockDisplay d : durabilityBars.values())
            if (d != null && d.isValid())
                d.setRotation(yaw, pitch);
    }

    private void spawnInteractionEntity() {
        int rows = (int) Math.ceil((double) inventory.getSize() / columns);
        float bgWidth = (float) (columns * spacing);
        float bgHeight = (float) (rows * spacing);

        interactionEntity = centerCache.getWorld().spawn(centerCache, org.bukkit.entity.Interaction.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setInteractionWidth(bgWidth);
            ent.setInteractionHeight(bgHeight);
            ent.setVisibleByDefault(false);
        });
        showEntityToPlayer(interactionEntity);
    }

    public org.bukkit.entity.Interaction getInteractionEntity() {
        return interactionEntity;
    }

    // setFrozen is no longer needed with gyroscopic approach, but we keep the
    // boolean

    private void calculateLookOffsets() {
        if (!frozen) {
            lookX = 0;
            lookY = 0;
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

        // Compute tilted local axes in billboard frame
        org.joml.Vector3f lx = new org.joml.Vector3f(1, 0, 0);
        org.joml.Vector3f ly = new org.joml.Vector3f(0, 1, 0);
        org.joml.Vector3f lz = new org.joml.Vector3f(0, 0, 1);
        tiltRotation.transform(lx);
        tiltRotation.transform(ly);
        tiltRotation.transform(lz);

        // Convert billboard-frame tilted axes to world-space vectors
        double xPrimeX = lx.x * rightVec.getX() + lx.y * upVec.getX() + lx.z * nx;
        double xPrimeY = lx.x * rightVec.getY() + lx.y * upVec.getY() + lx.z * ny;
        double xPrimeZ = lx.x * rightVec.getZ() + lx.y * upVec.getZ() + lx.z * nz;

        double yPrimeX = ly.x * rightVec.getX() + ly.y * upVec.getX() + ly.z * nx;
        double yPrimeY = ly.x * rightVec.getY() + ly.y * upVec.getY() + ly.z * ny;
        double yPrimeZ = ly.x * rightVec.getZ() + ly.y * upVec.getZ() + ly.z * nz;

        double zPrimeX = lz.x * rightVec.getX() + lz.y * upVec.getX() + lz.z * nx;
        double zPrimeY = lz.x * rightVec.getY() + lz.y * upVec.getY() + lz.z * ny;
        double zPrimeZ = lz.x * rightVec.getZ() + lz.y * upVec.getZ() + lz.z * nz;

        double denom = zPrimeX * dx + zPrimeY * dy + zPrimeZ * dz;
        if (Math.abs(denom) < 0.0001) {
            lookX = 0;
            lookY = 0;
            return;
        }

        double t = ((cx - ex) * zPrimeX + (cy - ey) * zPrimeY + (cz - ez) * zPrimeZ) / denom;
        if (t < 0) {
            lookX = 0;
            lookY = 0;
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
        int rows = (int) Math.ceil((double) inventory.getSize() / columns);
        double halfWidth = (columns * spacing) / 2.0;
        double halfHeight = (rows * spacing) / 2.0;

        if (Math.abs(lookX) > halfWidth || Math.abs(lookY) > halfHeight)
            return -1;

        int col = (int) Math.floor((lookX + halfWidth) / spacing);
        int row = (int) Math.floor((halfHeight - lookY) / spacing);

        if (col < 0 || col >= columns || row < 0 || row >= rows)
            return -1;

        int slot = row * columns + col;
        return (slot < inventory.getSize()) ? slot : -1;
    }

    public void toggleFilter(Material material) {
        if (filterMaterial == material) {
            filterMaterial = null;
        } else {
            filterMaterial = material;
        }
        updateAllScales();
    }

    public void clearFilter() {
        if (filterMaterial != null) {
            filterMaterial = null;
            updateAllScales();
        }
    }

    public void updateAllScales() {
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null && entry.display().isValid()) {
                ItemStack item = entry.display().getItemStack();
                boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);
                float targetScale = matches ? (entry.slot() == hoveredSlot ? 0.22f : 0.15f) : 0.05f;

                Transformation t = entry.display().getTransformation();
                t.getScale().set(targetScale, targetScale, targetScale);
                entry.display().setInterpolationDelay(0);
                entry.display().setInterpolationDuration(4);
                entry.display().setTransformation(t);

                // Quantity Text display
                TextDisplay amtText = fakeAmounts.get(entry.slot());
                if (amtText != null && amtText.isValid()) {
                    Transformation tText = amtText.getTransformation();
                    if (matches) {
                        tText.getScale().set(textScale, textScale, textScale);
                    } else {
                        tText.getScale().set(0f, 0f, 0f);
                    }
                    amtText.setInterpolationDelay(0);
                    amtText.setInterpolationDuration(4);
                    amtText.setTransformation(tText);
                }

                // Durability Bar
                BlockDisplay dbBg = durabilityBgs.get(entry.slot());
                BlockDisplay dbBar = durabilityBars.get(entry.slot());
                if (dbBg != null && dbBg.isValid() && dbBar != null && dbBar.isValid()) {
                    Transformation tBg = dbBg.getTransformation();
                    Transformation tBar = dbBar.getTransformation();
                    if (matches) {
                        tBg.getScale().set(0.12f, 0.015f, 0.001f);
                        if (item != null && item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable meta) {
                            double percent = (double) (item.getType().getMaxDurability() - meta.getDamage())
                                    / item.getType().getMaxDurability();
                            tBar.getScale().set(0.11f * (float) percent, 0.01f, 0.002f);
                        }
                    } else {
                        tBg.getScale().set(0f, 0f, 0f);
                        tBar.getScale().set(0f, 0f, 0f);
                    }
                    dbBg.setInterpolationDelay(0);
                    dbBg.setInterpolationDuration(4);
                    dbBg.setTransformation(tBg);

                    dbBar.setInterpolationDelay(0);
                    dbBar.setInterpolationDuration(4);
                    dbBar.setTransformation(tBar);
                }
            }
        }
    }

    private void updateAllBillboards(Display.Billboard billboardMode) {
        if (anchor != null && anchor.isValid()) {
            anchor.setBillboard(billboardMode);
        }
        if (background != null && background.isValid()) {
            background.setBillboard(billboardMode);
        }
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null && entry.display().isValid()) {
                entry.display().setBillboard(billboardMode);
            }
        }
        if (hoverHighlight != null && hoverHighlight.isValid()) {
            hoverHighlight.setBillboard(billboardMode);
        }
        if (hoverLabel != null && hoverLabel.isValid()) {
            hoverLabel.setBillboard(billboardMode);
        }
        for (TextDisplay display : fakeAmounts.values()) {
            if (display != null && display.isValid()) {
                display.setBillboard(billboardMode);
            }
        }
        for (BlockDisplay bg : durabilityBgs.values()) {
            if (bg != null && bg.isValid()) {
                bg.setBillboard(billboardMode);
            }
        }
        for (BlockDisplay bar : durabilityBars.values()) {
            if (bar != null && bar.isValid()) {
                bar.setBillboard(billboardMode);
            }
        }
    }

    private void showEntityToPlayer(Entity entity) {
        if (entity != null) {
            if (isSpawning) {
                entitiesToShow.add(entity);
            } else {
                StoragePeek plugin = StoragePeek.getInstance();
                fr.skynex.storagepeek.util.FoliaScheduler.runTask(plugin, player, () -> {
                    if (entity.isValid() && player.isOnline()) {
                        player.showEntity(plugin, entity);
                    }
                });
            }
        }
    }

    public boolean isValid() {
        if (cleanedUp) {
            return false;
        }
        if (handSlot != null) {
            if (!player.isSneaking()) {
                return false;
            }
            ItemStack item = handSlot == org.bukkit.inventory.EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
            if (item == null || !item.getType().name().contains("SHULKER_BOX")) {
                return false;
            }
            return true;
        }
        StoragePeek plugin = StoragePeek.getInstance();
        if (block != null) {
            if (block.getType() == Material.AIR) {
                return false;
            }
            if (!plugin.getRaycastTask().getAllowedBlocks().contains(block.getType())
                    && !plugin.getHookManager().isCustomContainer(block)) {
                return false;
            }
            if (!plugin.getProtectionManager().canAccess(player, block.getLocation())) {
                return false;
            }
        }
        if (entity != null) {
            if (!entity.isValid()) {
                return false;
            }
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
