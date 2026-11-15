package black.graft.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import black.graft.ModId;
import black.graft.Nullable;
import black.graft.client.FovModifier;
import black.graft.client.KeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** What client mods declared: key bindings and field-of-view steps. */
public final class ClientContent {
	private final List<KeyBindingImpl> keyBindings = new ArrayList<>();
	private final List<FovModifier> fovModifiers = new ArrayList<>();
	private volatile List<FovModifier> fovChain = List.of();
	private KeyMapping @Nullable [] created;

	KeyBinding keyBinding(ModId mod, String name, int defaultKey) {
		KeyBindingImpl binding = new KeyBindingImpl(mod, "key." + mod + "." + name, defaultKey);
		keyBindings.add(binding);
		return binding;
	}

	void modifyFov(FovModifier modifier) {
		fovModifiers.add(modifier);
		fovChain = List.copyOf(fovModifiers);
	}

	/**
	 * The mods' key mappings, created the first time they're asked for. Each mod gets its own category in Controls.
	 */
	public List<KeyMapping> mappings() {
		KeyMapping[] mine = created;
		if (mine == null) {
			Map<ModId, KeyMapping.Category> categories = new HashMap<>();
			mine = new KeyMapping[keyBindings.size()];
			for (int i = 0; i < mine.length; i++) {
				KeyBindingImpl b = keyBindings.get(i);
				KeyMapping.Category category = categories.computeIfAbsent(b.mod(),
					m -> KeyMapping.Category.register(Identifier.fromNamespaceAndPath(m.value(), "keys")));
				mine[i] = new KeyMapping(b.translationKey(), b.defaultKey(), category);
				b.bind(mine[i]);
			}
			created = mine;
		}
		return List.of(mine);
	}

	/** Vanilla's key mappings followed by the mods', for Graft's agent, which adds them as the options load. */
	public KeyMapping[] extend(KeyMapping[] vanilla) {
		KeyMapping[] mine = mappings().toArray(KeyMapping[]::new);
		KeyMapping[] all = new KeyMapping[vanilla.length + mine.length];
		System.arraycopy(vanilla, 0, all, 0, vanilla.length);
		System.arraycopy(mine, 0, all, vanilla.length, mine.length);
		return all;
	}

	/** Runs the mods' field-of-view steps over vanilla's. */
	public float modifyFov(float fov, float partialTick) {
		float result = fov;
		for (FovModifier m : fovChain) {
			result = m.modify(result, partialTick);
		}
		return result;
	}

	/** One declared key binding, and its vanilla mapping once the options have loaded. */
	static final class KeyBindingImpl implements KeyBinding {
		private final ModId mod;
		private final String translationKey;
		private final int defaultKey;
		private volatile @Nullable KeyMapping mapping;

		KeyBindingImpl(ModId mod, String translationKey, int defaultKey) {
			this.mod = mod;
			this.translationKey = translationKey;
			this.defaultKey = defaultKey;
		}

		ModId mod() {
			return mod;
		}

		String translationKey() {
			return translationKey;
		}

		int defaultKey() {
			return defaultKey;
		}

		void bind(KeyMapping created) {
			mapping = created;
		}

		@Override
		public boolean isDown() {
			KeyMapping m = mapping;
			return m != null && m.isDown();
		}

		@Override
		public boolean consumeClick() {
			KeyMapping m = mapping;
			return m != null && m.consumeClick();
		}

		@Override
		public KeyMapping mapping() {
			KeyMapping m = mapping;
			if (m == null) {
				throw new IllegalStateException(translationKey + " has no key mapping until the game's options load");
			}
			return m;
		}

		@Override
		public String toString() {
			return translationKey;
		}
	}
}
