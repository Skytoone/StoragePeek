package fr.skynex.storagepeek.hook;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LWCHook implements ProtectionHook {
    @Override
    public boolean canAccess(Player player, Location loc) {
        Protection protection = LWC.getInstance().findProtection(loc.getBlock());
        if (protection != null) {
            return LWC.getInstance().canAccessProtection(player, protection);
        }
        return true;
    }
}
