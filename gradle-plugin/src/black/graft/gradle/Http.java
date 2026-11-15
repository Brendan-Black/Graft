package black.graft.gradle;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Fetches a URL. The seam between the plugin and the network: tests put a pseudo behind it. */
public interface Http {
	/**
	 * @return the body of a 200 response
	 * @throws IOException on any other status, or if the request fails
	 */
	byte[] get(URI uri) throws IOException;

	/** The real network, through the JDK's client. */
	static Http jdk() {
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
		return uri -> {
			HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2)).header("User-Agent", "Graft-Gradle").GET().build();
			HttpResponse<byte[]> response;
			try {
				response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("interrupted while fetching " + uri, e);
			}
			if (response.statusCode() != 200) {
				throw new IOException("GET " + uri + " answered " + response.statusCode() + ", expected 200");
			}
			return response.body();
		};
	}
}
