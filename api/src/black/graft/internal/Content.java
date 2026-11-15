package black.graft.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import black.graft.ModId;
import black.graft.Registered;
import black.graft.VanillaTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Every block, item and creative tab entry the mods declared, and the code that puts them into Minecraft. */
public final class Content {
	private record BlockDecl<B extends Block>(ResourceKey<Block> key, Function<BlockBehaviour.Properties, B> factory, RegisteredContent<B> handle) {
		void create() {
			B block = factory.apply(BlockBehaviour.Properties.of().setId(key));
			Registry.register(BuiltInRegistries.BLOCK, key, block);
			handle.set(block);
		}
	}

	private record ItemDecl<I extends Item>(ResourceKey<Item> key, Function<Item.Properties, I> factory, RegisteredContent<I> handle) {
		void create() {
			I item = factory.apply(new Item.Properties().setId(key));
			if (item instanceof BlockItem blockItem) {
				blockItem.registerBlocks(Item.BY_BLOCK, item);
			}
			Registry.register(BuiltInRegistries.ITEM, key, item);
			handle.set(item);
		}
	}

	private final List<BlockDecl<?>> blocks = new ArrayList<>();
	private final List<ItemDecl<?>> items = new ArrayList<>();
	private final Map<VanillaTab, List<Registered<? extends ItemLike>>> tabs = new EnumMap<>(VanillaTab.class);
	private boolean registered;

	<B extends Block> RegisteredContent<B> block(ResourceKey<Block> key, Function<BlockBehaviour.Properties, B> factory) {
		RegisteredContent<B> handle = new RegisteredContent<>(key);
		blocks.add(new BlockDecl<>(key, factory, handle));
		return handle;
	}

	<I extends Item> RegisteredContent<I> item(ResourceKey<Item> key, Function<Item.Properties, I> factory) {
		RegisteredContent<I> handle = new RegisteredContent<>(key);
		items.add(new ItemDecl<>(key, factory, handle));
		return handle;
	}

	void addToTab(VanillaTab tab, Registered<? extends ItemLike> item) {
		tabs.computeIfAbsent(tab, _ -> new ArrayList<>()).add(item);
	}

	/**
	 * Creates and registers everything, the way vanilla's own bootstrap does: blocks, then their states into the global
	 * block-state id map, then items. Called once, before Minecraft's registries freeze.
	 *
	 * @param addBlockStates whether to add the new blocks' states to {@code Block.BLOCK_STATE_REGISTRY} here, as vanilla's
	 *                       bootstrap does: Graft's agent must; Fabric does it itself on registration
	 */
	public void register(boolean addBlockStates) {
		if (registered) {
			return;
		}
		registered = true;
		for (BlockDecl<?> b : blocks) {
			b.create();
		}
		for (BlockDecl<?> b : blocks) {
			if (!addBlockStates) {
				break;
			}
			for (BlockState state : b.handle().get().getStateDefinition().getPossibleStates()) {
				Block.BLOCK_STATE_REGISTRY.add(state);
				state.initCache();
			}
		}
		for (ItemDecl<?> i : items) {
			i.create();
		}
	}

	/** The mods' entries for a creative tab Minecraft is filling, in the order they were added. */
	public List<ItemStack> entriesFor(CreativeModeTab tab) {
		return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).map(this::entriesFor).orElse(List.of());
	}

	/** The mods' entries for the creative tab with this key: none unless it's one of vanilla's. */
	public List<ItemStack> entriesFor(ResourceKey<CreativeModeTab> key) {
		if (tabs.isEmpty() || !key.identifier().getNamespace().equals(ModId.MINECRAFT.value())) {
			return List.of();
		}
		List<ItemStack> entries = new ArrayList<>();
		for (VanillaTab vanilla : VanillaTab.values()) {
			if (vanilla.path().equals(key.identifier().getPath())) {
				for (Registered<? extends ItemLike> item : tabs.getOrDefault(vanilla, List.of())) {
					entries.add(new ItemStack(item.get()));
				}
			}
		}
		return entries;
	}

	/** The vanilla tabs mods added entries to. */
	public Set<VanillaTab> tabsWithEntries() {
		return Set.copyOf(tabs.keySet());
	}
}
