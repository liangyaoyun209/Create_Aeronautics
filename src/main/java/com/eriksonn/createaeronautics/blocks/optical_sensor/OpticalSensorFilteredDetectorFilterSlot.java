package com.eriksonn.createaeronautics.blocks.optical_sensor;

import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.logistics.block.redstone.FilteredDetectorFilterSlot;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;

public class OpticalSensorFilteredDetectorFilterSlot extends FilteredDetectorFilterSlot {
    Vector3d position = VecHelper.voxelSpace(8f, 5.5f, 0f);

    @Override
    protected Vector3d getLocalOffset(BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);

        if (facing == Direction.DOWN) {
            return VecHelper.voxelSpace(8f, 5.5f, 0f);
        } else if (facing == Direction.UP) {
            return VecHelper.voxelSpace(8f, 16 - 5.5f, 16f);
        } else {
            return super.getLocalOffset(state);
        }
    }

    @Override
    protected void rotate(BlockState state, MatrixStack ms) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        float yRot = AngleHelper.horizontalAngle(facing) + 180;

        if (facing == Direction.DOWN || facing == Direction.UP) {
//            MatrixTransformStack.of(ms)
//                    .rotateY(yRot);
        } else {
            MatrixTransformStack.of(ms)
                    .rotateY(yRot)
                    .rotateX(90);
        }
    }

}
