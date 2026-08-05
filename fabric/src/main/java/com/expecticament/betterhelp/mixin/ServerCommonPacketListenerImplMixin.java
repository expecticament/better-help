package com.expecticament.betterhelp.mixin;

import com.expecticament.betterhelp.click.CustomClickActions;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    @Shadow
    @Final protected MinecraftServer server;

    @Shadow
    public abstract GameProfile getOwner();

    @Inject(method = "handleCustomClickAction", at = @At("HEAD"), cancellable = true)
    private void betterhelp$handleCustomClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo callbackInfo) {
        ServerPlayer player = server.getPlayerList().getPlayer(getOwner().id());
        if (player == null) {
            return;
        }

        if (CustomClickActions.handle(player, packet.id(), packet.payload().orElse(null))) {
            callbackInfo.cancel();
        }
    }
}
