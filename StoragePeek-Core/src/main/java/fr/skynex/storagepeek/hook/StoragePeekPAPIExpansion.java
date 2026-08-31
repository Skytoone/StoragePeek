package fr.skynex.storagepeek.hook;

import fr.skynex.storagepeek.StoragePeek;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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

        switch (identifier.toLowerCase()) {
            case "session_active":
                return plugin.getActiveSessions().containsKey(player.getUniqueId()) ? "Yes" : "No";

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

            default:
                return null;
        }
    }
}
