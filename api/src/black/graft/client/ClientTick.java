package black.graft.client;

import black.graft.event.Event;
import net.minecraft.client.Minecraft;

/**
 * The client finished one game tick, normally 20 times a second, whether or not a world is open.
 *
 * @param client the game
 */
public record ClientTick(Minecraft client) implements Event {}
