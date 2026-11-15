package black.graft.gradle;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mojang's servers, as far as the plugin is concerned: canned bodies by URL. Strict: a URL with nothing canned fails the
 * test, rather than quietly returning something. Records every request, in order.
 */
final class PseudoHttp implements Http {
	private final Map<URI, byte[]> canned = new HashMap<>();
	private final List<URI> received = new ArrayList<>();

	PseudoHttp serve(String uri, String body) {
		canned.put(URI.create(uri), body.getBytes(StandardCharsets.UTF_8));
		return this;
	}

	@Override
	public synchronized byte[] get(URI uri) throws IOException {
		received.add(uri);
		byte[] body = canned.get(uri);
		if (body == null) {
			throw new IllegalStateException("no canned response for " + uri);
		}
		return body.clone();
	}

	synchronized List<URI> received() {
		return List.copyOf(received);
	}
}
