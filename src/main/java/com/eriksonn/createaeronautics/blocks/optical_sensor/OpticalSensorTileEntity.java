package com.eriksonn.createaeronautics.blocks.optical_sensor;

import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.physics.collision.detection.GJKEPA;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.List;

public class OpticalSensorTileEntity extends SmartTileEntity {
    private FilteringBehaviour filtering;

    public OpticalSensorTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        filtering = new FilteringBehaviour(this, new OpticalSensorFilteredDetectorFilterSlot()).moveText(new Vector3d(0, 5, 0));
        behaviours.add(filtering);

    }


    float beamLength = 10;
    float lastDetectionLength = 0;
    float detectionLength = 0;

    @Override
    public void tick() {
        super.tick();


        AirshipManager.AirshipOrientedInfo info = AirshipManager.INSTANCE.getInfo(level, worldPosition);

        Vector3d pos = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        Vector3d direction = Vector3d.atLowerCornerOf(getBlockState().getValue(OpticalSensorBlock.FACING).getNormal());

        OpticalSensorRayCastInfo resultingInfo = ray(level, pos, direction);

        boolean powered = resultingInfo.powered;

        if (info.onAirship) {
            pos = info.position;
            direction = GJKEPA.rotateQuaternion(direction, info.orientation);

            OpticalSensorRayCastInfo resultingInfo2 = ray(info.level, pos, direction);

//            if(powered) {
                detectionLength = Math.min(resultingInfo.length, resultingInfo2.length);
//            } else {
//                detectionLength = resultingInfo.length;
//            }
            powered = resultingInfo.powered || resultingInfo2.powered;
        } else {
            detectionLength = resultingInfo.length;
        }

        if(lastDetectionLength != detectionLength || getBlockState().getBlockState().getValue(OpticalSensorBlock.POWERED) != powered) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(OpticalSensorBlock.POWERED, powered));
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        lastDetectionLength = detectionLength;
    }

    private OpticalSensorRayCastInfo ray(World levelIn, Vector3d pos, Vector3d direction) {
        BlockRayTraceResult clip = levelIn.clip(new RayTraceContext(
                pos.add(direction),
                pos.add(direction.scale(beamLength)),
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.ANY,
                null
        ));

        boolean powered = false;

        float newDetectionLength = 0.0f;
            newDetectionLength = beamLength;
        if (clip.getType() == RayTraceResult.Type.BLOCK) {

            BlockPos hitPos = clip.getBlockPos();
            Vector3d diff = new Vector3d(hitPos.getX(), hitPos.getY(), hitPos.getZ()).subtract(pos);
            float dist = (float) diff.length();

            BlockState hitState = levelIn.getBlockState(hitPos);
            if (dist < beamLength) {
                if(filtering.getFilter().isEmpty()) {
                    powered = true;
                    newDetectionLength = dist;
                } else {
                    if(filtering.getFilter().getItem().equals(hitState.getBlock().asItem())) {
                        powered = true;
                        newDetectionLength = dist;
                    } else {
                        newDetectionLength = dist;
                    }
                }
            }
        }

        OpticalSensorRayCastInfo resultingInfo = new OpticalSensorRayCastInfo(powered, newDetectionLength);
        return resultingInfo;
    }

    class OpticalSensorRayCastInfo {
        boolean powered;
        float length;

        public OpticalSensorRayCastInfo(boolean powered, float length) {
            this.powered = powered;
            this.length = length;
        }
    }
}
