import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `maven-publish`
    id("black.java-standards")
    id("net.ltgt.errorprone")
    // Minecraft on the compile class path, fetched and checked by Graft's own plugin.
    id("black.graft.minecraft")
}

// mavenCentral: the java-standards checks download their formatter and compiler from there. JitPack: black-java's lib.
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

// The API and black-java's lib (black.TypedString) go inside the jar: the game runs Graft as a single agent jar.
val bundled = configurations.create("bundled") {
    isTransitive = false
}
configurations.compileOnly { extendsFrom(bundled) }
configurations.testImplementation { extendsFrom(bundled) }

dependencies {
    bundled(project(":api"))
    bundled("com.github.Brendan-Black.black-java:lib:1.3.5")
    errorprone("com.google.errorprone:error_prone_core:2.52.0")
    errorprone("com.uber.nullaway:nullaway:0.14.3")
}

// The sources keep a plain-javac layout: src/, resources/ and test/.
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
        enabled.set(this@configureEach.name == "compileJava") // tests pass bad values on purpose
        disableAllWarnings = true // only the checks below, and Error Prone's own errors, count
        error("NullAway")
        option("NullAway:AnnotatedPackages", "black.graft")
    }
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("black/graft/loader/graft.properties") { expand("version" to version) }
}

tasks.jar {
    archiveBaseName = "graft" // graft-<version>.jar, the name players download; the Maven artifact stays "loader"
    manifest {
        attributes(
            "Premain-Class" to "black.graft.loader.Agent",
            "Launcher-Agent-Class" to "black.graft.loader.Agent",
            "Main-Class" to "black.graft.loader.Launch",
            "Implementation-Title" to "Graft",
            "Implementation-Version" to project.version,
        )
    }
    dependsOn(bundled)
    from(bundled.map { zipTree(it) }) { exclude("META-INF/**", "LICENSE") }
    from(rootProject.file("LICENSE"))
}

// The tests are a plain main() (no test framework). The patch check reads the real Minecraft jars, which the build
// already fetched for compiling, and runs Graft's patches and the JVM's verifier over them.
val minecraftClasspath = extensions.getByType<black.graft.gradle.GraftMinecraftExtension>().classpath
val runTests = tasks.register<JavaExec>("runTests") {
    description = "Runs test/black/graft/AllTests."
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "black.graft.AllTests"
    workingDir = projectDir
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    inputs.files(minecraftClasspath)
    systemProperty("graft.test.minecraft", minecraftClasspath.asPath)
}
tasks.test { enabled = false }
tasks.check { dependsOn(runTests) }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "loader"
            version = providers.environmentVariable("VERSION").getOrElse(project.version.toString()) // the tag as written, which JitPack serves
            from(components["java"])
            pom {
                name = "Graft"
                description = "A Minecraft mod loader in pure JDK 25: a Java agent with a small, documented API."
                url = "https://github.com/Brendan-Black/Graft"
                licenses { license { name = "Apache-2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0" } }
            }
        }
    }
}
