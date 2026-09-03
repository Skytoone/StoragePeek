package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

public class DisplayEntityManager {

    private final StoragePeek plugin;

    public DisplayEntityManager(StoragePeek plugin) {
        this.plugin = plugin;
    }

    public void tagDisplayEntity(Entity entity) {
        if (entity != null) {
            entity.setPersistent(false);
            NamespacedKey displayKey = plugin.getDisplayKey();
            if (displayKey != null) {
                entity.getPersistentDataContainer().set(displayKey, PersistentDataType.BYTE, (byte) 1);
            }
        }
    }

    public int purgeOrphanedEntities() {
        int count = 0;
        NamespacedKey displayKey = plugin.getDisplayKey();
        if (displayKey == null) return count;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClasses(
                    ItemDisplay.class,
                    BlockDisplay.class,
                    TextDisplay.class,
                    Interaction.class)) {
                if (entity.getPersistentDataContainer().has(displayKey, PersistentDataType.BYTE)) {
                    entity.remove();
                    count++;
                }
            }
        }
        return count;
    }
}
