# =============================================================================
# OBX ProGuard configuration
# -----------------------------------------------------------------------------
# Optimizes and obfuscates the plugin while preserving the entry points Bukkit
# / Spigot / Paper / Folia look up reflectively (the Main class, Listener
# implementations, command executors, inventory holders, and BukkitRunnable
# subclasses). Shrinking is intentionally enabled so unused methods/classes are
# stripped from the obfuscated JAR.
# =============================================================================

# --- Library classes --------------------------------------------------------
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# --- Optimization passes ----------------------------------------------------
# Several iterations let ProGuard inline trivial methods, fold constants, and
# remove dead branches. 5 is the conventional sweet spot — beyond that returns
# diminish.
-optimizationpasses 5

# Skip optimizations that interfere with reflection-driven classloading: most
# notably class merging (which can change the public class name a plugin user
# would see in stack traces) and aggressive overloading (which makes legacy
# decompilers produce nonsense).
-optimizations !code/allocation/variable

# Collapse all renamable classes into the root package. Class names are already
# obfuscated to single letters; flattening the package path removes the repeated
# `dev/zcripted/obx/<pkg>/...` strings from every constant pool and directory
# entry. Kept classes (see -keepclassmembers below) retain their real names.
-repackageclasses ''

# --- Notes / warnings -------------------------------------------------------
-ignorewarnings
-dontnote net.md_5.**
-dontnote jdk.internal.jimage.**
-dontnote jdk.internal.jrtfs.**
-dontnote dev.zcripted.obx.feature.staff.gui.AdminSubMenu
-dontnote dev.zcripted.obx.core.gui.WarpMenuStyling
-dontnote dev.zcripted.obx.util.text.ComponentMessenger
-dontnote org.bukkit.scheduler.**
-dontwarn java.**
-dontwarn javax.**
-dontwarn org.bukkit.**
-dontwarn net.md_5.**
-dontwarn org.apache.commons.**
-dontwarn sun.**
-dontwarn com.sun.net.httpserver.**
-dontwarn dev.zcripted.obx.feature.staff.gui.AdminSubMenu$SubMenuType
-dontwarn dev.zcripted.obx.feature.staff.gui.AdminSubMenu$ClearMode

# --- Keep attributes useful for reflection ---------------------------------
-keepattributes *Annotation*,Signature,InnerClasses

# --- Plugin entry point -----------------------------------------------------
# Bukkit reads plugin.yml, then loads our Main class by name and invokes its
# public lifecycle methods. Keep both the class and every public member so
# nothing gets renamed underneath the platform.
-keep public class dev.zcripted.obx.OBX {
    public *;
}

# --- Bukkit event listeners -------------------------------------------------
# PluginManager.registerEvents finds @EventHandler-annotated methods by name
# and invokes them reflectively. Keep every Listener subclass with its public
# constructor and EventHandler methods. Allow non-EventHandler private members
# to be obfuscated.
# Listeners are instantiated directly (new XxxListener(...)) and registered by
# INSTANCE, so their class names need not survive — only the public constructor
# and @EventHandler methods (which PluginManager invokes reflectively by name).
# Allowing the class name to be obfuscated/merged is what shrinks the jar.
-keepclassmembers class * implements org.bukkit.event.Listener {
    public <init>(...);
    @org.bukkit.event.EventHandler <methods>;
}

# --- Command executors / tab completers ------------------------------------
# Main.bind() instantiates these directly, but Bukkit also calls onCommand()
# and onTabComplete() reflectively. Keep the public surface so the command
# dispatcher can find them on every fork.
# Command executors are instantiated directly by Main.bind(); Bukkit calls
# onCommand()/onTabComplete() on that instance (it never looks the class up by
# name). Keeping the methods is enough — the class name can be obfuscated.
# HelpGuiMenu already treats its command-category MAP as authoritative precisely
# because the package path is renamed under obfuscation (its getPackage() probe
# is a tolerated fallback), so renaming these is safe.
-keepclassmembers class * implements org.bukkit.command.CommandExecutor {
    public <init>(...);
    public boolean onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[]);
}
-keepclassmembers class * implements org.bukkit.command.TabCompleter {
    public <init>(...);
    public java.util.List onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[]);
}

# --- Inventory holders ------------------------------------------------------
# When a player clicks a OBX GUI, Bukkit hands the click event to the
# corresponding listener which calls inventory.getHolder() to recover our
# wrapper object. The holder's class identity must survive obfuscation
# (instanceof checks would otherwise miss).
# Holders are matched with instanceof in our own listeners. ProGuard renames the
# class and every instanceof reference CONSISTENTLY, so the checks still pass —
# the class name need not be preserved. getInventory() overrides the library
# interface so it is kept regardless; keep the public constructor too.
-keepclassmembers class * implements org.bukkit.inventory.InventoryHolder {
    public <init>(...);
    public *** getInventory();
}

# --- BukkitRunnable / BukkitTask --------------------------------------------
# A few legacy paths still reference these by type. The SchedulerAdapter has
# replaced direct usage internally, but Bukkit's scheduler resolves the run()
# method by name when a runnable is dispatched, so keep that signature intact.
-keep class org.bukkit.scheduler.BukkitRunnable { *; }
-keep class org.bukkit.scheduler.BukkitTask { *; }
-keepclassmembers class * extends org.bukkit.scheduler.BukkitRunnable {
    public void run();
}

# --- Enums ------------------------------------------------------------------
# Bukkit and our own placeholder maps occasionally compare enum constants by
# name. Keep the standard values()/valueOf(String) accessors.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Public constants used in plugin.yml / language defaults ---------------
# MessageDefaults is loaded via static initialization from LanguageManager and
# its public fields hold the embedded default strings. The class itself is
# fine to obfuscate — only its `defaults(...)` and `sectionComments(...)`
# entry points need a stable public surface.
-keepclassmembers class dev.zcripted.obx.core.language.MessageDefaults {
    public static *;
}
# --- Paper-native bootstrap (paper-plugin.yml references these by name) ---------
-keep class dev.zcripted.obx.bootstrap.OBXBootstrap { *; }
-keep class dev.zcripted.obx.bootstrap.OBXPaperLoader { *; }
-dontwarn io.papermc.paper.**
-dontwarn org.eclipse.aether.**
-dontwarn org.jetbrains.annotations.**
