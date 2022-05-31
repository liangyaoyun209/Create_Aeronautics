package com.eriksonn.createaeronautics.mixins;

import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandlerClient;
import com.simibubi.create.content.contraptions.components.structureMovement.sync.ContraptionInteractionPacket;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ContraptionHandlerClient.class)
public class ContraptionHandlerClientMixin {

    /**
     * @author RyanHCode
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    @Overwrite
    public static void rightClickingOnContraptionsGetsHandledLocally(InputEvent.ClickInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null)
            return;
        if (mc.level == null)
            return;
        if (!event.isUseItem())
            return;
        Vector3d playerEye = RaycastHelper.getTraceOrigin(player);

        Entity vehicle = Minecraft.getInstance().player.getVehicle();
        if(vehicle != null && vehicle instanceof AirshipContraptionEntity && !Minecraft.getInstance().gameRenderer.getMainCamera().isDetached()) {
        }

        double reach = mc.gameMode.getPickRange();
        if (mc.hitResult != null) {
            mc.hitResult.getLocation();
            reach = Math.max(mc.hitResult.getLocation()
                    .distanceTo(playerEye), reach);
        }

        Vector3d target = RaycastHelper.getTraceTarget(mc.player, reach, playerEye);

        if(vehicle != null && vehicle instanceof AirshipContraptionEntity) {
            ActiveRenderInfoMixin infoMixin = ((ActiveRenderInfoMixin) Minecraft.getInstance().gameRenderer.getMainCamera());
            Vector3f forwards = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
            target = /*((AirshipContraptionEntity) vehicle).getPassengerPosition(mc.player, 1f).add(0, mc.player.getEyeHeight(), 0)*/playerEye.add(forwards.x() * reach, forwards.y() * reach, forwards.z() * reach);
        }

        for (AbstractContraptionEntity contraptionEntity : mc.level
                .getEntitiesOfClass(AbstractContraptionEntity.class, new AxisAlignedBB(playerEye, target).inflate(15.0))) {

            Vector3d localOrigin = contraptionEntity.toLocalVector(playerEye, 1);
            Vector3d localTarget = contraptionEntity.toLocalVector(target, 1);
            Contraption contraption = contraptionEntity.getContraption();

            MutableObject<BlockRayTraceResult> mutableResult = new MutableObject<>();
            RaycastHelper.PredicateTraceResult predicateResult = RaycastHelper.rayTraceUntil(localOrigin, localTarget, p -> {
                Template.BlockInfo blockInfo = contraption.getBlocks()
                        .get(p);
                if (blockInfo == null)
                    return false;
                BlockState state = blockInfo.state;
                VoxelShape raytraceShape = state.getShape(Minecraft.getInstance().level, BlockPos.ZERO.below());
                if (raytraceShape.isEmpty())
                    return false;
                BlockRayTraceResult rayTrace = raytraceShape.clip(localOrigin, localTarget, p);
                if (rayTrace != null) {
                    mutableResult.setValue(rayTrace);
                    return true;
                }
                return false;
            });

            if (predicateResult == null || predicateResult.missed())
                return;

            BlockRayTraceResult rayTraceResult = mutableResult.getValue();
            Hand hand = event.getHand();
            Direction face = rayTraceResult.getDirection();
            BlockPos pos = rayTraceResult.getBlockPos();

            if (!contraptionEntity.handlePlayerInteraction(player, pos, face, hand))
                return;
            AllPackets.channel.sendToServer(new ContraptionInteractionPacket(contraptionEntity, hand, pos, face));
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

}
