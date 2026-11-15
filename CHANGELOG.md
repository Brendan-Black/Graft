# Changelog

## 0.1.0

The first release, for Minecraft 26.3. Graft is a Java agent that patches ten Minecraft methods with the JDK's ClassFile
API, and an API for mods. Mods get:

- descriptors with dependency ranges
- common and client entrypoints
- blocks, items and block items
- entries in vanilla creative tabs
- server and client events
- key bindings and field-of-view steps
- typed config files
- their own assets and data, offered as built-in packs

There is also `java -jar graft.jar server` for dedicated servers, the `black.graft.mod` Gradle plugin, and True Zoom as
the reference mod.
