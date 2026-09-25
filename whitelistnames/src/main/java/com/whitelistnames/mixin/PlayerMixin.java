package com.whitelistnames.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.whitelistnames.NameTagManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Chat, join/leave and death messages all use getDisplayName(). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component whitelistnames$useCustomName(Component original) {
        if ((Object) this instanceof ServerPlayer player) {
            Component nick = NameTagManager.nicknameFor(player);
            // keep vanilla's style (hover shows real username, click suggests /tell <username>)
            if (nick != null) return nick.copy().withStyle(original.getStyle());
        }
        return original;
    }
}
