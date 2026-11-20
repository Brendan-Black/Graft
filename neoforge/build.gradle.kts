import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `maven-publish`
    id("black.java-standards")
    id("net.ltgt.errorprone")
    // Minecraft on the compile class path. Since 26.1 NeoForge runs the game under Mojang's own names, as Graft does,
    // so nothing here is remapped.
    id("black.graft.minecraft")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.neoforged.net/releases/") {
        content { includeGroupAndSubgroups("net.neoforged") }
    }
}

group = "com.github.Brendan-Black.Graft"
version = providers.environmentVariable("VERSION").orElse(providers.gradleProperty("graftVersion")).get().removePrefix("v")

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

// The API, black-java's lib, TypedString and Version go inside the jar, as in Graft's own loader: one jar to install.
val bundled = configurations.create("bundled") {
    isTransitive = false
}
configurations.compileOnly { extendsFrom(bundled) }

// NeoForge's own pieces are provided by NeoForge at run time, so they're compile-only here. The universal jar holds
// NeoForge's classes without its patches to Minecraft, which Graft for NeoForge doesn't need.
dependencies {
    bundled(project(":api"))
    bundled("com.github.Brendan-Black.black-java:lib:1.3.7")
    bundled("com.github.Brendan-Black:TypedString:1.0.0")
    bundled("com.github.Brendan-Black.Version:Black:1.0.0")
    compileOnly("net.neoforged:neoforge:26.3.0.103:universal") { isTransitive = false }
    compileOnly("net.neoforged.fancymodloader:loader:12.0.12") { isTransitive = false }
    compileOnly("net.neoforged:bus:8.0.5") { isTransitive = false }
    compileOnly("net.neoforged:mergetool:2.0.7:api") { isTransitive = false } // NeoForge's Dist
    compileOnly("org.apache.maven:maven-artifact:3.10.0") { isTransitive = false } // mod versions
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
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to version) }
}

tasks.jar {
    archiveBaseName = "graft-neoforge"
    dependsOn(bundled)
    from(bundled.map { zipTree(it) }) { exclude("META-INF/**", "LICENSE") }
    from(rootProject.file("LICENSE"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "neoforge"
            version = providers.environmentVariable("VERSION").getOrElse(project.version.toString()) // the tag as written, which JitPack serves
            from(components["java"])
            pom {
                name = "Graft for NeoForge"
                description = "Runs Graft mods on NeoForge, alongside NeoForge mods."
                url = "https://github.com/Brendan-Black/Graft"
                licenses { license { name = "Apache-2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0" } }
            }
        }
    }
}

// The game tests, on NeoForge: NeoForge's own server install, Graft for NeoForge, and the same test mod
// jar that gameTest runs on Graft's loader and on Fabric, on Minecraft's GameTest server. check runs it.
//
// NeoForge ticks game tests only outside production (GameTestHooks.isGametestEnabled), and it decides it's in
// production unless Minecraft and NeoForge are on the class path, as its dev tooling puts them. So this launches the
// installed server that way: the patched server jar, marked with the Minecraft-Dists attribute FML's dev mode reads,
// and the universal jar, on the class path.
val neoForgeVersion = "26.3.0.103"
val neoForgeInstaller = configurations.create("neoForgeInstaller") { isTransitive = false }
val neoForgeMods = configurations.create("neoForgeMods") { isTransitive = false }
dependencies {
    neoForgeInstaller("net.neoforged:neoforge:$neoForgeVersion:installer")
    neoForgeMods(project(":testmod"))
}

// NeoForge's installer lays out a server: Minecraft's server jar from Mojang, patched, and NeoForge's libraries.
val serverDir = layout.buildDirectory.dir("neoforge-server")
val installServer = tasks.register<JavaExec>("installNeoForgeServer") {
    description = "Installs a NeoForge $neoForgeVersion server with NeoForge's own installer, for the game tests."
    group = "verification"
    classpath = neoForgeInstaller
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    val dir = serverDir.get().asFile
    outputs.dir(dir)
    workingDir = dir
    doFirst {
        dir.deleteRecursively()
        dir.mkdirs()
    }
    args("--install-server", dir.absolutePath)
}

// FML's dev mode wants the Minecraft jar to say which dists it holds; a server jar holds only the server.
val devMinecraftJar = tasks.register<Jar>("neoForgeDevMinecraftJar") {
    description = "The installed NeoForge server's patched Minecraft jar, marked for FML's dev mode."
    dependsOn(installServer)
    from(serverDir.map { zipTree(it.file("libraries/net/neoforged/minecraft-server-patched/$neoForgeVersion/minecraft-server-patched-$neoForgeVersion.jar")) }) {
        exclude("META-INF/MANIFEST.MF")
    }
    manifest { attributes("Minecraft-Dists" to "server") }
    archiveFileName = "minecraft-server-dev.jar"
    destinationDirectory = layout.buildDirectory.dir("neoforge-dev")
}

val gameTestDir = layout.buildDirectory.dir("gametest")
val syncGameTestMods = tasks.register<Sync>("syncNeoForgeGameTestMods") {
    from(neoForgeMods, tasks.jar)
    into(gameTestDir.map { it.dir("mods") })
}

val gameTest = tasks.register<JavaExec>("gameTest") {
    description = "Runs the test mod's game tests on NeoForge, through Graft for NeoForge."
    group = "verification"
    dependsOn(syncGameTestMods)
    inputs.files(neoForgeMods, tasks.jar, devMinecraftJar)
    mainClass = "net.neoforged.fml.startup.GameTestServer"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    val server = serverDir.get().asFile
    val devJar = devMinecraftJar.flatMap { it.archiveFile }
    val version = neoForgeVersion
    val dir = gameTestDir.get().asFile
    workingDir = dir
    systemProperty("neoforge.enableGameTest", "true")
    // The installer's argument file for this OS is read when the task runs: JVM options, then -classpath and its jars, then the main
    // class, then FML's own arguments. Its paths are relative to the server folder.
    doFirst {
        dir.resolve("universe").deleteRecursively()
        val argsFile = if (File.pathSeparatorChar == ';') "win_args.txt" else "unix_args.txt" // its class path uses this OS's separator
        val lines = server.resolve("libraries/net/neoforged/neoforge/$version/$argsFile").readLines().map { it.trim() }.filter { it.isNotEmpty() }
        val at = lines.indexOf("-classpath")
        check(at >= 0 && lines[at + 2] == "net.neoforged.fml.startup.Server") { "NeoForge's $argsFile has changed shape: $lines" }
        val jvm = lines.subList(0, at).flatMap { it.split(' ') }
            .map { if (it.startsWith("-DlibraryDirectory=")) "-DlibraryDirectory=" + server.resolve("libraries").absolutePath else it }
        jvmArgs(jvm)
        classpath(lines[at + 1].split(File.pathSeparator).map { server.resolve(it) })
        classpath(devJar, server.resolve("libraries/net/neoforged/neoforge/$version/neoforge-$version-universal.jar"))
        args(lines.subList(at + 3, lines.size).flatMap { it.split(' ') })
        args("--universe", dir.resolve("universe").absolutePath, "--report", dir.resolve("report.xml").absolutePath)
    }
}
tasks.check { dependsOn(gameTest) }
