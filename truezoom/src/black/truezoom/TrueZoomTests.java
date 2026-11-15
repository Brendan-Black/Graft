package black.truezoom;

import black.graft.ContentName;
import black.graft.Registered;
import black.graft.Setup;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * True Zoom's game tests, run in a real world by {@code gradle gameTest}, on Graft's loader and on Fabric. Each has its
 * test instance in {@code data/truezoom/test_instance/}.
 */
final class TrueZoomTests {
	private static final BlockPos SPOT = new BlockPos(0, 1, 0);

	private TrueZoomTests() {}

	static void declare(Setup setup, Registered<Block> block, Registered<BlockItem> blockItem, Registered<Item> snack) {
		setup.gameTest(new ContentName("places_block"), helper -> {
			helper.setBlock(SPOT, block.get());
			helper.assertBlockPresent(block.get(), SPOT);
			helper.succeed();
		});

		// Breaking it drops it: its loot table, from True Zoom's data, was loaded.
		setup.gameTest(new ContentName("drops_itself"), helper -> {
			helper.setBlock(SPOT, block.get());
			helper.getLevel().destroyBlock(helper.absolutePos(SPOT), true);
			helper.succeedWhen(() -> helper.assertItemEntityPresent(blockItem.get(), SPOT, 2.0));
		});

		setup.gameTest(new ContentName("snack_is_food"), helper -> {
			ItemStack stack = new ItemStack(snack.get());
			FoodProperties food = stack.get(DataComponents.FOOD);
			helper.assertTrue(food != null, "the zoom snack has food properties");
			helper.assertValueEqual(1, Objects.requireNonNull(food).nutrition(), "the zoom snack's nutrition");
			helper.succeed();
		});
	}
}
