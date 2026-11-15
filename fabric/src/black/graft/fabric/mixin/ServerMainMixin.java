package black.graft.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import black.graft.Nullable;
import black.graft.internal.GraftRuntime;
import net.minecraft.gametest.framework.GameTestMainUtil;
import net.minecraft.server.Main;

/**
 * Graft's game tests on Fabric. With {@code -Dgraft.gameTestServer=true}, a Fabric dedicated server hands over to
 * Minecraft's GameTest server once Fabric has started the mods (just before {@code server.properties} is read), so the
 * same tests that run on Graft's loader run here, with every Fabric mod loaded. The command line is the GameTest server's
 * ({@code --universe}, {@code --report}, {@code --tests}), and the process exits with the number of failed tests. Without
 * the property it does nothing.
 */
@Mixin(Main.class)
abstract class ServerMainMixin {
	@Unique
	private static String @Nullable [] graft$gameTestArgs;

	/** Keeps the GameTest server's arguments away from the dedicated server's parser, which would refuse them. */
	@ModifyVariable(method = "main", at = @At("HEAD"), argsOnly = true)
	private static String[] graft$takeGameTestArgs(String[] args) {
		if (!Boolean.getBoolean("graft.gameTestServer")) {
			return args;
		}
		graft$gameTestArgs = args;
		return new String[0];
	}

	@Inject(method = "main",
		at = @At(value = "INVOKE", target = "Ljava/nio/file/Paths;get(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", ordinal = 0),
		cancellable = true)
	private static void graft$runGameTests(String[] args, CallbackInfo info) {
		String[] gameTestArgs = graft$gameTestArgs;
		if (gameTestArgs == null) {
			return;
		}
		GraftRuntime.current(); // fails loudly if Fabric hadn't started Graft's mods yet
		try {
			GameTestMainUtil.runGameTestServer(gameTestArgs, _ -> {});
		} catch (Exception e) {
			throw new IllegalStateException("Minecraft's GameTest server failed to start: " + e, e);
		}
		info.cancel();
	}
}
