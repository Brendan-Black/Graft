# Changelog

## Unreleased

- **Versions are [Version](https://github.com/Brendan-Black/Version)'s.** `black.graft.Version` is gone: `ModInfo.version()`
  and `VersionRange` use `black.version.Version`, which Graft's jars now carry. Descriptors still take one to three numbers
  and no pre-releases. Two versions that differ only in trailing zeros are no longer equal: the more precise is greater,
  so `26.3.0` is above `26.3`, `(,2.0]` leaves out `2.0.0`, and `[1.2]` is `1.2` alone. Logs and messages show versions
  as written (`Minecraft 26.3`); `fabric.mod.json` and `neoforge.mods.toml` still write them in full.
- **black-java 1.3.7**, whose `lib` gets `TypedString` from its own repository.

## 0.1.1

- **Downloads.** Each release now attaches `graft-<version>.jar`, `graft-fabric-<version>.jar` and
  `graft-neoforge-<version>.jar` to its GitHub release, with their SHA-256 sums.
- **`side` in the descriptor.** `side=client` (a zoom, a minimap) or `side=server` (admin tools) marks a mod only one end
  needs. A server no longer asks joining players for those mods, so a player with True Zoom can join servers without it.
  A dedicated server doesn't load `side=client` mods, and one-sided mods can't add blocks or items. The default is
  `side=both`, as before. Mods that use the key need Graft 0.1.1: `depends.graft=[0.1.1,0.2)`.
- **Pack order.** Mods' assets and data now sit just above vanilla's, so a player's resource packs and a world's data
  packs override them, as they do vanilla's. Before, mod packs went on top of everything. This takes a thirteenth hook,
  in `PackRepository.rebuildSelected`. A mod pack already placed in an existing world or options file stays where it is.
- **Config files survive mod updates.** A setting a mod no longer has is taken out of its file with a warning, and kept
  at the end as a comment, instead of stopping the game. A value that doesn't fit still stops it (GRAFT-203).

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
