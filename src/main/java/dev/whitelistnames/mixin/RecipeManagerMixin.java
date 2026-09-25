package dev.whitelistnames.mixin;

import dev.whitelistnames.Settings;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Hides the Eye of Ender recipe while it's disabled with /eyerecipe. Crafting tables, the inventory
 * grid and crafters all look recipes up through getRecipeFor.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
	@Inject(method = "getRecipeFor", at = @At("RETURN"), cancellable = true)
	private void whitelistnames$blockEyeOfEnder(CallbackInfoReturnable<Optional<? extends RecipeHolder<?>>> cir) {
		if (!Settings.isEyeOfEnderRecipeDisabled()) return;
		Optional<? extends RecipeHolder<?>> result = cir.getReturnValue();
		if (result.isPresent() && isEyeOfEnder(result.get())) cir.setReturnValue(Optional.empty());
	}

	private static boolean isEyeOfEnder(RecipeHolder<?> holder) {
		return holder.id().location().toString().equals("minecraft:ender_eye");
	}
}
