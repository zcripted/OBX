// Shared Java config for every OBX module: Java 8 bytecode (matching the Spigot
// 1.12 baseline, so the single jar runs 1.8.8 -> 26.1 via runtime detection),
// the provided server APIs as compileOnly (also on the test classpath), and JUnit 5.
plugins {
    java
}

group = "dev.zcripted"
version = "1.0.0-beta-b1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    "compileOnly"("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    "compileOnly"("net.md-5:bungeecord-chat:1.12-SNAPSHOT")
    "compileOnly"("io.netty:netty-all:4.1.97.Final")
    "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
}

// Mirror Maven's `provided` scope: compileOnly deps must reach the test classpath.
configurations.named("testImplementation").configure {
    extendsFrom(configurations.named("compileOnly").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
