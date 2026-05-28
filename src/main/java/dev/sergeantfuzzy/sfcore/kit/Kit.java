package dev.sergeantfuzzy.sfcore.kit;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public final class Kit {

    private final String name;
    private final String displayName;
    private final long cooldownSeconds;
    private final List<ItemStack> items;
    private final boolean firstJoin;
    private final String iconMaterial;

    public Kit(String name, String displayName, long cooldownSeconds, List<ItemStack> items,
               boolean firstJoin, String iconMaterial) {
        this.name = name;
        this.displayName = displayName == null ? name : displayName;
        this.cooldownSeconds = Math.max(0L, cooldownSeconds);
        this.items = items == null ? Collections.<ItemStack>emptyList() : items;
        this.firstJoin = firstJoin;
        this.iconMaterial = iconMaterial;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public List<ItemStack> getItems() { return items; }
    public boolean isFirstJoin() { return firstJoin; }
    public String getIconMaterial() { return iconMaterial; }
}
