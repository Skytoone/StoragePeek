package fr.skynex.storagepeek.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;

public class GriefDefenderHook implements ProtectionHook {

    private Method getCoreMethod;
    private Method getClaimAtMethod;
    private Method isUserTrustedMethod;
    private Object containerTrustType;

    public GriefDefenderHook() {
        try {
            Class<?> gdClass = Class.forName("com.griefdefender.api.GriefDefender");
            getCoreMethod = gdClass.getMethod("getCore");
            Object gdCore = getCoreMethod.invoke(null);
            
            getClaimAtMethod = gdCore.getClass().getMethod("getClaimAt", Location.class);
            
            Class<?> claimClass = Class.forName("com.griefdefender.api.claim.Claim");
            Class<?> trustTypeClass = Class.forName("com.griefdefender.api.claim.TrustType");
            isUserTrustedMethod = claimClass.getMethod("isUserTrusted", UUID.class, trustTypeClass);
            
            Class<?> trustTypesClass = Class.forName("com.griefdefender.api.claim.TrustTypes");
            containerTrustType = trustTypesClass.getField("CONTAINER").get(null);
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[StoragePeek] Failed to initialize GriefDefender reflection hook: " + t.getMessage());
        }
    }

    @Override
    public boolean canAccess(Player player, Location loc) {
        if (getCoreMethod == null || getClaimAtMethod == null || isUserTrustedMethod == null || containerTrustType == null) {
            return true;
        }
        try {
            Object gdCore = getCoreMethod.invoke(null);
            if (gdCore != null) {
                Object claim = getClaimAtMethod.invoke(gdCore, loc);
                if (claim != null) {
                    boolean trusted = (boolean) isUserTrustedMethod.invoke(claim, player.getUniqueId(), containerTrustType);
                    return trusted;
                }
            }
        } catch (Throwable ignored) {}
        return true;
    }
}
