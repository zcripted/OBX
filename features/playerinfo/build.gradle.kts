plugins {
    id("obx.feature-conventions")
}

dependencies {
    // join nameplate coloring uses tablist sort teams
    implementation(project(":features:tablist"))
}
