# Graft

[![CI](https://github.com/Brendan-Black/Graft/actions/workflows/ci.yml/badge.svg)](https://github.com/Brendan-Black/Graft/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/Brendan-Black/Graft.svg)](https://jitpack.io/#Brendan-Black/Graft)
![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-informational)
![Java 25](https://img.shields.io/badge/Java-25-informational)
![No third-party dependencies](https://img.shields.io/badge/third--party%20dependencies-0-success)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

**A small, free Minecraft mod loader in pure JDK 25: one Java agent, a documented API, and no third-party code.**

Since 26.1, Minecraft ships with its real class and method names. Graft builds on that: it patches thirteen methods
with the JDK's own ClassFile API as the game loads them, and gives mods a short API for blocks, items, creative tabs,
events, key bindings, the field of view, config files and their own assets and data. Nothing is remapped, decompiled or
redistributed. Mods compile against Mojang's own jar.

A Graft mod is one jar that runs three ways: on Graft's own agent, light and standalone, **on Fabric, alongside Fabric
mods**, through Graft for Fabric, or **on NeoForge, alongside NeoForge mods**, through Graft for NeoForge. Nothing is
recompiled. The Gradle plugin writes the jar's `fabric.mod.json` and `neoforge.mods.toml` from its Graft descriptor.

This repository holds:

| Folder | What it is |
|---|---|
| [`api/`](api) | The API mods compile against (`black.graft`), and the runtime every platform shares |
| [`loader/`](loader) | Graft's own loader: a Java agent |
| [`fabric/`](fabric) | Graft for Fabric: a Fabric mod that runs Graft mods on Fabric |
| [`neoforge/`](neoforge) | Graft for NeoForge: a NeoForge mod that runs Graft mods on NeoForge |
| [`gradle-plugin/`](gradle-plugin) | `black.graft.mod`: puts Minecraft on a mod's compile class path and adds `runClient`/`runServer` |
| [`testmod/`](testmod) | A mod that exercises Graft's API in game tests on every platform; never published |

The reference mod, [True Zoom](https://github.com/Brendan-Black/TrueZoom), lives in its own repository and builds
against Graft from JitPack, the way any mod does.

## Playing with mods

Graft 0.1 runs on **Minecraft 26.3** with **Java 25** (the launcher's own runtime for 26.x).

Download the jars from [Releases](https://github.com/Brendan-Black/Graft/releases/latest): `graft-<version>.jar` (called
`graft.jar` below), `graft-fabric-<version>.jar` and `graft-neoforge-<version>.jar`. `SHA256SUMS` beside them lists each
jar's checksum.

**Client.** Put `graft.jar` somewhere permanent. In the Minecraft launcher, open *Installations*, edit (or copy) the 26.3
installation, open *More options*, and add this to the start of *JVM arguments*:

```text
-javaagent:/path/to/graft.jar
```

Put mod jars in the `mods` folder of the installation's game directory (`.minecraft/mods` by default). The launcher
log's first lines say what Graft found: `Graft 0.1.1 on Minecraft 26.3 (client), 1 mod: truezoom 1.0.0`.

**Server.** Put `graft.jar` next to the vanilla `server.jar`, mods in `mods/`, and start with:

```console
$ java -jar graft.jar server --nogui
```

Graft unpacks the server the way the vanilla jar does (into `versions/` and `libraries/`, every file checked against
the checksum the bundle lists) and starts it with mods. `--server-jar <file>` names a different server jar; everything
else is passed to Minecraft.

**On Fabric.** Install Fabric Loader 0.20.1+ and Fabric API for 26.3 as usual, then put `graft-fabric.jar` and the Graft
mods in `mods/`, with any Fabric mods you like.

**On NeoForge.** Install NeoForge 26.3.0.103+ for 26.3 as usual, then put `graft-neoforge.jar` and the Graft mods in
`mods/`, with any NeoForge mods you like.

**Clients and servers need the same mods**, at the same versions: blocks and items travel between them as numbers, so
anything else would show the wrong blocks. A Graft server refuses a player whose mods differ, and tells them which mods
and versions to install (GRAFT-106). On Fabric and NeoForge, their own registry sync does this job. Mods marked
`side=client` (a zoom, a minimap) or `side=server` (admin tools) don't count: either end can have them alone.

## Making a mod

You need JDK 25. In `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    // JitPack serves Graft's plugin by its repository's name.
    resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("black.graft.")) useModule("com.github.Brendan-Black.Graft:gradle-plugin:${requested.version}")
    }
}
```

In `build.gradle.kts`:

```kotlin
plugins {
    java
    id("black.graft.mod") version "v0.1.0"
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Brendan-Black.Graft:api:v0.1.0") // what you compile against
    graft("com.github.Brendan-Black.Graft:loader:v0.1.0")    // what runClient and runServer run with
}

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
```

The first build fetches Minecraft 26.3's client jar and libraries from Mojang into Gradle's cache, each checked against
the SHA-1 Mojang publishes (the version file itself is pinned by hash in the plugin). `./gradlew runClient` starts the
game with your mod loaded straight from the build; `./gradlew runServer` starts a dedicated server. Both run from
`run/` in your project. Your jar also gets a `fabric.mod.json` and a `META-INF/neoforge.mods.toml`, written from your
descriptor, so the same jar loads on Fabric and NeoForge.

### The descriptor

`src/main/resources/graft.mod.properties`:

```properties
id=examplemod
name=Example Mod
version=1.0.0
description=What it does, in a sentence.
authors=You
license=Apache-2.0
side=both
entrypoint=com.example.ExampleMod
client-entrypoint=com.example.client.ExampleClient
depends.minecraft=[26.3,26.4)
depends.graft=[0.1,0.2)
```

Only `id`, `name` and `version` are required. Ids are 2–64 of `a-z 0-9 _`. Versions are one to three numbers. Ranges
are written as in Maven: `[1.0,2.0)`, `[0.1,)`, `[1.2.0]`, or `*`. A bare `1.0` is refused, because Maven reads it as
"anything". An unknown key is an error, so a typo can't go unnoticed.

Versions compare as [Version](https://github.com/Brendan-Black/Version) compares them: the numbers first, and when they
tie, the more precise is greater, so `26.3.0` is above `26.3`. `(,2.0]` leaves out `2.0.0`, and `[1.2]` is `1.2` alone:
write a bound with as many parts as the versions it will be checked against.

`side` says where the mod is needed. `both`, the default, is for a mod on client and server alike, and a server checks that
joining players have it. `client` is for a mod only the player's own game needs: a dedicated server doesn't load it.
`server` is for a mod only a server needs: it loads wherever a server runs, single player included. Neither is asked of
joining players, and neither may add blocks or items, which both ends must have.

Assets go in `assets/<id>/` and data in `data/<id>/`, as in a resource or data pack. Graft offers them to the game as
always-on built-in packs, placed just above vanilla's: they can override vanilla's files, and players' resource packs and
worlds' data packs can override theirs. You don't write a `pack.mcmeta`.

### The entrypoints

```java
public final class ExampleMod implements Mod {
    @Override
    public void initialize(Setup setup) {
        Registered<Block> block = setup.block(new ContentName("example_block"),
                properties -> new Block(properties.mapColor(MapColor.STONE).strength(1f)));
        setup.addToTab(VanillaTab.BUILDING_BLOCKS, setup.blockItem(block));

        setup.events().listen(ServerStarting.class, event -> setup.logger().log(INFO, "starting"));
    }
}
```

```java
public final class ExampleClient implements ClientMod {
    @Override
    public void initializeClient(ClientSetup setup) {
        KeyBinding key = setup.keyBinding(new ContentName("wave"), InputConstants.KEY_G);
        setup.events().listen(ClientTick.class, tick -> {
            if (key.consumeClick()) { /* ... */ }
        });
    }
}
```

[True Zoom](https://github.com/Brendan-Black/TrueZoom) is a complete, small mod to start from.

### Testing in game

`setup.gameTest(name, helper -> { ... })` declares a test that runs in a real world, with a test instance at
`data/<id>/test_instance/<name>.json`, as vanilla's tests have. `./gradlew gameTest` runs them on Minecraft's own
GameTest server, headless, with your mod loaded through Graft, and `check` runs `gameTest`. A server refuses to start
when a declared test has no instance (GRAFT-204), so no test is skipped silently. See
[True Zoom's tests](https://github.com/Brendan-Black/TrueZoom/blob/main/src/black/truezoom/TrueZoomTests.java).

### The API

Everything a mod uses is in `black.graft` and below. Each type's Javadoc is the reference.
`black.graft.loader` is internal.

| Type | What it's for |
|---|---|
| `Mod`, `client.ClientMod` | Entrypoints: one runs everywhere, one only on the client |
| `Setup` | Declares blocks, items, block items and creative tab entries; events, config, a logger |
| `client.ClientSetup` | Declares key bindings and field-of-view steps; client events and config |
| `Registered<T>` | A handle to declared content, filled in when Minecraft's registries are populated |
| `ModId`, `ContentName` | Ids and names, checked when they're made |
| `VersionRange`, `Dependency`, `ModInfo` | What descriptors say; versions are [`black.version.Version`](https://github.com/Brendan-Black/Version) |
| `VanillaTab` | Minecraft's creative tabs |
| `event.Events`, `event.Event` | Listening; `ServerStarting`, `ServerStopping`, `ServerTick`, `client.ClientTick` |
| `client.KeyBinding`, `client.FovModifier` | A declared key; a field-of-view step |
| `config.Config`, `ConfigSpec`, `Setting` | Typed settings in `config/<id>.properties`, checked on load |
| `Setup.gameTest` | In-game tests, run by `gradle gameTest` on Minecraft's GameTest server |

The API keeps a few promises. Nothing in it returns null; what may be absent is an `Optional`. Every value is checked
when it's made: a `ModId`, a `VersionRange` or a `Setting` can't hold something invalid. Declarations are accepted only
while your entrypoint runs, and after that they fail with a message saying so. When a listener throws, the crash report
names your mod first.

## Parity

Graft is young. Here is where it stands against the two established loaders, for the features True Zoom and most small
mods need:

| | Forge | Fabric | Graft 0.1 |
|---|---|---|---|
| Mod metadata and dependency ranges | `mods.toml` | `fabric.mod.json` | `graft.mod.properties` |
| Common and client entrypoints | ✓ | ✓ | ✓ |
| Blocks, items, block items | `DeferredRegister` | `Registry.register` | `Setup.block/item/blockItem` |
| Adding to vanilla creative tabs | ✓ | ✓ | ✓ |
| Creative tabs of a mod's own | ✓ | ✓ | roadmap: the screen needs pages |
| Lifecycle and tick events | ✓ | ✓ | server start/stop/tick, client tick |
| Key bindings | ✓ | ✓ | ✓ |
| Field of view | event | mixin | `ClientSetup.modifyFov` |
| Config files | ✓ | library | ✓, typed and checked |
| Mod assets and data | ✓ | ✓ | ✓, no `pack.mcmeta` needed |
| Dev environment (`runClient`, `runServer`) | ForgeGradle | Loom | `black.graft.mod` |
| Runs alongside the other loader's mods | — | — | Fabric and NeoForge mods, through Graft for Fabric and Graft for NeoForge |
| Bytecode changes by mods | coremods, mixins | mixins | roadmap: a public patch API |
| Networking, custom packets | ✓ | ✓ | roadmap |
| Join check: same mods on both ends | ✓ | ✓ | ✓, on Graft's loader; on Fabric and NeoForge, theirs |
| Other registries (entities, menus…) | ✓ | ✓ | roadmap; data-driven ones work through data packs |
| Data generation, jar-in-jar, mod list screen | ✓ | ✓ | roadmap |

## How Graft hooks in

These are all of Graft's changes to Minecraft. Each is a single call into Graft's hooks, added when the class loads,
and none removes or reorders vanilla code. They're declared in one place,
[`MinecraftPatches`](loader/src/black/graft/loader/transform/MinecraftPatches.java). Graft's build applies every one to
the real 26.3 jar and runs the JVM's verifier over the result. When a Minecraft update moves a hook, Graft's build
fails, not your game.

| Class | Method | Where | For |
|---|---|---|---|
| `server.Bootstrap` | `bootStrap()` | start | running mods' entrypoints |
| `core.registries.BuiltInRegistries` | `freeze()` | start | creating mods' blocks and items |
| `world.item.CreativeModeTab` | `buildContents(...)` | after the vanilla generator | creative tab entries |
| `server.packs.repository.PackRepository` | constructor | start | mod assets and data as packs |
| `server.packs.repository.PackRepository` | `rebuildSelected(...)` | every return | newly added mod packs just above vanilla's |
| `server.MinecraftServer` | `runServer()` | before it calls `initServer()`, inside its crash handling | `ServerStarting` |
| `server.MinecraftServer` | `stopServer()` | start | `ServerStopping` |
| `server.MinecraftServer` | `tickServer(...)` | every return | `ServerTick` |
| `client.Options` | constructor | after `keyMappings` is set | key bindings, before saved keys load |
| `client.Camera` | `calculateFov(float)` | every return | field-of-view steps |
| `client.Minecraft` | `tick()` | every return | `ClientTick` |
| `server.network.ServerHandshakePacketListenerImpl` | `beginLogin(...)` | return | refusing a player whose mods differ |
| `network.Connection` | `initiateServerboundConnection(...)` | start | sending the client's mod fingerprint with its handshake |

## Publishing Graft mods

Graft mods are published as Fabric mods that depend on Graft for Fabric, and as NeoForge mods that depend on Graft for
NeoForge, which is honestly what they are there: they run on each loader with that one library. CurseForge supports
only Forge, NeoForge, Fabric and Quilt, so this is how Graft mods get there: one jar, uploaded for both loaders. On
Modrinth it can be tagged `fabric` and `neoforge` the same way. Graft's own loader can be listed under Modrinth's
`java-agent` tag. A `graft` tag of its own is something to ask Modrinth for once there are mods to show.

## When Graft stops the game

Graft refuses to start a game it can't run properly, and says why with a code:

| Code | Problem | What to do |
|---|---|---|
| GRAFT-101 | A mod's `graft.mod.properties` is missing or has invalid entries, or a jar isn't a Graft mod | Every problem is listed; a jar without a descriptor is probably for another loader |
| GRAFT-102 | Two mods with the same id | Remove one; both files are named |
| GRAFT-103 | A mod needs a mod that isn't installed | Install it, at a version in the range shown |
| GRAFT-104 | A mod needs another version of a mod, Minecraft or Graft | Update or downgrade as shown |
| GRAFT-105 | A file in `mods/` isn't a readable jar | Download it again |
| GRAFT-106 | A server refused you: your mods aren't its mods | Install exactly the mods and versions the message lists, and no others |
| GRAFT-201 | A mod's entrypoint class is missing or wrong | A bug in the mod: report it to its author |
| GRAFT-202 | A mod failed while setting up | A bug in the mod: the cause follows |
| GRAFT-203 | A config file has a value its mod can't use | Each key, value and accepted range is listed; fix or delete the line |
| GRAFT-204 | A mod declares a game test with no test instance | A bug in the mod: the message names the file to add |
| GRAFT-301 | A method Graft patches isn't there | This Graft is for another Minecraft version |
| GRAFT-302 | Patching a class failed | A bug in Graft: please report it |
| GRAFT-401 | No server jar, or not a Minecraft server bundle | Download `server.jar` from minecraft.net, or use `--server-jar` |
| GRAFT-402 | A file in the server bundle fails its checksum | Download `server.jar` again |
| GRAFT-501 | Minecraft's version can't be read, or it's a snapshot | Graft runs on releases only |

## Building Graft

```console
$ ./gradlew check                    # formatting, warnings, nulls, tests, and game tests on all three platforms
$ ./gradlew -p gradle-plugin check   # the Gradle plugin
$ ./gradlew :testmod:runClient       # the game, with the test mod
```

The code follows [Black Java](https://github.com/Brendan-Black/black-java), and the build enforces it. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

Graft is [Apache-2.0](LICENSE). Minecraft is Mojang's: Graft contains none of its code or assets, and
fetches the game only from Mojang, for your own use under the [Minecraft EULA](https://www.minecraft.net/eula). Graft
isn't affiliated with Mojang or Microsoft.
