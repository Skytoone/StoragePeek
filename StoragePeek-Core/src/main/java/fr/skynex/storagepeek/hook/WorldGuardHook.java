package fr.skynex.storagepeek.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook implements ProtectionHook {
    @Override
    public boolean canAccess(Player player, Location loc) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld())) {
            return true;
        }
        // Use queryState instead of testState:
        // testState() returns false when no region/flag is set (blocks everyone without a region)
        // queryState() returns null when the flag is unset — we only block on explicit DENY
        StateFlag.State state = query.queryState(BukkitAdapter.adapt(loc), localPlayer, Flags.CHEST_ACCESS);
        return state != StateFlag.State.DENY;
    }
}

