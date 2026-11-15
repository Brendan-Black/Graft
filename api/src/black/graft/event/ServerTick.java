package black.graft.event;

import net.minecraft.server.MinecraftServer;

/**
 * A server finished one game tick, normally 20 times a second.
 *
 * @param server the server
 */
public record ServerTick(MinecraftServer server) implements Event {}
