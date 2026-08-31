package fr.skynex.storagepeek.hook;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TownyHook implements ProtectionHook {
    @Override
    public boolean canAccess(Player player, Location loc) {
        return PlayerCacheUtil.getCachePermission(player, loc, loc.getBlock().getType(), TownyPermission.ActionType.SWITCH);
    }
}
