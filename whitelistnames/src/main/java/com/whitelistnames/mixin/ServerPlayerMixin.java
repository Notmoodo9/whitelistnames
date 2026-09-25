package com.whitelistnames.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.whitelistnames.NameTagManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tab list name. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
    private Component whitelistnames$tabName(Component original) {
        Component nick = NameTagManager.nicknameFor((ServerPlayer) (Object) this);
        return nick != null ? nick : original;
    }
}
