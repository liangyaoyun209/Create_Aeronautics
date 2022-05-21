package com.eriksonn.createaeronautics.physics.api;

import com.eriksonn.createaeronautics.blocks.propeller_bearing.PropellerBearingTileEntity;
import com.eriksonn.createaeronautics.physics.AbstractContraptionRigidbody;
import com.eriksonn.createaeronautics.physics.SimulatedContraptionRigidbody;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Phyiscs-related interface for BlockEntities that provide force.
 */
public interface IThrustProvider {

    /**
     * Gets the current force the tile entity is producing
     *
     * @param localPos Position of tile entity inside airship world
     * @param airPressure Local air pressure
     * @param velocity Rigidbody velocity of tile entity relative to ground
     * @param rigidbody The rigidbody this tile entity is part of
     * @return The force to apply to the rigidbody contraption
     */
    Vector3d getForce(BlockPos localPos, double airPressure, Vector3d velocity, AbstractContraptionRigidbody rigidbody);
}
