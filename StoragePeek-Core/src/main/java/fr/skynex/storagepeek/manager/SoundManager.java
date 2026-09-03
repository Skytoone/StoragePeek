package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {

    private final StoragePeek plugin;

    public SoundManager(StoragePeek plugin) {
        this.plugin = plugin;
    }

    public void playConfigSound(Player player, String soundPath, Sound defaultSound, float defaultVolume, float defaultPitch) {
        playConfigSoundAt(player, player != null ? player.getLocation() : null, soundPath, defaultSound, defaultVolume, defaultPitch);
    }

    public void playConfigSoundAt(Player player, Location loc, String soundPath, Sound defaultSound, float defaultVolume, float defaultPitch) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) {
            return;
        }
        String typeStr = plugin.getConfig().getString("sounds." + soundPath + ".type");
        Sound sound = defaultSound;
        if (typeStr != null) {
            try {
                String formatted = typeStr.toLowerCase().trim();
                NamespacedKey key = null;
                if (formatted.contains(":")) {
                    key = NamespacedKey.fromString(formatted);
                } else {
                    key = NamespacedKey.minecraft(formatted.replace('_', '.'));
                }

                Sound registrySound = key != null ? Registry.SOUNDS.get(key) : null;
                if (registrySound != null) {
                    sound = registrySound;
                } else {
                    @SuppressWarnings("deprecation")
                    Sound legacySound = Sound.valueOf(typeStr.toUpperCase().trim());
                    sound = legacySound;
                }
            } catch (Exception ignored) {}
        }
        double volume = plugin.getConfig().getDouble("sounds." + soundPath + ".volume", defaultVolume);
        double pitch = plugin.getConfig().getDouble("sounds." + soundPath + ".pitch", defaultPitch);
        Location soundLoc = (loc != null && loc.getWorld() != null) ? loc : player.getLocation();
        player.playSound(soundLoc, sound, (float) volume, (float) pitch);
    }
}
