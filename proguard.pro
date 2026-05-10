# =============================================================================
# SF-Core ProGuard configuration
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
-optimizations !class/merging/*,!code/allocation/variable

# --- Notes / warnings -------------------------------------------------------
-ignorewarnings
-dontnote net.md_5.**
-dontnote jdk.internal.jimage.**
-dontnote jdk.internal.jrtfs.**
-dontnote dev.sergeantfuzzy.sfcore.gui.admin.AdminSubMenu
-dontnote dev.sergeantfuzzy.sfcore.gui.shared.WarpMenuStyling
-dontnote dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger
-dontnote org.bukkit.scheduler.**
-dontwarn java.**
-dontwarn javax.**
-dontwarn org.bukkit.**
-dontwarn net.md_5.**
-dontwarn org.apache.commons.**
-dontwarn sun.**
-dontwarn com.sun.net.httpserver.**
-dontwarn dev.sergeantfuzzy.sfcore.gui.admin.AdminSubMenu$SubMenuType
-dontwarn dev.sergeantfuzzy.sfcore.gui.admin.AdminSubMenu$ClearMode

# --- Keep attributes useful for reflection ---------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# --- Plugin entry point -----------------------------------------------------
# Bukkit reads plugin.yml, then loads our Main class by name and invokes its
# public lifecycle methods. Keep both the class and every public member so
# nothing gets renamed underneath the platform.
-keep public class dev.sergeantfuzzy.sfcore.Main {
    public *;
}

# --- Bukkit event listeners -------------------------------------------------
# PluginManager.registerEvents finds @EventHandler-annotated methods by name
# and invokes them reflectively. Keep every Listener subclass with its public
# constructor and EventHandler methods. Allow non-EventHandler private members
# to be obfuscated.
-keep class * implements org.bukkit.event.Listener
-keepclassmembers class * implements org.bukkit.event.Listener {
    public <init>(...);
    @org.bukkit.event.EventHandler <methods>;
}

# --- Command executors / tab completers ------------------------------------
# Main.bind() instantiates these directly, but Bukkit also calls onCommand()
# and onTabComplete() reflectively. Keep the public surface so the command
# dispatcher can find them on every fork.
-keep class * implements org.bukkit.command.CommandExecutor
-keepclassmembers class * implements org.bukkit.command.CommandExecutor {
    public <init>(...);
    public boolean onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[]);
}
-keepclassmembers class * implements org.bukkit.command.TabCompleter {
    public <init>(...);
    public java.util.List onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[]);
}

# --- Inventory holders ------------------------------------------------------
# When a player clicks a SF-Core GUI, Bukkit hands the click event to the
# corresponding listener which calls inventory.getHolder() to recover our
# wrapper object. The holder's class identity must survive obfuscation
# (instanceof checks would otherwise miss).
-keep class * implements org.bukkit.inventory.InventoryHolder
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
-keepclassmembers class dev.sergeantfuzzy.sfcore.language.MessageDefaults {
    public static *;
}
