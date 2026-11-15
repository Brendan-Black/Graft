package black.graft.loader;

/**
 * Codes for the problems Graft stops the game over. A player sees the message and the code, not the source, so each
 * problem has its own code to search for or quote in a bug report. The README lists them all, with what to do.
 */
public enum ErrorCode {
	/** A mod's {@code graft.mod.properties} is missing, unreadable, or has invalid entries. */
	BAD_DESCRIPTOR("GRAFT-101"),
	/** Two installed mods have the same id. */
	DUPLICATE_MOD("GRAFT-102"),
	/** A mod depends on a mod that isn't installed. */
	MISSING_DEPENDENCY("GRAFT-103"),
	/** A mod depends on a mod, Minecraft or Graft at a version that isn't installed. */
	WRONG_VERSION("GRAFT-104"),
	/** A file in the mods folder can't be opened as a jar. */
	BAD_JAR("GRAFT-105"),
	/** A mod's entrypoint class is missing, isn't a {@code Mod}/{@code ClientMod}, or can't be constructed. */
	BAD_ENTRYPOINT("GRAFT-201"),
	/** A mod's entrypoint threw while it was setting up. */
	ENTRYPOINT_FAILED("GRAFT-202"),
	/** A config file has a value its mod can't use. */
	BAD_CONFIG("GRAFT-203"),
	/** A Minecraft method Graft patches isn't there, or doesn't look as expected: this Graft is for another version. */
	PATCH_TARGET_MISSING("GRAFT-301"),
	/** Patching a Minecraft class failed. */
	PATCH_FAILED("GRAFT-302"),
	/** The server jar is missing, or isn't a Minecraft server bundle. */
	BAD_SERVER_JAR("GRAFT-401"),
	/** A file inside the server bundle doesn't match its checksum. */
	CHECKSUM_MISMATCH("GRAFT-402"),
	/** The Minecraft version can't be read, or isn't a release version. */
	UNKNOWN_MINECRAFT("GRAFT-501");

	private final String code;

	ErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return code;
	}
}
