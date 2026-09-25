package com.whitelistnames.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.whitelistnames.NameStore;
import com.whitelistnames.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Custom "you are not whitelisted" disconnect message. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @ModifyReturnValue(method = "canPlayerLogin", at = @At("RETURN"))
    private Component whitelistnames$customKick(Component original) {
        if (original != null
                && original.getContents() instanceof TranslatableContents t
                && t.getKey().equals("multiplayer.disconnect.not_whitelisted")) {
            String msg = NameStore.config.notWhitelistedKickMessage;
            if (msg != null && !msg.isBlank()) return Text.of(msg);
        }
        return original;
    }
}
