// :core — platform-agnostic framework: module system, command/gui/storage/locale
// frameworks, the platform abstraction seam, and stateless util helpers. Depends
// on the :api surface.
plugins {
    id("obx.java-conventions")
}

dependencies {
    implementation(project(":api"))
}
