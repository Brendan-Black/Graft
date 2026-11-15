package black.graft.event;

import net.minecraft.server.MinecraftServer;

/**
 * A server is shutting down, before it saves its worlds for the last time.
 *
 * @param server the server
 */
public record ServerStopping(MinecraftServer server) implements Event {}
