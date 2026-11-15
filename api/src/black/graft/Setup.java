package black.graft;

import java.util.function.Consumer;
import java.util.function.Function;

import black.graft.config.Config;
import black.graft.config.ConfigSpec;
import black.graft.event.Events;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * What a mod can declare from {@link Mod#initialize}. Everything goes under the mod's own namespace, so two mods can't
 * collide, and is created inside Minecraft's bootstrap in the order it was declared, blocks before items.
 *
 * <pre>{@code
 * public void initialize(Setup setup) {
 * 	Registered<Block> block = setup.block(new ContentName("zoom_block"),
 * 		properties -> new Block(properties.mapColor(MapColor.STONE).strength(1f)));
 * 	setup.addToTab(VanillaTab.BUILDING_BLOCKS, setup.blockItem(block));
 * }
 * }</pre>
 *
 * Its methods fail once {@code initialize} has returned: content can only be declared while Minecraft's registries are
 * still open.
 *
 * <p>Implemented by Graft: mods receive it, they don't implement it.
 */
public interface Setup {
	/** This mod, as its descriptor describes it. */
	ModInfo mod();

	/** Whether this is a client or a dedicated server. */
	Dist dist();

	/**
	 * Declares a block.
	 *
	 * @param <B>     the block's class
	 * @param name    its name within this mod
	 * @param factory makes the block from properties that already carry its id; set the rest on them
	 * @return a handle that holds the block once Minecraft has made it
	 * @throws IllegalArgumentException if this mod already declared a block by that name
	 */
	<B extends Block> Registered<B> block(ContentName name, Function<BlockBehaviour.Properties, B> factory);

	/**
	 * Declares an item.
	 *
	 * @param <I>     the item's class
	 * @param name    its name within this mod
	 * @param factory makes the item from properties that already carry its id; set the rest on them
	 * @return a handle that holds the item once Minecraft has made it
	 * @throws IllegalArgumentException if this mod already declared an item by that name
	 */
	<I extends Item> Registered<I> item(ContentName name, Function<Item.Properties, I> factory);

	/**
	 * Declares the item that places {@code block}, with the block's name and id: what vanilla does for most blocks.
	 *
	 * @param block a block this mod declared
	 * @return a handle that holds the item once Minecraft has made it
	 * @throws IllegalArgumentException if {@code block} belongs to another mod, or already has an item
	 */
	Registered<BlockItem> blockItem(Registered<? extends Block> block);

	/**
	 * Lists an item at the end of one of Minecraft's creative tabs.
	 *
	 * @param tab  the tab
	 * @param item an item or block item this mod declared
	 */
	void addToTab(VanillaTab tab, Registered<? extends ItemLike> item);

	/**
	 * Declares an in-game test, run by Minecraft's GameTest server ({@code gradle gameTest}) and by {@code /test}. Like
	 * vanilla's, a test is two parts: this function, and a test instance in the mod's data at
	 * {@code data/<mod id>/test_instance/<name>.json} that says where and how long it runs:
	 *
	 * <pre>{@code
	 * {
	 * "type": "minecraft:function",
	 * "function": "<mod id>:<name>",
	 * "environment": "minecraft:default",
	 * "structure": "minecraft:empty",
	 * "max_ticks": 20
	 * }
	 * }</pre>
	 *
	 * A server checks as it starts that every declared test has its instance, and refuses to start if one doesn't, so a test
	 * can't be skipped silently.
	 *
	 * @param name its name within this mod
	 * @param test the test: arrange, act, and end with {@code helper.succeed()} or a failed assertion
	 * @throws IllegalArgumentException if this mod already declared a test by that name
	 */
	void gameTest(ContentName name, Consumer<GameTestHelper> test);

	/** Where to listen for game events. */
	Events events();

	/**
	 * Reads this mod's config file, {@code config/<mod id>.properties}, writing it with defaults first if needed.
	 *
	 * @param spec every setting the file holds
	 * @throws black.graft.config.ConfigException if a player's edits left it unusable; the game stops and says why
	 * @throws IllegalStateException              if this mod already loaded its common config
	 */
	Config config(ConfigSpec spec);

	/** A logger named after this mod, for its own messages. */
	System.Logger logger();
}
