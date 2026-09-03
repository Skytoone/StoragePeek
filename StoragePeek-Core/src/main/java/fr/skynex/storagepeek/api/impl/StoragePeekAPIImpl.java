package fr.skynex.storagepeek.api.impl;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.api.StoragePeekAPI;
import fr.skynex.storagepeek.api.audio.SlotHoverSound;
import fr.skynex.storagepeek.api.events.StoragePeekCloseEvent;
import fr.skynex.storagepeek.api.events.StoragePeekFilterChangeEvent;
import fr.skynex.storagepeek.api.events.StoragePeekOpenEvent;
import fr.skynex.storagepeek.api.events.StoragePeekPageChangeEvent;
import fr.skynex.storagepeek.api.events.StoragePeekToggleEvent;
import fr.skynex.storagepeek.api.provider.CustomContainerProvider;
import fr.skynex.storagepeek.api.security.LootSecurityFilter;
import fr.skynex.storagepeek.api.theme.CustomTheme;
import fr.skynex.storagepeek.api.transform.CustomDisplayTransform;
import fr.skynex.storagepeek.api.valuation.ItemValuer;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class StoragePeekAPIImpl implements StoragePeekAPI {

    private final StoragePeek plugin;
    private final Map<String, CustomTheme> customThemes = new ConcurrentHashMap<>();
    private final List<LootSecurityFilter> securityFilters = new CopyOnWriteArrayList<>();
    private final Map<Object, String> containerTaglines = new ConcurrentHashMap<>();
    private final List<SlotHoverSound> slotHoverSounds = new CopyOnWriteArrayList<>();
    private final Map<UUID, Integer> sessionPages = new ConcurrentHashMap<>();

    private final List<CustomDisplayTransform> customTransforms = new CopyOnWriteArrayList<>();
    private final List<ItemValuer> itemValuers = new CopyOnWriteArrayList<>();
    private final Map<Location, Color> containerBeamColors = new ConcurrentHashMap<>();

    public StoragePeekAPIImpl(@NotNull StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isSessionActive(@NotNull Player player) {
        return plugin.getActiveSessions().containsKey(player.getUniqueId());
    }

    @Override
    @Nullable
    public Block getActiveSessionBlock(@NotNull Player player) {
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        return session != null ? session.getBlock() : null;
    }

    @Override
    public boolean isStoragePeekDisabled(@NotNull Player player) {
        return plugin.getDisabledPlayers().contains(player.getUniqueId());
    }

    @Override
    public void setStoragePeekDisabled(@NotNull Player player, boolean disabled) {
        StoragePeekToggleEvent toggleEvent = new StoragePeekToggleEvent(player, !disabled);
        Bukkit.getPluginManager().callEvent(toggleEvent);
        if (toggleEvent.isCancelled()) {
            return;
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (disabled) {
            pdc.set(plugin.getDisabledKey(), PersistentDataType.BYTE, (byte) 1);
            plugin.getDisabledPlayers().add(player.getUniqueId());
            closePeekSession(player);
        } else {
            pdc.remove(plugin.getDisabledKey());
            plugin.getDisabledPlayers().remove(player.getUniqueId());
        }
    }

    @Override
    @NotNull
    public Set<UUID> getDisabledPlayerUUIDs() {
        return Collections.unmodifiableSet(plugin.getDisabledPlayers());
    }

    @Override
    public boolean openPeekSession(@NotNull Player player, @NotNull Block block) {
        if (isStoragePeekDisabled(player)) {
            return false;
        }

        StoragePeekOpenEvent openEvent = new StoragePeekOpenEvent(player, block);
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) {
            return false;
        }

        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session != null) {
            session.cleanup(true);
        }

        PeekSession newSession = new PeekSession(player, block, null);
        plugin.getActiveSessions().put(player.getUniqueId(), newSession);
        return true;
    }

    @Override
    public boolean openVirtualPeekSession(@NotNull Player player, @NotNull Inventory inventory, @Nullable String title) {
        if (isStoragePeekDisabled(player)) {
            return false;
        }

        StoragePeekOpenEvent openEvent = new StoragePeekOpenEvent(player, null);
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) {
            return false;
        }

        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session != null) {
            session.cleanup(true);
        }

        PeekSession newSession = new PeekSession(player, null, null, inventory, title);
        plugin.getActiveSessions().put(player.getUniqueId(), newSession);
        return true;
    }

    @Override
    public void closePeekSession(@NotNull Player player) {
        PeekSession session = plugin.getActiveSessions().remove(player.getUniqueId());
        sessionPages.remove(player.getUniqueId());
        if (session != null) {
            Block block = session.getBlock();
            session.cleanup(true);
            StoragePeekCloseEvent closeEvent = new StoragePeekCloseEvent(player, block);
            Bukkit.getPluginManager().callEvent(closeEvent);
        }
    }

    @Override
    public void registerContainerProvider(@NotNull CustomContainerProvider provider) {
        plugin.getHookManager().registerCustomContainerProvider(provider);
    }

    @Override
    public void unregisterContainerProvider(@NotNull CustomContainerProvider provider) {
        plugin.getHookManager().unregisterCustomContainerProvider(provider);
    }

    @Override
    public void registerCustomTheme(@NotNull CustomTheme theme) {
        customThemes.put(theme.getId().toUpperCase(), theme);
    }

    @Override
    public void unregisterCustomTheme(@NotNull String themeId) {
        customThemes.remove(themeId.toUpperCase());
    }

    @Override
    public boolean setSessionFrozen(@NotNull Player player, boolean frozen) {
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session != null) {
            session.setFrozen(frozen);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSessionFrozen(@NotNull Player player) {
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        return session != null && session.isFrozen();
    }

    @Override
    public void refreshSession(@NotNull Player player) {
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session != null) {
            session.refresh();
        }
    }

    @Override
    public boolean setSessionFilter(@NotNull Player player, @NotNull String filterName) {
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null) {
            return false;
        }

        try {
            PeekSession.FilterType newFilter = PeekSession.FilterType.valueOf(filterName.toUpperCase().trim());
            StoragePeekFilterChangeEvent filterEvent = new StoragePeekFilterChangeEvent(player, session.getActiveFilter().name(), newFilter.name());
            Bukkit.getPluginManager().callEvent(filterEvent);
            if (filterEvent.isCancelled()) {
                return false;
            }

            session.setActiveFilter(PeekSession.FilterType.valueOf(filterEvent.getNewFilter()));
            session.refresh();
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void registerLootSecurityFilter(@NotNull LootSecurityFilter filter) {
        if (!securityFilters.contains(filter)) {
            securityFilters.add(filter);
        }
    }

    @Override
    public void unregisterLootSecurityFilter(@NotNull LootSecurityFilter filter) {
        securityFilters.remove(filter);
    }

    @NotNull
    public List<LootSecurityFilter> getSecurityFilters() {
        return securityFilters;
    }

    @Override
    @NotNull
    public List<Block> findNearbyContainers(@NotNull Location center, double radius, @NotNull Material material) {
        List<Block> result = new ArrayList<>();
        org.bukkit.World world = center.getWorld();
        if (world == null) return result;

        int minChunkX = (center.getBlockX() - (int) radius) >> 4;
        int maxChunkX = (center.getBlockX() + (int) radius) >> 4;
        int minChunkZ = (center.getBlockZ() - (int) radius) >> 4;
        int maxChunkZ = (center.getBlockZ() + (int) radius) >> 4;

        double radiusSq = radius * radius;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                org.bukkit.Chunk chunk = world.getChunkAt(cx, cz);
                for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                    Block block = state.getBlock();
                    if (block.getLocation().distanceSquared(center) <= radiusSq) {
                        if (plugin.getHookManager().isCustomContainer(block) || plugin.getRaycastTask().getAllowedBlocks().contains(block.getType())) {
                            Inventory inv = plugin.getHookManager().getInventory(block, null);
                            if (inv != null && inv.contains(material)) {
                                result.add(block);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    @NotNull
    public Map<Material, Integer> getContainerSummary(@NotNull Block block) {
        Map<Material, Integer> summary = new HashMap<>();
        Inventory inv = plugin.getHookManager().getInventory(block, null);
        if (inv != null) {
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    summary.put(item.getType(), summary.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        return summary;
    }

    @Override
    public void setContainerTagline(@NotNull Block block, @Nullable String tagline) {
        if (tagline == null || tagline.isEmpty()) {
            containerTaglines.remove(block.getLocation());
        } else {
            containerTaglines.put(block.getLocation(), tagline);
        }
    }

    @Override
    public void setEntityTagline(@NotNull Entity entity, @Nullable String tagline) {
        if (tagline == null || tagline.isEmpty()) {
            containerTaglines.remove(entity.getUniqueId());
        } else {
            containerTaglines.put(entity.getUniqueId(), tagline);
        }
    }

    @Override
    public void clearContainerTagline(@NotNull Block block) {
        containerTaglines.remove(block.getLocation());
    }

    @Nullable
    public String getContainerTagline(@Nullable Block block, @Nullable Entity entity) {
        if (block != null && containerTaglines.containsKey(block.getLocation())) {
            return containerTaglines.get(block.getLocation());
        }
        if (entity != null && containerTaglines.containsKey(entity.getUniqueId())) {
            return containerTaglines.get(entity.getUniqueId());
        }
        if (block != null && block.getState() instanceof org.bukkit.block.TileState tileState) {
            org.bukkit.NamespacedKey labelKey = new org.bukkit.NamespacedKey(plugin, "custom_label");
            if (tileState.getPersistentDataContainer().has(labelKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                return tileState.getPersistentDataContainer().get(labelKey, org.bukkit.persistence.PersistentDataType.STRING);
            }
        }
        return null;
    }

    @Override
    public void registerSlotHoverSound(@NotNull Predicate<ItemStack> itemMatcher, @NotNull Sound sound, float volume, float pitch) {
        slotHoverSounds.add(new SlotHoverSound(itemMatcher, sound, volume, pitch));
    }

    @NotNull
    public List<SlotHoverSound> getSlotHoverSounds() {
        return slotHoverSounds;
    }

    @Override
    public boolean setSessionPage(@NotNull Player player, int page) {
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null) {
            return false;
        }

        int oldPage = getSessionPage(player);
        StoragePeekPageChangeEvent pageEvent = new StoragePeekPageChangeEvent(player, oldPage, page);
        Bukkit.getPluginManager().callEvent(pageEvent);
        if (pageEvent.isCancelled()) {
            return false;
        }

        sessionPages.put(player.getUniqueId(), Math.max(0, pageEvent.getNewPage()));
        session.refresh();
        return true;
    }

    @Override
    public int getSessionPage(@NotNull Player player) {
        return sessionPages.getOrDefault(player.getUniqueId(), 0);
    }

    public void clearSessionPage(@NotNull UUID playerUUID) {
        sessionPages.remove(playerUUID);
    }

    @Override
    public void registerCustomTransform(@NotNull CustomDisplayTransform transform) {
        if (!customTransforms.contains(transform)) {
            customTransforms.add(transform);
        }
    }

    @Override
    public void unregisterCustomTransform(@NotNull CustomDisplayTransform transform) {
        customTransforms.remove(transform);
    }

    @NotNull
    public List<CustomDisplayTransform> getCustomTransforms() {
        return customTransforms;
    }

    @Override
    public void registerItemValuer(@NotNull ItemValuer valuer) {
        if (!itemValuers.contains(valuer)) {
            itemValuers.add(valuer);
        }
    }

    @Override
    public void unregisterItemValuer(@NotNull ItemValuer valuer) {
        itemValuers.remove(valuer);
    }

    @Override
    public double getContainerTotalValue(@NotNull Block block, @Nullable Player player) {
        Inventory inv = plugin.getHookManager().getInventory(block, player);
        if (inv == null) return 0.0;

        double total = 0.0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                for (ItemValuer valuer : itemValuers) {
                    try {
                        double val = valuer.getValue(item);
                        if (val > 0) {
                            total += val * item.getAmount();
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        return total;
    }

    public double getItemValue(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR) return 0.0;
        for (ItemValuer valuer : itemValuers) {
            try {
                double val = valuer.getValue(item);
                if (val > 0) return val;
            } catch (Throwable ignored) {}
        }
        return 0.0;
    }

    @Override
    public void setContainerBeamColor(@NotNull Block block, @Nullable Color color) {
        if (color == null) {
            containerBeamColors.remove(block.getLocation());
        } else {
            containerBeamColors.put(block.getLocation(), color);
        }
    }

    @Override
    public void clearContainerBeamColor(@NotNull Block block) {
        containerBeamColors.remove(block.getLocation());
    }

    @Nullable
    public Color getContainerBeamColor(@NotNull Block block) {
        return containerBeamColors.get(block.getLocation());
    }

    @Nullable
    public CustomTheme getCustomTheme(@NotNull String themeId) {
        return customThemes.get(themeId.toUpperCase());
    }
}
