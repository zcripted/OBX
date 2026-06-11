plugins {
    id("obx.feature-conventions")
}

dependencies {
    // staff admin menu drives moderation + world (gamerule/server) controls
    implementation(project(":features:economy"))
    implementation(project(":features:moderation"))
    implementation(project(":features:world"))
}