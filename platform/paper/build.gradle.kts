plugins {
    java
}

group = "dev.zcripted"
version = "1.0.0-beta-b1"

// Paper-native module: the paper-plugin.yml bootstrapper + library loader. Compiles
// against the modern Paper API (Java 8 bytecode against the Paper classpath); these classes load ONLY on Paper >= 1.20
// (which uses paper-plugin.yml), never on Spigot/older Paper, so they do not affect
// the single-jar 1.8.8 baseline.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
