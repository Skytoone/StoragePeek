package fr.skynex.storagepeek.session;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DurabilityBarRenderer {

    private final StoragePeek plugin;
    private final PeekSession session;
    private final Map<Material, BlockData> blockDataCache;

    private final Map<Integer, BlockDisplay> durabilityBars = new HashMap<>();
    private final Map<Integer, BlockDisplay> durabilityBgs = new HashMap<>();

    public DurabilityBarRenderer(StoragePeek plugin, PeekSession session, Map<Material, BlockData> blockDataCache) {
        this.plugin = plugin;
        this.session = session;
        this.blockDataCache = blockDataCache;
    }

    public void spawnDurabilityBar(TextDisplay anchor, int slot, ItemStack item, float localX, float localY,
                                  Location centerCache, Player player, boolean animationsEnabled,
                                  boolean isSpawning, Consumer<Entity> showEntity) {
        if (item == null || item.getType().getMaxDurability() <= 0) return;
        if (!(item.getItemMeta() instanceof Damageable meta) || meta.getDamage() == 0) return;

        double percent = (double) (item.getType().getMaxDurability() - meta.getDamage()) / item.getType().getMaxDurability();
        Material barColor = getDurabilityColor(percent);
        Material filterMaterial = session.getFilterMaterial();
        boolean matches = (filterMaterial == null) || (item.getType() == filterMaterial);

        BlockDisplay bg = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setBlock(blockDataCache.computeIfAbsent(Material.BLACK_CONCRETE, Bukkit::createBlockData));
            ent.setBillboard(session.isFrozen() ? Display.Billboard.FIXED : Display.Billboard.CENTER);
            ent.setVisibleByDefault(false);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            float currentBgX = (animationsEnabled || !matches) ? 0f : 0.12f;
            float currentBgY = (animationsEnabled || !matches) ? 0f : 0.015f;
            float currentBgZ = (animationsEnabled || !matches) ? 0f : 0.001f;
            t.getScale().set(currentBgX, currentBgY, currentBgZ);
            t.getTranslation().set(localX - 0.06f, localY - 0.08f, 0.02f);
            ent.setTransformation(t);
        });
        anchor.addPassenger(bg);
        showEntity.accept(bg);
        durabilityBgs.put(slot, bg);

        BlockDisplay bar = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setBlock(blockDataCache.computeIfAbsent(barColor, Bukkit::createBlockData));
            ent.setBillboard(session.isFrozen() ? Display.Billboard.FIXED : Display.Billboard.CENTER);
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
        showEntity.accept(bar);
        durabilityBars.put(slot, bar);

        if (animationsEnabled && matches && !isSpawning) {
            FoliaScheduler.runLater(plugin, player, () -> {
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

    public void updateDurabilityBar(TextDisplay anchor, int slot, ItemStack item, float localX, float localY,
                                    Location centerCache, Player player, boolean animationsEnabled,
                                    boolean isSpawning, Consumer<Entity> showEntity) {
        if (!plugin.isDurabilityBarsEnabled() || item == null || item.getType().getMaxDurability() <= 0) {
            destroyDurabilityBar(slot);
            return;
        }
        if (!(item.getItemMeta() instanceof Damageable meta) || meta.getDamage() == 0) {
            destroyDurabilityBar(slot);
            return;
        }

        if (!durabilityBars.containsKey(slot)) {
            spawnDurabilityBar(anchor, slot, item, localX, localY, centerCache, player, animationsEnabled, isSpawning, showEntity);
        } else {
            BlockDisplay bar = durabilityBars.get(slot);
            if (bar != null && bar.isValid()) {
                double percent = (double) (item.getType().getMaxDurability() - meta.getDamage()) / item.getType().getMaxDurability();
                Material barColor = getDurabilityColor(percent);
                bar.setBlock(blockDataCache.computeIfAbsent(barColor, Bukkit::createBlockData));
                Transformation t = bar.getTransformation();

                Material filterMaterial = session.getFilterMaterial();
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
                Material filterMaterial = session.getFilterMaterial();
                boolean matches = (filterMaterial == null) || (item.getType() == filterMaterial);
                float targetBgX = matches ? 0.12f : 0f;
                float targetBgY = matches ? 0.015f : 0f;
                float targetBgZ = matches ? 0.001f : 0f;
                t.getScale().set(targetBgX, targetBgY, targetBgZ);
                bg.setTransformation(t);
            }
        }
    }

    public Material getDurabilityColor(double percent) {
        if (percent > 0.6) return plugin.getDurabilityColorHigh();
        if (percent > 0.3) return plugin.getDurabilityColorMedium();
        return plugin.getDurabilityColorLow();
    }

    public void destroyDurabilityBar(int slot) {
        BlockDisplay bg = durabilityBgs.remove(slot);
        if (bg != null) bg.remove();
        BlockDisplay bar = durabilityBars.remove(slot);
        if (bar != null) bar.remove();
    }

    public void updateScalesForEntry(PeekSessionDisplayManager.ItemEntry entry, Material filterMaterial) {
        BlockDisplay dbBg = durabilityBgs.get(entry.slot());
        BlockDisplay dbBar = durabilityBars.get(entry.slot());
        if (dbBg != null && dbBg.isValid() && dbBar != null && dbBar.isValid()) {
            ItemStack item = entry.display() != null ? entry.display().getItemStack() : null;
            boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);
            Transformation tBg = dbBg.getTransformation();
            Transformation tBar = dbBar.getTransformation();
            if (matches) {
                tBg.getScale().set(0.12f, 0.015f, 0.001f);
                if (item != null && item.getItemMeta() instanceof Damageable meta) {
                    double percent = (double) (item.getType().getMaxDurability() - meta.getDamage()) / item.getType().getMaxDurability();
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

    public void updateAllBillboards(Display.Billboard billboardMode) {
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

    public void setChildDisplayRotations(float yaw, float pitch) {
        for (BlockDisplay d : durabilityBgs.values()) {
            if (d != null && d.isValid()) d.setRotation(yaw, pitch);
        }
        for (BlockDisplay d : durabilityBars.values()) {
            if (d != null && d.isValid()) d.setRotation(yaw, pitch);
        }
    }

    public void animateScaleUp(Inventory inventory) {
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
            if (bar != null && bar.isValid() && inventory != null) {
                ItemStack item = inventory.getItem(entry.getKey());
                if (item != null && item.getItemMeta() instanceof Damageable meta) {
                    double percent = (double) (item.getType().getMaxDurability() - meta.getDamage()) / item.getType().getMaxDurability();
                    bar.setInterpolationDelay(0);
                    bar.setInterpolationDuration(5);
                    Transformation t = bar.getTransformation();
                    t.getScale().set(0.11f * (float) percent, 0.01f, 0.002f);
                    bar.setTransformation(t);
                }
            }
        }
    }

    public void animateScaleDown() {
        for (BlockDisplay bg : durabilityBgs.values()) {
            if (bg != null && bg.isValid()) {
                bg.setInterpolationDelay(0);
                bg.setInterpolationDuration(4);
                Transformation t = bg.getTransformation();
                t.getScale().set(0f, 0f, 0.01f);
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

    public void removeAll() {
        for (BlockDisplay bg : durabilityBgs.values()) {
            if (bg != null) bg.remove();
        }
        durabilityBgs.clear();

        for (BlockDisplay bar : durabilityBars.values()) {
            if (bar != null) bar.remove();
        }
        durabilityBars.clear();
    }

    public Map<Integer, BlockDisplay> getDurabilityBars() {
        return durabilityBars;
    }

    public Map<Integer, BlockDisplay> getDurabilityBgs() {
        return durabilityBgs;
    }
}
