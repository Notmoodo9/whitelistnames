package dev.whitelistnames.mixin;

import dev.whitelistnames.Settings;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Crafters cache recipe lookups, including "no recipe" answers. Reset the cache whenever /eyerecipe
 * changes, so turning the recipe off or back on takes effect right away.
 */
@Mixin(RecipeCache.class)
public abstract class RecipeCacheMixin {
	@Shadow
	private WeakReference<RecipeManager> cachedRecipeManager;

	@Unique
	private int whitelistnames$seenGeneration = -1;

	@Inject(method = "get", at = @At("HEAD"))
	private void whitelistnames$resetOnToggle(CallbackInfoReturnable<Optional<?>> cir) {
		int generation = Settings.recipeGeneration();
		if (generation != whitelistnames$seenGeneration) {
			whitelistnames$seenGeneration = generation;
			// Vanilla clears the cache when the recipe manager changes (e.g. /reload); make it think it did.
			cachedRecipeManager = new WeakReference<>(null);
		}
	}

	@Inject(method = "get", at = @At("RETURN"), cancellable = true)
	private void whitelistnames$blockEyeOfEnder(CallbackInfoReturnable<Optional<?>> cir) {
		if (Settings.isBlockedRecipe(cir.getReturnValue())) cir.setReturnValue(Optional.empty());
	}
}
