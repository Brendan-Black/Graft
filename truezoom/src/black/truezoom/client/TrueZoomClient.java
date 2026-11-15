package black.truezoom.client;

import com.mojang.blaze3d.platform.InputConstants;

import black.graft.ContentName;
import black.graft.client.ClientMod;
import black.graft.client.ClientSetup;
import black.graft.client.ClientTick;
import black.graft.client.KeyBinding;
import black.graft.config.Config;

/** The zoom: hold the key (C to start with) and the view narrows smoothly; let go and it eases back. */
public final class TrueZoomClient implements ClientMod {
	@Override
	public void initializeClient(ClientSetup setup) {
		Config config = setup.config(ZoomSettings.SPEC);
		Zoom zoom = new Zoom(config.get(ZoomSettings.FACTOR), config.get(ZoomSettings.SMOOTHING));
		KeyBinding key = setup.keyBinding(new ContentName("zoom"), InputConstants.KEY_C);

		setup.events().listen(ClientTick.class, _ -> zoom.tick(key.isDown()));
		setup.modifyFov(zoom::apply);
	}
}
