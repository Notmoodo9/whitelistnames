package dev.whitelistnames.mixin;

import dev.whitelistnames.Settings;
import net.minecraft.world.item.crafting.RecipeCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Crafters cache recipe lookups, so a cached Eye of Ender recipe is blocked here too. */
@Mixin(RecipeCache.class)
public abstract class RecipeCacheMixin {
	@Inject(method = "get", at = @At("RETURN"), cancellable = true)
	private void whitelistnames$blockEyeOfEnder(CallbackInfoReturnable<Optional<?>> cir) {
		if (Settings.isBlockedRecipe(cir.getReturnValue())) cir.setReturnValue(Optional.empty());
	}
}
