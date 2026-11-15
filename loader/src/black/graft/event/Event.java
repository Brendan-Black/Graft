package black.graft.event;

/**
 * Something that happened in the game, which mods can listen for with {@link Events#listen}. Every event is a record, so
 * what it carries is in its components.
 *
 * <p>A listener runs on the game thread the event happened on, inline: keep it short. An exception thrown by a listener
 * isn't handled: Graft wraps it in one that names the mod and lets it crash the game, as a bug in vanilla code would, so
 * the crash report says whose bug it is.
 */
public interface Event {}
