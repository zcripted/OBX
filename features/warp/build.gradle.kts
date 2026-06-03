plugins {
    id("obx.feature-conventions")
}

dependencies {
    // warp menus navigate back to the staff admin menu
    implementation(project(":features:staff"))
}
