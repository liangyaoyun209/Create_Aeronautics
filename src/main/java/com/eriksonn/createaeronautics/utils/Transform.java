package com.eriksonn.createaeronautics.utils;

import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Encapsulates a quaternion orientation and vector position
 */
public class Transform {

    public Vector3d position;
    public Quaternionf orientation;

    public Transform(Vector3d position, Quaternionf orientation) {
        this.position = position;
        this.orientation = orientation;
    }

}
