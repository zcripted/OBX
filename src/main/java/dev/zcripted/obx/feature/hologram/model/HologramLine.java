package dev.zcripted.obx.feature.hologram.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * One renderable element of a hologram. Three concrete variants live as nested
 * final classes so the model can switch on {@link #getType()} without a
 * dedicated visitor — fine for three cases, and it keeps storage / serialization
 * straightforward.
 *
 * <p>Future phases extend the variants (e.g. {@code IconLine} gains custom
 * model data and player-head support in Phase 6) by adding fields on the
 * nested class, not by introducing new top-level types.
 */
public abstract class HologramLine {

    public enum Type {
        TEXT,
        ICON,
        BLOCK
    }

    private final Type type;

    private HologramLine(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static TextLine text(String template) {
        return new TextLine(template);
    }

    public static IconLine icon(ItemStack stack) {
        return new IconLine(stack);
    }

    public static BlockLine block(Material material) {
        return new BlockLine(material);
    }

    /**
     * A per-viewer text line. {@code template} is the raw user-provided string
     * (legacy {@code &} codes, MiniMessage tags if Adventure is present, and
     * placeholders) — it is resolved per-viewer at render time by
     * {@code HologramTextResolver} (Phase 3).
     */
    public static final class TextLine extends HologramLine {
        private final String template;

        public TextLine(String template) {
            super(Type.TEXT);
            this.template = template == null ? "" : template;
        }

        public String getTemplate() {
            return template;
        }
    }

    /** An item-display / floating-item line. Phase 6 expands to player heads / CMD / in-hand. */
    public static final class IconLine extends HologramLine {
        private final ItemStack stack;

        public IconLine(ItemStack stack) {
            super(Type.ICON);
            this.stack = stack == null ? null : stack.clone();
        }

        public ItemStack getStack() {
            return stack == null ? null : stack.clone();
        }
    }

    /** A block-display line. */
    public static final class BlockLine extends HologramLine {
        private final Material material;

        public BlockLine(Material material) {
            super(Type.BLOCK);
            this.material = material == null ? Material.STONE : material;
        }

        public Material getMaterial() {
            return material;
        }
    }
}
