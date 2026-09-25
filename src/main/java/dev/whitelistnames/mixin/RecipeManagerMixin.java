package dev.whitelistnames.mixin;

import dev.whitelistnames.Settings;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Hides the Eye of Ender recipe while it's disabled with /eyerecipe. The crafting table and inventory
 * grid look recipes up through these getRecipeFor overloads (crafters too, via RecipeCacheMixin).
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
	@Inject(method = {
			"getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
			"getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
			"getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;"
	}, at = @At("RETURN"), cancellable = true)
	private void whitelistnames$blockEyeOfEnder(CallbackInfoReturnable<Optional<?>> cir) {
		if (Settings.isBlockedRecipe(cir.getReturnValue())) cir.setReturnValue(Optional.empty());
	}
}
