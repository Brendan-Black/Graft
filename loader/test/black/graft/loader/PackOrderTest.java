package black.graft.loader;

import static black.graft.Check.eq;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class PackOrderTest {
	private static final Set<String> MODS = Set.of("mod/alpha", "mod/beta");

	private PackOrderTest() {}

	public static void run() {
		// Minecraft adds a required pack that isn't selected yet at the top, over the player's own.
		List<String> fresh = List.of("vanilla", "players_pack", "mod/alpha", "mod/beta");
		eq(List.of("vanilla", "mod/alpha", "mod/beta", "players_pack"), place(fresh, List.of("vanilla", "players_pack")),
			"newly added mod packs move to just above vanilla, in order, under the player's packs");

		List<String> moved = List.of("vanilla", "players_pack", "mod/alpha", "mod/beta");
		eq(moved, place(moved, moved), "mod packs that were already selected stay where they are, so a player's order is kept");

		eq(List.of("vanilla", "mod/alpha", "mod/beta", "world_pack"),
			place(List.of("vanilla", "mod/beta", "world_pack", "mod/alpha"), List.of("vanilla", "mod/beta", "world_pack")),
			"a mod pack added later goes just above vanilla too, under the world's pack");

		eq(List.of("mod/alpha", "players_pack"), place(List.of("players_pack", "mod/alpha"), List.of("players_pack")),
			"with no vanilla pack, mod packs go to the bottom");
		eq(List.of("vanilla", "other_top"), place(List.of("vanilla", "other_top"), List.of("vanilla")), "other packs are left alone");
	}

	private static List<String> place(List<String> selected, List<String> requested) {
		return PackOrder.placeAboveVanilla(selected, Function.identity(), requested, MODS);
	}
}
