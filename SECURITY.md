# Security

## Reporting a problem

Please use [GitHub's private vulnerability reporting](https://github.com/Brendan-Black/Graft/security/advisories/new)
rather than a public issue. Say what an attacker could do and how to reproduce it.

## What to know

**Mods are programs.** Graft runs every jar in `mods/` with the same rights as the game: it can read your files and use
your network. Install mods only from people you trust. Graft doesn't sandbox them, and no Java mod loader does.

**What Graft checks.**

| What | How |
|---|---|
| Minecraft's files, in the Gradle plugin | The version file is pinned by SHA-1 in the plugin. Every jar, library and asset it lists is checked against the SHA-1 there before it's kept |
| The server bundle, in `java -jar graft.jar server` | Each file inside is checked against the SHA-256 the bundle lists before it's used. Paths that would leave `versions/` or `libraries/` are refused |
| Files Graft writes | Config files and downloads are written to a temporary name and moved into place, so an interruption never leaves half a file |

Graft makes no network requests while the game runs.
