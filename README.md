# Graft

[![CI](https://github.com/Brendan-Black/Graft/actions/workflows/ci.yml/badge.svg)](https://github.com/Brendan-Black/Graft/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/Brendan-Black/Graft.svg)](https://jitpack.io/#Brendan-Black/Graft)
![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-informational)
![Java 25](https://img.shields.io/badge/Java-25-informational)
![No third-party dependencies](https://img.shields.io/badge/third--party%20dependencies-0-success)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

**A small, free Minecraft mod loader in pure JDK 25: one Java agent, a documented API, and no third-party code.**

Since 26.1, Minecraft ships with its real class and method names. Graft builds on that: it patches ten methods
with the JDK's own ClassFile API as the game loads them, and gives mods a short API for blocks, items, creative tabs,
events, key bindings, the field of view, config files and their own assets and data. Nothing is remapped, decompiled or
redistributed. Mods compile against Mojang's own jar.

This repository holds three things:

| Folder | What it is |
|---|---|
| [`loader/`](loader) | Graft itself: the agent, and the API mods compile against (`black.graft`) |
| [`gradle-plugin/`](gradle-plugin) | `black.graft.mod`: puts Minecraft on a mod's compile class path and adds `runClient`/`runServer` |
| [`truezoom/`](truezoom) | True Zoom, the reference mod: hold C to zoom, plus a block and a snack |

## Playing with mods

Graft 0.1 runs on **Minecraft 26.3** with **Java 25** (the launcher's own runtime for 26.x).

**Client.** Put `graft.jar` somewhere permanent. In the Minecraft launcher, open *Installations*, edit (or copy) the 26.3
installation, open *More options*, and add this to the start of *JVM arguments*:

```text
-javaagent:/path/to/graft.jar
```

Put mod jars in the `mods` folder of the installation's game directory (`.minecraft/mods` by default). The launcher
log's first lines say what Graft found: `Graft 0.1.0 on Minecraft 26.3.0 (client), 1 mod: truezoom 1.0.0`.

**Server.** Put `graft.jar` next to the vanilla `server.jar`, mods in `mods/`, and start with:

```console
$ java -jar graft.jar server --nogui
```

Graft unpacks the server the way the vanilla jar does (into `versions/` and `libraries/`, every file checked against
the checksum the bundle lists) and starts it with mods. `--server-jar <file>` names a different server jar; everything
else is passed to Minecraft.

**Clients and servers need the same mods.** Graft 0.1 doesn't check this when you join: see [the roadmap](#parity).

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
    id("black.graft.mod") version "0.1.0"
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    graft("com.github.Brendan-Black.Graft:loader:0.1.0")
}

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
```

The first build fetches Minecraft 26.3's client jar and libraries from Mojang into Gradle's cache, each checked against
the SHA-1 Mojang publishes (the version file itself is pinned by hash in the plugin). `./gradlew runClient` starts the
game with your mod loaded straight from the build; `./gradlew runServer` starts a dedicated server. Both run from
`run/` in your project.

### The descriptor

`src/main/resources/graft.mod.properties`:

```properties
id=examplemod
name=Example Mod
version=1.0.0
description=What it does, in a sentence.
authors=You
license=Apache-2.0
entrypoint=com.example.ExampleMod
client-entrypoint=com.example.client.ExampleClient
depends.minecraft=[26.3,26.4)
depends.graft=[0.1,0.2)
```

Only `id`, `name` and `version` are required. Ids are 2–64 of `a-z 0-9 _`. Versions are one to three numbers. Ranges
are written as in Maven: `[1.0,2.0)`, `[0.1,)`, `[1.2.0]`, or `*`. A bare `1.0` is refused, because Maven reads it as
"anything". An unknown key is an error, so a typo can't go unnoticed.

Assets go in `assets/<id>/` and data in `data/<id>/`, as in a resource or data pack. Graft offers them to the game as
always-on built-in packs. You don't write a `pack.mcmeta`.

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

[True Zoom](truezoom/src/black/truezoom) is a complete, small mod to start from.

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
| `Version`, `VersionRange`, `Dependency`, `ModInfo` | What descriptors say |
| `VanillaTab` | Minecraft's creative tabs |
| `event.Events`, `event.Event` | Listening; `ServerStarting`, `ServerStopping`, `ServerTick`, `client.ClientTick` |
| `client.KeyBinding`, `client.FovModifier` | A declared key; a field-of-view step |
| `config.Config`, `ConfigSpec`, `Setting` | Typed settings in `config/<id>.properties`, checked on load |

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
| Bytecode changes by mods | coremods, mixins | mixins | roadmap: a public patch API |
| Networking, custom packets | ✓ | ✓ | roadmap |
| Join check: same mods on both ends | ✓ | ✓ | roadmap |
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
| `server.MinecraftServer` | `runServer()` | start | `ServerStarting` |
| `server.MinecraftServer` | `stopServer()` | start | `ServerStopping` |
| `server.MinecraftServer` | `tickServer(...)` | every return | `ServerTick` |
| `client.Options` | constructor | after `keyMappings` is set | key bindings, before saved keys load |
| `client.Camera` | `calculateFov(float)` | every return | field-of-view steps |
| `client.Minecraft` | `tick()` | every return | `ClientTick` |

## When Graft stops the game

Graft refuses to start a game it can't run properly, and says why with a code:

| Code | Problem | What to do |
|---|---|---|
| GRAFT-101 | A mod's `graft.mod.properties` is missing or has invalid entries, or a jar isn't a Graft mod | Every problem is listed; a jar without a descriptor is probably for another loader |
| GRAFT-102 | Two mods with the same id | Remove one; both files are named |
| GRAFT-103 | A mod needs a mod that isn't installed | Install it, at a version in the range shown |
| GRAFT-104 | A mod needs another version of a mod, Minecraft or Graft | Update or downgrade as shown |
| GRAFT-105 | A file in `mods/` isn't a readable jar | Download it again |
| GRAFT-201 | A mod's entrypoint class is missing or wrong | A bug in the mod: report it to its author |
| GRAFT-202 | A mod failed while setting up | A bug in the mod: the cause follows |
| GRAFT-203 | A config file has a value its mod can't use | Each key, value and accepted range is listed; fix or delete the line |
| GRAFT-301 | A method Graft patches isn't there | This Graft is for another Minecraft version |
| GRAFT-302 | Patching a class failed | A bug in Graft: please report it |
| GRAFT-401 | No server jar, or not a Minecraft server bundle | Download `server.jar` from minecraft.net, or use `--server-jar` |
| GRAFT-402 | A file in the server bundle fails its checksum | Download `server.jar` again |
| GRAFT-501 | Minecraft's version can't be read, or it's a snapshot | Graft runs on releases only |

## Building Graft

```console
$ ./gradlew check                    # the loader and True Zoom: formatting, warnings, nulls, tests
$ ./gradlew -p gradle-plugin check   # the Gradle plugin
$ ./gradlew :truezoom:runClient      # play with True Zoom
```

The code follows [Black Java](https://github.com/Brendan-Black/black-java), and the build enforces it. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

Graft and True Zoom are [Apache-2.0](LICENSE). Minecraft is Mojang's: Graft contains none of its code or assets, and
fetches the game only from Mojang, for your own use under the [Minecraft EULA](https://www.minecraft.net/eula). Graft
isn't affiliated with Mojang or Microsoft.
