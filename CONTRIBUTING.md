# Contributing

Thanks for helping. The most useful things right now, in order:

1. **A bug with a failing case.** See *Tests* below.
2. **A roadmap item from the README's parity table.** Open an issue first to agree on the API: what a mod writes
   matters more than how Graft does it.
3. **A port to a new Minecraft version.** Start with `./gradlew check`: the patch test lists every hook that moved.

## Building

JDK 25, or let Gradle download it.

```sh
./gradlew check                    # formatting, compiler warnings, nulls, and every test, on all three platforms
./gradlew -p gradle-plugin check   # the same for the Gradle plugin
./gradlew format                   # formats every Java file
./gradlew :testmod:runClient       # the game, with the test mod
./gradlew gameTest                 # game tests in a real world, on Graft, Fabric and NeoForge
```

The first build fetches Minecraft 26.3 from Mojang into Gradle's cache. After that, builds work offline.

## The standard

The code follows [Black Java](https://github.com/Brendan-Black/black-java/blob/master/README.md) and the
[black style guide](https://github.com/Brendan-Black/black). The build enforces the parts a tool can check. These are the
ones that come up most here:

- **Make the type say it.** Shaped strings are `TypedString` records that refuse anything else (`ModId`, `ContentName`),
  and versions and ranges are parsed once, where they enter.
- **Nulls don't cross a boundary.** Return `Optional`. A nullable record field says `@Nullable` and has an `Optional`
  accessor. NullAway fails the build otherwise.
- **Fail early, with a code a player can quote.** Anything that stops the game is a `LoadingException` with an
  `ErrorCode`, worded for the player who has to fix their mods folder. Add a row to the README's table for a new code.
- **Leave nothing half-done.** Files are written to a temporary name and moved into place. `ClassPatcher` changes a class
  only if every one of its patches finds its place.
- **No third-party runtime dependencies.** The JDK, and `black-java`'s `lib` bundled into the jar, are all Graft needs.
- **Patches stay minimal.** A patch adds one call into `Hooks` or `ClientHooks` and nothing else. Logic lives in Graft's
  own classes, where it can be tested.

## Tests

There's no framework: each module's `AllTests` runs groups of checks from `Check`. Tests are hermetic. The Gradle
plugin's network goes through the `Http` seam, with a strict `PseudoHttp` behind it in tests. `MinecraftPatchesTest`
reads the Minecraft jars the build already fetched to compile against. It applies every patch and runs the JVM verifier,
and it never loads the game.

In-game behaviour is tested with game tests on Minecraft's GameTest server. `testmod/` is a mod that exercises Graft's
API and is never published. Its tests run on Graft's loader (`:testmod:gameTest`),
on Fabric through Graft for Fabric (`:fabric:gameTest`), and on NeoForge through Graft for NeoForge
(`:neoforge:gameTest`, which installs a NeoForge server with NeoForge's own installer the first time). `check` runs them
all, so the three platforms are held to the same behaviour.

Every bug fixed gets a check that would have caught it.
