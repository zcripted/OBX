// TEMPORARY single-module build used to verify the decoupling/platform phases
// of the Maven -> Gradle migration. Replaced by the multi-module structure
// (api/core/platform/features/plugin) in Phase 4.
plugins {
    java
}

group = "dev.zcripted"
version = "1.0.0-beta-b1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.md-5.net/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

// Mirror Maven's `provided` scope: compileOnly deps (spigot-api, etc.) must also
// be visible to the test classpath (tests touch Bukkit's YamlConfiguration).
configurations.testImplementation.get().extendsFrom(configurations.compileOnly.get())

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-chat:1.12-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.97.Final")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

java {
    // Matches the Maven build: source/target 8 (not --release), compiled on JDK 17.
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
