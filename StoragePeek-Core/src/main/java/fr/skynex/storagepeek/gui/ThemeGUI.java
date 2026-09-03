package fr.skynex.storagepeek.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ThemeGUI {

    public static void openThemesMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, Component.text("StoragePeek - Themes"));

        gui.setItem(0, createGuiItem(Material.GLASS, "§fDefault", "§7Standard translucent stained glass."));
        gui.setItem(1, createGuiItem(Material.ENDER_PEARL, "§dEnder", "§7Obsidian background with portal particles."));
        gui.setItem(2, createGuiItem(Material.GOLD_INGOT, "§6Rich", "§7Gold block background with gold sparkle particles."));
        gui.setItem(3, createGuiItem(Material.PRISMARINE_SHARD, "§bAqua", "§7Dark prismarine background with bubbles."));
        gui.setItem(4, createGuiItem(Material.NETHERRACK, "§cNether", "§7Netherrack background with flames."));
        gui.setItem(5, createGuiItem(Material.GLOW_INK_SAC, "§aNeon", "§7Dark background with neon glow particles."));
        gui.setItem(6, createGuiItem(Material.WARPED_FUNGUS, "§3Cyberpunk", "§7Warped wart background with warped spores."));
        gui.setItem(7, createGuiItem(Material.RED_DYE, "§dRainbow", "§7Glazed terracotta background with cherry petals."));

        player.openInventory(gui);
    }

    private static ItemStack createGuiItem(Material mat, String name, String description) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(Arrays.asList(Component.text(description)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
