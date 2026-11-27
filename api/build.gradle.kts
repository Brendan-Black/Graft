import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `maven-publish`
    id("black.java-standards")
    id("net.ltgt.errorprone")
    // Minecraft on the compile class path: the API names Minecraft's own types (Block, Item, KeyMapping).
    id("black.graft.minecraft")
}

// mavenCentral: the java-standards checks download their formatter and compiler from there. JitPack: black-java's lib, TypedString and Version.
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

group = "com.github.Brendan-Black.Graft"
version = providers.environmentVariable("VERSION").orElse(providers.gradleProperty("graftVersion")).get().removePrefix("v")

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

// What mods compile against. Every platform (Graft's agent, Cambium) carries these classes inside its own jar.
dependencies {
    api("com.github.Brendan-Black.black-java:lib:1.3.7")
    api("com.github.Brendan-Black.Version:Black:1.0.0")
    errorprone("com.google.errorprone:error_prone_core:2.52.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}

sourceSets {
    main { java.setSrcDirs(listOf("src")) }
    test { java.setSrcDirs(listOf("test")) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-Xlint:all,-processing,-classfile")
    options.errorprone {
        enabled.set(this@configureEach.name == "compileJava") // tests pass bad values on purpose
        disableAllWarnings = true // only the checks below, and Error Prone's own errors, count
        error("NullAway")
        option("NullAway:AnnotatedPackages", "black.graft")
    }
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.jar { from(rootProject.file("LICENSE")) }

val runTests = tasks.register<JavaExec>("runTests") {
    description = "Runs test/black/graft/AllTests."
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "black.graft.AllTests"
    workingDir = projectDir
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}
tasks.test { enabled = false }
tasks.check { dependsOn(runTests) }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "api"
            version = providers.environmentVariable("VERSION").getOrElse(project.version.toString()) // the tag as written, which JitPack serves
            from(components["java"])
            pom {
                name = "Graft API"
                description = "The API Graft mods compile against; the same compiled mod runs on Graft's agent and on Cambium."
                url = "https://github.com/Brendan-Black/Graft"
                licenses { license { name = "Apache-2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0" } }
            }
        }
    }
}
