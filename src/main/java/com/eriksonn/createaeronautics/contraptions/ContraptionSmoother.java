package com.eriksonn.createaeronautics.contraptions;

import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import net.minecraft.util.math.vector.Vector3d;

import java.math.BigDecimal;

/**
 * Uses an EMA/Moving Average Filter in order to smooth out movement of contraptions on the client, to avoid jitters.
 */
public class ContraptionSmoother {

    /**
     * The airship to smooth
     */
    public AirshipContraptionEntity contraption;

    /**
     * The position of the airship on the current tick
     */
    public Vector3d latestPosition;

    /**
     * The latest orientation of the airship on the current tick
     */
    public Quaternionf latestOrientation;

    /**
     * The current smoothed orientation
     */
    public Quaternionf smoothedOrientation;

    /**
     * The current smoothed position
     */
    public Vector3d smoothedPosition;

    /**
     * Alpha, or how accurate the moving average should be to the reported position
     */
    public double alpha = 0.5;

    /**
     * Constructs a new contraption smoother
     */
    public ContraptionSmoother(AirshipContraptionEntity contraption, double alpha) {
        this.contraption = contraption;
        this.alpha = alpha;
        this.smoothedPosition = contraption.position();
        this.smoothedOrientation = contraption.quat;
    }

    /**
     * Ticks this smoother
     */
    public void tick() {
        // Get the latest position and orientation
        latestPosition = contraption.position();
        latestOrientation = contraption.quat.copy();
        latestOrientation.normalize();

        // Compute the new smoothed position
        this.smoothedPosition = lerp(smoothedPosition, latestPosition, alpha);
        this.smoothedOrientation = slerp(smoothedOrientation, latestOrientation, alpha);
        this.smoothedOrientation.normalize();
    }

    public static Vector3d lerp(Vector3d smoothedPosition, Vector3d latestPosition, double alpha) {
        return new Vector3d(
                fma(latestPosition.x - smoothedPosition.x, alpha, smoothedPosition.x),
                fma(latestPosition.y - smoothedPosition.y, alpha, smoothedPosition.y),
                fma(latestPosition.z - smoothedPosition.z, alpha, smoothedPosition.z)
        );
    }

    public static double fma(double a, double b, double c) {
        /*
         * Infinity and NaN arithmetic is not quite the same with two
         * roundings as opposed to just one so the simple expression
         * "a * b + c" cannot always be used to compute the correct
         * result.  With two roundings, the product can overflow and
         * if the addend is infinite, a spurious NaN can be produced
         * if the infinity from the overflow and the infinite addend
         * have opposite signs.
         */

        // First, screen for and handle non-finite input values whose
        // arithmetic is not supported by BigDecimal.
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)) {
            return Double.NaN;
        } else { // All inputs non-NaN
            boolean infiniteA = Double.isInfinite(a);
            boolean infiniteB = Double.isInfinite(b);
            boolean infiniteC = Double.isInfinite(c);
            double result;

            if (infiniteA || infiniteB || infiniteC) {
                if (infiniteA && b == 0.0 ||
                        infiniteB && a == 0.0 ) {
                    return Double.NaN;
                }
                // Store product in a double field to cause an
                // overflow even if non-strictfp evaluation is being
                // used.
                double product = a * b;
                if (Double.isInfinite(product) && !infiniteA && !infiniteB) {
                    // Intermediate overflow; might cause a
                    // spurious NaN if added to infinite c.
                    assert Double.isInfinite(c);
                    return c;
                } else {
                    result = product + c;
                    assert !Double.isFinite(result);
                    return result;
                }
            } else { // All inputs finite
                BigDecimal product = (new BigDecimal(a)).multiply(new BigDecimal(b));
                if (c == 0.0) { // Positive or negative zero
                    // If the product is an exact zero, use a
                    // floating-point expression to compute the sign
                    // of the zero final result. The product is an
                    // exact zero if and only if at least one of a and
                    // b is zero.
                    if (a == 0.0 || b == 0.0) {
                        return a * b + c;
                    } else {
                        // The sign of a zero addend doesn't matter if
                        // the product is nonzero. The sign of a zero
                        // addend is not factored in the result if the
                        // exact product is nonzero but underflows to
                        // zero; see IEEE-754 2008 section 6.3 "The
                        // sign bit".
                        return product.doubleValue();
                    }
                } else {
                    return product.add(new BigDecimal(c)).doubleValue();
                }
            }
        }
    }


    public static float fma(float a, float b, float c) {
        if (Float.isFinite(a) && Float.isFinite(b) && Float.isFinite(c)) {
            if (a == 0.0 || b == 0.0) {
                return a * b + c; // Handled signed zero cases
            } else {
                return (new BigDecimal((double)a * (double)b) // Exact multiply
                        .add(new BigDecimal((double)c)))      // Exact sum
                        .floatValue();                            // One rounding
                // to a float value
            }
        } else {
            // At least one of a,b, and c is non-finite. The result
            // will be non-finite as well and will be the same
            // non-finite value under double as float arithmetic.
            return (float)fma((double)a, (double)b, (double)c);
        }
    }

    public static Quaternionf slerp(Quaternionf current, Quaternionf target, double alpha) {
        double cosom = fma(current.i(), target.i(), fma(current.j(), target.j(), fma(current.k(), target.k(), current.r() * target.r())));
        double absCosom = Math.abs(cosom);
        double scale0, scale1;
        if (1.0 - absCosom > 1E-6) {
            double sinSqr = 1.0 - absCosom * absCosom;
            double sinom = 1.0 / Math.sqrt(sinSqr);
            double omega = Math.atan2(sinSqr * sinom, absCosom);
            scale0 = Math.sin((1.0 - alpha) * omega) * sinom;
            scale1 = Math.sin(alpha * omega) * sinom;
        } else {
            scale0 = 1.0 - alpha;
            scale1 = alpha;
        }
        scale1 = cosom >= 0.0 ? scale1 : -scale1;
        double x = fma(scale0, current.i(), scale1 * target.i());
        double y = fma(scale0, current.j(), scale1 * target.j());
        double z = fma(scale0, current.k(), scale1 * target.k());
        double w = fma(scale0, current.r(), scale1 * target.r());
        return new Quaternionf((float) x, (float) y, (float) z, (float) w);
    }


}
