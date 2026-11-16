# Changelog

## 0.1.0

The first release, for Minecraft 26.3. Graft is a Java agent that patches twelve Minecraft methods with the JDK's
ClassFile API, and an API for mods. Mods get:

- descriptors with dependency ranges
- common and client entrypoints
- blocks, items and block items
- entries in vanilla creative tabs
- server and client events
- key bindings and field-of-view steps
- typed config files
- their own assets and data, offered as built-in packs

A Graft server refuses a player whose mods differ from its own, naming the mods and versions to install (GRAFT-106),
instead of letting mismatched block and item numbers show the wrong blocks.

Graft for Fabric runs the same mod jars on Fabric Loader 0.20.1 and Fabric API for 26.3, alongside Fabric mods; the
Gradle plugin writes each mod's `fabric.mod.json` from its descriptor. Graft for NeoForge does the same on NeoForge
26.3.0.103, from a `neoforge.mods.toml` the plugin also writes. Mods compile against the new `api` artifact.

There is also `java -jar graft.jar server` for dedicated servers and the `black.graft.mod` Gradle plugin. The reference
mod, True Zoom, has its own repository.
