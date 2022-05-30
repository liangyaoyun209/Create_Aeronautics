package com.eriksonn.createaeronautics.utils;

import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class MathUtils {
    /**
     * Rotates a vector by a quaternion
     *
     * @param V The vector to be rotated
     * @param Q The quaternion to rotate by
     * @return The rotated vector
     */
    public static Vector3d rotateQuat(Vector3d V, Quaternionf Q)
    {
        Quaternionf q=new Quaternionf((float)V.x,(float)V.y,(float)V.z,0.0f);
        Quaternionf Q2 = Q.copy();
        q.mul(Q2);
        Q2.conj();
        Q2.mul(q);
        return new Vector3d(Q2.i(),Q2.j(),Q2.k());
    }
    /**
     * Rotates a vector by the inverse of a quaternion
     *
     * @param V The vector to be rotated
     * @param Q The quaternion to rotate by
     * @return The rotated vector
     */
    public static Vector3d rotateQuatReverse(Vector3d V, Quaternionf Q)
    {
        Quaternionf q=new Quaternionf((float)V.x,(float)V.y,(float)V.z,0.0f);
        Quaternionf Q2 = Q.copy();
        Q2.conj();
        q.mul(Q2);
        Q2.conj();
        Q2.mul(q);
        return new Vector3d(Q2.i(),Q2.j(),Q2.k());
    }

    /**
     * Clamps a normalized vector inside a cone, giving a maximum angle between the returned vector
     * and the axis vector of the cone
     *
     * @param v Vector to be clamped
     * @param coneAxis Central axis of the cone
     * @param coneAngle Maximum angle in radians between the axis vector and the output vector
     * @return Clamped vector
     */
    public static Vector3d clampIntoCone(Vector3d v,Vector3d coneAxis,double coneAngle)
    {
        double vv = v.dot(v);
        double vn = v.dot(coneAxis);
        double nn = coneAxis.dot(coneAxis);
        //the 1.005 is to prevent floating point issues when v=axis,
        //and also have it behave smoother when v is almost the opposite of axis
        double disc = nn*vv*1.005 - vn*vn;
        //quadratic formula
        double offsetDistance = (-vn + Math.sqrt(disc)/Math.tan(coneAngle))/nn;
        if(offsetDistance<0)
            return v;

        return (v.add(coneAxis.scale(offsetDistance))).normalize();
    }

    /**
     * Constructs a quaternion that will rotate a given start vector to an end vector along the shortest possible path
     * @param start The start vector the rotation begins at
     * @param end The end vector the rotation should end at
     * @return A rotation quaternion from vector start to end
     */
    public static Quaternionf getQuaternionFromVectorRotation(Vector3d start, Vector3d end)
    {
        Vector3f cross = new Vector3f(start.cross(end));
        Quaternionf Q = new Quaternionf(cross.x(),cross.y(),cross.z(),1.0f+(float)start.dot(end));
        Q.normalize();
        return Q;
    }

    /**
     * Tests if a vector is inside a cylinder
     *
     * @param axisVector Central axis of the cylinder, must be normalized
     * @param relativePosition Vector to be tested, relative to the base of the cylinder
     * @param cylinderLength Length of the cylinder
     * @param cylinderRadius Radius of the cylinder
     * @return If the check passed
     */
    public static boolean isInCylinder(Vector3d axisVector, Vector3d relativePosition, double cylinderLength, double cylinderRadius) {
        double distance = axisVector.dot(relativePosition);
        if (distance < 0 || distance > cylinderLength)
            return false;

        relativePosition = relativePosition.subtract(axisVector.scale(distance));
        return relativePosition.lengthSqr() <= cylinderRadius * cylinderRadius;
    }
}
