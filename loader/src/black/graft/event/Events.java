package black.graft.event;

import java.util.function.Consumer;

/**
 * Where a mod listens for {@link Event}s. Listeners for one event run in the order mods are loaded (by mod id), and within
 * a mod in the order they were added.
 *
 * <p>Implemented by Graft: mods receive it from their setup.
 */
public interface Events {
	/**
	 * Calls {@code listener} every time an event of exactly {@code type} happens.
	 *
	 * @param <E>      the event
	 * @param type     the event's record class, such as {@code ServerTick.class}
	 * @param listener what to do
	 */
	<E extends Event> void listen(Class<E> type, Consumer<? super E> listener);
}
