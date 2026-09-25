package com.whitelistnames.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setBillboardConstraints")
    void whitelistnames$setBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setTransformation")
    void whitelistnames$setTransformation(Transformation transformation);
}
