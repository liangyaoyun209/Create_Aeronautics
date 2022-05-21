package com.eriksonn.createaeronautics.physics.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public interface IFloatingBlockProvider {
    double getRelativeStrength();
    double getVerticalFriction();
    boolean isAltitudeLocking();
    void tickOnForceApplication(World pLevel, BlockPos pPos, Vector3d relativeGravityForce, Vector3d frictionForce);
}
