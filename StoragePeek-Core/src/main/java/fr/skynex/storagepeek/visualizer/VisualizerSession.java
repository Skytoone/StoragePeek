package fr.skynex.storagepeek.visualizer;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

import java.util.*;

public class VisualizerSession {

    private final StoragePeek plugin;
    private final Block block;
    private final Inventory inventory;
    private final Set<UUID> viewers = new HashSet<>();
    private final List<Display> spawnedEntities = new ArrayList<>();
    private final Map<Integer, ItemDisplay> itemDisplays = new HashMap<>();
    private BlockDisplay progressBarBg;
    private BlockDisplay progressBarVal;
    private org.bukkit.entity.TextDisplay lecternTag;
    private boolean cleanedUp = false;
    private int tickCounter = 0;
    private org.bukkit.block.BlockFace blockFacing = org.bukkit.block.BlockFace.NORTH;

    public VisualizerSession(Block block, Inventory inventory) {
        this.plugin = StoragePeek.getInstance();
        this.block = block;
        this.inventory = inventory;

        BlockData data = block.getBlockData();
        if (data instanceof Directional directional) {
            this.blockFacing = directional.getFacing();
        }

        spawnDisplays();
    }

    public void addViewer(Player player) {
        if (viewers.add(player.getUniqueId())) {
            updateVisibilityForPlayer(player, true);
        }
    }

    public void removeViewer(Player player) {
        if (viewers.remove(player.getUniqueId())) {
            updateVisibilityForPlayer(player, false);
        }
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    private void updateVisibilityForPlayer(Player player, boolean show) {
        boolean visibleToAll = plugin.getConfig().getBoolean("visualizers.visible-to-all", true);
        if (visibleToAll) {
            return; // Managed globally by visibleByDefault
        }

        for (Display display : spawnedEntities) {
            if (display.isValid()) {
                if (show) {
                    player.showEntity(plugin, display);
                } else {
                    player.hideEntity(plugin, display);
                }
            }
        }
    }

    private void registerDisplay(Display display) {
        plugin.tagDisplayEntity(display);
        boolean visibleToAll = plugin.getConfig().getBoolean("visualizers.visible-to-all", true);
        display.setVisibleByDefault(visibleToAll);

        spawnedEntities.add(display);

        if (!visibleToAll) {
            for (UUID uuid : viewers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.showEntity(plugin, display);
                }
            }
        }
    }

    private Location getOffsetLoc(double localX, double localY, double localZ) {
        double x = 0;
        double z = 0;
        switch (blockFacing) {
            case NORTH:
                x = localX;
                z = localZ;
                break;
            case SOUTH:
                x = -localX;
                z = -localZ;
                break;
            case EAST:
                x = -localZ;
                z = localX;
                break;
            case WEST:
                x = localZ;
                z = -localX;
                break;
            default:
                x = localX;
                z = localZ;
                break;
        }
        Location loc = block.getLocation().add(0.5 + x, localY, 0.5 + z);
        float yaw = switch (blockFacing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
        loc.setYaw(yaw);
        return loc;
    }

    private void spawnDisplays() {
        Material blockType = block.getType();
        String typeName = blockType.name();

        if (typeName.contains("CRAFTING_TABLE")) {
            setupCraftingTableDisplays();
        } else if (typeName.contains("FURNACE") || typeName.contains("SMOKER")) {
            setupFurnaceDisplays();
        } else if (typeName.contains("BREWING_STAND")) {
            setupBrewingStandDisplays();
        } else if (typeName.contains("ANVIL")) {
            setupAnvilDisplays();
        } else if (typeName.contains("ENCHANTING_TABLE")) {
            setupEnchantingTableDisplays();
        } else if (typeName.contains("JUKEBOX")) {
            setupJukeboxDisplays();
        } else if (typeName.contains("LECTERN")) {
            setupLecternDisplays();
        } else if (typeName.contains("CHISELED_BOOKSHELF")) {
            setupChiseledBookshelfDisplays();
        }
    }

    private void setupCraftingTableDisplays() {
        // Spawns 9 items flat on the table, plus 1 result slot spinning above
        // Grid: slots 1 to 9
        for (int i = 1; i <= 9; i++) {
            int row = (i - 1) / 3;
            int col = (i - 1) % 3;
            double localX = (col - 1) * 0.22;
            double localZ = (row - 1) * 0.22;

            Location loc = getOffsetLoc(localX, 1.002, localZ);
            ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
                ent.setBillboard(Display.Billboard.FIXED);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                Transformation t = ent.getTransformation();
                t.getScale().set(0.16f, 0.16f, 0.16f);
                t.getLeftRotation().rotationX((float) Math.toRadians(-90));
                ent.setTransformation(t);
            });
            registerDisplay(display);
            itemDisplays.put(i, display);
        }

        // Result slot: slot 0
        Location resultLoc = getOffsetLoc(0.0, 1.25, 0.0);
        ItemDisplay resultDisplay = resultLoc.getWorld().spawn(resultLoc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.24f, 0.24f, 0.24f);
            ent.setTransformation(t);
        });
        registerDisplay(resultDisplay);
        itemDisplays.put(0, resultDisplay);
    }

    private void setupFurnaceDisplays() {
        // Input slot (0) - left
        Location inputLoc = getOffsetLoc(-0.2, 1.2, 0.0);
        ItemDisplay inputDisp = inputLoc.getWorld().spawn(inputLoc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.22f, 0.22f, 0.22f);
            ent.setTransformation(t);
        });
        registerDisplay(inputDisp);
        itemDisplays.put(0, inputDisp);

        // Output slot (2) - right
        Location outputLoc = getOffsetLoc(0.2, 1.2, 0.0);
        ItemDisplay outputDisp = outputLoc.getWorld().spawn(outputLoc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.22f, 0.22f, 0.22f);
            ent.setTransformation(t);
        });
        registerDisplay(outputDisp);
        itemDisplays.put(2, outputDisp);

        // Progress Bar Background
        Location barLoc = getOffsetLoc(0.0, 1.15, -0.42);
        progressBarBg = barLoc.getWorld().spawn(barLoc, BlockDisplay.class, ent -> {
            ent.setBlock(Bukkit.createBlockData(Material.BLACK_CONCRETE));
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.3f, 0.04f, 0.01f);
            t.getTranslation().set(-0.15f, 0f, 0f);
            ent.setTransformation(t);
        });
        registerDisplay(progressBarBg);

        // Progress Bar Value
        progressBarVal = barLoc.getWorld().spawn(barLoc, BlockDisplay.class, ent -> {
            ent.setBlock(Bukkit.createBlockData(Material.ORANGE_CONCRETE));
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0f, 0.04f, 0.012f);
            t.getTranslation().set(-0.15f, 0f, 0f);
            ent.setTransformation(t);
        });
        registerDisplay(progressBarVal);
    }

    private void setupBrewingStandDisplays() {
        // Bottles: slots 0, 1, 2
        // Base triangle offsets
        Location bottle0 = getOffsetLoc(-0.25, 0.2, -0.15);
        ItemDisplay b0 = bottle0.getWorld().spawn(bottle0, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.25f, 0.25f, 0.25f);
            ent.setTransformation(t);
        });
        registerDisplay(b0);
        itemDisplays.put(0, b0);

        Location bottle1 = getOffsetLoc(0.25, 0.2, -0.15);
        ItemDisplay b1 = bottle1.getWorld().spawn(bottle1, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.25f, 0.25f, 0.25f);
            ent.setTransformation(t);
        });
        registerDisplay(b1);
        itemDisplays.put(1, b1);

        Location bottle2 = getOffsetLoc(0.0, 0.2, 0.28);
        ItemDisplay b2 = bottle2.getWorld().spawn(bottle2, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.25f, 0.25f, 0.25f);
            ent.setTransformation(t);
        });
        registerDisplay(b2);
        itemDisplays.put(2, b2);

        // Ingredient: slot 3
        Location ingLoc = getOffsetLoc(0.0, 0.85, 0.0);
        ItemDisplay ingDisp = ingLoc.getWorld().spawn(ingLoc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.2f, 0.2f, 0.2f);
            ent.setTransformation(t);
        });
        registerDisplay(ingDisp);
        itemDisplays.put(3, ingDisp);
    }

    private void setupAnvilDisplays() {
        // Input slot 0 flat on anvil
        Location loc = getOffsetLoc(0.0, 1.01, 0.0);
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.35f, 0.35f, 0.35f);
            t.getLeftRotation().rotationX((float) Math.toRadians(-90));
            ent.setTransformation(t);
        });
        registerDisplay(display);
        itemDisplays.put(0, display);
    }

    private void setupEnchantingTableDisplays() {
        // Floating item: slot 0
        Location loc = getOffsetLoc(0.0, 1.08, 0.0);
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.25f, 0.25f, 0.25f);
            ent.setTransformation(t);
        });
        registerDisplay(display);
        itemDisplays.put(0, display);
    }

    private void setupJukeboxDisplays() {
        Location loc = getOffsetLoc(0.0, 1.05, 0.0);
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.24f, 0.24f, 0.24f);
            t.getLeftRotation().rotationX((float) Math.toRadians(-90));
            ent.setTransformation(t);
        });
        registerDisplay(display);
        itemDisplays.put(0, display);
    }

    private void setupLecternDisplays() {
        Location loc = getOffsetLoc(0.0, 1.05, 0.0);
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Transformation t = ent.getTransformation();
            t.getScale().set(0.28f, 0.28f, 0.28f);
            t.getLeftRotation().rotationXYZ((float) Math.toRadians(-60), 0.0f, 0.0f);
            ent.setTransformation(t);
        });
        registerDisplay(display);
        itemDisplays.put(0, display);

        Location tagLoc = getOffsetLoc(0.0, 1.35, 0.0);
        lecternTag = tagLoc.getWorld().spawn(tagLoc, org.bukkit.entity.TextDisplay.class, ent -> {
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));
            ent.text(net.kyori.adventure.text.Component.empty());
        });
        registerDisplay(lecternTag);
    }

    private void setupChiseledBookshelfDisplays() {
        for (int i = 0; i <= 5; i++) {
            int row = i / 3;
            int col = i % 3;
            double localX = (col - 1) * 0.27;
            // Row 0 is top row, Row 1 is bottom row
            double localY = (row == 0) ? 0.68 : 0.22;
            double localZ = -0.15; // Deeper inside the block

            Location loc = getOffsetLoc(localX, localY, localZ);
            ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
                ent.setBillboard(Display.Billboard.FIXED);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                Transformation t = ent.getTransformation();
                t.getScale().set(0.24f, 0.28f, 0.24f);
                ent.setTransformation(t);

                ent.setInterpolationDelay(0);
                ent.setInterpolationDuration(3);
            });
            registerDisplay(display);
            itemDisplays.put(i, display);
        }
    }

    public boolean isValid() {
        if (cleanedUp) return false;
        if (block == null || block.getType() == Material.AIR) return false;
        return true;
    }

    public void update() {
        if (cleanedUp)
            return;
        if (!isValid()) {
            cleanup();
            return;
        }
        tickCounter++;

        // Sync Item displays
        if (inventory != null) {
            for (Map.Entry<Integer, ItemDisplay> entry : itemDisplays.entrySet()) {
                int slot = entry.getKey();
                ItemDisplay display = entry.getValue();
                if (display.isValid()) {
                    ItemStack item = inventory.getItem(slot);
                    ItemStack currentDisplayItem = display.getItemStack();

                    if (item == null || item.getType() == Material.AIR) {
                        if (currentDisplayItem != null && currentDisplayItem.getType() != Material.AIR) {
                            display.setItemStack(null);
                        }
                    } else if (currentDisplayItem == null
                            || currentDisplayItem.getType() == Material.AIR
                            || !item.isSimilar(currentDisplayItem)
                            || item.getAmount() != currentDisplayItem.getAmount()) {
                        display.setItemStack(item.clone());
                    }

                    // Apply slow rotation to floating slots (Crafting Result, Furnace slots,
                    // Enchanting slot)
                    if (slot == 0 || (block.getType().name().contains("FURNACE")
                            || block.getType().name().contains("SMOKER"))) {
                        Transformation t = display.getTransformation();
                        float angle = (float) (System.currentTimeMillis() / 400.0);

                        if (block.getType().name().contains("CRAFTING_TABLE") && slot == 0) {
                            t.getLeftRotation().rotationY(angle);
                        } else if (block.getType().name().contains("ENCHANTING_TABLE")) {
                            t.getLeftRotation().rotationY(angle);
                        } else if (block.getType().name().contains("FURNACE")
                                || block.getType().name().contains("SMOKER")) {
                            t.getLeftRotation().rotationY(angle);
                        }
                        display.setTransformation(t);
                    }
                }
            }
        }

        // Handle Furnace progression
        if (block.getState() instanceof Furnace furnace) {
            double cookProgress = 0.0;
            int cookTotal = furnace.getCookTimeTotal();
            if (cookTotal > 0) {
                cookProgress = (double) furnace.getCookTime() / cookTotal;
            }

            boolean isCooking = cookProgress > 0.0;
            if (progressBarBg != null && progressBarBg.isValid()) {
                progressBarBg.setVisibleByDefault(
                        isCooking && plugin.getConfig().getBoolean("visualizers.visible-to-all", true));
            }

            if (progressBarVal != null && progressBarVal.isValid()) {
                progressBarVal.setVisibleByDefault(
                        isCooking && plugin.getConfig().getBoolean("visualizers.visible-to-all", true));

                if (isCooking) {
                    Transformation t = progressBarVal.getTransformation();
                    float width = (float) (0.3f * cookProgress);
                    t.getScale().set(width, 0.04f, 0.012f);
                    t.getTranslation().set(-0.15f, 0f, 0f);
                    progressBarVal.setTransformation(t);

                    // Spawn small smoke/flame particles
                    if (tickCounter % 5 == 0) {
                        block.getWorld().spawnParticle(Particle.FLAME, getOffsetLoc(0.0, 1.05, 0.0), 1, 0.1, 0.02, 0.1,
                                0.01);
                    }
                }
            }
        }

        // Handle Brewing Stand particles
        if (block.getState() instanceof BrewingStand stand) {
            boolean hasFuel = stand.getFuelLevel() > 0;
            if (hasFuel && tickCounter % 6 == 0) {
                // Flame particles at the base of the brewing stand
                block.getWorld().spawnParticle(Particle.SMALL_FLAME, getOffsetLoc(0.0, 0.12, 0.0), 1, 0.12, 0.02, 0.12,
                        0.01);
            }
            if (stand.getBrewingTime() > 0 && tickCounter % 4 == 0) {
                // Rising bubble and potion vapor effects
                block.getWorld().spawnParticle(Particle.BUBBLE, getOffsetLoc(0.0, 0.5, 0.0), 4, 0.15, 0.2, 0.15, 0.02);
                block.getWorld().spawnParticle(Particle.EFFECT, getOffsetLoc(0.0, 0.5, 0.0), 2, 0.12, 0.2, 0.12, 0.01);
            }
        }

        // Handle Enchanting table glyph particles
        if (block.getType() == Material.ENCHANTING_TABLE && tickCounter % 3 == 0 && inventory != null) {
            ItemStack item = inventory.getItem(0);
            if (item != null && item.getType() != Material.AIR) {
                Location center = getOffsetLoc(0.0, 1.1, 0.0);
                double angle = (System.currentTimeMillis() / 250.0);
                double r = 0.35;
                double px = Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location particleLoc = center.clone().add(px, 0, pz);
                particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        // Handle Jukebox ticking
        if (block.getType() == Material.JUKEBOX && block.getState() instanceof org.bukkit.block.Jukebox jb) {
            ItemStack record = jb.getRecord();
            ItemDisplay display = itemDisplays.get(0);
            if (display != null && display.isValid()) {
                ItemStack currentDisplayItem = display.getItemStack();
                boolean dispEmpty = currentDisplayItem == null || currentDisplayItem.getType() == Material.AIR;
                if (record == null || record.getType() == Material.AIR) {
                    if (!dispEmpty)
                        display.setItemStack(null);
                } else {
                    if (dispEmpty || !record.isSimilar(currentDisplayItem)) {
                        display.setItemStack(record.clone());
                    }
                    Transformation t = display.getTransformation();
                    float angle = (float) (System.currentTimeMillis() / 300.0);
                    t.getLeftRotation().rotationXYZ((float) Math.toRadians(-90), 0.0f, angle);
                    display.setTransformation(t);

                    if (jb.isPlaying() && tickCounter % 4 == 0) {
                        block.getWorld().spawnParticle(Particle.NOTE, getOffsetLoc(0.0, 1.15, 0.0), 1, 0.15, 0.1, 0.15,
                                0.1);
                    }
                }
            }
        }

        // Handle Lectern ticking
        if (block.getType() == Material.LECTERN && block.getState() instanceof org.bukkit.block.Lectern lectern) {
            ItemStack book = lectern.getInventory().getItem(0);
            ItemDisplay display = itemDisplays.get(0);
            boolean hasBook = book != null && book.getType() != Material.AIR;

            if (display != null && display.isValid()) {
                if (!hasBook) {
                    display.setItemStack(null);
                } else {
                    ItemStack current = display.getItemStack();
                    if (current == null || book == null || current.getType() != book.getType()) {
                        display.setItemStack(new ItemStack(Material.WRITTEN_BOOK));
                    }
                }
            }

            if (lecternTag != null && lecternTag.isValid()) {
                if (!hasBook) {
                    lecternTag.text(net.kyori.adventure.text.Component.empty());
                } else {
                    String title = "Book";
                    int totalPages = 1;
                    if (book != null && book.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta bookMeta) {
                        if (bookMeta.getTitle() != null && !bookMeta.getTitle().isEmpty()) {
                            title = bookMeta.getTitle();
                        }
                        totalPages = bookMeta.getPageCount();
                    }
                    int currentPage = lectern.getPage() + 1;
                    lecternTag.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§e" + title + "\n§7Page " + currentPage + "/" + totalPages));
                }
            }
        }

        // Handle Chiseled Bookshelf ticking & selection animation
        if (block.getType() == Material.CHISELED_BOOKSHELF && inventory != null) {
            boolean anySneaking = false;
            for (UUID uuid : viewers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && p.isSneaking()) {
                    anySneaking = true;
                    break;
                }
            }

            float targetZ = anySneaking ? 0.08f : 0f;

            for (Map.Entry<Integer, ItemDisplay> entry : itemDisplays.entrySet()) {
                int slot = entry.getKey();
                ItemDisplay display = entry.getValue();
                if (display.isValid()) {
                    ItemStack book = inventory.getItem(slot);
                    ItemStack currentDisplayItem = display.getItemStack();
                    boolean dispEmpty = currentDisplayItem == null || currentDisplayItem.getType() == Material.AIR;

                    if (book == null || book.getType() == Material.AIR) {
                        if (!dispEmpty) {
                            display.setItemStack(null);
                        }
                    } else if (dispEmpty || !book.isSimilar(currentDisplayItem)) {
                        display.setItemStack(book.clone());
                    }

                    Transformation t = display.getTransformation();
                    t.getTranslation().set(0f, 0f, targetZ);
                    display.setTransformation(t);
                }
            }
        }
    }

    public void cleanup() {
        if (cleanedUp)
            return;
        cleanedUp = true;

        for (Display display : spawnedEntities) {
            if (display != null) {
                display.remove();
            }
        }
        spawnedEntities.clear();
        itemDisplays.clear();
    }
}
