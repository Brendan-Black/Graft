package black.graft.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import black.graft.ModId;
import black.graft.event.Event;

/**
 * Delivers events to the listeners mods added. Listeners are added on the startup thread while mods initialize; then
 * {@link #freeze()} publishes them, and events can be posted from any game thread.
 */
public final class EventBus {
	private final Map<Class<?>, List<Listener<?>>> adding = new HashMap<>();
	private volatile Map<Class<?>, List<Listener<?>>> frozen = Map.of();
	private volatile boolean open = true;

	private record Listener<E>(ModId mod, Consumer<? super E> action) {}

	/**
	 * @throws IllegalStateException after {@link #freeze()}
	 */
	public <E extends Event> void listen(ModId mod, Class<E> type, Consumer<? super E> listener) {
		if (!open) {
			throw new IllegalStateException("mod " + mod + " added a listener for " + type.getSimpleName()
				+ " after startup; add listeners from initialize()");
		}
		adding.computeIfAbsent(type, _ -> new ArrayList<>()).add(new Listener<>(mod, listener));
	}

	/** Stops adding listeners and makes the ones added visible to every thread. */
	public void freeze() {
		Map<Class<?>, List<Listener<?>>> copy = new HashMap<>();
		adding.forEach((type, list) -> copy.put(type, List.copyOf(list)));
		frozen = Map.copyOf(copy);
		open = false;
	}

	/**
	 * Calls every listener for the event's class, in order.
	 *
	 * @throws ListenerException wrapping whatever a listener threw, naming its mod; the remaining listeners don't run
	 */
	public void post(Event event) {
		List<Listener<?>> listeners = frozen.get(event.getClass());
		if (listeners == null) {
			return;
		}
		for (Listener<?> l : listeners) {
			deliver(l, event);
		}
	}

	private static <E> void deliver(Listener<E> listener, Event event) {
		@SuppressWarnings("unchecked") // listen() filed this listener under exactly event's class
		E typed = (E) event;
		try {
			listener.action().accept(typed);
		} catch (RuntimeException e) {
			throw new ListenerException(listener.mod(), event, e);
		}
	}

	/** A mod's event listener threw. Minecraft's crash report shows this message first, so it names the mod. */
	public static final class ListenerException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		ListenerException(ModId mod, Event event, RuntimeException cause) {
			super("mod " + mod + "'s listener for " + event.getClass().getSimpleName() + " threw " + cause, cause);
		}
	}
}
