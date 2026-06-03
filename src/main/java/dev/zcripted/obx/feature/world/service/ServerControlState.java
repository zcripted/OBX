package dev.zcripted.obx.feature.world.service;

public final class ServerControlState {

    private static volatile boolean joinLocked = false;
    private static volatile boolean redstoneFrozen = false;

    private ServerControlState() {
    }

    public static boolean isJoinLocked() {
        return joinLocked;
    }

    public static boolean toggleJoinLock() {
        joinLocked = !joinLocked;
        return joinLocked;
    }

    public static void setJoinLocked(boolean locked) {
        joinLocked = locked;
    }

    public static boolean isRedstoneFrozen() {
        return redstoneFrozen;
    }

    public static boolean toggleRedstoneFrozen() {
        redstoneFrozen = !redstoneFrozen;
        return redstoneFrozen;
    }

    public static void setRedstoneFrozen(boolean frozen) {
        redstoneFrozen = frozen;
    }
}
