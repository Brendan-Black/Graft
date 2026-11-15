package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import black.graft.Dependency;
import black.graft.ModId;
import black.graft.Version;

/** Checks that the mods found can run together: one of each id, and every dependency installed at a version it accepts. */
public final class Resolution {
	private Resolution() {}

	/**
	 * @param candidates the mods found
	 * @param minecraft  the game's version, which {@code depends.minecraft} is checked against
	 * @param graft      this loader's version, which {@code depends.graft} is checked against
	 * @return the mods in load order: by id, so every machine with the same mods loads them the same way
	 * @throws LoadingException listing every problem: duplicates first, then missing mods, then wrong versions
	 */
	public static List<ModCandidate> resolve(List<ModCandidate> candidates, Version minecraft, Version graft) {
		Map<ModId, ModCandidate> byId = new LinkedHashMap<>();
		List<String> duplicates = new ArrayList<>();
		for (ModCandidate c : candidates) {
			ModCandidate earlier = byId.putIfAbsent(c.info().id(), c);
			if (earlier != null) {
				duplicates.add("mod " + c.info().id() + " is installed twice: " + earlier.source() + " (" + earlier.info().version()
					+ ") and " + c.source() + " (" + c.info().version() + "). Remove one.");
			}
		}
		if (!duplicates.isEmpty()) {
			throw new LoadingException(ErrorCode.DUPLICATE_MOD, duplicates);
		}

		List<String> missing = new ArrayList<>();
		List<String> wrongVersion = new ArrayList<>();
		for (ModCandidate c : byId.values()) {
			for (Dependency d : c.info().dependencies()) {
				Version installed;
				if (d.id().equals(ModId.MINECRAFT)) {
					installed = minecraft;
				} else if (d.id().equals(ModId.GRAFT)) {
					installed = graft;
				} else {
					ModCandidate other = byId.get(d.id());
					if (other == null) {
						missing.add(c.info().name() + " (" + c.info().id() + ") needs mod " + d.id() + " " + d.versions() + ", which isn't installed");
						continue;
					}
					installed = other.info().version();
				}
				if (!d.versions().contains(installed)) {
					wrongVersion.add(c.info().name() + " (" + c.info().id() + ") needs " + d.id() + " " + d.versions() + ", but " + installed
						+ " is installed");
				}
			}
		}
		if (!missing.isEmpty()) {
			missing.addAll(wrongVersion);
			throw new LoadingException(ErrorCode.MISSING_DEPENDENCY, missing);
		}
		if (!wrongVersion.isEmpty()) {
			throw new LoadingException(ErrorCode.WRONG_VERSION, wrongVersion);
		}

		List<ModCandidate> ordered = new ArrayList<>(byId.values());
		ordered.sort(Comparator.comparing(c -> c.info().id()));
		return List.copyOf(ordered);
	}
}
