package black.graft.loader;

import black.graft.Nullable;
import black.graft.Registered;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Graft's {@link Registered}: filled in once, on the startup thread, when Minecraft's registries are populated. */
final class RegisteredContent<T> implements Registered<T> {
	private final ResourceKey<? super T> key;
	private volatile @Nullable T value;

	RegisteredContent(ResourceKey<? super T> key) {
		this.key = key;
	}

	@Override
	public Identifier id() {
		return key.identifier();
	}

	@Override
	public ResourceKey<? super T> key() {
		return key;
	}

	@Override
	public boolean isPresent() {
		return value != null;
	}

	@Override
	public T get() {
		T v = value;
		if (v == null) {
			throw new IllegalStateException(id() + " doesn't exist until Minecraft's registries are populated: call get() from an event"
				+ " listener or game code, not from initialize()");
		}
		return v;
	}

	void set(T created) {
		if (value != null) {
			throw new IllegalStateException(id() + " was created twice");
		}
		value = created;
	}

	@Override
	public String toString() {
		return id().toString();
	}
}
