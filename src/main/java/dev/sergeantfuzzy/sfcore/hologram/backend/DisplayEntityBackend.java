package dev.sergeantfuzzy.sfcore.hologram.backend;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramLine;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramSettings;
import dev.sergeantfuzzy.sfcore.hologram.text.HologramTextResolver;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper / Spigot ≥ 1.19.4 backend. Spawns real {@code TextDisplay} /
 * {@code BlockDisplay} / {@code ItemDisplay} entities, then manages visibility
 * with {@code Player#hideEntity} / {@code showEntity}.
 *
 * <p>Per plan §1.5 / §7.1 the class file itself <strong>never imports the
 * 1.19.4-only types</strong>. Every Display-API call goes through cached
 * reflective {@link Method} / {@link Class} handles resolved in {@link #INIT}.
 * On 1.12.x — where {@link BackendSelector} would never choose this backend —
 * the class still loads cleanly because no Display symbol is referenced at
 * file scope.
 */
public final class DisplayEntityBackend implements HologramBackend {

    private final Main plugin;
    private final String description;

    /** Live entities allocated per hologram, ordered by line index. */
    private final Map<UUID, List<Entity>> spawnedByHologram = new ConcurrentHashMap<>();

    /** Map hologram-id-UUID → its Hologram for tear-down. Keyed by random UUID per spawn. */
    public DisplayEntityBackend(Main plugin, String description) {
        this.plugin = plugin;
        this.description = description;
    }

    @Override
    public void spawn(Hologram hologram, Collection<? extends Player> initialViewers) {
        if (!INIT.ok || hologram == null) {
            return;
        }
        Location origin = hologram.getLocation();
        World world = origin == null ? null : origin.getWorld();
        if (world == null) {
            return;
        }
        // Despawn any prior entities for this hologram before re-spawning.
        destroy(hologram);

        List<HologramLine> lines = hologram.getLines();
        HologramSettings settings = hologram.getSettings();
        List<Entity> entities = new ArrayList<>(lines.size());
        List<Integer> entityIds = new ArrayList<>(lines.size());

        double lineSpacing = 0.27 * settings.getScale();
        double y = 0.0;
        // We render lines top-down: the first line in the list sits highest.
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            Location lineLoc = origin.clone().add(0.0, (lines.size() - 1 - i) * lineSpacing + 0.1, 0.0);
            Entity entity = spawnLine(world, lineLoc, line, settings, hologram);
            if (entity == null) {
                continue;
            }
            entities.add(entity);
            entityIds.add(entity.getEntityId());
        }

        // Phase 5 — block-display board behind the text. The board entity
        // shares the hologram's billboard so it tracks the same orientation
        // as the line stack, sitting slightly behind on entity-local Z.
        if (settings.isBoardEnabled()) {
            Entity board = spawnBoard(world, origin, settings, lineSpacing, lines.size());
            if (board != null) {
                entities.add(board);
                entityIds.add(board.getEntityId());
            }
        }

        spawnedByHologram.put(hologramKey(hologram), entities);
        hologram.setEntityIds(entityIds);

        // Hide from everyone first, then reveal to the initial viewer set.
        if (INIT.playerHideEntity != null && INIT.playerShowEntity != null) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                hideFrom(online, entities);
            }
            if (initialViewers != null) {
                for (Player viewer : initialViewers) {
                    if (viewer != null) {
                        showTo(viewer, entities);
                        hologram.getCurrentViewers().add(viewer.getUniqueId());
                    }
                }
            }
        } else if (initialViewers != null) {
            // No hideEntity/showEntity — entities are visible to every player in tracking range.
            for (Player viewer : initialViewers) {
                if (viewer != null) {
                    hologram.getCurrentViewers().add(viewer.getUniqueId());
                }
            }
        }
        hologram.clearDirty();
    }

    @Override
    public void updateVisibility(Hologram hologram, Player viewer, boolean shouldSee) {
        if (!INIT.ok || hologram == null || viewer == null) {
            return;
        }
        List<Entity> entities = spawnedByHologram.get(hologramKey(hologram));
        if (entities == null) {
            return;
        }
        UUID uuid = viewer.getUniqueId();
        boolean already = hologram.getCurrentViewers().contains(uuid);
        if (shouldSee && !already) {
            showTo(viewer, entities);
            hologram.getCurrentViewers().add(uuid);
        } else if (!shouldSee && already) {
            hideFrom(viewer, entities);
            hologram.getCurrentViewers().remove(uuid);
        }
    }

    @Override
    public void applyMutations(Hologram hologram) {
        if (!INIT.ok || hologram == null || !hologram.isDirty()) {
            return;
        }
        // Simplest correct strategy: re-spawn. Lines / settings can change in
        // ways that don't map 1:1 onto a partial-update path (icon→text swaps,
        // line removals, scale changes that affect spacing). The performance
        // hit is acceptable because edits are operator-driven and infrequent.
        Collection<Player> viewers = snapshotViewers(hologram);
        destroy(hologram);
        spawn(hologram, viewers);
    }

    @Override
    public void destroy(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        List<Entity> entities = spawnedByHologram.remove(hologramKey(hologram));
        if (entities != null) {
            for (Entity entity : entities) {
                if (entity != null) {
                    try {
                        entity.remove();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        hologram.setEntityIds(Collections.<Integer>emptyList());
        hologram.getCurrentViewers().clear();
    }

    @Override
    public boolean supportsPerViewerText() {
        return true;
    }

    @Override
    public String describe() {
        return description;
    }

    private Entity spawnLine(World world, Location loc, HologramLine line, HologramSettings settings) {
        return spawnLine(world, loc, line, settings, null);
    }

    private Entity spawnLine(World world, Location loc, HologramLine line, HologramSettings settings, Hologram hologram) {
        try {
            switch (line.getType()) {
                case TEXT:
                    return spawnText(world, loc, (HologramLine.TextLine) line, settings, hologram);
                case ICON:
                    return spawnIcon(world, loc, (HologramLine.IconLine) line, settings);
                case BLOCK:
                    return spawnBlock(world, loc, (HologramLine.BlockLine) line, settings);
                default:
                    return null;
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[Holograms] Failed to spawn display line: " + throwable.getMessage());
            return null;
        }
    }

    private Entity spawnText(World world, Location loc, HologramLine.TextLine line, HologramSettings settings) throws Exception {
        return spawnText(world, loc, line, settings, null);
    }

    private Entity spawnText(World world, Location loc, HologramLine.TextLine line, HologramSettings settings, Hologram hologram) throws Exception {
        Entity entity = (Entity) INIT.worldSpawn.invoke(world, loc, INIT.textDisplay);
        applyCommonDisplaySettings(entity, settings);
        if (INIT.setText != null) {
            String resolved = HologramTextResolver.resolve(line.getTemplate(), hologram, null);
            if (resolved == null || resolved.isEmpty()) {
                resolved = ChatColor.translateAlternateColorCodes('&', line.getTemplate());
            }
            INIT.setText.invoke(entity, resolved);
        }
        if (INIT.setLineWidth != null) {
            INIT.setLineWidth.invoke(entity, settings.getLineWidth());
        }
        if (INIT.setShadowed != null) {
            INIT.setShadowed.invoke(entity, settings.hasShadow());
        }
        if (INIT.setSeeThrough != null) {
            INIT.setSeeThrough.invoke(entity, settings.isSeeThrough());
        }
        if (INIT.setTextOpacity != null) {
            byte opacity = (byte) Math.max(-128, Math.min(127, settings.getTextOpacity() - 128));
            INIT.setTextOpacity.invoke(entity, opacity);
        }
        if (INIT.setBackgroundColor != null && INIT.colorFromArgb != null) {
            try {
                Object color = INIT.colorFromArgb.invoke(null, settings.getBackgroundColor());
                INIT.setBackgroundColor.invoke(entity, color);
            } catch (Throwable ignored) {
            }
        }
        if (INIT.setAlignment != null && INIT.alignmentEnum != null) {
            try {
                Object enumValue = enumValueOf(INIT.alignmentEnum, settings.getTextAlignment().name());
                if (enumValue != null) {
                    INIT.setAlignment.invoke(entity, enumValue);
                }
            } catch (Throwable ignored) {
            }
        }
        return entity;
    }

    private Entity spawnIcon(World world, Location loc, HologramLine.IconLine line, HologramSettings settings) throws Exception {
        Entity entity = (Entity) INIT.worldSpawn.invoke(world, loc, INIT.itemDisplay);
        applyCommonDisplaySettings(entity, settings);
        ItemStack stack = line.getStack();
        if (stack != null && INIT.setItemStack != null) {
            INIT.setItemStack.invoke(entity, stack);
        }
        return entity;
    }

    /**
     * Spawns the configured block-display board, sized via Transformation so a
     * single {@code BlockDisplay} entity carries the whole rectangle.
     *
     * <p>The board sits in entity-local space: translation pulls the model
     * back along {@code -Z} by {@code offsetBack}, shifts left by half its
     * width so it stays centered, and vertically aligns with the line stack;
     * scale stretches the unit block to {@code (width, height, depth)}.
     * Because both the board and the text lines run the same billboard, the
     * board rotates with them and stays behind the text on every viewer.
     */
    private Entity spawnBoard(World world, Location origin, HologramSettings settings,
                              double lineSpacing, int lineCount) {
        if (INIT.blockDisplay == null || INIT.setBlock == null
                || INIT.materialCreateBlockData == null || INIT.setTransformation == null
                || INIT.transformationCtor == null || INIT.vector3fCtor == null
                || INIT.quaternionfCtor == null) {
            return null;
        }
        Material material = Material.matchMaterial(settings.getBoardMaterial());
        if (material == null || !material.isBlock()) {
            // Fall back to STONE — every version since 1.8 has it and isBlock()-true.
            // WHITE_CONCRETE only exists on 1.13+ so we can't reference it directly
            // without breaking the legacy compile target.
            material = Material.STONE;
        }
        try {
            Entity entity = (Entity) INIT.worldSpawn.invoke(world, origin.clone(), INIT.blockDisplay);
            Object blockData = INIT.materialCreateBlockData.invoke(material);
            INIT.setBlock.invoke(entity, blockData);

            // Match the text's billboard + view range so visibility tracks together.
            if (INIT.setBillboard != null && INIT.billboardEnum != null) {
                Object enumValue = enumValueOf(INIT.billboardEnum, settings.getBillboard().name());
                if (enumValue != null) {
                    INIT.setBillboard.invoke(entity, enumValue);
                }
            }
            if (INIT.setViewRange != null) {
                float vr = (float) Math.min(64.0, Math.max(1.0, settings.getShowRange() / 16.0));
                INIT.setViewRange.invoke(entity, vr);
            }

            float width = (float) (settings.getBoardWidth() * settings.getScale());
            float configuredHeight = (float) settings.getBoardHeight();
            float height;
            if (configuredHeight > 0f) {
                height = (float) (configuredHeight * settings.getScale());
            } else {
                // Auto-fit to the line stack with a small margin above + below.
                float stackHeight = (float) (Math.max(1, lineCount) * lineSpacing + 0.2);
                height = stackHeight;
            }
            float depth = 0.05f;
            float midY = (float) ((Math.max(1, lineCount) - 1) * lineSpacing / 2.0 + 0.1);
            float backOffset = (float) settings.getBoardOffsetBack();

            Object translation = INIT.vector3fCtor.newInstance(-width / 2f, midY - height / 2f, -backOffset - depth);
            Object leftRotation = INIT.quaternionfCtor.newInstance(0f, 0f, 0f, 1f);
            Object scaleVec = INIT.vector3fCtor.newInstance(width, height, depth);
            Object rightRotation = INIT.quaternionfCtor.newInstance(0f, 0f, 0f, 1f);
            Object transformation = INIT.transformationCtor.newInstance(translation, leftRotation, scaleVec, rightRotation);
            INIT.setTransformation.invoke(entity, transformation);
            return entity;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[Holograms] Failed to spawn board: " + throwable.getMessage());
            return null;
        }
    }

    private Entity spawnBlock(World world, Location loc, HologramLine.BlockLine line, HologramSettings settings) throws Exception {
        Entity entity = (Entity) INIT.worldSpawn.invoke(world, loc, INIT.blockDisplay);
        applyCommonDisplaySettings(entity, settings);
        Material material = line.getMaterial();
        if (material != null && INIT.setBlock != null && INIT.materialCreateBlockData != null) {
            try {
                Object data = INIT.materialCreateBlockData.invoke(material);
                INIT.setBlock.invoke(entity, data);
            } catch (Throwable ignored) {
            }
        }
        return entity;
    }

    private void applyCommonDisplaySettings(Entity entity, HologramSettings settings) {
        try {
            if (INIT.setBillboard != null && INIT.billboardEnum != null) {
                Object enumValue = enumValueOf(INIT.billboardEnum, settings.getBillboard().name());
                if (enumValue != null) {
                    INIT.setBillboard.invoke(entity, enumValue);
                }
            }
            if (INIT.setViewRange != null) {
                // viewRange is a multiplier of the server's tracking range; map our
                // show-range to a sensible 0..max value (the API caps at ~64).
                float vr = (float) Math.min(64.0, Math.max(1.0, settings.getShowRange() / 16.0));
                INIT.setViewRange.invoke(entity, vr);
            }
            if (INIT.setTransformation != null && INIT.transformationCtor != null) {
                Object translation = INIT.vector3fCtor.newInstance(0f, 0f, 0f);
                Object leftRotation = INIT.quaternionfCtor.newInstance(0f, 0f, 0f, 1f);
                float s = (float) settings.getScale();
                Object scaleVec = INIT.vector3fCtor.newInstance(s, s, s);
                Object rightRotation = INIT.quaternionfCtor.newInstance(0f, 0f, 0f, 1f);
                Object transformation = INIT.transformationCtor.newInstance(translation, leftRotation, scaleVec, rightRotation);
                INIT.setTransformation.invoke(entity, transformation);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().fine("[Holograms] Display setter failed: " + throwable.getMessage());
        }
    }

    private void showTo(Player viewer, List<Entity> entities) {
        if (INIT.playerShowEntity == null) {
            return;
        }
        for (Entity entity : entities) {
            try {
                INIT.playerShowEntity.invoke(viewer, plugin, entity);
            } catch (Throwable ignored) {
            }
        }
    }

    private void hideFrom(Player viewer, List<Entity> entities) {
        if (INIT.playerHideEntity == null) {
            return;
        }
        for (Entity entity : entities) {
            try {
                INIT.playerHideEntity.invoke(viewer, plugin, entity);
            } catch (Throwable ignored) {
            }
        }
    }

    private Collection<Player> snapshotViewers(Hologram hologram) {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : hologram.getCurrentViewers()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    private UUID hologramKey(Hologram hologram) {
        return UUID.nameUUIDFromBytes(hologram.getId().value().getBytes());
    }

    private static Object enumValueOf(Class<?> enumClass, String name) {
        if (enumClass == null || name == null) {
            return null;
        }
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object value = Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
            return value;
        } catch (Throwable ignored) {
            return null;
        }
    }

    Main getPlugin() {
        return plugin;
    }

    // ── Reflection cache ──────────────────────────────────────────────────

    private static final ReflectionState INIT = new ReflectionState();

    private static final class ReflectionState {
        final boolean ok;
        final Class<?> textDisplay;
        final Class<?> blockDisplay;
        final Class<?> itemDisplay;
        final Class<?> billboardEnum;
        final Class<?> alignmentEnum;
        final Method worldSpawn;
        final Method setText;
        final Method setLineWidth;
        final Method setShadowed;
        final Method setSeeThrough;
        final Method setTextOpacity;
        final Method setBackgroundColor;
        final Method setAlignment;
        final Method setBillboard;
        final Method setViewRange;
        final Method setItemStack;
        final Method setBlock;
        final Method materialCreateBlockData;
        final Method setTransformation;
        final Method playerHideEntity;
        final Method playerShowEntity;
        final Method colorFromArgb;
        final java.lang.reflect.Constructor<?> transformationCtor;
        final java.lang.reflect.Constructor<?> vector3fCtor;
        final java.lang.reflect.Constructor<?> quaternionfCtor;

        ReflectionState() {
            Class<?> tdLocal = null, bdLocal = null, idLocal = null;
            Class<?> billboardLocal = null, alignmentLocal = null;
            Method spawnLocal = null, setTextLocal = null, setLineWidthLocal = null;
            Method setShadowedLocal = null, setSeeThroughLocal = null, setTextOpacityLocal = null;
            Method setBackgroundColorLocal = null, setAlignmentLocal = null, setBillboardLocal = null;
            Method setViewRangeLocal = null, setItemStackLocal = null, setBlockLocal = null;
            Method materialCreateBlockDataLocal = null, setTransformationLocal = null;
            Method playerHideLocal = null, playerShowLocal = null, colorFromArgbLocal = null;
            java.lang.reflect.Constructor<?> transformationCtorLocal = null;
            java.lang.reflect.Constructor<?> vector3fCtorLocal = null, quaternionfCtorLocal = null;
            boolean okLocal = false;
            try {
                tdLocal = Class.forName("org.bukkit.entity.TextDisplay");
                bdLocal = Class.forName("org.bukkit.entity.BlockDisplay");
                idLocal = Class.forName("org.bukkit.entity.ItemDisplay");
                Class<?> displayClass = Class.forName("org.bukkit.entity.Display");
                billboardLocal = Class.forName("org.bukkit.entity.Display$Billboard");
                try {
                    alignmentLocal = Class.forName("org.bukkit.entity.TextDisplay$TextAlignment");
                } catch (ClassNotFoundException ignored) {
                }
                spawnLocal = World.class.getMethod("spawn", Location.class, Class.class);
                try {
                    setTextLocal = tdLocal.getMethod("setText", String.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    setLineWidthLocal = tdLocal.getMethod("setLineWidth", int.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    setShadowedLocal = tdLocal.getMethod("setShadowed", boolean.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    setSeeThroughLocal = tdLocal.getMethod("setSeeThrough", boolean.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    setTextOpacityLocal = tdLocal.getMethod("setTextOpacity", byte.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    Class<?> colorClass = Class.forName("org.bukkit.Color");
                    setBackgroundColorLocal = tdLocal.getMethod("setBackgroundColor", colorClass);
                    try {
                        colorFromArgbLocal = colorClass.getMethod("fromARGB", int.class);
                    } catch (NoSuchMethodException ignored) {
                    }
                } catch (Throwable ignored) {
                }
                if (alignmentLocal != null) {
                    try {
                        setAlignmentLocal = tdLocal.getMethod("setAlignment", alignmentLocal);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                try {
                    setBillboardLocal = displayClass.getMethod("setBillboard", billboardLocal);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    setViewRangeLocal = displayClass.getMethod("setViewRange", float.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    setItemStackLocal = idLocal.getMethod("setItemStack", ItemStack.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    Class<?> blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
                    setBlockLocal = bdLocal.getMethod("setBlock", blockDataClass);
                    materialCreateBlockDataLocal = Material.class.getMethod("createBlockData");
                } catch (Throwable ignored) {
                }
                try {
                    Class<?> transformationClass = Class.forName("org.bukkit.util.Transformation");
                    Class<?> vector3fClass = Class.forName("org.joml.Vector3f");
                    Class<?> quaternionfClass = Class.forName("org.joml.Quaternionf");
                    setTransformationLocal = displayClass.getMethod("setTransformation", transformationClass);
                    transformationCtorLocal = transformationClass.getConstructor(vector3fClass, quaternionfClass, vector3fClass, quaternionfClass);
                    vector3fCtorLocal = vector3fClass.getConstructor(float.class, float.class, float.class);
                    quaternionfCtorLocal = quaternionfClass.getConstructor(float.class, float.class, float.class, float.class);
                } catch (Throwable ignored) {
                }
                // Player#hideEntity / showEntity — present on Bukkit since 1.18+,
                // Paper has shipped them since well before that. Probe for the
                // two-arg variant accepting (Plugin, Entity).
                Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
                Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
                try {
                    playerHideLocal = playerClass.getMethod("hideEntity", pluginClass, Entity.class);
                    playerShowLocal = playerClass.getMethod("showEntity", pluginClass, Entity.class);
                } catch (NoSuchMethodException ignored) {
                    // Older API — entities will be visible to all players in tracking range.
                }
                okLocal = true;
            } catch (Throwable ignored) {
                okLocal = false;
            }
            ok = okLocal;
            textDisplay = tdLocal;
            blockDisplay = bdLocal;
            itemDisplay = idLocal;
            billboardEnum = billboardLocal;
            alignmentEnum = alignmentLocal;
            worldSpawn = spawnLocal;
            setText = setTextLocal;
            setLineWidth = setLineWidthLocal;
            setShadowed = setShadowedLocal;
            setSeeThrough = setSeeThroughLocal;
            setTextOpacity = setTextOpacityLocal;
            setBackgroundColor = setBackgroundColorLocal;
            setAlignment = setAlignmentLocal;
            setBillboard = setBillboardLocal;
            setViewRange = setViewRangeLocal;
            setItemStack = setItemStackLocal;
            setBlock = setBlockLocal;
            materialCreateBlockData = materialCreateBlockDataLocal;
            setTransformation = setTransformationLocal;
            playerHideEntity = playerHideLocal;
            playerShowEntity = playerShowLocal;
            colorFromArgb = colorFromArgbLocal;
            transformationCtor = transformationCtorLocal;
            vector3fCtor = vector3fCtorLocal;
            quaternionfCtor = quaternionfCtorLocal;
        }
    }

    /** Suppresses unused-warnings on local helper. */
    @SuppressWarnings("unused")
    private static Field unusedFieldRef() {
        return null;
    }
}
