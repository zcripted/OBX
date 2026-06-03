package dev.zcripted.obx.feature.jail.model;

import org.bukkit.Location;

public final class Jail {

    private final String name;
    private final Location location;

    public Jail(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public String getName() { return name; }
    public Location getLocation() { return location; }
}
