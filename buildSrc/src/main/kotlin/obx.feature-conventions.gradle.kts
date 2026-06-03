// Convention for feature modules: Java config + dependency on the stable :api
// surface and the :core framework. Feature-to-feature deps (the few that exist)
// are declared in the feature's own build.gradle.kts.
plugins {
    id("obx.java-conventions")
}

dependencies {
    "implementation"(project(":api"))
    "implementation"(project(":core"))
}
