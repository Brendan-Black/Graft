import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-gradle-plugin`
    `maven-publish`
    // Formatting and compiler warnings, checked by `check`: https://github.com/Brendan-Black/black-java
    id("black.java-standards") version "1.2.0"
    // NullAway: whatever isn't @Nullable is never null, checked at compile time.
    id("net.ltgt.errorprone") version "5.2.0"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

group = "com.github.Brendan-Black.Graft"
version = providers.environmentVariable("VERSION")
    .orElse(providers.fileContents(layout.projectDirectory.file("../gradle.properties")).asText.map { text ->
        Regex("(?m)^graftVersion=(.+)$").find(text)?.groupValues?.get(1)?.trim() ?: error("graftVersion missing from gradle.properties")
    }).get().removePrefix("v")

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.52.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}

// The plugin shares the loader's JSON reader: one copy of the code, synced in here and compiled into both.
val sharedJson = tasks.register<Sync>("sharedJson") {
    from("../loader/src/black/graft/json")
    into(layout.buildDirectory.dir("shared/black/graft/json"))
}
sourceSets {
    main { java.setSrcDirs(listOf(layout.projectDirectory.dir("src"), files(layout.buildDirectory.dir("shared")).builtBy(sharedJson))) }
    test { java.setSrcDirs(listOf("test")) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-Xlint:all,-processing,-classfile,-this-escape")
    options.errorprone {
        enabled.set(this@configureEach.name == "compileJava")
        disableAllWarnings = true
        error("NullAway")
        option("NullAway:AnnotatedPackages", "black.graft")
    }
}

gradlePlugin {
    plugins {
        register("minecraft") {
            id = "black.graft.minecraft"
            implementationClass = "black.graft.gradle.MinecraftPlugin"
            displayName = "Graft: Minecraft on the compile class path"
        }
        register("mod") {
            id = "black.graft.mod"
            implementationClass = "black.graft.gradle.ModPlugin"
            displayName = "Graft: build and run a Graft mod"
        }
    }
}

// Plain main() tests, as in the loader.
val runTests = tasks.register<JavaExec>("runTests") {
    description = "Runs test/black/graft/gradle/AllTests."
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "black.graft.gradle.AllTests"
    workingDir = projectDir
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}
tasks.test { enabled = false }
tasks.check { dependsOn(runTests) }

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            url = "https://github.com/Brendan-Black/Graft"
            licenses { license { name = "Apache-2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0" } }
        }
    }
}
