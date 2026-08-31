package fr.skynex.storagepeek.listener;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public class TransferVisualizerListener implements Listener {

    private final StoragePeek plugin;

    public TransferVisualizerListener(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!plugin.getConfig().getBoolean("visualizers.hopper-transfer", true)) {
            return;
        }

        Location sourceLoc = event.getSource().getLocation();
        Location destLoc = event.getDestination().getLocation();

        if (sourceLoc == null || destLoc == null) {
            return;
        }

        if (!sourceLoc.getWorld().equals(destLoc.getWorld())) {
            return;
        }

        // Limit range to prevent massive visual noise and loading issues
        if (sourceLoc.distanceSquared(destLoc) > 256) { // 16 blocks max
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Spawn client-side flying item animation
        Location start = sourceLoc.clone().add(0.5, 0.5, 0.5);
        Location end = destLoc.clone().add(0.5, 0.5, 0.5);

        // We run it on display creation thread (entity thread safety)
        ItemDisplay display = start.getWorld().spawn(start, ItemDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setItemStack(item.clone());
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setBrightness(new Display.Brightness(15, 15));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.18f, 0.18f, 0.18f);
            ent.setTransformation(t);

            // Configure interpolation duration for a smooth glide
            ent.setInterpolationDelay(0);
            ent.setInterpolationDuration(8);
        });

        // Perform translation teleport
        display.teleport(end);

        // Schedule deletion on regional thread using FoliaScheduler
        FoliaScheduler.runLater(plugin, display, () -> {
            if (display.isValid()) {
                display.remove();
            }
        }, 8L);
    }
}
