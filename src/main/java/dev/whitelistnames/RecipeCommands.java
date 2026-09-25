package dev.whitelistnames;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

/** /eyerecipe [enable|disable] - turns the Eye of Ender crafting recipe on or off. */
public final class RecipeCommands {
	private RecipeCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// Ops only: same permission as vanilla /op (or /gamemode in singleplayer/LAN, where /op doesn't exist).
		CommandNode<CommandSourceStack> permSource = dispatcher.getRoot().getChild("op");
		if (permSource == null) permSource = dispatcher.getRoot().getChild("gamemode");
		Predicate<CommandSourceStack> permission = permSource != null ? permSource.getRequirement() : s -> false;

		dispatcher.register(Commands.literal("eyerecipe")
				.requires(permission)
				.executes(RecipeCommands::status)
				.then(Commands.literal("enable").executes(ctx -> set(ctx, false)))
				.then(Commands.literal("disable").executes(ctx -> set(ctx, true))));
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		boolean disabled = Settings.isEyeOfEnderRecipeDisabled();
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Eye of Ender crafting is " + (disabled ? "disabled." : "enabled.")), false);
		return disabled ? 0 : 1;
	}

	private static int set(CommandContext<CommandSourceStack> ctx, boolean disabled) {
		Settings.setEyeOfEnderRecipeDisabled(disabled);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Eye of Ender crafting is now " + (disabled ? "disabled." : "enabled.")), true);
		return 1;
	}
}
