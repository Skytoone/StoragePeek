package fr.skynex.storagepeek.hook;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public class LootGlowHook {

    private boolean active = false;
    private Object apiInstance = null;
    private Method detectItemRarityMethod = null;

    public LootGlowHook() {
        init();
    }

    public void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("LootGlow")) {
            try {
                Class<?> hookClass = Class.forName("fr.skynex.lootglow.api.util.LootGlowHook");
                Method getApiMethod = hookClass.getMethod("getAPI");
                Object opt = getApiMethod.invoke(null);
                if (opt instanceof Optional<?> optional && optional.isPresent()) {
                    apiInstance = optional.get();
                    Class<?> apiInterface = Class.forName("fr.skynex.lootglow.api.LootGlowAPI");
                    detectItemRarityMethod = apiInterface.getMethod("detectItemRarity", ItemStack.class);
                    active = true;
                    Bukkit.getLogger().info("[StoragePeek] Successfully hooked into LootGlow API!");
                }
            } catch (Throwable t) {
                active = false;
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public String getItemRarity(ItemStack item) {
        if (!active || apiInstance == null || detectItemRarityMethod == null || item == null) {
            return null;
        }
        try {
            Object result = detectItemRarityMethod.invoke(apiInstance, item);
            return result instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public Color getRarityColor(ItemStack item) {
        String rarity = getItemRarity(item);
        if (rarity == null) return null;
        return switch (rarity.toUpperCase()) {
            case "MYTHIC" -> Color.fromRGB(255, 50, 50);       // Crimson Red
            case "LEGENDARY" -> Color.fromRGB(255, 170, 0);   // Amber / Gold
            case "EPIC" -> Color.fromRGB(170, 0, 170);       // Dark Purple
            case "RARE" -> Color.fromRGB(85, 255, 255);       // Aqua / Cyan
            case "UNCOMMON" -> Color.fromRGB(85, 255, 85);    // Lime Green
            default -> Color.fromRGB(240, 240, 240);          // White
        };
    }

    public String formatRarityHoverLabel(ItemStack item, String defaultName) {
        if (!active || item == null) return defaultName;
        String rarity = getItemRarity(item);
        if (rarity == null) return defaultName;
        String prefix = switch (rarity.toUpperCase()) {
            case "MYTHIC" -> "§c[★ MYTHIC ★] ";
            case "LEGENDARY" -> "§6[★ LEGENDARY ★] ";
            case "EPIC" -> "§d[★ EPIC ★] ";
            case "RARE" -> "§b[★ RARE ★] ";
            case "UNCOMMON" -> "§a[★ UNCOMMON ★] ";
            default -> "§f";
        };
        return prefix + defaultName;
    }

    public boolean isHighRarity(ItemStack item) {
        String rarity = getItemRarity(item);
        if (rarity == null) return false;
        String r = rarity.toUpperCase();
        return r.equals("MYTHIC") || r.equals("LEGENDARY");
    }

    public void triggerQuickActionParticles(Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null) return;
        org.bukkit.World world = from.getWorld();
        org.bukkit.util.Vector vec = to.toVector().subtract(from.toVector());
        double length = vec.length();
        if (length < 0.1) return;
        org.bukkit.util.Vector step = vec.clone().normalize().multiply(0.25);
        int points = (int) (length / 0.25);

        for (int i = 0; i < Math.min(30, points); i++) {
            Location pLoc = from.clone().add(step.clone().multiply(i));
            world.spawnParticle(Particle.END_ROD, pLoc, 1, 0.02, 0.02, 0.02, 0.01);
            world.spawnParticle(Particle.WAX_OFF, pLoc, 1, 0.04, 0.04, 0.04, 0.02);
        }
    }

    public void triggerItemPopJump(Location loc, ItemStack itemStack) {
        if (loc == null || loc.getWorld() == null || itemStack == null) return;
        org.bukkit.World world = loc.getWorld();
        Location spawnLoc = loc.clone().add(0, 0.2, 0);
        
        try {
            org.bukkit.entity.Item itemEntity = world.spawn(spawnLoc, org.bukkit.entity.Item.class, ent -> {
                ent.setItemStack(itemStack.clone());
                ent.setPickupDelay(32767);
                ent.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.15, 0.32, (Math.random() - 0.5) * 0.15));
            });
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 6, 0.15, 0.15, 0.15, 0.04);
            
            fr.skynex.storagepeek.util.FoliaScheduler.runLater(StoragePeek.getInstance(), itemEntity, () -> {
                if (itemEntity.isValid()) itemEntity.remove();
            }, 10L);
        } catch (Throwable ignored) {}
    }

    public void triggerMagnetAbsorptionEffect(Location playerLoc, Location chestLoc) {
        if (playerLoc == null || chestLoc == null || playerLoc.getWorld() == null) return;
        org.bukkit.World world = playerLoc.getWorld();
        org.bukkit.util.Vector vec = chestLoc.toVector().subtract(playerLoc.toVector());
        double dist = vec.length();
        if (dist < 0.1) return;
        org.bukkit.util.Vector dir = vec.normalize().multiply(0.4);
        int steps = (int) (dist / 0.4);

        for (int i = 0; i < Math.min(25, steps); i++) {
            Location pLoc = playerLoc.clone().add(dir.clone().multiply(i));
            world.spawnParticle(Particle.GLOW, pLoc, 2, 0.1, 0.1, 0.1, 0.02);
            world.spawnParticle(Particle.PORTAL, pLoc, 1, 0.05, 0.05, 0.05, 0.05);
        }
    }

    public void spawnMythicVaultAura(Location loc, String highestRarity) {
        if (loc == null || loc.getWorld() == null) return;
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        Particle particle = "MYTHIC".equalsIgnoreCase(highestRarity) ? Particle.TOTEM_OF_UNDYING : Particle.SOUL_FIRE_FLAME;
        center.getWorld().spawnParticle(particle, center, 4, 0.35, 0.35, 0.35, 0.05);
    }

    public void spawnLootGlowBeaconBeam(Location loc, Color beamColor) {
        if (loc == null || loc.getWorld() == null) return;
        Location start = loc.clone().add(0.5, 1.0, 0.5);
        Color color = beamColor != null ? beamColor : Color.fromRGB(255, 215, 0);

        for (double y = 0; y < 14.0; y += 0.5) {
            Location pLoc = start.clone().add(0, y, 0);
            start.getWorld().spawnParticle(Particle.END_ROD, pLoc, 2, 0.1, 0.1, 0.1, 0.01);
            start.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(color, 1.2f));
        }
    }
}
