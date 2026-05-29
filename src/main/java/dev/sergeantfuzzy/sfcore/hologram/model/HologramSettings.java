package dev.sergeantfuzzy.sfcore.hologram.model;

/**
 * Mutable settings block for a single hologram. Defaults are chosen so a
 * freshly-created hologram is visible and well-behaved without any further
 * configuration — operators can ignore everything here until they want to
 * tune behavior.
 *
 * <p>Every Phase 1 setting (billboard, scale, ranges, doubleSided) is wired
 * here. Later phases add fields without breaking persistence: the YAML
 * serializer reads each key under {@code settings:} and ignores anything
 * unknown, so older saved holograms continue to load.
 */
public final class HologramSettings {

    public enum Billboard {
        FIXED,
        VERTICAL,
        HORIZONTAL,
        CENTER
    }

    public enum TextAlignment {
        CENTER,
        LEFT,
        RIGHT
    }

    private Billboard billboard = Billboard.CENTER;
    private TextAlignment textAlignment = TextAlignment.CENTER;
    private double scale = 1.0;
    private double showRange = 48.0;
    private double updateRange = 64.0;
    private boolean doubleSided = true;
    private boolean shadow = false;
    private boolean seeThrough = false;
    private int backgroundColor = 0x40000000; // ARGB — Minecraft's default transparent black.
    private int textOpacity = 255;
    private int lineWidth = 200;

    /** Phase 4. */
    private boolean interactionEnabled = false;
    private double interactionWidth = 1.0;
    private double interactionHeight = 1.0;
    private long interactionCooldownMs = 500L;

    /** Phase 6. */
    private String viewPermission = null;
    private boolean hideBehindWalls = false;

    public Billboard getBillboard() {
        return billboard;
    }

    public void setBillboard(Billboard billboard) {
        if (billboard != null) {
            this.billboard = billboard;
        }
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public void setTextAlignment(TextAlignment alignment) {
        if (alignment != null) {
            this.textAlignment = alignment;
        }
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.05, Math.min(20.0, scale));
    }

    public double getShowRange() {
        return showRange;
    }

    public void setShowRange(double range) {
        this.showRange = Math.max(1.0, range);
    }

    public double getUpdateRange() {
        return updateRange;
    }

    public void setUpdateRange(double range) {
        this.updateRange = Math.max(showRange, range);
    }

    public boolean isDoubleSided() {
        return doubleSided;
    }

    public void setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
    }

    public boolean hasShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int argb) {
        this.backgroundColor = argb;
    }

    public int getTextOpacity() {
        return textOpacity;
    }

    public void setTextOpacity(int opacity) {
        this.textOpacity = Math.max(0, Math.min(255, opacity));
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int width) {
        this.lineWidth = Math.max(20, width);
    }

    public boolean isInteractionEnabled() {
        return interactionEnabled;
    }

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
    }

    public double getInteractionWidth() {
        return interactionWidth;
    }

    public void setInteractionWidth(double width) {
        this.interactionWidth = Math.max(0.1, width);
    }

    public double getInteractionHeight() {
        return interactionHeight;
    }

    public void setInteractionHeight(double height) {
        this.interactionHeight = Math.max(0.1, height);
    }

    public long getInteractionCooldownMs() {
        return interactionCooldownMs;
    }

    public void setInteractionCooldownMs(long ms) {
        this.interactionCooldownMs = Math.max(0L, ms);
    }

    public String getViewPermission() {
        return viewPermission;
    }

    public void setViewPermission(String permission) {
        this.viewPermission = permission == null || permission.isEmpty() ? null : permission;
    }

    public boolean isHideBehindWalls() {
        return hideBehindWalls;
    }

    public void setHideBehindWalls(boolean hide) {
        this.hideBehindWalls = hide;
    }
}
