package fr.skynex.storagepeek.hook;

import fr.skynex.storagepeek.StoragePeek;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LandsHook implements ProtectionHook {
    private final LandsIntegration landsIntegration;

    public LandsHook() {
        this.landsIntegration = LandsIntegration.of(StoragePeek.getInstance());
    }

    @Override
    public boolean canAccess(Player player, Location loc) {
        Area area = landsIntegration.getArea(loc);
        if (area != null) {
            RoleFlag interactFlag = landsIntegration.getFlagRegistry().getRole("interact_container");
            return interactFlag == null || area.hasRoleFlag(player.getUniqueId(), interactFlag);
        }
        return true;
    }
}
