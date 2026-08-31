package fr.skynex.storagepeek.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class BentoBoxHook implements ProtectionHook {
    @Override
    public boolean canAccess(Player player, Location loc) {
        Island island = BentoBox.getInstance().getIslands().getIslandAt(loc).orElse(null);
        if (island != null) {
            return island.isAllowed(User.getInstance(player),
                    BentoBox.getInstance().getFlagsManager().getFlag("CONTAINER").orElse(null));
        }
        return true;
    }
}
