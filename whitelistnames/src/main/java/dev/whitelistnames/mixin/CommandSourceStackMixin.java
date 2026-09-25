package dev.whitelistnames.mixin;

import dev.whitelistnames.NickCommands;
import dev.whitelistnames.NickStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Adds online players' nicknames to player-name tab completion. */
@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin {
	@Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true, require = 0)
	private void whitelistnames$suggestNicknames(CallbackInfoReturnable<Collection<String>> cir) {
		List<String> names = new ArrayList<>(cir.getReturnValue());
		for (ServerPlayer player : ((CommandSourceStack) (Object) this).getServer().getPlayerList().getPlayers()) {
			String nick = NickStore.getNick(player.getUUID());
			if (nick != null) {
				String option = NickCommands.quoteIfNeeded(nick);
				if (!names.contains(option)) names.add(option);
			}
		}
		cir.setReturnValue(names);
	}
}
