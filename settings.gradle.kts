pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Lets Gradle AUTO-DOWNLOAD a JDK toolchain when one isn't installed locally — used so the
// run-paper `runServer` task can launch Paper 1.21.4 on Java 21 even though the build runs on JDK 17.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.md-5.net/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        // PlaceholderAPI (compileOnly in :features:economy - soft-depend at runtime)
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

rootProject.name = "OBX"

include(":api", ":core", ":plugin")

listOf(
    "backpack", "chat", "deathdrop", "economy", "enchant", "hologram", "hub", "item", "jail", "kit",
    "mail", "moderation", "nickname", "playerinfo", "playerstate", "scoreboard", "staff",
    "tablist", "teleport", "warp", "world",
).forEach { include(":features:$it") }
