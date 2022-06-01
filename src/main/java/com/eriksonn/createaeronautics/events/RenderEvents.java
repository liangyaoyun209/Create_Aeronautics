package com.eriksonn.createaeronautics.events;

import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.contraptions.ContraptionSmoother;
import com.eriksonn.createaeronautics.mixins.ActiveRenderInfoMixin;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.eriksonn.createaeronautics.utils.Transform;
import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RenderEvents {

    /**
     * Renders block outline for contraptions
     */
    /*public static void drawLine(VertexConsumer consumer, Matrix4f matrix, Vector3d vecA, Vector3d vecB, Vector3d normal, int r, int g, int b, int a)
    {
        consumer.vertex(matrix, (float)vecA.x, (float)vecA.y, (float)vecA.z).color(r, g, b, a).normal((float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        consumer.vertex(matrix, (float)vecB.x, (float)vecB.y, (float)vecB.z).color(r, g, b, a).normal((float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }*/

    @SubscribeEvent
    public static void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        Entity vehicle = Minecraft.getInstance().player.getVehicle();
        if(vehicle != null && vehicle instanceof AirshipContraptionEntity && !Minecraft.getInstance().gameRenderer.getMainCamera().isDetached()) {
            Entity camera = (Minecraft.getInstance().getCameraEntity() == null ? Minecraft.getInstance().player : Minecraft.getInstance().getCameraEntity());
//            Vector3d position = camera.position().add(new Vector3d(0.0, camera.getEyeHeight(), 0.0));
            AirshipContraptionEntity airship = (AirshipContraptionEntity) vehicle;
            Vector3d position = airship.getPassengerPosition(Minecraft.getInstance().player, 0.0f).add(new Vector3d(0.0, camera.getEyeHeight(), 0.0));
            ActiveRenderInfoMixin infoMixin = ((ActiveRenderInfoMixin) event.getInfo());
            infoMixin.invokeSetPosition(position.x, position.y, position.z);

            Vector3f forwards = infoMixin.getForwards();
            Vector3d rotatedForwards = MathUtils.rotateQuat(new Vector3d(forwards), airship.smoothedRenderTransform.orientation);
            forwards.set((float) rotatedForwards.x, (float) rotatedForwards.y, (float) rotatedForwards.z);

            Vector3f up = infoMixin.getUp();
            Vector3d rotatedUp = MathUtils.rotateQuat(new Vector3d(up), airship.smoothedRenderTransform.orientation);
            up.set((float) rotatedUp.x, (float) rotatedUp.y, (float) rotatedUp.z);

            Vector3f left = infoMixin.getLeft();
            Vector3d rotatedLeft = MathUtils.rotateQuat(new Vector3d(left), airship.smoothedRenderTransform.orientation);
            left.set((float) rotatedLeft.x, (float) rotatedLeft.y, (float) rotatedLeft.z);
        }
    }

    @SubscribeEvent
    public static void renderStartEvent(TickEvent.RenderTickEvent e)
    {
        float partialTicks = AnimationTickHolder.getPartialTicks();

        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;

        for (Map.Entry<Integer, AirshipContraptionEntity> entry : AirshipManager.INSTANCE.AllClientAirships.entrySet()) {
            AirshipContraptionEntity airship = entry.getValue();

            // if we're at the very start of a tick, no need for interpolation
            if(partialTicks == 0.0) airship.smoothedRenderTransform = airship.previousRenderTransform;

            // same for the end of the tick
            if(partialTicks == 1.0) airship.previousRenderTransform = airship.smoothedRenderTransform;


            Quaternionf smoothieRotation = ContraptionSmoother.slerp(airship.previousRenderTransform.orientation.copy(), airship.renderTransform.orientation, partialTicks);
            Vector3d smoothiePos = ContraptionSmoother.lerp(airship.previousRenderTransform.position, airship.renderTransform.position, partialTicks);
            smoothieRotation.normalize();

            airship.smoothedRenderTransform = new Transform(smoothiePos, smoothieRotation);
            // entry.getValue().smoothedRenderTransform
        }

        BlockPos pos = mc.player.blockPosition();

        if (!mc.player.isOnGround())
            return;
        if (mc.isPaused())
            return;

        List<AirshipContraptionEntity> possibleContraptions = mc.level.getEntitiesOfClass(AirshipContraptionEntity.class, mc.player.getBoundingBox().inflate(10.0));

        for (AirshipContraptionEntity contraption : possibleContraptions) {
            if(contraption.collidingEntities.containsKey(mc.player)) {
                float speed = (float) (contraption.simulatedRigidbody.rotate(contraption.simulatedRigidbody.getAngularVelocity()).y * (180.0 / Math.PI));
                mc.player.yRot = mc.player.yRotO + speed * partialTicks * 0.05f;
                mc.player.yBodyRot = mc.player.yRot;
            }
        }


    }
    @SubscribeEvent
    public static void renderList(RenderWorldLastEvent event) {


        Minecraft mc = Minecraft.getInstance();
        mc.getProfiler().push("renderVehicleDebug");

        ClientPlayerEntity player = mc.player;
        if(player == null) return;

        Vector3d playerEye = RaycastHelper.getTraceOrigin(mc.player);

        Entity vehicle = Minecraft.getInstance().player.getVehicle();
        if(vehicle != null && vehicle instanceof AirshipContraptionEntity && !Minecraft.getInstance().gameRenderer.getMainCamera().isDetached()) {
//            playerEye = ((AirshipContraptionEntity) vehicle).getPassengerPosition(mc.player, 1f).add(0, mc.player.getEyeHeight(), 0);
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

        for (AirshipContraptionEntity contraptionEntity : mc.level
                .getEntitiesOfClass(AirshipContraptionEntity.class, new AxisAlignedBB(playerEye, target).inflate(15.0))) {

            Vector3d localOrigin = contraptionEntity.toLocalVector(playerEye, 1);
            Vector3d localTarget = contraptionEntity.toLocalVector(target, 1);
            Contraption contraption = contraptionEntity.getContraption();

            MutableObject<BlockRayTraceResult> mutableResult = new MutableObject<>();

            AtomicReference<VoxelShape> voxelShape = new AtomicReference();
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
                    voxelShape.set(raytraceShape);
                    return true;
                }
                return false;
            });

            if (predicateResult == null || predicateResult.missed())
                continue;

            BlockRayTraceResult rayTraceResult = mutableResult.getValue();
            BlockPos blockPos = rayTraceResult.getBlockPos();

             renderBlockOutline(contraptionEntity, event.getMatrixStack(), voxelShape.get(), blockPos, event.getPartialTicks());
        }
        mc.getProfiler().pop();

    }

    public static void renderBlockOutline(AirshipContraptionEntity airship, MatrixStack stack, VoxelShape shape, BlockPos blockPos, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        stack.pushPose();

        Vector3d playerPos = mc.gameRenderer.getMainCamera().getPosition();
        stack.translate(-playerPos.x, -playerPos.y, -playerPos.z);

        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
        IVertexBuilder builder = buffer.getBuffer(RenderType.LINES);
        stack.translate(0.5, 0.5, 0.5);
        Vector3d airshipPos = airship.smoothedRenderTransform.position;
        stack.translate(airshipPos.x, airshipPos.y, airshipPos.z);
        Quaternionf orientation = airship.smoothedRenderTransform.orientation.copy();
        orientation.conj();
        stack.mulPose(orientation.toMojangQuaternion());

        stack.translate(-airship.centerOfMassOffset.x, -airship.centerOfMassOffset.y, -airship.centerOfMassOffset.z);
        stack.translate(-0.5, -0.5, -0.5);
        Matrix4f matrix = stack.last().pose();

        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {

            // The points of the triangle
            Vector3d vecA = new Vector3d(minX, minY, minZ).add(new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

//            vecA = vecA.subtract(0.5, 0.5, 0.5).subtract(airship.centerOfMassOffset);
//            vecA = MathUtils.rotateQuat(vecA, airship.smoothedRenderTransform.orientation);
//            vecA  = vecA.add(0.5, 0.5, 0.5).add(airship.smoothedRenderTransform.position);

            Vector3d vecB = new Vector3d(maxX, maxY, maxZ).add(new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

//            vecB = vecB.subtract(0.5, 0.5, 0.5).subtract(airship.centerOfMassOffset);
//            vecB = MathUtils.rotateQuat(vecB, airship.smoothedRenderTransform.orientation);
//            vecB  = vecB.add(0.5, 0.5, 0.5).add(airship.smoothedRenderTransform.position);

            int r = 0, g = 0, b = 0, a = (int) (0.4 * 255.0);

            double xDiff = (maxX - minX);
            double yDiff = (maxY - minY);
            double zDiff = (maxZ - minZ);

            double magnitude = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
            xDiff /= magnitude;
            yDiff /= magnitude;
            zDiff /= magnitude;

            Matrix3f lastNormal = stack.last().normal();
            builder.vertex(matrix, (float)vecA.x, (float)vecA.y, (float)vecA.z).color(r, g, b, a).normal(lastNormal, (float) xDiff, (float) yDiff, (float) zDiff).endVertex();
            builder.vertex(matrix, (float)vecB.x, (float)vecB.y, (float)vecB.z).color(r, g, b, a).normal(lastNormal, (float) xDiff, (float) yDiff, (float) zDiff).endVertex();
        });

        buffer.endBatch();
        stack.popPose();
    }

}
