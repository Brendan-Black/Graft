package black.graft;

/**
 * Minecraft's own creative inventory tabs, which mods add items to with {@link Setup#addToTab}. The search tab finds
 * whatever is in these. Tabs of a mod's own are on Graft's roadmap: the creative screen has a fixed grid of tab slots,
 * and vanilla fills it.
 */
public enum VanillaTab {
	/** Building Blocks. */
	BUILDING_BLOCKS("building_blocks"),
	/** Colored Blocks. */
	COLORED_BLOCKS("colored_blocks"),
	/** Natural Blocks. */
	NATURAL_BLOCKS("natural_blocks"),
	/** Functional Blocks. */
	FUNCTIONAL_BLOCKS("functional_blocks"),
	/** Redstone Blocks. */
	REDSTONE_BLOCKS("redstone_blocks"),
	/** Tools &amp; Utilities. */
	TOOLS_AND_UTILITIES("tools_and_utilities"),
	/** Combat. */
	COMBAT("combat"),
	/** Food &amp; Drinks. */
	FOOD_AND_DRINKS("food_and_drinks"),
	/** Ingredients. */
	INGREDIENTS("ingredients"),
	/** Spawn Eggs. */
	SPAWN_EGGS("spawn_eggs"),
	/** Operator Utilities, shown only to operators with the option turned on. */
	OP_BLOCKS("op_blocks");

	private final String path;

	VanillaTab(String path) {
		this.path = path;
	}

	/** The tab's id path in the {@code minecraft} namespace, {@code building_blocks}. */
	public String path() {
		return path;
	}
}
