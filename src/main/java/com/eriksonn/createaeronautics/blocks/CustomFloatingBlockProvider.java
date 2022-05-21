package com.eriksonn.createaeronautics.blocks;

import com.eriksonn.createaeronautics.physics.api.IFloatingBlockProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class CustomFloatingBlockProvider implements IFloatingBlockProvider {

    final double strength;
    final boolean altitudeLocking;
    final double verticalFriciton;
    public CustomFloatingBlockProvider(double strength,double verticalFriciton,boolean altitudeLocking)
    {
        this.strength=strength;
        this.verticalFriciton=verticalFriciton;
        this.altitudeLocking=altitudeLocking;
    }

    @Override
    public double getRelativeStrength() {
        return strength;
    }

    @Override
    public double getVerticalFriction() {
        return verticalFriciton;
    }

    @Override
    public boolean isAltitudeLocking() {
        return altitudeLocking;
    }

    @Override
    public void tickOnForceApplication(World pLevel, BlockPos pPos, Vector3d gravityForce, Vector3d frictionForce) {

    }
}
