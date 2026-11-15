package black.graft.gradle;

/**
 * The keys of Mojang's version files and asset indexes that the plugin reads. {@link #LIBRARIES} and {@link #OBJECTS} also
 * name the cache's folders: the cache lays files out as the launcher does, under the key that lists them.
 */
final class MojangJson {
	static final String ID = "id";
	static final String NAME = "name";
	static final String URL = "url";
	static final String SHA1 = "sha1";
	static final String RULES = "rules";
	static final String ARCH = "arch";
	static final String DOWNLOADS = "downloads";
	static final String LIBRARIES = "libraries";
	static final String OBJECTS = "objects";

	private MojangJson() {}
}
