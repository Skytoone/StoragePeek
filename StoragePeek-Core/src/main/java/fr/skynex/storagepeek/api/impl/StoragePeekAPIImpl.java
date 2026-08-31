package fr.skynex.storagepeek.api.impl;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.api.StoragePeekAPI;
import fr.skynex.storagepeek.api.events.StoragePeekCloseEvent;
import fr.skynex.storagepeek.api.events.StoragePeekFilterChangeEvent;
import fr.skynex.storagepeek.api.events.StoragePeekOpenEvent;
import fr.skynex.storagepeek.api.events.StoragePeekToggleEvent;
import fr.skynex.storagepeek.api.provider.CustomContainerProvider;
import fr.skynex.storagepeek.api.theme.CustomTheme;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StoragePeekAPIImpl implements StoragePeekAPI {

    private final StoragePeek plugin;
    private final Map<String, CustomTheme> customThemes = new ConcurrentHashMap<>();

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
    public void closePeekSession(@NotNull Player player) {
        PeekSession session = plugin.getActiveSessions().remove(player.getUniqueId());
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

    @Nullable
    public CustomTheme getCustomTheme(@NotNull String themeId) {
        return customThemes.get(themeId.toUpperCase());
    }
}
