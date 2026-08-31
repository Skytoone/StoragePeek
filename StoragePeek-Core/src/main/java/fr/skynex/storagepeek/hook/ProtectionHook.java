package fr.skynex.storagepeek.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ProtectionHook {
    boolean canAccess(Player player, Location loc);
}
