package black.graft.loader.transform;

import static java.lang.constant.ConstantDescs.CD_Collection;
import static java.lang.constant.ConstantDescs.CD_float;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;

import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import black.graft.loader.transform.Patch.Injection;

/**
 * Every change Graft makes to Minecraft 26.3, in one place. Each one is a single call into Graft's hook classes; the
 * README's "How Graft hooks in" table lists the same ones. A build test applies them all to the real game jar and runs the
 * JVM's verifier over the result, so a Minecraft update that moves a hook fails Graft's build rather than a player's game.
 */
public final class MinecraftPatches {
	private static final String SERVER_CLASS = "net/minecraft/server/MinecraftServer";
	private static final String TAB_CLASS = "net/minecraft/world/item/CreativeModeTab";
	private static final String OPTIONS_CLASS = "net/minecraft/client/Options";
	private static final String MINECRAFT_CLASS = "net/minecraft/client/Minecraft";
	private static final String NO_ARGS_VOID = "()V";
	private static final ClassDesc MINECRAFT_SERVER = ClassDesc.ofInternalName(SERVER_CLASS);
	private static final ClassDesc CREATIVE_MODE_TAB = ClassDesc.ofInternalName(TAB_CLASS);
	private static final ClassDesc TAB_OUTPUT = ClassDesc.ofInternalName(TAB_CLASS + "$Output");
	private static final ClassDesc REPOSITORY_SOURCES = ClassDesc.ofInternalName("net/minecraft/server/packs/repository/RepositorySource").arrayType();
	private static final ClassDesc MINECRAFT = ClassDesc.ofInternalName(MINECRAFT_CLASS);
	private static final ClassDesc OPTIONS = ClassDesc.ofInternalName(OPTIONS_CLASS);
	private static final ClassDesc KEY_MAPPINGS = ClassDesc.ofInternalName("net/minecraft/client/KeyMapping").arrayType();
	private static final String KEY_MAPPINGS_FIELD = "keyMappings";

	private MinecraftPatches() {}

	/**
	 * The patches every game needs, client or dedicated server.
	 *
	 * @param hooks the class whose static methods the patches call: {@code black.graft.loader.Hooks} in the game
	 */
	public static List<Patch> common(ClassDesc hooks) {
		return List.of(
			new Patch("net/minecraft/server/Bootstrap", "bootStrap", NO_ARGS_VOID,
				"starting mods before Minecraft bootstraps", new Injection.AtHead(),
				code -> code.invokestatic(hooks, "initialize", MethodTypeDesc.of(CD_void))),
			new Patch("net/minecraft/core/registries/BuiltInRegistries", "freeze", NO_ARGS_VOID,
				"adding mod blocks and items before the registries freeze", new Injection.AtHead(),
				code -> code.invokestatic(hooks, "registerContent", MethodTypeDesc.of(CD_void))),
			new Patch(TAB_CLASS, "buildContents",
				"(Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;)V",
				"listing mod items in creative tabs",
				new Injection.AfterInvoke(TAB_CLASS + "$DisplayItemsGenerator", "accept",
					"(Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;Lnet/minecraft/world/item/CreativeModeTab$Output;)V"),
				// local 2 is the ItemDisplayBuilder the vanilla generator just filled, local 0 the tab. Output is protected, so the
				// call to it is made here, inside CreativeModeTab, with the entries Graft returns.
				code -> code.aload(2)
					.aload(0)
					.invokestatic(hooks, "tabEntries", MethodTypeDesc.of(CD_Collection, CREATIVE_MODE_TAB))
					.invokeinterface(TAB_OUTPUT, "acceptAll", MethodTypeDesc.of(CD_void, CD_Collection))),
			new Patch("net/minecraft/server/packs/repository/PackRepository", INIT_NAME,
				"([Lnet/minecraft/server/packs/repository/RepositorySource;)V",
				"offering mod assets and data as built-in packs", new Injection.AtHead(),
				code -> code.aload(1).invokestatic(hooks, "extendPackSources", MethodTypeDesc.of(REPOSITORY_SOURCES, REPOSITORY_SOURCES)).astore(1)),
			new Patch(SERVER_CLASS, "runServer", NO_ARGS_VOID,
				"the ServerStarting event, inside runServer's crash handling",
				new Injection.BeforeInvoke(SERVER_CLASS, "initServer", "()Z"),
				code -> code.aload(0).invokestatic(hooks, "serverStarting", MethodTypeDesc.of(CD_void, MINECRAFT_SERVER))),
			new Patch(SERVER_CLASS, "stopServer", NO_ARGS_VOID,
				"the ServerStopping event", new Injection.AtHead(),
				code -> code.aload(0).invokestatic(hooks, "serverStopping", MethodTypeDesc.of(CD_void, MINECRAFT_SERVER))),
			new Patch(SERVER_CLASS, "tickServer", "(Ljava/util/function/BooleanSupplier;)V",
				"the ServerTick event", new Injection.BeforeReturns(),
				code -> code.aload(0).invokestatic(hooks, "serverTick", MethodTypeDesc.of(CD_void, MINECRAFT_SERVER))));
	}

	/**
	 * The patches only a client needs. Their classes don't exist on a dedicated server.
	 *
	 * @param clientHooks the class whose static methods the patches call: {@code black.graft.loader.ClientHooks} in the game
	 */
	public static List<Patch> client(ClassDesc clientHooks) {
		return List.of(
			new Patch(OPTIONS_CLASS, INIT_NAME, "(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
				"adding mod key bindings before saved keys load",
				new Injection.AfterFieldWrite(Opcode.PUTFIELD, OPTIONS_CLASS, KEY_MAPPINGS_FIELD),
				code -> code.aload(0)
					.aload(0)
					.getfield(OPTIONS, KEY_MAPPINGS_FIELD, KEY_MAPPINGS)
					.invokestatic(clientHooks, "extendKeyMappings", MethodTypeDesc.of(KEY_MAPPINGS, KEY_MAPPINGS))
					.putfield(OPTIONS, KEY_MAPPINGS_FIELD, KEY_MAPPINGS)),
			new Patch("net/minecraft/client/Camera", "calculateFov", "(F)F",
				"mod field-of-view modifiers", new Injection.BeforeReturns(),
				// the field of view is on the stack; local 1 is the partial tick
				code -> code.fload(1).invokestatic(clientHooks, "modifyFov", MethodTypeDesc.of(CD_float, CD_float, CD_float))),
			new Patch(MINECRAFT_CLASS, "tick", NO_ARGS_VOID,
				"the ClientTick event", new Injection.BeforeReturns(),
				code -> code.aload(0).invokestatic(clientHooks, "clientTick", MethodTypeDesc.of(CD_void, MINECRAFT))));
	}
}
