package fr.skynex.storagepeek.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;

public class PlotSquaredHook implements ProtectionHook {

    private Method adaptLocationMethod;
    private Method getPlotMethod;
    private Method isAddedMethod;
    private Method isOwnerMethod;

    public PlotSquaredHook() {
        try {
            Class<?> bukkitAdapterClass = Class.forName("com.plotsquared.bukkit.util.BukkitAdapter");
            adaptLocationMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.Location.class);
            
            Class<?> psLocationClass = Class.forName("com.plotsquared.core.location.Location");
            getPlotMethod = psLocationClass.getMethod("getPlot");
            
            Class<?> plotClass = Class.forName("com.plotsquared.core.plot.Plot");
            isAddedMethod = plotClass.getMethod("isAdded", UUID.class);
            isOwnerMethod = plotClass.getMethod("isOwner", UUID.class);
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[StoragePeek] Failed to initialize PlotSquared reflection hook: " + t.getMessage());
        }
    }

    @Override
    public boolean canAccess(Player player, Location loc) {
        if (adaptLocationMethod == null || getPlotMethod == null || isAddedMethod == null || isOwnerMethod == null) {
            return true;
        }
        try {
            Object psLoc = adaptLocationMethod.invoke(null, loc);
            if (psLoc != null) {
                Object plot = getPlotMethod.invoke(psLoc);
                if (plot != null) {
                    UUID uuid = player.getUniqueId();
                    boolean isAdded = (boolean) isAddedMethod.invoke(plot, uuid);
                    boolean isOwner = (boolean) isOwnerMethod.invoke(plot, uuid);
                    return isAdded || isOwner;
                }
            }
        } catch (Throwable ignored) {}
        return true;
    }
}
