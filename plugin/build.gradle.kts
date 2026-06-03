// :plugin — the aggregator. Holds the thin OBX bootstrap + resources, depends on
// core + every feature, and Shadow-merges them all into one jar. ProGuard runs on
// the merged jar in Phase 5 to produce the obfuscated + unobfuscated artifacts.
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

tasks.shadowJar {
    archiveBaseName.set("OBX")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
