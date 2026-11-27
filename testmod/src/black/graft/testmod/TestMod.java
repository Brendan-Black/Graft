package black.graft.testmod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import black.graft.ContentName;
import black.graft.Mod;
import black.graft.Registered;
import black.graft.Setup;
import black.graft.VanillaTab;
import black.graft.config.Config;
import black.graft.config.ConfigSpec;
import black.graft.config.Setting;
import black.graft.event.ServerStarting;
import black.graft.event.ServerTick;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Graft's API, exercised in a real world: every test here runs on Graft's own loader and on Cambium, so the two
 * platforms are held to the same behaviour. It's only built and run by Graft's own build, never published.
 */
public final class TestMod implements Mod {
	private static final Setting.WholeNumber ANSWER = new Setting.WholeNumber("answer", 42, 0, 100, "A setting the tests read back.");
	private static final String VANILLA_PACK = "vanilla";
	private static final ContentName TEST_BLOCK = new ContentName("test_block");
	private static final ResourceKey<CreativeModeTab> INGREDIENTS = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
		Identifier.withDefaultNamespace(VanillaTab.INGREDIENTS.path()));

	private final AtomicBoolean serverStarting = new AtomicBoolean();
	private final AtomicLong serverTicks = new AtomicLong();

	@Override
	public void initialize(Setup setup) {
		Registered<Block> block = setup.block(TEST_BLOCK, Block::new);
		Registered<BlockItem> blockItem = setup.blockItem(block);
		Registered<Item> gadget = setup.item(new ContentName("test_gadget"), Item::new);
		setup.addToTab(VanillaTab.INGREDIENTS, gadget);
		Config config = setup.config(ConfigSpec.of(ANSWER));
		int answer = config.get(ANSWER);

		setup.events().listen(ServerStarting.class, _ -> serverStarting.set(true));
		setup.events().listen(ServerTick.class, _ -> serverTicks.incrementAndGet());

		setup.gameTest(new ContentName("registered_ids"), helper -> {
			helper.assertValueEqual(Identifier.fromNamespaceAndPath("graft_testmod", TEST_BLOCK.value()), block.id(), "the block's id");
			helper.assertValueEqual(block.get(), BuiltInRegistries.BLOCK.getValue(block.id()), "the registry holds the block");
			helper.assertValueEqual(gadget.get(), BuiltInRegistries.ITEM.getValue(gadget.id()), "the registry holds the item");
			helper.succeed();
		});

		setup.gameTest(new ContentName("block_item_links_block"), helper -> {
			helper.assertValueEqual(block.get(), blockItem.get().getBlock(), "the block item places the block");
			helper.assertValueEqual(blockItem.get(), block.get().asItem(), "the block knows its item");
			helper.assertValueEqual(block.id(), blockItem.id(), "they share an id");
			helper.succeed();
		});

		setup.gameTest(new ContentName("server_starting_fired"), helper -> {
			helper.assertTrue(serverStarting.get(), "ServerStarting reached the mod");
			helper.succeed();
		});

		setup.gameTest(new ContentName("server_ticks_counted"), helper -> {
			long start = serverTicks.get();
			helper.succeedWhen(() -> helper.assertTrue(serverTicks.get() >= start + 3, "ServerTick reached the mod three times"));
		});

		setup.gameTest(new ContentName("config_written"), helper -> {
			helper.assertValueEqual(42, answer, "the setting's default");
			helper.assertTrue(Files.isRegularFile(Path.of("config", "graft_testmod.properties")), "the config file was written");
			helper.succeed();
		});

		// A world that starts with vanilla and one other data pack, as a new world does: Minecraft adds the mod's required pack, and
		// Graft puts it just above vanilla, under the world's own pack. Graft's loader names it mod/<id>; Fabric and NeoForge order
		// their own packs, so there it's skipped. The selection is put back afterwards; nothing is reloaded.
		setup.gameTest(new ContentName("mod_data_above_vanilla"), helper -> {
			PackRepository packs = helper.getLevel().getServer().getPackRepository();
			List<String> before = List.copyOf(packs.getSelectedIds());
			String mine = "mod/graft_testmod";
			String other = before.stream().filter(id -> !id.equals(VANILLA_PACK) && !id.startsWith("mod/")).findFirst().orElse("");
			if (before.contains(mine) && !other.isEmpty()) {
				try {
					packs.setSelected(List.of(VANILLA_PACK, other));
					List<String> placed = List.copyOf(packs.getSelectedIds());
					helper.assertValueEqual(List.of(VANILLA_PACK, mine, other), placed, "the mod's data pack, just above vanilla's and under the world's");
				} finally {
					packs.setSelected(before);
				}
			}
			helper.succeed();
		});

		setup.gameTest(new ContentName("creative_tab_lists_item"), helper -> {
			CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), false, helper.getLevel().registryAccess());
			CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(INGREDIENTS);
			helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(gadget.get())), "the Ingredients tab lists the gadget");
			helper.succeed();
		});
	}
}
