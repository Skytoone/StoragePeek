package fr.skynex.storagepeek.listener;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class ExhibitionFrameListener implements Listener {

    private final NamespacedKey itemKey;
    private final NamespacedKey activeKey;
    private final Map<UUID, ItemDisplay> spawnedDisplays = new java.util.concurrent.ConcurrentHashMap<>();
    private FoliaScheduler.RepeatingTask tickTask;

    public ExhibitionFrameListener(StoragePeek plugin) {
        this.itemKey = new NamespacedKey(plugin, "stored_item");
        this.activeKey = new NamespacedKey(plugin, "exhibition_3d");

        // Start ticking displays for rotation
        this.tickTask = FoliaScheduler.runTimer(plugin, null, this::tickRotations, 1L, 1L);

        // Load displays in already loaded chunks
        FoliaScheduler.runLaterGlobal(plugin, this::loadAllActiveFrames, 5L);
    }

    private void tickRotations() {
        float angle = (float) (System.currentTimeMillis() / 400.0);
        for (ItemDisplay display : new HashSet<>(spawnedDisplays.values())) {
            if (display != null && display.isValid()) {
                Transformation t = display.getTransformation();
                t.getLeftRotation().rotationY(angle);
                display.setTransformation(t);
            }
        }
    }

    private void loadAllActiveFrames() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClasses(ItemFrame.class, GlowItemFrame.class)) {
                checkAndSpawnDisplay(entity);
            }
        }
    }

    public void cleanupAll() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (ItemDisplay display : spawnedDisplays.values()) {
            if (display != null) {
                display.remove();
            }
        }
        spawnedDisplays.clear();
    }

    private void checkAndSpawnDisplay(Entity entity) {
        if (spawnedDisplays.containsKey(entity.getUniqueId())) {
            return;
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(activeKey, PersistentDataType.BOOLEAN)) {
            Boolean active = pdc.get(activeKey, PersistentDataType.BOOLEAN);
            if (Boolean.TRUE.equals(active)) {
                String serialized = pdc.get(itemKey, PersistentDataType.STRING);
                if (serialized != null) {
                    ItemStack item = deserializeItem(serialized);
                    if (item != null && item.getType() != Material.AIR) {
                        spawnDisplay(entity, item);
                    }
                }
            }
        }
    }

    private void spawnDisplay(Entity frame, ItemStack item) {
        BlockFace facing = BlockFace.SOUTH;
        if (frame instanceof ItemFrame jf) {
            facing = jf.getFacing();
            jf.setVisible(false);
            jf.setItem(null); // Keep visual empty
        }

        Location loc = frame.getLocation().add(facing.getDirection().multiply(0.32));
        loc.setY(loc.getY() + 0.05); // Centering adjustment

        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            StoragePeek.getInstance().tagDisplayEntity(ent);
            ent.setItemStack(item.clone());
            ent.setBillboard(Display.Billboard.FIXED);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);

            Transformation t = ent.getTransformation();
            t.getScale().set(0.35f, 0.35f, 0.35f);
            ent.setTransformation(t);
        });

        spawnedDisplays.put(frame.getUniqueId(), display);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame || entity instanceof GlowItemFrame)) {
            return;
        }

        Player player = event.getPlayer();
        ItemFrame frame = (ItemFrame) entity;
        PersistentDataContainer pdc = frame.getPersistentDataContainer();

        boolean is3D = pdc.has(activeKey, PersistentDataType.BOOLEAN) && Boolean.TRUE.equals(pdc.get(activeKey, PersistentDataType.BOOLEAN));

        if (is3D) {
            // Interact with active 3D frame: cancel default and retrieve item
            event.setCancelled(true);
            retrieveItem(frame, player);
            return;
        }

        // Toggle on: Must sneak and click with empty hand and frame must have an item
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            ItemStack frameItem = frame.getItem();
            if (frameItem != null && frameItem.getType() != Material.AIR) {
                event.setCancelled(true);
                enable3DMode(frame, player, frameItem);
            }
        }
    }

    private void enable3DMode(ItemFrame frame, Player player, ItemStack item) {
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        pdc.set(activeKey, PersistentDataType.BOOLEAN, true);
        pdc.set(itemKey, PersistentDataType.STRING, serializeItem(item));

        spawnDisplay(frame, item);

        player.getWorld().playSound(frame.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, frame.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.05);
    }

    private void retrieveItem(ItemFrame frame, Player player) {
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        String serialized = pdc.get(itemKey, PersistentDataType.STRING);

        pdc.remove(activeKey);
        pdc.remove(itemKey);

        ItemDisplay display = spawnedDisplays.remove(frame.getUniqueId());
        if (display != null) {
            display.remove();
        }

        frame.setVisible(true);
        frame.setItem(null);

        if (serialized != null) {
            ItemStack item = deserializeItem(serialized);
            if (item != null && item.getType() != Material.AIR) {
                if (player != null) {
                    // Give item to player; drop leftovers at frame location
                    HashMap<Integer, ItemStack> remain = player.getInventory().addItem(item);
                    for (ItemStack r : remain.values()) {
                        frame.getLocation().getWorld().dropItemNaturally(frame.getLocation(), r);
                    }
                } else {
                    // No player (frame broken by physics/explosion): drop item in world
                    frame.getLocation().getWorld().dropItemNaturally(frame.getLocation(), item);
                }
            }
        }

        if (player != null) {
            player.getWorld().playSound(frame.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
        } else {
            frame.getLocation().getWorld().playSound(frame.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
            ItemFrame frame = (ItemFrame) entity;
            PersistentDataContainer pdc = frame.getPersistentDataContainer();
            if (pdc.has(activeKey, PersistentDataType.BOOLEAN) && Boolean.TRUE.equals(pdc.get(activeKey, PersistentDataType.BOOLEAN))) {
                // Toggled 3D frame is damaged: retrieve item first to prevent loss
                event.setCancelled(true);
                retrieveItem(frame, null);
                frame.remove(); // Break frame
                frame.getLocation().getWorld().dropItemNaturally(frame.getLocation(), new ItemStack(frame.getType() == org.bukkit.entity.EntityType.GLOW_ITEM_FRAME ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME));
            }
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
            ItemFrame frame = (ItemFrame) entity;
            PersistentDataContainer pdc = frame.getPersistentDataContainer();
            if (pdc.has(activeKey, PersistentDataType.BOOLEAN) && Boolean.TRUE.equals(pdc.get(activeKey, PersistentDataType.BOOLEAN))) {
                retrieveItem(frame, null);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
                checkAndSpawnDisplay(entity);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
                ItemDisplay display = spawnedDisplays.remove(entity.getUniqueId());
                if (display != null) {
                    display.remove();
                }
            }
        }
    }

    private String serializeItem(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }

    private ItemStack deserializeItem(String data) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(data);
            return config.getItemStack("item");
        } catch (Exception ex) {
            return null;
        }
    }
}
