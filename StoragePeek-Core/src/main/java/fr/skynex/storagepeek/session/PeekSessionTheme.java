package fr.skynex.storagepeek.session;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PeekSessionTheme {

    public enum Theme {
        DEFAULT, ENDER, RICH, AQUA, NETHER, NEON, CYBERPUNK, RAINBOW
    }

    private Theme theme = Theme.DEFAULT;
    private Material backgroundMaterial;

    private boolean themesEnabled;
    private boolean enderThemeEnabled;
    private Material enderBackground;
    private boolean enderParticles;

    private boolean richThemeEnabled;
    private Material richBackground;
    private boolean richParticles;
    private int richThreshold;

    private final Set<Material> preciousMaterials = new HashSet<>();
    private final Map<String, Material> customBackgrounds = new HashMap<>();
    private String shulkerBgType;

    public PeekSessionTheme(StoragePeek plugin) {
        this.backgroundMaterial = plugin.getDefaultBackground();
        loadThemeConfig(plugin);
    }

    public void loadThemeConfig(StoragePeek plugin) {
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

        this.shulkerBgType = plugin.getShulkerBgType();
        this.customBackgrounds.clear();
        this.customBackgrounds.putAll(plugin.getCustomBackgrounds());
    }

    public void setupDynamics(StoragePeek plugin, Player player, Block block, Entity entity, EquipmentSlot handSlot, Inventory inventory) {
        if (inventory == null || !themesEnabled) {
            return;
        }

        // Load custom player theme if set
        org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
        String pTheme = pdc.get(plugin.getThemeKey(), org.bukkit.persistence.PersistentDataType.STRING);
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
            ItemStack item = handSlot == EquipmentSlot.HAND ? 
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
            case ENDER -> backgroundMaterial = Material.OBSIDIAN;
            case RICH -> backgroundMaterial = Material.GOLD_BLOCK;
            case AQUA -> backgroundMaterial = Material.DARK_PRISMARINE;
            case NETHER -> backgroundMaterial = Material.NETHERRACK;
            case NEON -> backgroundMaterial = Material.BLACK_CONCRETE;
            case CYBERPUNK -> backgroundMaterial = Material.WARPED_WART_BLOCK;
            case RAINBOW -> backgroundMaterial = Material.MAGENTA_GLAZED_TERRACOTTA;
            default -> {}
        }
    }

    public Theme getTheme() {
        return theme;
    }

    public Material getBackgroundMaterial() {
        return backgroundMaterial;
    }

    public boolean isEnderParticles() {
        return enderParticles;
    }

    public boolean isRichParticles() {
        return richParticles;
    }
}
