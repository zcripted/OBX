plugins {
    id("obx.feature-conventions")
}

dependencies {
    // PlaceholderAPI is a soft dependency: compileOnly so it is NEVER shaded; the
    // expansion class only loads when the PlaceholderAPI plugin is present at runtime.
    compileOnly("me.clip:placeholderapi:2.11.6")
}
