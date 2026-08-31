package fr.skynex.storagepeek.visualizer;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizerListener implements Listener {

    private final StoragePeek plugin;
    private final VisualizerManager manager;
    private final Map<UUID, Block> lastClickedBlocks = new ConcurrentHashMap<>();

    public VisualizerListener(StoragePeek plugin, VisualizerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                lastClickedBlocks.put(event.getPlayer().getUniqueId(), block);
                
                // Jukebox Handling
                if (block.getType() == Material.JUKEBOX && plugin.getConfig().getBoolean("visualizers.jukebox", true)) {
                    Player player = event.getPlayer();
                    FoliaScheduler.runLater(plugin, player, () -> {
                        if (block.getType() == Material.JUKEBOX && block.getState() instanceof org.bukkit.block.Jukebox jb) {
                            ItemStack record = jb.getRecord();
                            if (record != null && record.getType() != Material.AIR) {
                                manager.addPlayerToSession(block, player, null);
                            } else {
                                manager.clearSession(block);
                            }
                        }
                    }, 1L);
                }
                
                // Lectern Handling
                if (block.getType() == Material.LECTERN && plugin.getConfig().getBoolean("visualizers.lectern", true)) {
                    Player player = event.getPlayer();
                    FoliaScheduler.runLater(plugin, player, () -> {
                        if (block.getType() == Material.LECTERN && block.getState() instanceof org.bukkit.block.Lectern lectern) {
                            ItemStack book = lectern.getInventory().getItem(0);
                            if (book != null && book.getType() != Material.AIR) {
                                manager.addPlayerToSession(block, player, null);
                            } else {
                                manager.clearSession(block);
                            }
                        }
                    }, 1L);
                }

                // Chiseled Bookshelf Handling
                if (block.getType() == Material.CHISELED_BOOKSHELF && plugin.getConfig().getBoolean("visualizers.chiseled-bookshelf", true)) {
                    Player player = event.getPlayer();
                    FoliaScheduler.runLater(plugin, player, () -> {
                        if (block.getType() == Material.CHISELED_BOOKSHELF && block.getState() instanceof org.bukkit.block.ChiseledBookshelf shelf) {
                            boolean hasBooks = false;
                            for (int i = 0; i < shelf.getInventory().getSize(); i++) {
                                ItemStack b = shelf.getInventory().getItem(i);
                                if (b != null && b.getType() != Material.AIR) {
                                    hasBooks = true;
                                    break;
                                }
                            }
                            if (hasBooks) {
                                manager.addPlayerToSession(block, player, shelf.getInventory());
                            } else {
                                manager.clearSession(block);
                            }
                        }
                    }, 1L);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryType type = event.getInventory().getType();
        boolean matches = switch (type) {
            case WORKBENCH, FURNACE, BREWING, ANVIL, ENCHANTING -> true;
            default -> false;
        };

        if (!matches) {
            return;
        }

        Block block = lastClickedBlocks.get(player.getUniqueId());
        if (block == null) {
            return;
        }

        // Safety distance check (8 blocks max)
        Location pLoc = player.getLocation();
        Location bLoc = block.getLocation();
        if (!pLoc.getWorld().equals(bLoc.getWorld()) || pLoc.distanceSquared(bLoc) > 64) {
            return;
        }

        // Verify that the clicked block matches the inventory type
        Material blockType = block.getType();
        String blockName = blockType.name();
        boolean validBlock = false;

        switch (type) {
            case WORKBENCH:
                validBlock = blockName.contains("CRAFTING_TABLE") && plugin.getConfig().getBoolean("visualizers.crafting-table", true);
                break;
            case FURNACE:
                validBlock = (blockName.contains("FURNACE") || blockName.contains("SMOKER")) && plugin.getConfig().getBoolean("visualizers.furnace", true);
                break;
            case BREWING:
                validBlock = blockName.contains("BREWING_STAND") && plugin.getConfig().getBoolean("visualizers.brewing-stand", true);
                break;
            case ANVIL:
                validBlock = blockName.contains("ANVIL") && plugin.getConfig().getBoolean("visualizers.anvil", true);
                break;
            case ENCHANTING:
                validBlock = blockName.contains("ENCHANTING_TABLE") && plugin.getConfig().getBoolean("visualizers.enchanting-table", true);
                break;
            default:
                break;
        }

        if (validBlock) {
            manager.addPlayerToSession(block, player, event.getInventory());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            manager.removePlayerFromAll(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.removePlayerFromAll(player);
        lastClickedBlocks.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        manager.clearSession(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        for (Block b : event.blockList()) {
            manager.clearSession(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        for (Block b : event.blockList()) {
            manager.clearSession(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            manager.clearSession(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            manager.clearSession(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        manager.clearSession(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(org.bukkit.event.block.BlockFadeEvent event) {
        manager.clearSession(event.getBlock());
    }
}
