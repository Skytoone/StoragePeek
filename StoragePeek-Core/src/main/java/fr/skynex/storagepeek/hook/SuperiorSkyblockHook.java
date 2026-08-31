package fr.skynex.storagepeek.hook;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SuperiorSkyblockHook implements ProtectionHook {

    // All known privilege names used by SuperiorSkyblock2 configs
    private static final String[] PRIVILEGE_NAMES = {
        "CHEST", "CONTAINER", "OPEN_CONTAINERS", "CHEST_ACCESS", "USE_CHEST", "CONTAINERS"
    };

    @Override
    public boolean canAccess(Player player, Location loc) {
        Island island = SuperiorSkyblockAPI.getIslandAt(loc);
        // No island at this location — allow
        if (island == null) return true;

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);

        // Primary check: the player must be the owner, a member, or co-op of this island.
        // Visitors are ALWAYS denied — "being allowed to open a chest on a visit" (island privilege)
        // does NOT mean they should spy via StoragePeek hologram.
        boolean isPartOfIsland = island.getOwner().equals(superiorPlayer)
            || island.isMember(superiorPlayer)
            || island.isCoop(superiorPlayer);

        if (!isPartOfIsland) {
            return false;
        }

        // Secondary check: among members/coop, respect the island's chest privilege if it exists.
        // For example, a co-op player may not have chest access on a given island.
        for (String privName : PRIVILEGE_NAMES) {
            IslandPrivilege priv = IslandPrivilege.getByName(privName);
            if (priv != null) {
                return island.hasPermission(superiorPlayer, priv);
            }
        }

        // If no chest privilege is defined, allow members/owner/coop
        return true;
    }
}
