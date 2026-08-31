package fr.skynex.storagepeek.api.security;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result returned by a {@link LootSecurityFilter} determining if an item should be displayed, hidden, or masked with a placeholder.
 */
public class SecurityResult {

    public enum Type {
        ALLOW,
        HIDE,
        MASK
    }

    private final Type type;
    private final Material placeholderMaterial;
    private final String customName;

    private SecurityResult(@NotNull Type type, @Nullable Material placeholderMaterial, @Nullable String customName) {
        this.type = type;
        this.placeholderMaterial = placeholderMaterial;
        this.customName = customName;
    }

    public static SecurityResult allow() {
        return new SecurityResult(Type.ALLOW, null, null);
    }

    public static SecurityResult hide() {
        return new SecurityResult(Type.HIDE, null, null);
    }

    public static SecurityResult maskWithPlaceholder(@NotNull Material material, @Nullable String customName) {
        return new SecurityResult(Type.MASK, material, customName);
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public Material getPlaceholderMaterial() {
        return placeholderMaterial;
    }

    @Nullable
    public String getCustomName() {
        return customName;
    }
}
