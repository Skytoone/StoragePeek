package fr.skynex.storagepeek.hook;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventionHook implements ProtectionHook {
    @Override
    public boolean canAccess(Player player, Location loc) {
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, false, null);
        if (claim != null) {
            return claim.checkPermission(player, ClaimPermission.Inventory, null) == null;
        }
        return true;
    }
}
