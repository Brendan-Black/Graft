package black.graft.client;

/** One step in working out the field of view, added with {@link ClientSetup#modifyFov}. Called every frame: keep it cheap. */
@FunctionalInterface
public interface FovModifier {
	/**
	 * @param fov         the field of view so far, in degrees
	 * @param partialTick how far this frame is between the last game tick and the next, from 0 to 1, for smooth motion
	 * @return the field of view to use, in degrees
	 */
	float modify(float fov, float partialTick);
}
