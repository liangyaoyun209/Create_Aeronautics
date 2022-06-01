package com.eriksonn.createaeronautics.utils.math;


import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

/**
 * this class has to exist because MOJANG DECIDED TO MAKE QUATERNION MATH CLIENT ONLY
 */
public final class Quaternionf {
    public static final Quaternionf ONE = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
    private float i;
    private float j;
    private float k;
    private float r;

    public Quaternionf(float pI, float pJ, float pK, float pR) {
        this.i = pI;
        this.j = pJ;
        this.k = pK;
        this.r = pR;
    }

    public Quaternionf(Vector3f pVector, float pAngle, boolean pDegrees) {
        if (pDegrees) {
            pAngle *= ((float)Math.PI / 180F);
        }

        float f = sin(pAngle / 2.0F);
        this.i = pVector.x() * f;
        this.j = pVector.y() * f;
        this.k = pVector.z() * f;
        this.r = cos(pAngle / 2.0F);
    }

    public Quaternionf(float p_i48102_1_, float p_i48102_2_, float p_i48102_3_, boolean p_i48102_4_) {
        if (p_i48102_4_) {
            p_i48102_1_ *= ((float)Math.PI / 180F);
            p_i48102_2_ *= ((float)Math.PI / 180F);
            p_i48102_3_ *= ((float)Math.PI / 180F);
        }

        float f = sin(0.5F * p_i48102_1_);
        float f1 = cos(0.5F * p_i48102_1_);
        float f2 = sin(0.5F * p_i48102_2_);
        float f3 = cos(0.5F * p_i48102_2_);
        float f4 = sin(0.5F * p_i48102_3_);
        float f5 = cos(0.5F * p_i48102_3_);
        this.i = f * f3 * f5 + f1 * f2 * f4;
        this.j = f1 * f2 * f5 - f * f3 * f4;
        this.k = f * f2 * f5 + f1 * f3 * f4;
        this.r = f1 * f3 * f5 - f * f2 * f4;
    }

    public Quaternionf(Quaternionf pOther) {
        this.i = pOther.i;
        this.j = pOther.j;
        this.k = pOther.k;
        this.r = pOther.r;
    }

    public Quaternionf(Quaternion tiltQuat) {
        this.i = tiltQuat.i();
        this.j = tiltQuat.j();
        this.k = tiltQuat.k();
        this.r = tiltQuat.r();
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
           Quaternionf quaternion = (Quaternionf)p_equals_1_;
            if (Float.compare(quaternion.i, this.i) != 0) {
                return false;
            } else if (Float.compare(quaternion.j, this.j) != 0) {
                return false;
            } else if (Float.compare(quaternion.k, this.k) != 0) {
                return false;
            } else {
                return Float.compare(quaternion.r, this.r) == 0;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int i = Float.floatToIntBits(this.i);
        i = 31 * i + Float.floatToIntBits(this.j);
        i = 31 * i + Float.floatToIntBits(this.k);
        return 31 * i + Float.floatToIntBits(this.r);
    }

    public String toString() {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append("Quaternion[").append(this.r()).append(" + ");
        stringbuilder.append(this.i()).append("i + ");
        stringbuilder.append(this.j()).append("j + ");
        stringbuilder.append(this.k()).append("k]");
        return stringbuilder.toString();
    }

    public float i() {
        return this.i;
    }

    public float j() {
        return this.j;
    }

    public float k() {
        return this.k;
    }

    public float r() {
        return this.r;
    }

    public void mul(Quaternionf pOther) {
        float f = this.i();
        float f1 = this.j();
        float f2 = this.k();
        float f3 = this.r();
        float f4 = pOther.i();
        float f5 = pOther.j();
        float f6 = pOther.k();
        float f7 = pOther.r();
        this.i = f3 * f4 + f * f7 + f1 * f6 - f2 * f5;
        this.j = f3 * f5 - f * f6 + f1 * f7 + f2 * f4;
        this.k = f3 * f6 + f * f5 - f1 * f4 + f2 * f7;
        this.r = f3 * f7 - f * f4 - f1 * f5 - f2 * f6;
    }

    public void mul(float pMultiplier) {
        this.i *= pMultiplier;
        this.j *= pMultiplier;
        this.k *= pMultiplier;
        this.r *= pMultiplier;
    }

    public void conj() {
        this.i = -this.i;
        this.j = -this.j;
        this.k = -this.k;
    }

    public void set(float pI, float pJ, float pK, float pR) {
        this.i = pI;
        this.j = pJ;
        this.k = pK;
        this.r = pR;
    }

    private static float cos(float pAngle) {
        return (float)Math.cos((double)pAngle);
    }

    private static float sin(float pAngle) {
        return (float)Math.sin((double)pAngle);
    }

    public static float fastInvSqrt(float pNumber) {
        float f = 0.5F * pNumber;
        int i = Float.floatToIntBits(pNumber);
        i = 1597463007 - (i >> 1);
        pNumber = Float.intBitsToFloat(i);
        return pNumber * (1.5F - f * pNumber * pNumber);
    }

    public void normalize() {
        float f = this.i() * this.i() + this.j() * this.j() + this.k() * this.k() + this.r() * this.r();
        if (f > 1.0E-6F) {
            float f1 = fastInvSqrt(f);
            this.i *= f1;
            this.j *= f1;
            this.k *= f1;
            this.r *= f1;
        } else {
            this.i = 0.0F;
            this.j = 0.0F;
            this.k = 0.0F;
            this.r = 0.0F;
        }

    }
    
    public Quaternionf copy() {
        return new Quaternionf(this);
    }

    public net.minecraft.util.math.vector.Quaternion toMojangQuaternion() {
        return new net.minecraft.util.math.vector.Quaternion(this.i, this.j, this.k, this.r);
    }
}