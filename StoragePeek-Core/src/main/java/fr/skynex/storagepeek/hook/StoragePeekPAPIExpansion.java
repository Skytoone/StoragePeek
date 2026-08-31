package fr.skynex.storagepeek.hook;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl;
import fr.skynex.storagepeek.session.PeekSession;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StoragePeekPAPIExpansion extends PlaceholderExpansion {

    private final StoragePeek plugin;

    public StoragePeekPAPIExpansion(@NotNull StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Skynex";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "storagepeek";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        StoragePeekAPIImpl apiImpl = (StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();

        switch (identifier.toLowerCase()) {
            case "session_active":
                return session != null ? "Yes" : "No";

            case "disabled":
                return String.valueOf(plugin.getDisabledPlayers().contains(player.getUniqueId()));

            case "active_theme":
                String theme = "DEFAULT";
                org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
                org.bukkit.NamespacedKey themeKey = new org.bukkit.NamespacedKey(plugin, "theme");
                if (pdc.has(themeKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    theme = pdc.get(themeKey, org.bukkit.persistence.PersistentDataType.STRING);
                }
                return theme != null ? theme.toUpperCase() : "DEFAULT";

            case "nearest_chest_distance":
                Location target = plugin.getRaycastTask().getCompassTarget(player);
                if (target != null && target.getWorld() != null && target.getWorld().equals(player.getWorld())) {
                    double dist = player.getLocation().distance(target);
                    return String.format("%.1fm", dist);
                }
                return "N/A";

            case "session_block_type":
                if (session != null && session.getBlock() != null) {
                    return session.getBlock().getType().name();
                }
                return "NONE";

            case "session_page":
                if (session != null) {
                    return String.valueOf(apiImpl.getSessionPage(player) + 1);
                }
                return "0";

            case "session_item_count":
                if (session != null) {
                    Inventory inv = session.getInventory();
                    if (inv != null) {
                        int count = 0;
                        for (ItemStack item : inv.getContents()) {
                            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                                count += item.getAmount();
                            }
                        }
                        return String.valueOf(count);
                    }
                }
                return "0";

            case "session_total_value":
                if (session != null && session.getBlock() != null) {
                    double val = apiImpl.getContainerTotalValue(session.getBlock(), player);
                    return String.format("%.2f", val);
                }
                return "0.00";

            case "session_tagline":
                if (session != null && session.getBlock() != null) {
                    String tagline = apiImpl.getContainerTagline(session.getBlock(), session.getEntity());
                    return tagline != null ? tagline : "";
                }
                return "";

            case "session_frozen":
                if (session != null) {
                    return String.valueOf(session.isFrozen());
                }
                return "false";

            default:
                return null;
        }
    }
}
