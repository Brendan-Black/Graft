import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `maven-publish`
    id("black.java-standards")
    id("net.ltgt.errorprone")
    // Minecraft on the compile class path. Since 26.1 Fabric runs the game under Mojang's own names, as Graft does,
    // so nothing here is remapped.
    id("black.graft.minecraft")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.fabricmc.net/") {
        content {
            includeGroup("net.fabricmc")
            includeGroup("net.fabricmc.fabric-api")
        }
    }
}

group = "com.github.Brendan-Black.Graft"
version = providers.environmentVariable("VERSION").orElse(providers.gradleProperty("graftVersion")).get().removePrefix("v")

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

// The API and black-java's lib go inside the jar, as in Graft's own loader: one jar to install.
val bundled: Configuration by configurations.creating {
    isTransitive = false
}
configurations.compileOnly { extendsFrom(bundled) }

// Fabric's own pieces are provided by Fabric at run time, so they're compile-only here.
dependencies {
    bundled(project(":api"))
    bundled("com.github.Brendan-Black.black-java:lib:1.2.0")
    compileOnly("net.fabricmc:fabric-loader:0.20.1")
    compileOnly("net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7")
    compileOnly("net.fabricmc.fabric-api:fabric-api-base:2.0.6+fcdff87f5d")
    compileOnly("net.fabricmc.fabric-api:fabric-lifecycle-events-v1:4.1.9+ffef5f675d")
    compileOnly("net.fabricmc.fabric-api:fabric-creative-tab-api-v1:5.0.22+fcdff87f5d")
    compileOnly("net.fabricmc.fabric-api:fabric-key-mapping-api-v1:2.0.8+3434d6d95d")
    errorprone("com.google.errorprone:error_prone_core:2.52.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}

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

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("fabric.mod.json") { expand("version" to version) }
}

tasks.jar {
    archiveBaseName = "graft-fabric"
    dependsOn(bundled)
    from(bundled.map { zipTree(it) }) { exclude("META-INF/**", "LICENSE") }
    from(rootProject.file("LICENSE"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "fabric"
            from(components["java"])
            pom {
                name = "Graft for Fabric"
                description = "Runs Graft mods on Fabric, alongside Fabric mods."
                url = "https://github.com/Brendan-Black/Graft"
                licenses { license { name = "Apache-2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0" } }
            }
        }
    }
}
