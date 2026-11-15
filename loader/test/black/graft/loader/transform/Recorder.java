package black.graft.loader.transform;

import java.util.ArrayList;
import java.util.List;

/** Stands in for Graft's hook classes in {@link PatcherTest}: records what the patched code called, with what. */
public final class Recorder {
	private static final List<String> notes = new ArrayList<>();

	private Recorder() {}

	static void reset() {
		notes.clear();
	}

	static List<String> notes() {
		return List.copyOf(notes);
	}

	public static void note(String what) {
		notes.add(what);
	}

	public static int[] extend(int[] values) {
		int[] out = java.util.Arrays.copyOf(values, values.length + 1);
		out[values.length] = 99;
		return out;
	}

	public static float scale(float fov, float factor) {
		return fov * factor;
	}
}
