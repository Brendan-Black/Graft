package black.graft.event;

import net.minecraft.server.MinecraftServer;

/**
 * A server is about to start: a dedicated server, or the one a client runs for single player. Its worlds aren't loaded
 * yet.
 *
 * @param server the server
 */
public record ServerStarting(MinecraftServer server) implements Event {}
