package black.truezoom;

import black.graft.ContentName;
import black.graft.Mod;
import black.graft.Registered;
import black.graft.Setup;
import black.graft.VanillaTab;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * True Zoom on both sides: a block and a snack, which a server needs to know about too. The zoom itself is client-only,
 * in {@link black.truezoom.client.TrueZoomClient}.
 */
public final class TrueZoom implements Mod {
	@Override
	public void initialize(Setup setup) {
		Registered<Block> block = setup.block(new ContentName("zoom_block"), properties -> new Block(properties
			.mapColor(MapColor.STONE)
			.instrument(NoteBlockInstrument.BANJO)
			.sound(SoundType.ANVIL)
			.strength(1f)));
		Registered<BlockItem> blockItem = setup.blockItem(block);

		Registered<Item> snack = setup.item(new ContentName("zoom_food"), properties -> new Item(properties
			.food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build())));

		setup.addToTab(VanillaTab.BUILDING_BLOCKS, blockItem);
		setup.addToTab(VanillaTab.FOOD_AND_DRINKS, snack);
	}
}
