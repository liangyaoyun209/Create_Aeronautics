package com.eriksonn.createaeronautics.mixins;

import com.eriksonn.createaeronautics.contraptions.AirshipContraption;
import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.dimension.AirshipDimensionManager;
import com.eriksonn.createaeronautics.world.FakeAirshipClientWorld;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.components.structureMovement.*;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderInfo;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value= ContraptionRenderInfo.class)
public class ContraptionRenderInfoMixin {

    @Mutable
    @Shadow(remap = false)
    private boolean visible;
    @Inject(locals = LocalCapture.CAPTURE_FAILHARD,remap=false,method = "beginFrame", at = @At("TAIL"))
    public void afterBeginFrame(BeginFrameEvent event,CallbackInfo ci,AbstractContraptionEntity entity) {
        if (entity instanceof ControlledContraptionEntity && entity.level instanceof FakeAirshipClientWorld) {
            int plotId = ((FakeAirshipClientWorld) entity.level).airship.plotId;
            AirshipContraptionEntity airshipEntity = AirshipManager.INSTANCE.AllClientAirships.get(plotId);
            if (airshipEntity != null) {
                visible = event.getClippingHelper().isVisible(airshipEntity.getBoundingBoxForCulling().inflate(2));
            }
        }
//
    }

}
