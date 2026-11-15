package black.graft.loader;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;

import java.util.ArrayList;
import java.util.List;

import black.graft.ModId;
import black.graft.event.Event;

public final class EventBusTest {
	private record Ping(int n) implements Event {}

	private record Pong() implements Event {}

	private EventBusTest() {}

	public static void run() {
		ModId first = new ModId("first");
		ModId second = new ModId("second");
		List<String> heard = new ArrayList<>();

		EventBus bus = new EventBus();
		bus.listen(first, Ping.class, p -> heard.add("first " + p.n()));
		bus.listen(second, Ping.class, p -> heard.add("second " + p.n()));
		bus.listen(first, Pong.class, _ -> heard.add("pong"));
		bus.post(new Ping(0));
		eq(List.of(), heard, "nothing is delivered before the bus is frozen");

		bus.freeze();
		bus.post(new Ping(1));
		bus.post(new Pong());
		eq(List.of("first 1", "second 1", "pong"), heard, "listeners run in the order added, only for their event");

		ok(fails(IllegalStateException.class, () -> bus.listen(first, Ping.class, _ -> {}), "late").getMessage().contains("after startup"),
			"no listening after startup");

		EventBus failing = new EventBus();
		List<String> after = new ArrayList<>();
		failing.listen(second, Ping.class, _ -> {
			throw new IllegalArgumentException("boom");
		});
		failing.listen(first, Ping.class, _ -> after.add("ran"));
		failing.freeze();
		EventBus.ListenerException e = fails(EventBus.ListenerException.class, () -> failing.post(new Ping(2)), "a listener throws");
		ok(e.getMessage().startsWith("mod second's listener for Ping threw"), "the mod is named first in the message: " + e.getMessage());
		ok(e.getCause() instanceof IllegalArgumentException, "the cause is kept");
		eq(List.of(), after, "later listeners don't run once one has failed");
	}
}
