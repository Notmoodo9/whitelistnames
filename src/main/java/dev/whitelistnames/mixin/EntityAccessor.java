package dev.whitelistnames.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla's startRiding refuses vehicles that aren't saved to disk (like players), so nametags
 * are mounted by hand, doing the same two steps startRiding does.
 */
@Mixin(Entity.class)
public interface EntityAccessor {
	@Accessor("vehicle")
	void whitelistnames$setVehicle(Entity vehicle);

	@Invoker("addPassenger")
	void whitelistnames$addPassenger(Entity passenger);
}
