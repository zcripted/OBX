package dev.zcripted.obx.hologram.backend;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramLine;
import dev.zcripted.obx.hologram.model.HologramSettings;
import dev.zcripted.obx.hologram.text.HologramTextResolver;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy backend for Spigot / Paper 1.12 → 1.19.3 where display entities are
 * unavailable. Uses invisible marker armor stands with custom names for text,
 * head-worn items for icons, and head-worn block items for blocks (plan §8).
 *
 * <p>Per the plan §8.2 — per-viewer text is not supported; the renderer
 * resolves text with no player context and the same string is shown to every
 * viewer. The class never imports a Paper-only or 1.19.4-only type so it
 * compiles and loads cleanly on the 1.12.2 baseline.
 */
public final class ArmorStandBackend implements HologramBackend {

    private final OBX plugin;
    private final String description;

    private final Map<UUID, List<Entity>> spawnedByHologram = new ConcurrentHashMap<>();

    /** Cached Player#hideEntity / showEntity, probed once. May be {@code null} on very old API levels. */
    private static final Method PLAYER_HIDE_ENTITY = resolveHide();
    private static final Method PLAYER_SHOW_ENTITY = resolveShow();

    public ArmorStandBackend(OBX plugin, String description) {
        this.plugin = plugin;
        this.description = description;
    }

    @Override
    public void spawn(Hologram hologram, Collection<? extends Player> initialViewers) {
        if (hologram == null) {
            return;
        }
        Location origin = hologram.getLocation();
        World world = origin == null ? null : origin.getWorld();
        if (world == null) {
            return;
        }
        destroy(hologram);

        List<HologramLine> lines = hologram.getLines();
        HologramSettings settings = hologram.getSettings();
        List<Entity> entities = new ArrayList<>(lines.size());
        List<Integer> entityIds = new ArrayList<>(lines.size());

        double lineSpacing = 0.27 * settings.getScale();
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            Location lineLoc = origin.clone().add(0.0, (lines.size() - 1 - i) * lineSpacing + 0.1, 0.0);
            Entity entity = spawnLine(world, lineLoc, line, hologram);
            if (entity == null) {
                continue;
            }
            entities.add(entity);
            entityIds.add(entity.getEntityId());
        }
        spawnedByHologram.put(hologramKey(hologram), entities);
        hologram.setEntityIds(entityIds);

        if (PLAYER_HIDE_ENTITY != null && PLAYER_SHOW_ENTITY != null) {
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
        if (hologram == null || viewer == null) {
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
        if (hologram == null || !hologram.isDirty()) {
            return;
        }
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
        return false;
    }

    @Override
    public String describe() {
        return description;
    }

    private Entity spawnLine(World world, Location loc, HologramLine line, Hologram hologram) {
        try {
            ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setRemoveWhenFarAway(false);
            stand.setCustomNameVisible(false);
            switch (line.getType()) {
                case TEXT: {
                    HologramLine.TextLine text = (HologramLine.TextLine) line;
                    String resolved = HologramTextResolver.resolve(text.getTemplate(), hologram, null);
                    if (resolved == null || resolved.isEmpty()) {
                        resolved = ChatColor.translateAlternateColorCodes('&', text.getTemplate());
                    }
                    stand.setCustomName(resolved);
                    stand.setCustomNameVisible(true);
                    break;
                }
                case ICON: {
                    HologramLine.IconLine icon = (HologramLine.IconLine) line;
                    ItemStack stack = icon.getStack();
                    if (stack != null && stand.getEquipment() != null) {
                        stand.getEquipment().setHelmet(stack);
                    }
                    break;
                }
                case BLOCK: {
                    HologramLine.BlockLine block = (HologramLine.BlockLine) line;
                    Material material = block.getMaterial();
                    if (material != null && stand.getEquipment() != null) {
                        stand.getEquipment().setHelmet(new ItemStack(material));
                    }
                    break;
                }
                default:
                    break;
            }
            // Restart-surviving marker so a crash can't leave orphans that the
            // next boot duplicates (scrubbed in HologramRenderer.spawnAll).
            dev.zcripted.obx.hologram.HologramTag.tag(stand);
            return stand;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[Holograms] Failed to spawn armor-stand line: " + throwable.getMessage());
            return null;
        }
    }

    private void showTo(Player viewer, List<Entity> entities) {
        if (PLAYER_SHOW_ENTITY == null) {
            return;
        }
        for (Entity entity : entities) {
            try {
                PLAYER_SHOW_ENTITY.invoke(viewer, plugin, entity);
            } catch (Throwable ignored) {
            }
        }
    }

    private void hideFrom(Player viewer, List<Entity> entities) {
        if (PLAYER_HIDE_ENTITY == null) {
            return;
        }
        for (Entity entity : entities) {
            try {
                PLAYER_HIDE_ENTITY.invoke(viewer, plugin, entity);
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

    private static Method resolveHide() {
        try {
            return Class.forName("org.bukkit.entity.Player").getMethod("hideEntity",
                    Class.forName("org.bukkit.plugin.Plugin"), Entity.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method resolveShow() {
        try {
            return Class.forName("org.bukkit.entity.Player").getMethod("showEntity",
                    Class.forName("org.bukkit.plugin.Plugin"), Entity.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    OBX getPlugin() {
        return plugin;
    }
}
