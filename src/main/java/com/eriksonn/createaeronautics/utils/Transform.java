package com.eriksonn.createaeronautics.utils;

import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Encapsulates a quaternion orientation and vector position
 */
public class Transform {

    public Vector3d position;
    public Quaternion orientation;

    public Transform(Vector3d position, Quaternion orientation) {
        this.position = position;
        this.orientation = orientation;
    }

}
