pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    // black.java-standards (formatting and compiler warnings) comes from JitPack, which serves it by its repository's name.
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "black.java-standards") useModule("com.github.Brendan-Black.black-java:plugin:${requested.version}")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "gradle-plugin"
