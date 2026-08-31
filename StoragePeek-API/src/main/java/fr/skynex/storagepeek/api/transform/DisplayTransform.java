package fr.skynex.storagepeek.api.transform;

import org.bukkit.Color;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Model representing custom 3D display transformations (scale, rotations, offsets, glow colors, and billboard modes).
 */
public class DisplayTransform {

    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final float rotationX;
    private final float rotationY;
    private final float rotationZ;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final Color glowColor;
    private final ItemDisplay.ItemDisplayTransform itemTransform;

    private DisplayTransform(Builder builder) {
        this.scaleX = builder.scaleX;
        this.scaleY = builder.scaleY;
        this.scaleZ = builder.scaleZ;
        this.rotationX = builder.rotationX;
        this.rotationY = builder.rotationY;
        this.rotationZ = builder.rotationZ;
        this.offsetX = builder.offsetX;
        this.offsetY = builder.offsetY;
        this.offsetZ = builder.offsetZ;
        this.glowColor = builder.glowColor;
        this.itemTransform = builder.itemTransform;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    @Nullable public Color getGlowColor() { return glowColor; }
    @NotNull public ItemDisplay.ItemDisplayTransform getItemTransform() { return itemTransform; }

    public static class Builder {
        private float scaleX = 1.0f;
        private float scaleY = 1.0f;
        private float scaleZ = 1.0f;
        private float rotationX = 0f;
        private float rotationY = 0f;
        private float rotationZ = 0f;
        private float offsetX = 0f;
        private float offsetY = 0f;
        private float offsetZ = 0f;
        private Color glowColor = null;
        private ItemDisplay.ItemDisplayTransform itemTransform = ItemDisplay.ItemDisplayTransform.FIXED;

        public Builder scale(float scale) {
            this.scaleX = scale;
            this.scaleY = scale;
            this.scaleZ = scale;
            return this;
        }

        public Builder scale(float x, float y, float z) {
            this.scaleX = x;
            this.scaleY = y;
            this.scaleZ = z;
            return this;
        }

        public Builder rotation(float x, float y, float z) {
            this.rotationX = x;
            this.rotationY = y;
            this.rotationZ = z;
            return this;
        }

        public Builder rotationY(float y) {
            this.rotationY = y;
            return this;
        }

        public Builder offset(float x, float y, float z) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            return this;
        }

        public Builder glowColor(@Nullable Color glowColor) {
            this.glowColor = glowColor;
            return this;
        }

        public Builder itemTransform(@NotNull ItemDisplay.ItemDisplayTransform transform) {
            this.itemTransform = transform;
            return this;
        }

        public DisplayTransform build() {
            return new DisplayTransform(this);
        }
    }
}
