package black.truezoom.client;

import com.mojang.blaze3d.platform.InputConstants;

import black.graft.ContentName;
import black.graft.client.ClientMod;
import black.graft.client.ClientSetup;
import black.graft.client.ClientTick;
import black.graft.client.KeyBinding;
import black.graft.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The zoom: hold the key (C to start with) and the view narrows smoothly; let go and it eases back. Double-tap it and hold
 * instead, and you rush the way you're facing, faster every tick, until you let go; jump meanwhile and you climb the same
 * way, for as long as you hold jump.
 */
public final class TrueZoomClient implements ClientMod {
	private static final double TICKS_PER_SECOND = 20;
	/** Looking nearly straight up or down leaves no direction along the ground to rush in. */
	private static final double LEVEL_LOOK = 1e-4;

	@Override
	public void initializeClient(ClientSetup setup) {
		Config config = setup.config(ZoomSettings.SPEC);
		Zoom zoom = new Zoom(config.get(ZoomSettings.FACTOR), config.get(ZoomSettings.SMOOTHING));
		Boost boost = new Boost(config.get(ZoomSettings.BOOST_SPEED) / TICKS_PER_SECOND);
		KeyBinding key = setup.keyBinding(new ContentName("zoom"), InputConstants.KEY_C);

		setup.events().listen(ClientTick.class, tick -> {
			Minecraft client = tick.client();
			LocalPlayer player = client.player;
			double speed = boost.tick(key.isDown());
			zoom.tick(key.isDown() && !boost.boosting()); // a boost is for moving, not looking closer
			if (player == null || player.isPassenger()) {
				return;
			}
			double rise = boost.rise(client.options.keyJump.isDown(), player.onGround());
			if (speed > 0) {
				rush(player, speed, rise);
			}
		});
		setup.modifyFov(zoom::apply);
	}

	/**
	 * Moves the player along the ground the way they face, at {@code speed} blocks per tick, and up at {@code rise} blocks
	 * per tick; with no rise, they keep their fall or jump.
	 */
	private static void rush(LocalPlayer player, double speed, double rise) {
		Vec3 look = player.getLookAngle();
		Vec3 ground = new Vec3(look.x, 0, look.z);
		Vec3 along = ground.length() < LEVEL_LOOK ? Vec3.ZERO : ground.normalize().scale(speed);
		player.setDeltaMovement(along.x, rise > 0 ? rise : player.getDeltaMovement().y, along.z);
	}
}
