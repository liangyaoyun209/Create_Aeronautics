package com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing;

import com.eriksonn.createaeronautics.index.CAEntityTypes;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.IControlContraption;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class GyroscopicControlledContraptionEntity extends ControlledContraptionEntity{
    Quaternionf tiltQuat= Quaternionf.ONE;
    Direction direction=Direction.UP;
    public GyroscopicControlledContraptionEntity(EntityType<?> type, World world) {
        super(type, world);
    }
    public static ControlledContraptionEntity create(World world, IControlContraption controller,
                                                     Contraption contraption) {
        GyroscopicControlledContraptionEntity entity =
                new GyroscopicControlledContraptionEntity(CAEntityTypes.GYROSCOPIC_CONTROLLED_CONTRAPTION.get(), world);
        entity.setControllerPos(controller.getBlockPosition());
        entity.setContraption(contraption);
        return entity;
    }

    @Override
    public Vector3d applyRotation(Vector3d localPos, float partialTicks) {
        localPos = VecHelper.rotate(localPos, getAngle(partialTicks), rotationAxis);
        localPos = MathUtils.rotateQuatReverse(localPos,tiltQuat);
        return localPos;
    }

    @Override
    public Vector3d reverseRotation(Vector3d localPos, float partialTicks) {
        localPos = MathUtils.rotateQuat(localPos,tiltQuat);
        localPos = VecHelper.rotate(localPos, -getAngle(partialTicks), rotationAxis);
        return localPos;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void doLocalTransforms(float partialTicks, MatrixStack[] matrixStacks) {
        float angle = getAngle(partialTicks);
        Direction.Axis axis = getRotationAxis();
        Vector3d normal = new Vector3d(direction.getStepX(),direction.getStepY(),direction.getStepZ());
        normal=normal.scale(12/16.0);
        for (MatrixStack stack : matrixStacks)
            MatrixTransformStack.of(stack)
                    .nudge(getId())
                    .centre()
                    .translate(normal.scale(-1))
                    .multiply(tiltQuat.toMojangQuaternion())
                    .translate(normal)
                    .rotate(angle, axis)
                    .unCentre();
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos=controllerPos;
    }
    public void setContraption(Contraption contraption) {
        super.setContraption(contraption);
    }
}
