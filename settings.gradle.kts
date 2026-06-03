pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.md-5.net/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "OBX"

include(":api", ":core", ":plugin", ":platform:paper")

listOf(
    "chat", "economy", "enchant", "hologram", "hub", "item", "jail", "kit", "mail",
    "moderation", "nickname", "playerinfo", "playerstate", "scoreboard", "staff",
    "tablist", "teleport", "warp", "world",
).forEach { include(":features:$it") }
