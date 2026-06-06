import proguard.gradle.ProGuardTask

buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("com.guardsquare:proguard-gradle:7.5.0") }
}

// :plugin — the aggregator. Holds the thin OBX bootstrap + resources, depends on
// core + every feature. Shadow merges them into one jar; ProGuard then obfuscates
// that merged jar, producing the shippable obfuscated + unobfuscated artifacts.
plugins {
    id("obx.java-conventions")
    alias(libs.plugins.shadow)
    // Local live-test harness: `./gradlew runServer` boots a real Paper server with the freshly
    // shadow-built OBX jar. Test/dev only — not part of the shippable build.
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    listOf(
        "backpack", "chat", "deathdrop", "economy", "enchant", "hologram", "hub", "item", "jail", "kit",
        "mail", "moderation", "nickname", "playerinfo", "playerstate", "scoreboard", "staff",
        "tablist", "teleport", "warp", "world",
    ).forEach { implementation(project(":features:$it")) }
    // ProGuard needs the PlaceholderAPI superclass on the library classpath (the
    // economy module compiles its %obx_*% expansion against it, compileOnly).
    compileOnly("me.clip:placeholderapi:2.11.6")
}

// Shadow produces the UNOBFUSCATED merged jar: OBX-<version>-unobf.jar
tasks.shadowJar {
    archiveBaseName.set("OBX")
    archiveClassifier.set("unobf")
    archiveVersion.set(project.version.toString())
}

// ProGuard reads the shaded jar and writes the OBFUSCATED jar: OBX-<version>.jar
tasks.register<ProGuardTask>("proguard") {
    group = "build"
    description = "Obfuscates the Shadow-merged jar into the shippable OBX jar."

    dependsOn(tasks.shadowJar)
    injars(tasks.shadowJar.get().archiveFile.get().asFile)
    outjars(layout.buildDirectory.file("libs/OBX-${project.version}.jar").get().asFile)

    // Library classpath: the JDK base module + the provided server APIs (not shaded).
    libraryjars(
        mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class"),
        "${System.getProperty("java.home")}/jmods/java.base.jmod",
    )
    libraryjars(configurations.named("compileClasspath"))

    configuration(rootProject.file("proguard.pro"))
    printmapping(layout.buildDirectory.file("proguard/mapping.txt").get().asFile)
}

// `build` produces both jars (ProGuard depends on Shadow).
tasks.named("build") {
    dependsOn("proguard")
}

// ── Live test harness ───────────────────────────────────────────────────────
// `./gradlew runServer` downloads Paper 1.21.4 (first run only), then boots it with the unobfuscated
// shadow jar (readable stack traces) loaded as a plugin. Paper 1.21.4 requires Java 21 to RUN, while
// the project compiles on JDK 17 — so the server JVM is a Java-21 toolchain (auto-provisioned by the
// foojay resolver configured in settings.gradle.kts).
//
// NOTE: starting the server requires accepting Mojang's EULA. On first run Paper writes
// `plugin/run/eula.txt` with `eula=false` and stops; set it to `eula=true` to proceed.
tasks.runServer {
    minecraftVersion("1.21.4")
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    )
}
