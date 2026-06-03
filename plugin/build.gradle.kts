import org.gradle.api.tasks.bundling.Jar
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
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    listOf(
        "chat", "economy", "enchant", "hologram", "hub", "item", "jail", "kit", "mail",
        "moderation", "nickname", "playerinfo", "playerstate", "scoreboard", "staff",
        "tablist", "teleport", "warp", "world",
    ).forEach { implementation(project(":features:$it")) }
}

// Shadow produces the UNOBFUSCATED merged jar: OBX-<version>-unobf.jar
tasks.shadowJar {
    archiveBaseName.set("OBX")
    archiveClassifier.set("unobf")
    archiveVersion.set(project.version.toString())
    // Bundle the Java-17 Paper-native bootstrap/loader classes by merging the
    // :platform:paper jar directly (a plain project dependency is rejected by
    // Gradle's JVM-version compatibility check: 8 consumer vs 17 producer).
    from(zipTree(project(":platform:paper").tasks.named<Jar>("jar").flatMap { it.archiveFile }))
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
