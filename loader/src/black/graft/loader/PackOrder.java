package black.graft.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Where mods' packs go in a pack list. Minecraft adds a required pack that isn't selected yet at the top of the list, the
 * highest priority, over a player's resource packs and a world's data packs. Vanilla's own pack isn't pinned, so no pack
 * position puts a pack just above it. This moves mods' newly added packs there instead: they override vanilla's files,
 * and every pack a player or world chose overrides theirs. A mod pack that was already selected stays where it is, so a
 * player who moved one keeps their order.
 */
final class PackOrder {
	/** Vanilla's pack, in both the client's resource packs and a server's data packs. */
	static final String VANILLA = "vanilla";

	private PackOrder() {}

	/**
	 * @param selected  the packs Minecraft selected, lowest priority first
	 * @param id        a pack's id
	 * @param requested the ids that were asked for, before Minecraft added the required packs missing from them
	 * @param mods      the ids of mods' packs
	 * @return {@code selected}, with each mod pack not in {@code requested} moved to just above vanilla's, in the order
	 *         they were in; {@code selected} itself if there's nothing to move
	 */
	static <T> List<T> placeAboveVanilla(List<T> selected, Function<T, String> id, Collection<String> requested, Set<String> mods) {
		List<T> added = new ArrayList<>();
		List<T> rest = new ArrayList<>();
		for (T pack : selected) {
			String name = id.apply(pack);
			(mods.contains(name) && !requested.contains(name) ? added : rest).add(pack);
		}
		if (added.isEmpty()) {
			return selected;
		}
		int vanilla = 0;
		while (vanilla < rest.size() && !id.apply(rest.get(vanilla)).equals(VANILLA)) {
			vanilla++;
		}
		rest.addAll(vanilla < rest.size() ? vanilla + 1 : 0, added);
		return List.copyOf(rest);
	}
}
