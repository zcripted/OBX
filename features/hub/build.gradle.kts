plugins {
    id("obx.feature-conventions")
}

dependencies {
    // /hub (admin) opens the staff admin hub menu
    implementation(project(":features:staff"))
}