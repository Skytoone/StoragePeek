package fr.skynex.storagepeek.api.audio;

import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Defines a custom sound effect played when a player hovers over matching item slots in 3D preview mode.
 */
public class SlotHoverSound {

    private final Predicate<ItemStack> itemMatcher;
    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SlotHoverSound(@NotNull Predicate<ItemStack> itemMatcher, @NotNull Sound sound, float volume, float pitch) {
        this.itemMatcher = itemMatcher;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @NotNull
    public Predicate<ItemStack> getItemMatcher() {
        return itemMatcher;
    }

    @NotNull
    public Sound getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}
