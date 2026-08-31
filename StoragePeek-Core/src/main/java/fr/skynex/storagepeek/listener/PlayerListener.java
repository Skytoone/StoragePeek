package fr.skynex.storagepeek.listener;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final StoragePeek plugin;
    private final Map<UUID, Long> combatTimes = new ConcurrentHashMap<>();

    public PlayerListener(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isAutoEnableOnJoin()) {
            org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
            if (pdc.has(plugin.getDisabledKey(), org.bukkit.persistence.PersistentDataType.BYTE)) {
                pdc.remove(plugin.getDisabledKey());
            }
        } else {
            org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
            if (pdc.has(plugin.getDisabledKey(), org.bukkit.persistence.PersistentDataType.BYTE)) {
                plugin.getDisabledPlayers().add(player.getUniqueId());
            }
        }
        plugin.startRaycastTask(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cleanupPlayer(player);
        combatTimes.remove(player.getUniqueId());
        plugin.getDisabledPlayers().remove(player.getUniqueId());
        plugin.stopRaycastTask(player);
        // Clean up any compass arrow entity that may still be active for this player
        if (plugin.getRaycastTask() != null) {
            plugin.getRaycastTask().cleanupCompassArrow(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            cleanupPlayer(player);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        net.kyori.adventure.text.Component title = event.getView().title();
        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer serializer = 
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        String plainTitle = serializer.serialize(title);
        
        if (plainTitle.equals("StoragePeek - Themes")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 8) {
                return;
            }
            
            String selectedTheme = switch (slot) {
                case 0 -> "default";
                case 1 -> "ender";
                case 2 -> "rich";
                case 3 -> "aqua";
                case 4 -> "nether";
                case 5 -> "neon";
                case 6 -> "cyberpunk";
                case 7 -> "rainbow";
                default -> "default";
            };

            if (!selectedTheme.equals("default") && !player.hasPermission("storagepeek.theme." + selectedTheme)) {
                player.sendMessage(plugin.getMessageManager().getMessage("theme-no-permission").replace("{theme}", selectedTheme));
                player.closeInventory();
                plugin.playConfigSound(player, "hover", org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }

            org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
            org.bukkit.NamespacedKey themeKey = new org.bukkit.NamespacedKey(plugin, "theme");
            pdc.set(themeKey, org.bukkit.persistence.PersistentDataType.STRING, selectedTheme);
            player.sendMessage(plugin.getMessageManager().getMessage("theme-updated").replace("{theme}", selectedTheme));
            player.closeInventory();
            plugin.playConfigSound(player, "sort", org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            fr.skynex.storagepeek.session.PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
            if (session != null) {
                event.setCancelled(true);
                fr.skynex.storagepeek.session.PeekSession.FilterType current = session.getActiveFilter();
                fr.skynex.storagepeek.session.PeekSession.FilterType next = switch (current) {
                    case ALL -> fr.skynex.storagepeek.session.PeekSession.FilterType.RESOURCES;
                    case RESOURCES -> fr.skynex.storagepeek.session.PeekSession.FilterType.FOOD;
                    case FOOD -> fr.skynex.storagepeek.session.PeekSession.FilterType.EQUIPMENT;
                    case EQUIPMENT -> fr.skynex.storagepeek.session.PeekSession.FilterType.ALL;
                };
                session.setActiveFilter(next);
                player.sendMessage(plugin.getMessageManager().getMessage("filter-updated").replace("{filter}", next.name().toLowerCase()));
                plugin.playConfigSound(player, "sort", org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!plugin.isCombatCullingEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            Player damager = null;
            if (event.getDamager() instanceof Player p) {
                damager = p;
            } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
                damager = p;
            }

            if (damager != null && damager != victim) {
                long now = System.currentTimeMillis();
                combatTimes.put(victim.getUniqueId(), now);
                combatTimes.put(damager.getUniqueId(), now);

                forceCloseSession(victim);
                forceCloseSession(damager);
            }
        }
    }

    private void forceCloseSession(Player player) {
        if (player.hasPermission("storagepeek.bypass.combat")) {
            return;
        }
        PeekSession session = plugin.getActiveSessions().remove(player.getUniqueId());
        if (session != null) {
            session.cleanup(true);
        }
    }

    private void cleanupPlayer(Player player) {
        plugin.getRaycastTask().clearCache(player.getUniqueId());
        PeekSession session = plugin.getActiveSessions().remove(player.getUniqueId());
        if (session != null) {
            session.cleanup(true);
        }
    }

    public boolean isInLocalCombat(Player player) {
        if (player.hasPermission("storagepeek.bypass.combat")) {
            return false;
        }
        if (!plugin.isCombatCullingEnabled()) {
            return false;
        }
        double cooldown = plugin.getCombatCullingCooldown();
        if (cooldown <= 0) {
            return false;
        }
        Long lastCombat = combatTimes.get(player.getUniqueId());
        if (lastCombat == null) {
            return false;
        }
        boolean inCombat = (System.currentTimeMillis() - lastCombat) < (cooldown * 1000L);
        if (!inCombat) {
            combatTimes.remove(player.getUniqueId());
        }
        return inCombat;
    }

    @EventHandler
    public void onWorldLoad(org.bukkit.event.world.WorldLoadEvent event) {
        plugin.addDisabledWorldToCache(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(org.bukkit.event.world.WorldUnloadEvent event) {
        plugin.removeDisabledWorldFromCache(event.getWorld());
    }
}
