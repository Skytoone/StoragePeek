package fr.skynex.storagepeek.hook;

import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ResidenceHook implements ProtectionHook {
    @Override
    public boolean canAccess(Player player, Location loc) {
        return FlagPermissions.has(loc, player, Flags.container, true);
    }
}
