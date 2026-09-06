package fr.skynex.storagepeek.session;

import fr.skynex.storagepeek.StoragePeek;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

import java.util.function.Consumer;

public class SessionHeaderRenderer {

    private final StoragePeek plugin;

    private TextDisplay fillIndicator;
    private TextDisplay taglineBanner;
    private TextDisplay homeBanner;
    private TextDisplay lockIndicator;
    private TextDisplay pageBanner;

    public SessionHeaderRenderer(StoragePeek plugin) {
        this.plugin = plugin;
    }

    public void spawnHeaderBanners(TextDisplay anchor, Location centerCache, Inventory inventory, float bgHeight,
                                  Block block, Entity entity, Player player, int size, int currentPage,
                                  Consumer<Entity> showEntity) {
        boolean fillEnabled = plugin.getConfig().getBoolean("visualizers.fill-indicator", true)
                && plugin.getConfig().getBoolean("holograms.fill-indicator-enabled", true);
        if (fillEnabled) {
            int totalSlots = inventory != null ? inventory.getSize() : 0;
            int usedSlots = 0;
            if (inventory != null) {
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        usedSlots++;
                    }
                }
            }
            int fillPercent = totalSlots > 0 ? (usedSlots * 100 / totalSlots) : 0;
            String fillText = fillPercent >= 90
                ? "§c§l[⚠️ CONTAINER FULL - " + fillPercent + "%]"
                : "§7Storage Capacity: " + fillPercent + "%";
            Color bgColor = fillPercent >= 90
                ? Color.fromARGB(200, 180, 20, 20)
                : Color.fromARGB(120, 0, 0, 0);

            fillIndicator = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setDefaultBackground(true);
                ent.setBackgroundColor(bgColor);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.text(LegacyComponentSerializer.legacySection().deserialize(fillText));
                Transformation t = ent.getTransformation();
                t.getTranslation().set(0f, -bgHeight / 2f - 0.22f, 0.05f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(fillIndicator);
            showEntity.accept(fillIndicator);
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
                ent.setBackgroundColor(Color.fromARGB(160, 20, 20, 20));
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.text(LegacyComponentSerializer.legacySection().deserialize(tagline));
                Transformation t = ent.getTransformation();
                t.getTranslation().set(0f, bgHeight / 2f + 0.25f, 0.05f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(taglineBanner);
            showEntity.accept(taglineBanner);
        }

        if (plugin.getSethomeXHook() != null && plugin.getSethomeXHook().isActive() && block != null) {
            String homeName = plugin.getSethomeXHook().getNearbyHomeName(player, block.getLocation());
            if (homeName != null) {
                homeBanner = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                    plugin.tagDisplayEntity(ent);
                    ent.setVisibleByDefault(false);
                    ent.setBillboard(Display.Billboard.CENTER);
                    ent.setBrightness(new Display.Brightness(15, 15));
                    ent.setDefaultBackground(true);
                    ent.setBackgroundColor(Color.fromARGB(180, 20, 80, 160));
                    ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                    ent.text(LegacyComponentSerializer.legacySection().deserialize("§b🏠 [HOME CHEST: " + homeName + "]"));
                    Transformation t = ent.getTransformation();
                    t.getTranslation().set(0f, bgHeight / 2f + 0.65f, 0.05f);
                    ent.setTransformation(t);
                });
                anchor.addPassenger(homeBanner);
                showEntity.accept(homeBanner);
            }
        }

        if (plugin.getConfig().getBoolean("holograms.lock-indicator-enabled", true)) {
            boolean isProtectedArea = (block != null && !plugin.getProtectionManager().canAccess(player, block.getLocation()));
            String lockText = isProtectedArea ? "§c🔒 Locked §7(Protected)" : "§a🔓 Unlocked §7(Access Granted)";
            Color lockBg = isProtectedArea ? Color.fromARGB(180, 150, 20, 20) : Color.fromARGB(140, 20, 120, 20);

            lockIndicator = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setDefaultBackground(true);
                ent.setBackgroundColor(lockBg);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.text(LegacyComponentSerializer.legacySection().deserialize(lockText));
                Transformation t = ent.getTransformation();
                t.getTranslation().set(0f, bgHeight / 2f + 0.45f, 0.05f);
                t.getScale().set(0.8f, 0.8f, 0.8f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(lockIndicator);
            showEntity.accept(lockIndicator);
        }

        if (plugin.getConfig().getBoolean("holograms.pagination-enabled", true) && size > 27) {
            int totalPages = (int) Math.ceil((double) size / 27);
            String pageText = "§e◀ Page " + (currentPage + 1) + " / " + totalPages + " ▶  §7(/sp page next)";
            pageBanner = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setDefaultBackground(true);
                ent.setBackgroundColor(Color.fromARGB(160, 30, 30, 50));
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.text(LegacyComponentSerializer.legacySection().deserialize(pageText));
                Transformation t = ent.getTransformation();
                t.getTranslation().set(0f, -bgHeight / 2f - 0.42f, 0.05f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(pageBanner);
            showEntity.accept(pageBanner);
        }
    }

    public void setChildDisplayRotations(float yaw, float pitch) {
        if (fillIndicator != null && fillIndicator.isValid()) fillIndicator.setRotation(yaw, pitch);
        if (taglineBanner != null && taglineBanner.isValid()) taglineBanner.setRotation(yaw, pitch);
        if (homeBanner != null && homeBanner.isValid()) homeBanner.setRotation(yaw, pitch);
        if (lockIndicator != null && lockIndicator.isValid()) lockIndicator.setRotation(yaw, pitch);
        if (pageBanner != null && pageBanner.isValid()) pageBanner.setRotation(yaw, pitch);
    }

    public void animateScaleDown() {
        if (lockIndicator != null && lockIndicator.isValid()) {
            lockIndicator.setInterpolationDelay(0);
            lockIndicator.setInterpolationDuration(4);
            Transformation t = lockIndicator.getTransformation();
            t.getScale().set(0f, 0f, 0f);
            lockIndicator.setTransformation(t);
        }
        if (pageBanner != null && pageBanner.isValid()) {
            pageBanner.setInterpolationDelay(0);
            pageBanner.setInterpolationDuration(4);
            Transformation t = pageBanner.getTransformation();
            t.getScale().set(0f, 0f, 0f);
            pageBanner.setTransformation(t);
        }
    }

    public void removeAll() {
        if (fillIndicator != null) { fillIndicator.remove(); fillIndicator = null; }
        if (taglineBanner != null) { taglineBanner.remove(); taglineBanner = null; }
        if (homeBanner != null) { homeBanner.remove(); homeBanner = null; }
        if (lockIndicator != null) { lockIndicator.remove(); lockIndicator = null; }
        if (pageBanner != null) { pageBanner.remove(); pageBanner = null; }
    }

    public TextDisplay getFillIndicator() {
        return fillIndicator;
    }

    public TextDisplay getTaglineBanner() {
        return taglineBanner;
    }

    public TextDisplay getHomeBanner() {
        return homeBanner;
    }

    public TextDisplay getLockIndicator() {
        return lockIndicator;
    }

    public TextDisplay getPageBanner() {
        return pageBanner;
    }
}
