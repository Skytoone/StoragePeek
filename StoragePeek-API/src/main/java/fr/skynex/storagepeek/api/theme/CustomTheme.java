package fr.skynex.storagepeek.api.theme;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a custom cosmetic theme for StoragePeek preview displays.
 */
public class CustomTheme {

    private final String id;
    private final Material backgroundMaterial;
    private final Particle particleEffect;
    private final Sound soundEffect;
    private final Color nameplateBackgroundColor;

    public CustomTheme(@NotNull String id,
                       @NotNull Material backgroundMaterial,
                       @Nullable Particle particleEffect,
                       @Nullable Sound soundEffect,
                       @Nullable Color nameplateBackgroundColor) {
        this.id = id.toUpperCase();
        this.backgroundMaterial = backgroundMaterial;
        this.particleEffect = particleEffect;
        this.soundEffect = soundEffect;
        this.nameplateBackgroundColor = nameplateBackgroundColor;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public Material getBackgroundMaterial() {
        return backgroundMaterial;
    }

    @Nullable
    public Particle getParticleEffect() {
        return particleEffect;
    }

    @Nullable
    public Sound getSoundEffect() {
        return soundEffect;
    }

    @Nullable
    public Color getNameplateBackgroundColor() {
        return nameplateBackgroundColor;
    }
}
