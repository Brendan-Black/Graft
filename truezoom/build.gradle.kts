import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("black.java-standards")
    id("net.ltgt.errorprone")
    // Minecraft to compile against, the graft configuration, and runClient/runServer.
    id("black.graft.mod")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

group = "black.truezoom"
version = "1.0.0"

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }

// A mod outside this repository writes graft("com.github.Brendan-Black.Graft:loader:<version>") instead.
dependencies {
    graft(project(":loader"))
    testImplementation(project(":loader"))
    errorprone("com.google.errorprone:error_prone_core:2.52.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test { java.setSrcDirs(listOf("test")) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-Xlint:all,-processing,-classfile")
    options.errorprone {
        enabled.set(this@configureEach.name == "compileJava")
        disableAllWarnings = true
        error("NullAway")
        option("NullAway:AnnotatedPackages", "black.truezoom")
    }
}

val runTests = tasks.register<JavaExec>("runTests") {
    description = "Runs test/black/truezoom/AllTests."
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "black.truezoom.AllTests"
    workingDir = projectDir
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}
tasks.test { enabled = false }
tasks.check { dependsOn(runTests) }

tasks.jar { from(rootProject.file("LICENSE")) }
