package com.eriksonn.createaeronautics.mixins;

import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public abstract class WorldRendererMixin {

    @Inject(method = "renderLevel(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V", at = @At("HEAD"))
    public void renderLevel(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo info) {
        ClientPlayerEntity player = Minecraft.getInstance().player;

        if(player.getVehicle() != null && player.getVehicle() instanceof AirshipContraptionEntity && !Minecraft.getInstance().gameRenderer.getMainCamera().isDetached()) {
            AirshipContraptionEntity airship = (AirshipContraptionEntity) player.getVehicle();
            pMatrixStack.mulPose(airship.smoothedRenderTransform.orientation.toMojangQuaternion());
//            Vector3d translation = player.getPosition(pPartialTicks).subtract(player.position());
//            pMatrixStack.translate(translation.x, translation.y, translation.z);
        }
    }
}
