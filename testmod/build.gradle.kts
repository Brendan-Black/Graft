import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    eclipse
    id("black.java-standards")
    id("net.ltgt.errorprone")
    // Minecraft, the graft configuration, and gameTest, as for any mod.
    id("black.graft.mod")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

version = "1.0.0"

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }

// Graft's API exercised in game tests; built and run by Graft's own build only, never published.
dependencies {
    compileOnly(project(":api"))
    graft(project(":loader"))
    errorprone("com.google.errorprone:error_prone_core:2.52.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}

// The IDE (Eclipse model, as VS Code imports it) shows compile-only project dependencies to test code only; this shows the API to src/ too.
val ideApi = configurations.create("ideApi") { isTransitive = false }
dependencies { ideApi(project(":api")) }
eclipse.classpath.plusConfigurations.add(ideApi)

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-Xlint:all,-processing,-classfile")
    options.errorprone {
        disableAllWarnings = true
        error("NullAway")
        option("NullAway:AnnotatedPackages", "black.graft")
    }
}
